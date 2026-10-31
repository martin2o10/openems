package io.openems.edge.evse.chargepoint.goe;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode.FC4;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static io.openems.edge.evse.api.common.ApplySetPoint.MIN_CURRENT;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertMilliAmpereToWatt;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Objects;

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
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.GoE", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Modbus")
public class EvseGoeImpl extends AbstractOpenemsModbusComponent implements EvseGoe, EvseChargePoint,
		ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	private static final int PHASE_SWITCH_MODE_SINGLE_PHASE = 1;
	private static final int PHASE_SWITCH_MODE_THREE_PHASE = 2;

	/** Maps unknown values of register 107 to INTERNAL, as documented by go-e. */
	private static final ElementToChannelConverter TO_ERROR_CODE = new ElementToChannelConverter(//
			value -> ErrorCode.fromValue(value instanceof Number n ? n.intValue() : -1).getValue());

	private final Logger log = LoggerFactory.getLogger(EvseGoeImpl.class);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	/** Last Set-Point in [mA] that was queued for writing; null if none yet. */
	private Integer lastSetPoint = null;

	/** Last state of ALLOW that was queued for writing; null if none yet. */
	private Boolean lastAllowCharging = null;

	public EvseGoeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseGoe.ChannelId.values() //
		);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		readElementOnce(FC4, this.getModbusProtocol(), ModbusUtils::retryOnNull, new StringWordElement(304, 6)) //
				.thenAccept(serialNumber -> setValue(this, EvseGoe.ChannelId.SERIAL_NUMBER, serialNumber));
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
		var modbusProtocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(100, Priority.HIGH, //
						m(EvseGoe.ChannelId.CAR_STATE, new UnsignedWordElement(100)), //
						new DummyRegisterElement(101, 104), //
						m(EvseGoe.ChannelId.FIRMWARE_VERSION, new StringWordElement(105, 2)), //
						m(EvseGoe.ChannelId.ERROR, new UnsignedWordElement(107), TO_ERROR_CODE), //
						m(pr.channelVoltageL1(), new UnsignedDoublewordElement(108).wordOrder(LSWMSW),
								SCALE_FACTOR_3), //
						m(pr.channelVoltageL2(), new UnsignedDoublewordElement(110).wordOrder(LSWMSW),
								SCALE_FACTOR_3), //
						m(pr.channelVoltageL3(), new UnsignedDoublewordElement(112).wordOrder(LSWMSW),
								SCALE_FACTOR_3), //
						m(pr.channelCurrentL1(), new UnsignedDoublewordElement(114).wordOrder(LSWMSW),
								SCALE_FACTOR_2), //
						m(pr.channelCurrentL2(), new UnsignedDoublewordElement(116).wordOrder(LSWMSW),
								SCALE_FACTOR_2), //
						m(pr.channelCurrentL3(), new UnsignedDoublewordElement(118).wordOrder(LSWMSW),
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(120).wordOrder(LSWMSW),
								SCALE_FACTOR_MINUS_2)), //
				new FC4ReadInputRegistersTask(128, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedDoublewordElement(128).wordOrder(LSWMSW), SCALE_FACTOR_2)), //
				new FC4ReadInputRegistersTask(144, Priority.LOW, //
						m(EvseGoe.ChannelId.VOLTAGE_N, new UnsignedDoublewordElement(144).wordOrder(LSWMSW)), //
						m(pr.channelActivePowerL1(), new UnsignedDoublewordElement(146).wordOrder(LSWMSW),
								SCALE_FACTOR_2), //
						m(pr.channelActivePowerL2(), new UnsignedDoublewordElement(148).wordOrder(LSWMSW),
								SCALE_FACTOR_2), //
						m(pr.channelActivePowerL3(), new UnsignedDoublewordElement(150).wordOrder(LSWMSW),
								SCALE_FACTOR_2), //
						m(EvseGoe.ChannelId.POWER_FACTOR_L1, new UnsignedDoublewordElement(152).wordOrder(LSWMSW)), //
						m(EvseGoe.ChannelId.POWER_FACTOR_L2, new UnsignedDoublewordElement(154).wordOrder(LSWMSW)), //
						m(EvseGoe.ChannelId.POWER_FACTOR_L3, new UnsignedDoublewordElement(156).wordOrder(LSWMSW))), //
				new FC4ReadInputRegistersTask(205, Priority.LOW, //
						m(EvseGoe.ChannelId.PHASES, new UnsignedWordElement(205))), //
				new FC3ReadRegistersTask(200, Priority.LOW, //
						m(EvseGoe.ChannelId.SET_ALLOW_CHARGING, new UnsignedWordElement(200))), //
				new FC3ReadRegistersTask(211, Priority.LOW, //
						m(EvseGoe.ChannelId.HARDWARE_MAX_CURRENT, new UnsignedWordElement(211))), //
				new FC3ReadRegistersTask(299, Priority.HIGH, //
						m(EvseGoe.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(299), SCALE_FACTOR_3)), //
				new FC3ReadRegistersTask(332, Priority.LOW, //
						m(EvseGoe.ChannelId.SET_PHASE_SWITCH_MODE, new UnsignedWordElement(332))));

		if (!this.config.readOnly()) {
			// FC6 is not supported by go-e. Register 299 is volatile; the EEPROM register
			// 300 must never be written cyclically.
			modbusProtocol.addTasks(//
					new FC16WriteRegistersTask(299, //
							m(EvseGoe.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(299),
									SCALE_FACTOR_3)), //
					new FC16WriteRegistersTask(200, //
							m(EvseGoe.ChannelId.SET_ALLOW_CHARGING, new UnsignedWordElement(200))), //
					new FC16WriteRegistersTask(332, //
							m(EvseGoe.ChannelId.SET_PHASE_SWITCH_MODE, new UnsignedWordElement(332))));
		}
		return modbusProtocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			final CarState carState = this.getCarStateChannel().getNextValue().asEnum();
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, carState.isReadyForCharging);
		}
		}
	}

	private SingleOrThreePhase getPhases() {
		// A single-phase wired charge point can never charge three-phased
		if (this.config.wiring() == SINGLE_PHASE) {
			return SINGLE_PHASE;
		}
		final var configuredPhases = switch (this.getSetPhaseSwitchModeChannel().value().orElse(0)) {
		case PHASE_SWITCH_MODE_SINGLE_PHASE -> SINGLE_PHASE;
		case PHASE_SWITCH_MODE_THREE_PHASE -> THREE_PHASE;
		default -> null; // register not available or automatic mode of the charge point
		};
		if (configuredPhases != null) {
			return configuredPhases;
		}
		// Fall back to the phases that actually carry current
		final var phaseCount = evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().get(), //
				this.getCurrentL2().get(), //
				this.getCurrentL3().get());
		return phaseCount == null || phaseCount == 3 //
				? THREE_PHASE // no current at all: assume the default wiring
				: SINGLE_PHASE;
	}

	/**
	 * Gets the configured maximum charging current, limited by the hardware
	 * maximum of the charge point.
	 *
	 * @return the maximum charging current in [mA]
	 */
	private int getMaxCurrentInMilliAmpere() {
		var result = this.config.maxCurrent();
		var hardwareMax = this.getHardwareMaxCurrentChannel().value().get();
		if (hardwareMax != null) {
			result = Math.min(result, (int) Math.round(hardwareMax * 1000));
		}
		return result;
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config.readOnly()) {
			return ChargePointAbilities.create().build();
		}

		final var phases = this.getPhases();
		final var maxCurrent = this.getMaxCurrentInMilliAmpere();
		final var phaseSwitchDirection = this.getPhaseSwitchAbility(phases);

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, MIN_CURRENT, maxCurrent)) //
				.setPhaseSwitchManual(phaseSwitchDirection,
						getOppositePhaseApplySetPointAbility(phaseSwitchDirection, maxCurrent)) //
				.setIsEvConnected(this.getCarState().isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	/**
	 * Advertises the possible phase-switch direction, i.e. the opposite of the
	 * currently active phase mode. Returns null if phase switching is not possible
	 * at all.
	 *
	 * @param phases the currently active {@link SingleOrThreePhase}
	 * @return the {@link PhaseSwitchDirection} or null
	 */
	private PhaseSwitchDirection getPhaseSwitchAbility(SingleOrThreePhase phases) {
		if (this.config.wiring() == SINGLE_PHASE //
				|| this.getSetPhaseSwitchModeChannel().value().get() == null) {
			return null;
		}
		return phases == THREE_PHASE //
				? PhaseSwitchDirection.TO_SINGLE_PHASE //
				: PhaseSwitchDirection.TO_THREE_PHASE;
	}

	private static ApplySetPoint.Ability.Watt getOppositePhaseApplySetPointAbility(
			PhaseSwitchDirection phaseSwitchDirection, int maxCurrent) {
		if (phaseSwitchDirection == null) {
			return null;
		}
		final var oppositePhase = phaseSwitchDirection.getTargetPhase();
		return new ApplySetPoint.Ability.Watt(oppositePhase, //
				convertMilliAmpereToWatt(oppositePhase, MIN_CURRENT), //
				convertMilliAmpereToWatt(oppositePhase, maxCurrent));
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}
		this.applySetPoint(actions.getApplySetPointInMilliAmpere().value());

		final var phaseSwitch = actions.phaseSwitch();
		if (phaseSwitch != null && phaseSwitch.direction() != null) {
			this.applyPhaseSwitch(phaseSwitch.direction());
		}
	}

	private void applyPhaseSwitch(PhaseSwitchDirection direction) {
		final var phaseSwitchMode = direction == PhaseSwitchDirection.TO_SINGLE_PHASE //
				? PHASE_SWITCH_MODE_SINGLE_PHASE //
				: PHASE_SWITCH_MODE_THREE_PHASE;
		if (this.getSetPhaseSwitchModeChannel().value().orElse(0) == phaseSwitchMode) {
			return;
		}
		try {
			this.setSetPhaseSwitchMode(phaseSwitchMode);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply phase switch: " + e.getMessage());
		}
	}

	/**
	 * Applies the Set-Point in [mA].
	 *
	 * <p>
	 * Charging is started and stopped via ALLOW, because register 299 accepts
	 * only 6..32 A. Registers are written on change, and again if the readback
	 * differs, e.g. after a restart of the charge point.
	 *
	 * @param setPointInMilliAmpere the Set-Point in [mA]
	 */
	private void applySetPoint(int setPointInMilliAmpere) {
		try {
			this.applyAllowCharging(setPointInMilliAmpere > 0);
			if (setPointInMilliAmpere == 0) {
				this.lastSetPoint = 0;
				return;
			}
			final var setPointInWholeAmpere = Math.round(setPointInMilliAmpere / 1000f) * 1000;
			final var readback = this.getSetChargingCurrentChannel().value().get();
			if (Objects.equals(this.lastSetPoint, setPointInMilliAmpere) //
					&& (readback == null || readback == setPointInWholeAmpere)) {
				return;
			}
			this.lastSetPoint = setPointInMilliAmpere;
			this.setSetChargingCurrent(setPointInMilliAmpere);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply set-point: " + e.getMessage());
		}
	}

	private void applyAllowCharging(boolean allowCharging) throws OpenemsNamedException {
		final var readback = this.getSetAllowChargingChannel().value().get();
		if (Objects.equals(this.lastAllowCharging, allowCharging) //
				&& (readback == null || readback == allowCharging)) {
			return;
		}
		this.lastAllowCharging = allowCharging;
		this.setSetAllowCharging(allowCharging);
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
					.append(this.channel(EvseGoe.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString());
		}
		return b.toString();
	}
}
