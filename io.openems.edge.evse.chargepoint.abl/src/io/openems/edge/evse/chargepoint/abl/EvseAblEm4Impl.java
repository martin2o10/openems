package io.openems.edge.evse.chargepoint.abl;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode.FC3;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.evse.api.common.ApplySetPoint.MIN_CURRENT;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
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
import io.openems.common.types.SemanticVersion;
import io.openems.common.types.Tuple2;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Implementation for the ABL eM4 based on ABL "Modbus TCP -
 * Schnittstellenbeschreibung Wallbox eM4 Controller / Extender", R 01,
 * 19.12.2023.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Abl.Em4", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Modbus")
public class EvseAblEm4Impl extends AbstractOpenemsModbusComponent implements EvseAblEm4, EvseChargePoint,
		ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	private static final long SAME_SET_POINT_INTERVAL_SECONDS = 30;
	private static final long NEW_SET_POINT_INTERVAL_SECONDS = 5;

	/** Chapter 3.4: all values 0xF0..0xFF of the outlet status are errors. */
	private static final ElementToChannelConverter TO_OUTLET_STATUS = new ElementToChannelConverter(//
			value -> value instanceof Integer i && i >= 0xF0 && i <= 0xFF //
					? OutletStatus.ERROR.getValue() //
					: value);

	/** Chapter 3.3: bit 15..12 major, bit 11..8 minor, bit 7..0 patch. */
	private static final ElementToChannelConverter TO_FIRMWARE_VERSION = new ElementToChannelConverter(//
			value -> value instanceof Integer i //
					? new SemanticVersion((i >> 12) & 0xF, (i >> 8) & 0xF, i & 0xFF).toString() //
					: null);

	private final Logger log = LoggerFactory.getLogger(EvseAblEm4Impl.class);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ComponentManager componentManager;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	private Tuple2<Instant, Integer> previousSetPoint = null;

	public EvseAblEm4Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseAblEm4.ChannelId.values() //
		);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		readElementOnce(FC3, this.getModbusProtocol(), ModbusUtils::retryOnNull, new StringWordElement(0x0110, 16)) //
				.thenAccept(serialNumber -> setValue(this, EvseAblEm4.ChannelId.SERIAL_NUMBER, serialNumber));
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = config;
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var pr = this.getPhaseRotation();
		final var outlet = this.config.outlet().startAddress;
		var modbusProtocol = new ModbusProtocol(this, //
				// Chapter 3.4: values of the outlet meter
				new FC3ReadRegistersTask(outlet + 0x01, Priority.HIGH, //
						m(pr.channelCurrentL1(), new UnsignedDoublewordElement(outlet + 0x01), SCALE_FACTOR_2), //
						m(pr.channelCurrentL2(), new UnsignedDoublewordElement(outlet + 0x03), SCALE_FACTOR_2), //
						m(pr.channelCurrentL3(), new UnsignedDoublewordElement(outlet + 0x05), SCALE_FACTOR_2), //
						m(pr.channelVoltageL1(), new UnsignedDoublewordElement(outlet + 0x07), SCALE_FACTOR_2), //
						m(pr.channelVoltageL2(), new UnsignedDoublewordElement(outlet + 0x09), SCALE_FACTOR_2), //
						m(pr.channelVoltageL3(), new UnsignedDoublewordElement(outlet + 0x0B), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(outlet + 0x0D)), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedDoublewordElement(outlet + 0x0F), SCALE_FACTOR_1)), //
				new FC3ReadRegistersTask(outlet + 0x31, Priority.HIGH, //
						m(EvseAblEm4.ChannelId.OUTLET_STATUS, new UnsignedWordElement(outlet + 0x31),
								TO_OUTLET_STATUS), //
						m(EvseAblEm4.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(outlet + 0x32),
								SCALE_FACTOR_2), //
						m(EvseAblEm4.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, new UnsignedWordElement(outlet + 0x33),
								SCALE_FACTOR_2)), //
				// Chapter 3.3: properties of the product
				new FC3ReadRegistersTask(0x0122, Priority.LOW, //
						m(EvseAblEm4.ChannelId.FIRMWARE_VERSION, new UnsignedWordElement(0x0122),
								TO_FIRMWARE_VERSION), //
						new DummyRegisterElement(0x0123), //
						m(EvseAblEm4.ChannelId.HARDWARE_MAX_CURRENT, new UnsignedWordElement(0x0124), SCALE_FACTOR_2)));

		if (!this.config.readOnly()) {
			// Chapter 2: the eM4 supports only function codes 3 and 16
			modbusProtocol.addTasks(//
					new FC16WriteRegistersTask(outlet + 0x32, //
							m(EvseAblEm4.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(outlet + 0x32),
									SCALE_FACTOR_2)));
		}
		return modbusProtocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		if (TOPIC_CYCLE_BEFORE_PROCESS_IMAGE.equals(event.getTopic())) {
			final OutletStatus outletStatus = this.getOutletStatusChannel().getNextValue().asEnum();
			final boolean isReadyForCharging = outletStatus != null && outletStatus.isReadyForCharging;
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReadyForCharging);
		}
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config.readOnly()) {
			return ChargePointAbilities.create().build();
		}

		final OutletStatus outletStatus = this.getOutletStatus();
		final var phases = this.config.wiring();
		final var maxCurrent = this.getHardwareMaxCurrentChannel().value().orElse(MIN_CURRENT);

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, MIN_CURRENT, maxCurrent)) //
				.setIsEvConnected(outletStatus != null && outletStatus.isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}
		this.applySetPoint(actions.getApplySetPointInMilliAmpere().value());
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseAblEm4.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString());
		}
		return b.toString();
	}

	private void applySetPoint(int setPointInMilliAmpere) {
		try {
			if (setPointInMilliAmpere == 0) {
				// stopping is never throttled; it is reconciled with the readback in every
				// cycle and the current is written immediately on restart
				this.previousSetPoint = null;
				final var readback = this.getSetChargingCurrentChannel().value().get();
				if (readback == null || readback != 0) {
					this.setSetChargingCurrent(0);
				}
				return;
			}
			final var now = Instant.now(this.componentManager.getClock());
			if (this.isSetPointWriteThrottled(now, setPointInMilliAmpere)) {
				return;
			}
			this.previousSetPoint = Tuple2.of(now, setPointInMilliAmpere);
			this.setSetChargingCurrent(setPointInMilliAmpere);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply set-point", e);
		}
	}

	private boolean isSetPointWriteThrottled(Instant now, int setPointInMilliAmpere) {
		if (this.previousSetPoint == null) {
			return false;
		}
		final var previousValue = this.previousSetPoint.b();
		final var elapsedSeconds = Duration.between(this.previousSetPoint.a(), now).getSeconds();
		final var interval = previousValue == setPointInMilliAmpere //
				? SAME_SET_POINT_INTERVAL_SECONDS //
				: NEW_SET_POINT_INTERVAL_SECONDS;
		return elapsedSeconds < interval;
	}
}
