package io.openems.edge.heat.broetje.neo;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.meter.api.ElectricityMeter.calculatePhasesFromActivePower;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
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
public class HeatBroetjeNeoImpl extends AbstractOpenemsModbusComponent implements HeatBroetjeNeo, ManagedHeatPump,
		Heat, ElectricityMeter, ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler {

	public static final String FACTORY_ID = "Heat.Broetje.Neo";

	/** Flags of the NEO-RKM are "active" for values greater than zero. */
	private static final ElementToChannelConverter FLAG = new ElementToChannelConverter(
			value -> value == null ? null : ((Number) value).intValue() > 0);

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

	private Config config;
	private SetpointHandler setpointHandler;
	private HeatPumpAbilities abilities;
	private HeatPumpAction pendingAction = HeatPumpAction.OWN_CONTROL;

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
				Duration.ofSeconds(config.writeInterval()));
		this.abilities = new HeatPumpAbilities(new ApplyPower.Continuous(config.minPower(), config.maxPower()),
				config.minRunTime(), config.minOffTime(), true);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var protocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(10, Priority.HIGH, //
						m(HeatBroetjeNeo.ChannelId.OUTDOOR_TEMPERATURE, new SignedWordElement(10)), //
						m(HeatBroetjeNeo.ChannelId.DOMESTIC_HOT_WATER_TEMPERATURE, new SignedWordElement(11)), //
						m(HeatBroetjeNeo.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(12)), //
						m(HeatBroetjeNeo.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(13)), //
						m(Heat.ChannelId.TEMPERATURE, new SignedWordElement(14)), //
						new DummyRegisterElement(15, 24), //
						m(HeatBroetjeNeo.ChannelId.COMPRESSOR_RUNNING, new SignedWordElement(25), FLAG), //
						m(HeatBroetjeNeo.ChannelId.DEVICE_FAULT, new SignedWordElement(26), FLAG), //
						m(HeatBroetjeNeo.ChannelId.DEFROST_VALVE_ACTIVE, new SignedWordElement(27), FLAG), //
						new DummyRegisterElement(28), //
						m(HeatBroetjeNeo.ChannelId.COMPRESSOR_SPEED, new SignedWordElement(29)), //
						m(HeatBroetjeNeo.ChannelId.COEFFICIENT_OF_PERFORMANCE, new SignedWordElement(30),
								SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(31), //
						m(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE, new SignedWordElement(32), FLAG), //
						new DummyRegisterElement(33), //
						m(Heat.ChannelId.TARGET_TEMPERATURE, new SignedWordElement(34)), //
						new DummyRegisterElement(35, 40), //
						m(HeatBroetjeNeo.ChannelId.DEMAND, new UnsignedWordElement(41))), //
				new FC4ReadInputRegistersTask(68, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(68),
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(70)), //
						new DummyRegisterElement(72, 73), //
						m(ManagedHeatPump.ChannelId.THERMAL_POWER, new UnsignedDoublewordElement(74),
								SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(100, Priority.LOW, //
						m(HeatBroetjeNeo.ChannelId.OPERATING_MODE, new UnsignedWordElement(100))));

		if (!this.config.readOnly()) {
			protocol.addTasks(//
					new FC6WriteRegisterTask(125, //
							m(HeatBroetjeNeo.ChannelId.POWER_SETPOINT, new UnsignedWordElement(125))), //
					new FC6WriteRegisterTask(149, //
							m(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE, new SignedWordElement(149))), //
					new FC6WriteRegisterTask(150, //
							m(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE,
									new SignedWordElement(150))));
		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.updateOperatingState();
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE -> this.applyAction();
		}
	}

	private void updateOperatingState() {
		setValue(this, ManagedHeatPump.ChannelId.OPERATING_STATE, OperatingStateMapper.map(//
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.DEVICE_FAULT), //
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE), //
				this.channel(HeatBroetjeNeo.ChannelId.OPERATING_MODE).value().asEnum(), //
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.COMPRESSOR_RUNNING), //
				this.<Boolean>channelValue(HeatBroetjeNeo.ChannelId.DEFROST_VALVE_ACTIVE), //
				this.channel(HeatBroetjeNeo.ChannelId.DEMAND).value().asEnum()));
	}

	@SuppressWarnings("unchecked")
	private <T> T channelValue(HeatBroetjeNeo.ChannelId channelId) {
		return (T) this.channel(channelId).value().get();
	}

	@Override
	public HeatPumpAbilities getHeatPumpAbilities() {
		return this.abilities;
	}

	@Override
	public void apply(HeatPumpAction action) {
		this.pendingAction = action == null ? HeatPumpAction.OWN_CONTROL : action;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	/**
	 * Takes over the action of a Controller. Like for the simulator, the action is
	 * only valid for the current Cycle; without an action the heat pump returns to
	 * its own control.
	 */
	private void applyAction() {
		var action = this.pendingAction;
		this.pendingAction = HeatPumpAction.OWN_CONTROL;
		this._setAction(action);

		if (this.config.readOnly()) {
			return;
		}
		var result = this.setpointHandler.apply(Instant.now(this.componentManager.getClock()), action);
		if (result == null) {
			return;
		}
		try {
			this.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT)
					.setNextWriteValue(result.powerSetpoint());
			this.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE)
					.setNextWriteValue(result.gridOperatorReleaseValue());
			this.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE)
					.setNextWriteValue(result.gridOperatorReleaseActivate());
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to write setpoints: " + e.getMessage());
		}
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
				.append(this.config.readOnly() ? "|ReadOnly" : "") //
				.toString();
	}
}
