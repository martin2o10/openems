package io.openems.edge.controller.heatpump.monitor;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timedata.api.utils.CalculateStateCounter;

/**
 * This Controller monitors a heat pump that is not necessarily integrated as an
 * OpenEMS device, e.g. via a separate electricity meter or via a running signal
 * from the heat pump itself. It is strictly read-only and provides cycle
 * statistics (starts, run time, durations).
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.HeatPump.Monitor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class HeatPumpMonitorImpl extends AbstractOpenemsComponent
		implements HeatPumpMonitor, Controller, OpenemsComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(HeatPumpMonitorImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config = null;
	private ChannelAddress runningChannelAddress = null;
	private ChannelAddress powerChannelAddress = null;

	private CalculateActiveTime calculateRunTime = null;
	private CalculateStateCounter calculateStarts = null;

	/** Whether the heat pump is currently considered running. */
	private boolean isRunning = false;
	/** Timestamp when the raw signal first suggested running; or null. */
	private Instant rawRunningSince = null;
	/** Timestamp when the raw signal first suggested pausing; or null. */
	private Instant notRunningSince = null;
	/** Timestamp when the current run started (after the start debounce); or null. */
	private Instant startedAt = null;
	/** Timestamp when the monitored heat pump stopped running; or null. */
	private Instant stoppedAt = null;
	/** Whether the raw signal was available in the last cycle. */
	private boolean isSignalAvailable = true;

	public HeatPumpMonitorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				HeatPumpMonitor.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;

		if (!config.heatPumpRunningChannelAddress().isBlank()) {
			this.runningChannelAddress = ChannelAddress.fromString(config.heatPumpRunningChannelAddress());
		}
		if (!config.heatPumpPowerChannelAddress().isBlank()) {
			this.powerChannelAddress = ChannelAddress.fromString(config.heatPumpPowerChannelAddress());
		}

		/* Validate configuration: at least one signal must be configured. */
		if (this.runningChannelAddress == null && this.powerChannelAddress == null) {
			throw new IllegalArgumentException("Heat Pump Monitor [" + config.id()
					+ "]: configure at least a 'Running Channel' or a 'Power Channel'");
		}
		if (this.powerChannelAddress != null //
				&& config.powerStopThreshold() > config.powerStartThreshold()) {
			throw new IllegalArgumentException("Heat Pump Monitor [" + config.id()
					+ "]: 'Power Stop Threshold' must not be greater than 'Power Start Threshold'");
		}
		if (config.minimumRunTimeSeconds() < 0 || config.minimumPauseTimeSeconds() < 0) {
			throw new IllegalArgumentException("Heat Pump Monitor [" + config.id()
					+ "]: 'Minimum Run Time' and 'Minimum Pause Time' must not be negative");
		}

		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		/* Initialize the Timedata-based calculations lazily; the clock is not available before. */
		if (this.calculateRunTime == null) {
			this.calculateRunTime = new CalculateActiveTime(this, HeatPumpMonitor.ChannelId.HEAT_PUMP_RUN_TIME_TOTAL,
					this.componentManager.getClock());
			this.calculateStarts = new CalculateStateCounter(this, HeatPumpMonitor.ChannelId.HEAT_PUMP_STARTS_TOTAL);
		}

		/* Continuously keep the total run time and the starts counter up-to-date. */
		this.calculateRunTime.update(this.isRunning);
		this.calculateStarts.update(false);

		/* Evaluate the raw running signal. */
		final var rawRunning = this.getRawRunning();
		if (rawRunning == null) {
			/* Signal is unavailable: keep the current state. */
			return;
		}

		final var now = Instant.now(this.componentManager.getClock());
		if (this.isRunning) {
			if (rawRunning) {
				this.notRunningSince = null;
			} else {
				final var pauseSince = this.notRunningSince == null ? now : this.notRunningSince;
				this.notRunningSince = pauseSince;

				/* Stop: the signal is inactive for at least the 'Minimum Pause Time'. */
				if (!now.minusSeconds(this.config.minimumPauseTimeSeconds()).isBefore(pauseSince)) {
					this.isRunning = false;
					this.stoppedAt = pauseSince;
					this.channel(HeatPumpMonitor.ChannelId.HEAT_PUMP_LAST_RUN_DURATION)
							.setNextValue(Duration.between(this.startedAt, pauseSince).toSeconds());
					this.rawRunningSince = null;
				}
			}

		} else {
			if (rawRunning) {
				final var runSince = this.rawRunningSince == null ? now : this.rawRunningSince;
				this.rawRunningSince = runSince;

				/* Start: the signal is active for at least the 'Minimum Run Time'. */
				if (!now.minusSeconds(this.config.minimumRunTimeSeconds()).isBefore(runSince)) {
					this.isRunning = true;
					this.startedAt = now;
					this.calculateStarts.update(true);
					if (this.stoppedAt != null) {
						this.channel(HeatPumpMonitor.ChannelId.HEAT_PUMP_LAST_PAUSE_DURATION)
								.setNextValue(Duration.between(this.stoppedAt, runSince).toSeconds());
					}
					this.notRunningSince = null;
					this.rawRunningSince = null;
				}

			} else {
				this.rawRunningSince = null;
			}
		}

		this._setHeatPumpRunning(this.isRunning);
	}

	/**
	 * Gets the raw running signal of the heat pump.
	 *
	 * <p>
	 * If a 'Running Channel' is configured, its value is evaluated (Boolean or
	 * Integer). Otherwise the electrical power is compared against the start/stop
	 * thresholds depending on the current state (hysteresis).
	 *
	 * @return true / false; or null if the signal is currently unavailable; in
	 *         that case the caller must not change the state
	 */
	private Boolean getRawRunning() throws OpenemsNamedException {
		Boolean rawRunning;
		if (this.runningChannelAddress != null) {
			final var value = this.componentManager.getChannel(this.runningChannelAddress).value().get();
			final var asBoolean = TypeUtils.getAsType(OpenemsType.BOOLEAN, value);
			if (asBoolean != null) {
				rawRunning = asBoolean;
			} else {
				final var asInteger = TypeUtils.getAsType(OpenemsType.INTEGER, value);
				rawRunning = asInteger == null ? null : asInteger != 0;
			}

		} else {
			final var power = TypeUtils.getAsType(OpenemsType.INTEGER, //
					this.componentManager.getChannel(this.powerChannelAddress).value().get());
			if (power == null) {
				rawRunning = null;
			} else {
				rawRunning = this.isRunning /* hysteresis */
						? power > this.config.powerStopThreshold() //
						: power > this.config.powerStartThreshold();
			}
		}

		/* Log availability changes instead of logging every cycle. */
		if (rawRunning == null && this.isSignalAvailable) {
			this.isSignalAvailable = false;
			this.logWarn(this.log, "Heat pump signal is unavailable. Keeping the current state.");
		} else if (rawRunning != null && !this.isSignalAvailable) {
			this.isSignalAvailable = true;
			this.logInfo(this.log, "Heat pump signal is available again.");
		}

		return rawRunning;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
