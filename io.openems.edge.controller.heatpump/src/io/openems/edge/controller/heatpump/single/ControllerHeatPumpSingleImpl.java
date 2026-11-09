package io.openems.edge.controller.heatpump.single;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.energy.api.handler.RescheduleMode.OPTIMIZE_CURRENT_PERIOD;

import java.time.Instant;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.DifferentModes.Period;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.api.ManagedHeatPump;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.HeatPump.Single", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences({ "heatPump" })
public class ControllerHeatPumpSingleImpl extends AbstractOpenemsComponent
		implements Controller, ControllerHeatPumpSingle, OpenemsComponent, EnergySchedulable {

	private final AntiCycling antiCycling = new AntiCycling();

	@Reference
	private ComponentManager componentManager;

	@Reference(target = "(&(id=${config.heatPump_id})(enabled=true))")
	private ManagedHeatPump heatPump;

	private Config config;
	private EshWithDifferentModes<Mode, EnergyScheduler.Config, EnergyScheduler.ScheduleContext> energyScheduleHandler;

	public ControllerHeatPumpSingleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerHeatPumpSingle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.energyScheduleHandler = EnergyScheduler.buildEnergyScheduleHandler(this,
				this::createEnergySchedulerConfig);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.energyScheduleHandler.triggerReschedule("ControllerHeatPumpSingleImpl::modified()",
				OPTIMIZE_CURRENT_PERIOD);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Actual logic is carried out in the Controller.HeatPump.Cluster
	}

	@Override
	public Params getParams() {
		var abilities = this.heatPump.getHeatPumpAbilities();
		if (abilities == null) {
			return null;
		}
		return new Params(this.id(), this.getActualMode(), this.heatPump.getActivePower().get(), abilities);
	}

	/**
	 * Gets the actual {@link Mode}; for {@link Mode#SMART} the Mode of the
	 * current Energy Scheduler period ({@link Mode#SURPLUS} as fallback).
	 *
	 * @return the actual {@link Mode}
	 */
	protected Mode getActualMode() {
		if (this.config.mode() != Mode.SMART) {
			return this.config.mode();
		}
		return Optional.ofNullable(this.energyScheduleHandler) //
				.map(EshWithDifferentModes::getCurrentPeriod) //
				.map(Period::mode) //
				.filter(m -> m != Mode.SMART) //
				.orElse(Mode.SURPLUS);
	}

	@Override
	public void apply(Mode mode, HeatPumpAction action) {
		setValue(this, ControllerHeatPumpSingle.ChannelId.ACTUAL_MODE, mode);
		setValue(this, ControllerHeatPumpSingle.ChannelId.REQUESTED_POWER,
				action instanceof HeatPumpAction.Power p ? p.power() : null);

		var abilities = this.heatPump.getHeatPumpAbilities();
		if (abilities == null) {
			setValue(this, ControllerHeatPumpSingle.ChannelId.ANTI_CYCLING_STATE, AntiCyclingState.UNDEFINED);
			return;
		}
		var result = this.antiCycling.apply(Instant.now(this.componentManager.getClock()), action,
				this.heatPump.getOperatingState().isRunning(), abilities);
		setValue(this, ControllerHeatPumpSingle.ChannelId.ANTI_CYCLING_STATE, result.state());

		if (this.heatPump.isReadOnly()) {
			return;
		}
		this.heatPump.apply(result.action());
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	private EnergyScheduler.Config createEnergySchedulerConfig() {
		if (this.config == null || this.heatPump == null) {
			return null;
		}
		var abilities = this.heatPump.getHeatPumpAbilities();
		var maxPower = abilities != null && abilities.canApplyPower() ? abilities.applyPower().maxPower() : 0;
		var capacity = EnergyScheduler.calculateStorageCapacity(this.config.storageVolume(),
				this.config.minStorageTemperature(), this.config.maxStorageTemperature());
		var energy = EnergyScheduler.calculateStorageEnergy(this.config.storageVolume(),
				this.config.minStorageTemperature(), this.getStorageTemperature(), capacity);
		return new EnergyScheduler.Config(this.config.mode(), maxPower, capacity, energy,
				this.config.averageHeatDemand(), this.config.estimatedCop() / 10.0);
	}

	private Integer getStorageTemperature() {
		if (this.heatPump instanceof Heat heat) {
			return heat.getTemperatureChannel().value().get();
		}
		return null;
	}
}
