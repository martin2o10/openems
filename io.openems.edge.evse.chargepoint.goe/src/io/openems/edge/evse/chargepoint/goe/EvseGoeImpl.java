package io.openems.edge.evse.chargepoint.goe;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static io.openems.edge.evse.api.common.ApplySetPoint.MIN_CURRENT;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
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

	/**
	 * Register 299 (AMPERE_VOLATILE) holds whole Amperes in 6..32; the Channel
	 * follows the OpenEMS convention and holds Milliamperes.
	 */
	private static final ElementToChannelConverter AMPERE_TO_MILLIAMPERE = new ElementToChannelConverter(//
			value -> ((Number) value).intValue() * 1000, //
			value -> (int) Math.round(((Number) value).doubleValue() / 1000));

	/** Registers holding 0/1 map to/from Boolean. */
	private static final ElementToChannelConverter ZERO_ONE_TO_BOOLEAN = new ElementToChannelConverter(//
			value -> ((Number) value).intValue() != 0, //
			value -> Boolean.TRUE.equals(value) ? 1 : 0);

	/** Converts go-e 0.1 Ampere units to [mA]. */
	private static final ElementToChannelConverter DECIAMPERE_TO_MILLIAMPERE = new ElementToChannelConverter(//
			value -> ((Number) value).longValue() * 100, //
			value -> value);

	/** Converts go-e 0.01 Watt units to [W]. */
	private static final ElementToChannelConverter CENTIWATT_TO_WATT = new ElementToChannelConverter(//
			value -> ((Number) value).longValue() * 0.01, //
			value -> value);

	/** Converts go-e 0.1 Kilowatt units to [W]. */
	private static final ElementToChannelConverter DECI_KILOWATT_TO_WATT = new ElementToChannelConverter(//
			value -> ((Number) value).longValue() * 100, //
			value -> value);

	/** Converts go-e 0.1 Kilowatt-hour units to [Wh]. */
	private static final ElementToChannelConverter DECI_KILOWATT_HOURS_TO_WATT_HOURS = new ElementToChannelConverter(//
			value -> ((Number) value).longValue() * 100, //
			value -> value);

	/**
	 * Converts go-e Deka-Watt-seconds (10 Ws) to [Wh]: 10 Ws / 3600 = 1/360 Wh.
	 * Values are floored, so charged energy is never overstated (the go-e
	 * Modbus API v2 example rounds 1'000'000 Ws down to 277 Wh).
	 */
	private static final ElementToChannelConverter DEKA_WATT_SECONDS_TO_WATT_HOURS = new ElementToChannelConverter(//
			value -> Math.floor(((Number) value).longValue() / 360.), //
			value -> value);

	/** Parses the raw value of input register 107 to an {@link ErrorCode}. */
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

	/** Mirrors the last written state of the ALLOW register (200). */
	private boolean chargingEnabled = false;

	// Read-once Tasks; removed from the ModbusProtocol after the first
	// successful read
	private FC4ReadInputRegistersTask fwvTask = null;
	private FC4ReadInputRegistersTask snrTask = null;
	private FC3ReadRegistersTask hardwareMaxCurrentTask = null;

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
				// Car state and current measurement (Input Registers)
				new FC4ReadInputRegistersTask(100, Priority.HIGH, //
						m(EvseGoe.ChannelId.CAR_STATE, new UnsignedWordElement(100)), //
						// 101 (PP_CABLE) and 102-106 are not used here
						new DummyRegisterElement(101, 106), //
						m(EvseGoe.ChannelId.ERROR, new UnsignedWordElement(107), TO_ERROR_CODE), //
						m(pr.channelVoltageL1(), new UnsignedDoublewordElement(108).wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(pr.channelVoltageL2(), new UnsignedDoublewordElement(110).wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(pr.channelVoltageL3(), new UnsignedDoublewordElement(112).wordOrder(LSWMSW), SCALE_FACTOR_3), //
						m(pr.channelCurrentL1(), new UnsignedDoublewordElement(114).wordOrder(LSWMSW),
								DECIAMPERE_TO_MILLIAMPERE), //
						m(pr.channelCurrentL2(), new UnsignedDoublewordElement(116).wordOrder(LSWMSW),
								DECIAMPERE_TO_MILLIAMPERE), //
						m(pr.channelCurrentL3(), new UnsignedDoublewordElement(118).wordOrder(LSWMSW),
								DECIAMPERE_TO_MILLIAMPERE), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(120).wordOrder(LSWMSW),
								CENTIWATT_TO_WATT)), //

				// Energy (Input Registers)
				new FC4ReadInputRegistersTask(128, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedDoublewordElement(128).wordOrder(LSWMSW),
								DECI_KILOWATT_HOURS_TO_WATT_HOURS), //
						// 130-131 are not used here
						new DummyRegisterElement(130, 131), //
						m(EvseGoe.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(132).wordOrder(LSWMSW),
								DEKA_WATT_SECONDS_TO_WATT_HOURS)), //

				// Per-phase power and power factor (Input Registers)
				new FC4ReadInputRegistersTask(144, Priority.LOW, //
						m(EvseGoe.ChannelId.VOLTAGE_N, new UnsignedDoublewordElement(144).wordOrder(LSWMSW)), //
						m(pr.channelActivePowerL1(), new UnsignedDoublewordElement(146).wordOrder(LSWMSW),
								DECI_KILOWATT_TO_WATT), //
						m(pr.channelActivePowerL2(), new UnsignedDoublewordElement(148).wordOrder(LSWMSW),
								DECI_KILOWATT_TO_WATT), //
						m(pr.channelActivePowerL3(), new UnsignedDoublewordElement(150).wordOrder(LSWMSW),
								DECI_KILOWATT_TO_WATT), //
						m(EvseGoe.ChannelId.POWER_FACTOR_L1, new UnsignedDoublewordElement(152).wordOrder(LSWMSW)), //
						m(EvseGoe.ChannelId.POWER_FACTOR_L2, new UnsignedDoublewordElement(154).wordOrder(LSWMSW)), //
						m(EvseGoe.ChannelId.POWER_FACTOR_L3, new UnsignedDoublewordElement(156).wordOrder(LSWMSW))), //

				// Phase states before/after contactor (Input Register)
				new FC4ReadInputRegistersTask(205, Priority.LOW, //
						m(EvseGoe.ChannelId.PHASES, new UnsignedWordElement(205))), //

				// Charging state readbacks (Holding Registers)
				new FC3ReadRegistersTask(200, Priority.LOW, //
						m(EvseGoe.ChannelId.SET_ALLOW_CHARGING, new UnsignedWordElement(200), ZERO_ONE_TO_BOOLEAN)), //
				new FC3ReadRegistersTask(299, Priority.HIGH, //
						m(EvseGoe.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(299),
								AMPERE_TO_MILLIAMPERE)), //
				new FC3ReadRegistersTask(332, Priority.LOW, //
						m(EvseGoe.ChannelId.PHASE_SWITCH_MODE, new UnsignedWordElement(332))), //

				// Read-once registers; the Tasks remove themselves after the
				// first successful read (see removeTask())
				this.fwvTask = new FC4ReadInputRegistersTask(105, Priority.LOW, //
						m(EvseGoe.ChannelId.FWV, new StringWordElement(105, 2) //
								.onUpdateCallback(v -> this.removeTask(this.fwvTask)))), //
				this.snrTask = new FC4ReadInputRegistersTask(304, Priority.LOW, //
						m(EvseGoe.ChannelId.SNR, new StringWordElement(304, 6) //
								.onUpdateCallback(v -> this.removeTask(this.snrTask)))), //
				this.hardwareMaxCurrentTask = new FC3ReadRegistersTask(211, Priority.LOW, //
						m(EvseGoe.ChannelId.HARDWARE_MAX_CURRENT, new UnsignedWordElement(211)) //
								.onUpdateCallback(v -> this.removeTask(this.hardwareMaxCurrentTask))));

		if (!this.config.readOnly()) {
			/*
			 * Writes via FC16 only: FN6 (Preset Single Register) is not supported by
			 * go-e. The EEPROM-Register 40301 (AMPERE_EEPROM) must never be written
			 * cyclically (max ~100.000 write cycles); charging regulation uses the
			 * volatile register 299 instead.
			 */
			modbusProtocol.addTasks(//
					new FC16WriteRegistersTask(299, //
							m(EvseGoe.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(299),
									AMPERE_TO_MILLIAMPERE)), //
					new FC16WriteRegistersTask(200, //
							m(EvseGoe.ChannelId.SET_ALLOW_CHARGING, new UnsignedWordElement(200),
									ZERO_ONE_TO_BOOLEAN)));
		}
		return modbusProtocol;
	}

	/**
	 * Removes a read-once {@link Task} from the {@link ModbusProtocol}; called
	 * from the element's onUpdateCallback after the first successful read.
	 *
	 * @param task the {@link Task}
	 */
	private void removeTask(Task task) {
		this.getModbusProtocol().removeTask(task);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			final CarState carState = this.getCarStateChannel().getNextValue().asEnum();
			setValue(this, EvseGoe.ChannelId.IS_EV_CONNECTED, carState.isEvConnected);
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, carState.isReadyForCharging);
		}
		}
	}

	private SingleOrThreePhase getPhases() {
		// A single-phase wired charge point can never charge three-phased
		if (this.config.wiring() == SINGLE_PHASE) {
			return SINGLE_PHASE;
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
	 * Gets the maximum charging current in [mA]: the configured value, limited
	 * by the hardware maximum current from register 211 (AMPERE_MAX), e.g. 16 A
	 * for the 11 kW variant of the go-e Gemini.
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

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, MIN_CURRENT, //
						this.getMaxCurrentInMilliAmpere())) //
				.setIsEvConnected(this.getCarState().isEvConnected) //
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

	/**
	 * Applies the Set-Point in [mA].
	 *
	 * <p>
	 * Enable-on-charge (KEBA pattern): the transition from 0 to a Set-Point
	 * greater than zero writes ALLOW=1 once; returning to zero writes ALLOW=0.
	 * The current register is written on change only, so identical Set-Points
	 * are not queued again. A Set-Point of zero does not write the current
	 * register, because register 299 accepts only 6..32 A; stopping the charging
	 * session is handled by ALLOW=0 alone.
	 *
	 * @param setPointInMilliAmpere the Set-Point in [mA]
	 */
	private void applySetPoint(int setPointInMilliAmpere) {
		try {
			this.applyAllowCharging(setPointInMilliAmpere);

			// Write-on-change: skip identical Set-Points
			if (this.lastSetPoint != null && this.lastSetPoint == setPointInMilliAmpere) {
				return;
			}
			this.lastSetPoint = setPointInMilliAmpere;

			if (setPointInMilliAmpere == 0) {
				return;
			}
			this.setSetChargingCurrent(setPointInMilliAmpere);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply set-point: " + e.getMessage());
		}
	}

	/**
	 * Writes the ALLOW register (200) on the enable/disable transitions of a
	 * charging session.
	 *
	 * @param setPointInMilliAmpere the Set-Point in [mA]
	 * @throws OpenemsNamedException on error
	 */
	private void applyAllowCharging(int setPointInMilliAmpere) throws OpenemsNamedException {
		final var isCharging = setPointInMilliAmpere > 0;
		if (this.chargingEnabled == isCharging) {
			return;
		}
		this.chargingEnabled = isCharging;
		this.setSetAllowCharging(isCharging);
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
