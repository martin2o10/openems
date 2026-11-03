package io.openems.edge.simulator.heatpump;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpControlCapability;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.heat.api.RemainingHeatEnergyCalculator;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.HeatPump", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SimulatorHeatPumpImpl extends AbstractOpenemsComponent implements SimulatorHeatPump, ManagedHeatPump,
		Heat, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	private Config config;
	private HeatPumpModel model;
	private Instant lastStep;
	private Integer appliedTargetActivePower;
	private Integer appliedActivePowerLimit;

	public SimulatorHeatPumpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Heat.ChannelId.values(), //
				ManagedHeatPump.ChannelId.values(), //
				SimulatorHeatPump.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;
		this.model = new HeatPumpModel(new HeatPumpModel.Parameters(//
				config.minPower(), config.maxPower(), config.minRunTime(), config.minOffTime(),
				config.storageVolume(), config.targetTemperature() / 10.0, config.hysteresis() / 10.0,
				config.maxStorageTemperature() / 10.0, config.outdoorTemperature() / 10.0,
				config.roomTemperature() / 10.0, config.heatLossCoefficient(), config.carnotEfficiency() / 100.0),
				config.initialStorageTemperature() / 10.0);
		this.lastStep = Instant.now(this.componentManager.getClock());

		setValue(this, ManagedHeatPump.ChannelId.MIN_ACTIVE_POWER, config.minPower());
		setValue(this, ManagedHeatPump.ChannelId.MAX_ACTIVE_POWER, config.maxPower());
		setValue(this, ManagedHeatPump.ChannelId.MIN_RUN_TIME, config.minRunTime());
		setValue(this, ManagedHeatPump.ChannelId.MIN_OFF_TIME, config.minOffTime());
		setValue(this, ManagedHeatPump.ChannelId.CONTROL_CAPABILITY, HeatPumpControlCapability.CONTINUOUS);
		setValue(this, Heat.ChannelId.TARGET_TEMPERATURE, config.targetTemperature());
		this.updateChannels();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.simulate();
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE ->
			this.calculateConsumptionEnergy.update(this.getActivePower().get());
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.applySetpoints();
		}
	}

	/**
	 * Takes over the setpoints written by a Controller. Like a real device, the
	 * setpoints are only valid for the current Cycle.
	 */
	private void applySetpoints() {
		this.appliedActivePowerLimit = this.getActivePowerLimitChannel().getNextWriteValueAndReset().orElse(null);
		this.appliedTargetActivePower = this.getTargetActivePowerChannel().getNextWriteValueAndReset().orElse(null);
		setValue(this, ManagedHeatPump.ChannelId.ACTIVE_POWER_LIMIT, this.appliedActivePowerLimit);
		setValue(this, ManagedHeatPump.ChannelId.TARGET_ACTIVE_POWER, this.appliedTargetActivePower);
	}

	private void simulate() {
		var now = Instant.now(this.componentManager.getClock());
		this.model.step(Duration.between(this.lastStep, now), this.appliedTargetActivePower,
				this.appliedActivePowerLimit);
		this.lastStep = now;
		this.updateChannels();
	}

	private void updateChannels() {
		var power = this.model.getElectricPower();
		var powerPerPhase = power / 3;
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, power);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER_L1, powerPerPhase);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER_L2, powerPerPhase);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER_L3, power - 2 * powerPerPhase);
		setValue(this, ManagedHeatPump.ChannelId.THERMAL_POWER, this.model.getThermalPower());
		setValue(this, ManagedHeatPump.ChannelId.OPERATING_STATE, this.model.getOperatingState());

		var temperature = (int) Math.round(this.model.getStorageTemperature() * 10);
		setValue(this, Heat.ChannelId.TEMPERATURE, temperature);
		setValue(this, Heat.ChannelId.REMAINING_HEAT_ENERGY, RemainingHeatEnergyCalculator
				.calculate(this.config.storageVolume(), temperature, this.config.targetTemperature()));

		setValue(this, SimulatorHeatPump.ChannelId.COEFFICIENT_OF_PERFORMANCE, this.model.getCop());
		setValue(this, SimulatorHeatPump.ChannelId.STARTS, this.model.getStarts());
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("P:").append(this.getActivePower().asString()) //
				.append("|T:").append(this.getTemperatureChannel().value().asString()) //
				.append("|").append(this.getOperatingState().getName()) //
				.toString();
	}
}
