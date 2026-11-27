package io.openems.edge.heat.broetje.neo;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverters.SCALE_FACTOR_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.meter.api.ElectricityMeter.calculatePhasesFromActivePower;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = HeatBroetjeNeoImpl.FACTORY_ID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
})
@GenerateTargetsFromReferences("Modbus")
public class HeatBroetjeNeoImpl extends AbstractOpenemsModbusComponent implements HeatBroetjeNeo, ManagedHeatPump, Heat,
		ElectricityMeter, ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler {

	public static final String FACTORY_ID = "Heat.Broetje.Neo";

	/**
	 * Raw value -500 (= -50.0 °C) means "sensor not connected"; it must not be
	 * written to the channel. Temperature channels are in 0.1 °C, so the raw value
	 * is passed through unchanged otherwise.
	 */
	private static final int TEMPERATURE_SENTINEL = -500;

	/** Flags of the NEO-RKM are "active" for values greater than zero. */
	private static final ElementToChannelConverter FLAG = new ElementToChannelConverter(
			value -> value == null ? null : ((Number) value).intValue() > 0);

	/** Temperature converter that suppresses the sentinel value. */
	private static final ElementToChannelConverter TEMPERATURE = new ElementToChannelConverter(
			value -> value instanceof Number n && n.intValue() == TEMPERATURE_SENTINEL ? null : value);

	/** COP converter; register 30 is x 0.1 and the channel is DOUBLE. */
	private static final ElementToChannelConverter COP = new ElementToChannelConverter(
			value -> value instanceof Number n ? n.doubleValue() / 10 : null);

	private final Logger log = LoggerFactory.getLogger(HeatBroetjeNeoImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private volatile Config config;
	private SetpointHandler setpointHandler;
	/** True if a Controller applied an action in the current Cycle. */
	private volatile boolean actionApplied = false;

	public HeatBroetjeNeoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Heat.ChannelId.values(), //
				ManagedHeatPump.ChannelId.values(), //
				HeatBroetjeNeo.ChannelId.values() //
		);
		calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.applyConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	private void applyConfig(Config config) {
		this.config = config;
		this.setpointHandler = new SetpointHandler(config.minPower(), config.maxPower(),
				Duration.ofSeconds(config.writeIntervalSeconds()));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		// TODO Spec section 6 / open point 4: on deactivation in non-read-only
		// mode, write OwnControl (register 150 = 0) once as fallback to the
		// heat pump's own control; depends on clarified NEO-RKM behaviour on
		// communication loss
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var config = this.config;
		var protocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(10, Priority.HIGH, //
						this.statusElements(config).toArray(new ModbusElement[0])), //
				new FC4ReadInputRegistersTask(60, Priority.HIGH, //
						// each UINT32 doubleword element covers two registers
						m(HeatBroetjeNeo.ChannelId.THERMAL_ENERGY_HEATING, new UnsignedDoublewordElement(60),
								SCALE_FACTOR_3), // TODO Spec 12.1: unit Wh vs. kWh to be clarified on the device
						m(HeatBroetjeNeo.ChannelId.ELECTRIC_ENERGY_HEATING, new UnsignedDoublewordElement(62),
								SCALE_FACTOR_3), //
						m(HeatBroetjeNeo.ChannelId.THERMAL_ENERGY_DHWS, new UnsignedDoublewordElement(64),
								SCALE_FACTOR_3), //
						m(HeatBroetjeNeo.ChannelId.ELECTRIC_ENERGY_DHWS, new UnsignedDoublewordElement(66),
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(68),
								SCALE_FACTOR_3), // TODO Spec 12.1: unit Wh vs. kWh to be clarified on the device
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(70)), //
						m(HeatBroetjeNeo.ChannelId.THERMAL_ENERGY_TOTAL, new UnsignedDoublewordElement(72),
								SCALE_FACTOR_3), //
						// register 74: x 0.1 kW -> W
						// TODO probe on device: doubleword at 74 also covers register 75
						m(ManagedHeatPump.ChannelId.THERMAL_POWER, new UnsignedDoublewordElement(74), SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(100, Priority.LOW, //
						m(HeatBroetjeNeo.ChannelId.OPERATING_MODE, new UnsignedWordElement(100))));

		// Optional register groups ("Kür"), each polled only if enabled
		if (config.readOperatingHours()) {
			protocol.addTask(new FC4ReadInputRegistersTask(42, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.OPERATING_HOURS_1, new UnsignedDoublewordElement(42)), //
					m(HeatBroetjeNeo.ChannelId.OPERATING_HOURS_2, new UnsignedDoublewordElement(44))));
		}
		if (config.readHoldingRegisters()) {
			protocol.addTask(new FC3ReadRegistersTask(101, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.HOLDING_101, new UnsignedWordElement(101)), //
					m(HeatBroetjeNeo.ChannelId.HOLDING_102, new UnsignedWordElement(102)), //
					m(HeatBroetjeNeo.ChannelId.HOLDING_103, new UnsignedWordElement(103)), //
					m(HeatBroetjeNeo.ChannelId.HOLDING_104, new UnsignedWordElement(104)), //
					m(HeatBroetjeNeo.ChannelId.HOLDING_105, new UnsignedWordElement(105)), //
					m(HeatBroetjeNeo.ChannelId.HOLDING_106, new UnsignedWordElement(106))));
		}
		if (config.readExternalRequest()) {
			protocol.addTask(new FC3ReadRegistersTask(117, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.EXTERNAL_REQUEST_117, new UnsignedWordElement(117), FLAG)));
			protocol.addTask(new FC3ReadRegistersTask(127, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.EXTERNAL_REQUEST_127, new UnsignedWordElement(127), FLAG)));
		}
		if (config.readSpeedSetpoint()) {
			protocol.addTask(new FC3ReadRegistersTask(126, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.COMPRESSOR_SPEED_SETPOINT, new SignedWordElement(126))));
		}
		if (config.readExternalSensorOverrides()) {
			protocol.addTask(new FC3ReadRegistersTask(129, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.SENSOR_OVERRIDE_129, new SignedWordElement(129)), //
					m(HeatBroetjeNeo.ChannelId.SENSOR_OVERRIDE_130, new SignedWordElement(130)), //
					m(HeatBroetjeNeo.ChannelId.SENSOR_OVERRIDE_131, new SignedWordElement(131)), //
					m(HeatBroetjeNeo.ChannelId.SENSOR_OVERRIDE_132, new SignedWordElement(132)), //
					m(HeatBroetjeNeo.ChannelId.SENSOR_OVERRIDE_133, new SignedWordElement(133)), //
					m(HeatBroetjeNeo.ChannelId.SENSOR_OVERRIDE_134, new SignedWordElement(134))));
		}
		if (config.readSecondStage()) {
			protocol.addTask(new FC3ReadRegistersTask(147, Priority.LOW, //
					m(HeatBroetjeNeo.ChannelId.SECOND_STAGE_147, new UnsignedWordElement(147)), //
					m(HeatBroetjeNeo.ChannelId.SECOND_STAGE_148, new UnsignedWordElement(148))));
		}

		// Write path: only active in non-read-only mode; in read-only mode no
		// FC6 task exists, so no Modbus write access happens at all
		if (!config.readOnly()) {
			protocol.addTasks(//
					new FC6WriteRegisterTask(125, //
							m(HeatBroetjeNeo.ChannelId.POWER_SETPOINT, new UnsignedWordElement(125))), //
					new FC6WriteRegisterTask(149, //
							m(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE, new UnsignedWordElement(149))), //
					new FC6WriteRegisterTask(150, //
							m(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE, new UnsignedWordElement(150))));
		}
		return protocol;
	}

	/**
	 * Builds the elements of the main status task (registers 10-41); optional
	 * register groups are either mapped to channels or skipped as dummies.
	 *
	 * @param config the {@link Config}
	 * @return the elements in ascending register order
	 */
	private List<ModbusElement> statusElements(Config config) {
		var elements = new ArrayList<ModbusElement>();
		elements.add(m(HeatBroetjeNeo.ChannelId.OUTDOOR_TEMPERATURE, new SignedWordElement(10), TEMPERATURE));
		elements.add(
				m(HeatBroetjeNeo.ChannelId.DOMESTIC_HOT_WATER_TEMPERATURE, new SignedWordElement(11), TEMPERATURE));
		elements.add(m(HeatBroetjeNeo.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(12), TEMPERATURE));
		elements.add(m(HeatBroetjeNeo.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(13), TEMPERATURE));
		elements.add(m(Heat.ChannelId.TEMPERATURE, new SignedWordElement(14), TEMPERATURE));
		if (config.readSourceTemperatures()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.SOURCE_FLOW_TEMPERATURE, new SignedWordElement(15), TEMPERATURE));
			elements.add(m(HeatBroetjeNeo.ChannelId.SOURCE_RETURN_TEMPERATURE, new SignedWordElement(16), TEMPERATURE));
		} else {
			elements.add(new DummyRegisterElement(15, 16));
		}
		if (config.readRefrigerantTemperatures()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.SUCTION_GAS_TEMPERATURE, new SignedWordElement(17), TEMPERATURE));
			elements.add(m(HeatBroetjeNeo.ChannelId.EVAPORATION_TEMPERATURE, new SignedWordElement(18), TEMPERATURE));
			elements.add(m(HeatBroetjeNeo.ChannelId.CONDENSATION_TEMPERATURE, new SignedWordElement(19), TEMPERATURE));
			elements.add(m(HeatBroetjeNeo.ChannelId.HOT_GAS_TEMPERATURE, new SignedWordElement(20), TEMPERATURE));
		} else {
			elements.add(new DummyRegisterElement(17, 20));
		}
		if (config.readPressures()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.PRESSURE_1, new SignedWordElement(21)));
			elements.add(m(HeatBroetjeNeo.ChannelId.PRESSURE_2, new SignedWordElement(22)));
		} else {
			elements.add(new DummyRegisterElement(21, 22));
		}
		if (config.readPumpFlags()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.PUMP_1_ACTIVE, new SignedWordElement(23), FLAG));
			elements.add(m(HeatBroetjeNeo.ChannelId.PUMP_2_ACTIVE, new SignedWordElement(24), FLAG));
		} else {
			elements.add(new DummyRegisterElement(23, 24));
		}
		elements.add(m(HeatBroetjeNeo.ChannelId.COMPRESSOR_RUNNING, new SignedWordElement(25), FLAG));
		elements.add(m(HeatBroetjeNeo.ChannelId.DEVICE_FAULT, new SignedWordElement(26), FLAG));
		elements.add(m(HeatBroetjeNeo.ChannelId.DEFROST_VALVE_ACTIVE, new SignedWordElement(27), FLAG));
		if (config.readFlowRate()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.VOLUME_FLOW, new SignedWordElement(28)));
		} else {
			elements.add(new DummyRegisterElement(28));
		}
		elements.add(m(HeatBroetjeNeo.ChannelId.COMPRESSOR_SPEED, new SignedWordElement(29)));
		elements.add(m(HeatBroetjeNeo.ChannelId.COEFFICIENT_OF_PERFORMANCE, new SignedWordElement(30), COP));
		if (config.readFreshWaterTemperature()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.FRESH_WATER_TEMPERATURE, new SignedWordElement(31), TEMPERATURE));
		} else {
			elements.add(new DummyRegisterElement(31));
		}
		elements.add(m(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE, new SignedWordElement(32), FLAG));
		if (config.readDelayedOutdoorTemperature()) {
			elements.add(
					m(HeatBroetjeNeo.ChannelId.OUTDOOR_TEMPERATURE_DELAYED, new SignedWordElement(33), TEMPERATURE));
		} else {
			elements.add(new DummyRegisterElement(33));
		}
		elements.add(m(Heat.ChannelId.TARGET_TEMPERATURE, new SignedWordElement(34), TEMPERATURE));
		if (config.readMixerCircuitSetpoints()) {
			elements.add(m(HeatBroetjeNeo.ChannelId.MIXER_CIRCUIT_SETPOINT_1, new SignedWordElement(35), TEMPERATURE));
			elements.add(m(HeatBroetjeNeo.ChannelId.MIXER_CIRCUIT_SETPOINT_2, new SignedWordElement(36), TEMPERATURE));
		} else {
			elements.add(new DummyRegisterElement(35, 36));
		}
		if (config.readExpansionValve()) {
			elements.add(new DummyRegisterElement(37, 39));
			elements.add(m(HeatBroetjeNeo.ChannelId.EXPANSION_VALVE, new SignedWordElement(40)));
		} else {
			elements.add(new DummyRegisterElement(37, 40));
		}
		elements.add(m(HeatBroetjeNeo.ChannelId.DEMAND, new UnsignedWordElement(41)));
		return elements;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.actionApplied = false;
			this.updateOperatingState();
		}
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE -> this.applyFallback();
		}
	}

	private void updateOperatingState() {
		if (this.getModbusCommunicationFailedChannel().value().orElse(false)) {
			// Modbus communication lost: channels are invalidated by the read
			// tasks; report UNDEFINED explicitly
			setValue(this, ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.UNDEFINED);
			return;
		}
		setValue(this, ManagedHeatPump.ChannelId.OPERATING_STATE, OperatingStateMapper.map(//
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.DEVICE_FAULT), //
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE), //
				this.channel(HeatBroetjeNeo.ChannelId.OPERATING_MODE).value().asEnum(), //
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.COMPRESSOR_RUNNING), //
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.DEFROST_VALVE_ACTIVE), //
				this.channel(HeatBroetjeNeo.ChannelId.DEMAND).value().asEnum()));
	}

	/**
	 * If no Controller applied an action in this Cycle, fall back to the heat
	 * pump's own control (OwnControl).
	 */
	private void applyFallback() {
		if (this.config == null || this.config.readOnly() || this.actionApplied) {
			return;
		}
		var result = this.setpointHandler.apply(Instant.now(this.componentManager.getClock()), null);
		if (result != null) {
			this.writeResult(result);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T channelValue(HeatBroetjeNeo.ChannelId channelId) {
		return (T) this.channel(channelId).value().get();
	}

	@Override
	public HeatPumpAbilities getHeatPumpAbilities() {
		var config = this.config;
		if (config == null || this.getModbusCommunicationFailedChannel().value().orElse(false)) {
			return null;
		}
		ApplyPower applyPower = config.readOnly() //
				? null //
				: new ApplyPower.Continuous(config.minPower(), config.maxPower());
		return new HeatPumpAbilities(applyPower, config.minRunTimeSeconds(), config.minOffTimeSeconds());
	}

	@Override
	public void apply(HeatPumpAction action) {
		this._setAction(action);
		this.actionApplied = true;
		var config = this.config;
		if (config == null || config.readOnly()) {
			// read-only mode: never write to the device
			return;
		}
		var result = this.setpointHandler.apply(Instant.now(this.componentManager.getClock()), action);
		if (result != null) {
			this.writeResult(result);
		}
	}

	private void writeResult(SetpointHandler.Result result) {
		try {
			if (result.powerSetpoint() != null) {
				this.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT)
						.setNextWriteValue(result.powerSetpoint());
			}
			if (result.gridOperatorReleaseValue() != null) {
				this.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE)
						.setNextWriteValue(result.gridOperatorReleaseValue());
			}
			if (result.gridOperatorReleaseActivate() != null) {
				this.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE)
						.setNextWriteValue(result.gridOperatorReleaseActivate());
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to write setpoints: " + e.getMessage());
		}
	}

	@Override
	public boolean isReadOnly() {
		return this.config == null || this.config.readOnly();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("P:").append(this.getActivePower().asString()) //
				.append("|T:").append(this.getThermalPower().asString()) //
				.append("|").append(this.getOperatingState().getName()) //
				.append(this.isReadOnly() ? "|ReadOnly" : "") //
				.toString();
	}
}
