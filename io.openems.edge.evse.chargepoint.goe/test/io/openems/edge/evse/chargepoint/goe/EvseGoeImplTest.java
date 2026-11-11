package io.openems.edge.evse.chargepoint.goe;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertMilliAmpereToWatt;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseGoeImplTest {

	@Test
	public void testActivation() throws Exception {
		final var sut = new EvseGoeImpl();
		prepareGoeTest(sut, createFixtureBridge(32), createDummyClock(), config()) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Component activates without Modbus communication failure") //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.CHARGING) //
						.output(EvseGoe.ChannelId.ERROR, ErrorCode.NO_ERROR))

				.next(new TestCase("Device information is read") //
						.output(EvseGoe.ChannelId.FIRMWARE_VERSION, "506") //
						.output(EvseGoe.ChannelId.SERIAL_NUMBER, "0123456789AB") //
						.output(EvseGoe.ChannelId.HARDWARE_MAX_CURRENT, 32000))

				.deactivate();
	}

	@Test
	public void testReadMeterValues() throws Exception {
		final var sut = new EvseGoeImpl();
		prepareGoeTest(sut, createFixtureBridge(32), createDummyClock(), config()) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Voltages are read in V and scaled to mV") //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230000) //
						.output(EvseGoe.ChannelId.VOLTAGE_N, 0))

				.next(new TestCase("Currents are read in 0.1 A and scaled to mA") //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 10300) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 10700) //
						.output(ElectricityMeter.ChannelId.CURRENT, 31500))

				.next(new TestCase("Powers are read in 0.01 W and 0.1 kW units and scaled to W") //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3600) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1200) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1200) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1200))

				.next(new TestCase("Energy is read in 0.1 kWh units and scaled to Wh") //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 36000L))

				.next(new TestCase("go-e specific values") //
						.output(EvseGoe.ChannelId.POWER_FACTOR_L1, 98) //
						.output(EvseGoe.ChannelId.POWER_FACTOR_L2, 97) //
						.output(EvseGoe.ChannelId.POWER_FACTOR_L3, 99) //
						.output(EvseGoe.ChannelId.PHASES, 0b00111000))

				.deactivate();

		// READ_WRITE Channels: TestCase.output() checks the write value, so the
		// readback is asserted directly
		assertEquals(16000, sut.getSetChargingCurrentChannel().value().get());
		assertEquals(true, sut.getSetAllowChargingChannel().value().get());
		assertEquals(2, sut.getSetPhaseSwitchModeChannel().value().get());
	}

	@ParameterizedTest(name = "CAR_STATE {0} -> {1}")
	@CsvSource({ //
			"2, CHARGING, true, true", //
			"1, READY_NO_EV, false, false", //
			"0, DEFECT, false, false", //
			"3, WAITING_FOR_VEHICLE, true, true", //
			"4, FINISHED_EV_CONNECTED, false, true", //
			"99, UNDEFINED, false, false" //
	})
	public void testCarStateMapping(int carStateRegister, CarState expectedCarState, boolean expectedReadyForCharging,
			boolean expectedEvConnected) throws Exception {
		final var sut = new EvseGoeImpl();
		final var bridge = createFixtureBridge(32).withInputRegisters(100, carStateRegister);
		final var test = prepareGoeTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, expectedCarState) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, expectedReadyForCharging));

		assertEquals(expectedEvConnected, sut.getChargePointAbilities().isEvConnected());
		test.deactivate();
	}

	@ParameterizedTest(name = "ERROR {0} -> {1}")
	@CsvSource({ //
			"0, NO_ERROR, false, false, false, false", //
			"1, RCCB, true, false, false, false", //
			"3, PHASE, false, true, false, false", //
			"8, NO_GROUND, false, false, true, false", //
			"4711, INTERNAL, false, false, false, true" // unknown values map to INTERNAL as documented by go-e
	})
	public void testErrorMapping(int errorRegister, ErrorCode expectedErrorCode, boolean rccb, boolean phase,
			boolean noGround, boolean internal) throws Exception {
		final var sut = new EvseGoeImpl();
		final var bridge = createFixtureBridge(32).withInputRegisters(107, errorRegister);
		prepareGoeTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.ERROR, expectedErrorCode) //
						.output(EvseGoe.ChannelId.ERR_RCCB, rccb) //
						.output(EvseGoe.ChannelId.ERR_PHASE, phase) //
						.output(EvseGoe.ChannelId.ERR_NO_GROUND, noGround) //
						.output(EvseGoe.ChannelId.ERR_INTERNAL, internal)) //
				.deactivate();
	}

	@Test
	public void testErrorIsUndefinedOnCommunicationLoss() throws Exception {
		final var sut = new EvseGoeImpl();
		// no input registers: all FC4 read tasks fail
		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(200, 1) //
				.withRegisters(211, 32) //
				.withRegisters(299, 16) //
				.withRegisters(332, 2);
		final var test = prepareGoeTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20);

		assertNull(sut.channel(EvseGoe.ChannelId.ERROR).value().get(),
				"A communication loss must not be reported as INTERNAL error");
		test.deactivate();
	}

	@ParameterizedTest(name = "wiring={0}, AMPERE_MAX={1} A, maxCurrent={2} mA -> {3}, {4} mA")
	@CsvSource({ //
			"THREE_PHASE, 32, 32000, THREE_PHASE, 32000", //
			"THREE_PHASE, 32, 16000, THREE_PHASE, 16000", // configured maximum below hardware maximum
			"THREE_PHASE, 16, 32000, THREE_PHASE, 16000", // 11 kW variant below configured maximum
			"SINGLE_PHASE, 32, 32000, SINGLE_PHASE, 32000" //
	})
	public void testApplySetPointAbility(SingleOrThreePhase wiring, int ampereMax, int maxCurrent,
			SingleOrThreePhase expectedPhase, int expectedMax) throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, createFixtureBridge(ampereMax), createDummyClock(), config() //
				.setWiring(wiring) //
				.setMaxCurrent(maxCurrent)) //
				.next(new TestCase(), 20);

		final var abilities = sut.getChargePointAbilities();
		final var applySetPoint = (ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint();
		assertEquals(expectedPhase, applySetPoint.phase());
		assertEquals(ApplySetPoint.MIN_CURRENT, applySetPoint.min());
		assertEquals(expectedMax, applySetPoint.max());
		assertTrue(abilities.isEvConnected());
		assertTrue(abilities.isReadyForCharging());
		test.deactivate();
	}

	@Test
	public void testPhaseSwitchAbility() throws Exception {
		final var sut = new EvseGoeImpl();
		final var bridge = createFixtureBridge(32);
		final var test = prepareGoeTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20);

		// register 332 reads 2: possible switch is TO_SINGLE_PHASE within single-phase
		// limits
		var phaseSwitch = sut.getChargePointAbilities().phaseSwitch();
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, phaseSwitch.direction());
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, phaseSwitch.oppositePhaseApplySetPoint().phase());
		assertEquals(convertMilliAmpereToWatt(SingleOrThreePhase.SINGLE_PHASE, ApplySetPoint.MIN_CURRENT),
				phaseSwitch.oppositePhaseApplySetPoint().min());
		assertEquals(convertMilliAmpereToWatt(SingleOrThreePhase.SINGLE_PHASE, 32000),
				phaseSwitch.oppositePhaseApplySetPoint().max());

		// register 332 reads 1 -> single-phase, even while three phases carry current
		test.next(new TestCase().onBeforeProcessImage(() -> bridge.withRegisters(332, 1)));
		test.next(new TestCase(), 20);
		var abilities = sut.getChargePointAbilities();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, abilities.applySetPoint().phase());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, abilities.phaseSwitch().direction());

		// register 332 reads 0 (automatic mode of the charge point) -> fall back to
		// the current heuristic; here only L1 carries current
		test.next(new TestCase().onBeforeProcessImage(() -> bridge //
				.withRegisters(332, 0) //
				.withInputRegisters(116, 0, 0) //
				.withInputRegisters(118, 0, 0)));
		test.next(new TestCase(), 20);
		abilities = sut.getChargePointAbilities();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, abilities.applySetPoint().phase());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, abilities.phaseSwitch().direction());

		test.deactivate();
	}

	@Test
	public void testNoPhaseSwitchAbility() throws Exception {
		final var singlePhaseWired = new EvseGoeImpl();
		final var singlePhaseTest = prepareGoeTest(singlePhaseWired, createFixtureBridge(32), createDummyClock(),
				config().setWiring(SingleOrThreePhase.SINGLE_PHASE)) //
				.next(new TestCase(), 20);
		assertNull(singlePhaseWired.getChargePointAbilities().phaseSwitch(),
				"Single-phase wiring must not offer phase switching");
		singlePhaseTest.deactivate();

		final var withoutPhaseSwitchMode = new EvseGoeImpl();
		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(200, 1) //
				.withRegisters(211, 32) //
				.withRegisters(299, 16); // register 332 is not available
		final var withoutPhaseSwitchModeTest = prepareGoeTest(withoutPhaseSwitchMode, bridge, createDummyClock(),
				config()) //
				.next(new TestCase(), 20);
		assertNull(withoutPhaseSwitchMode.getChargePointAbilities().phaseSwitch(),
				"Phase switching must not be offered if register 332 is not readable");
		withoutPhaseSwitchModeTest.deactivate();
	}

	@Test
	public void testApplyPhaseSwitch() throws Exception {
		final var sut = new EvseGoeImpl();
		final var bridge = createFixtureBridge(32);
		final var test = prepareGoeTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20);

		// register 332 reads 2: switching to single-phase writes 1
		sut.apply(phaseSwitchActions(sut, PhaseSwitchDirection.TO_SINGLE_PHASE));
		assertEquals(1, sut.getSetPhaseSwitchModeChannel().getNextWriteValueAndReset().get());
		assertEquals(1, getWriteRegisterValue(sut, 332));

		// register 332 reads 1: switching to three-phase writes 2
		test.next(new TestCase().onBeforeProcessImage(() -> bridge.withRegisters(332, 1)));
		test.next(new TestCase(), 20);
		sut.apply(phaseSwitchActions(sut, PhaseSwitchDirection.TO_THREE_PHASE));
		assertEquals(2, sut.getSetPhaseSwitchModeChannel().getNextWriteValueAndReset().get());

		test.deactivate();
	}

	@Test
	public void testSetPointWriteInterval() throws Exception {
		final var sut = new EvseGoeImpl();
		final var clock = createDummyClock();
		final var test = prepareGoeTest(sut, createFixtureBridge(32), clock, config()) //
				.next(new TestCase(), 20);

		// first Set-Point is written
		sut.apply(actions(sut, 16000));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(16, getWriteRegisterValue(sut, 299));

		// a changed Set-Point is delayed for 5 seconds
		sut.apply(actions(sut, 20000));
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"A changed Set-Point must not be written within 5 seconds");

		clock.leap(6, SECONDS);
		sut.apply(actions(sut, 20000));
		assertEquals(20000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(20, getWriteRegisterValue(sut, 299));

		// the identical Set-Point is not written again within 30 seconds
		clock.leap(10, SECONDS);
		sut.apply(actions(sut, 20000));
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"An identical Set-Point must not be written within 30 seconds");

		// ... but after 30 seconds, e.g. to restore it after a restart of the charge
		// point
		clock.leap(21, SECONDS);
		sut.apply(actions(sut, 20000));
		assertEquals(20000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		// the Set-Point is written in whole Amperes and never above the requested
		// current: 16500 mA -> 16 A register value
		clock.leap(6, SECONDS);
		sut.apply(actions(sut, 16500));
		assertEquals(16500, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(16, getWriteRegisterValue(sut, 299));

		test.deactivate();
	}

	@Test
	public void testAllowCharging() throws Exception {
		final var sut = new EvseGoeImpl();
		final var clock = createDummyClock();
		final var bridge = createFixtureBridge(32);
		final var test = prepareGoeTest(sut, bridge, clock, config()) //
				.next(new TestCase(), 20);

		// ALLOW reads 1: a positive Set-Point writes only the current
		sut.apply(actions(sut, 16000));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertFalse(sut.getSetAllowChargingChannel().getNextWriteValue().isPresent(),
				"ALLOW must not be re-written if the charge point already allows charging");

		// stopping is applied immediately via ALLOW=0; register 299 accepts only 6..32 A
		sut.apply(actions(sut, 0));
		assertEquals(false, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());
		assertEquals(0, getWriteRegisterValue(sut, 200));
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"A zero Set-Point must not write the current register");

		// ALLOW reads 0: restarting writes ALLOW=1 and the current
		test.next(new TestCase().onBeforeProcessImage(() -> bridge.withRegisters(200, 0)));
		test.next(new TestCase(), 20);
		clock.leap(6, SECONDS);
		sut.apply(actions(sut, 16000));
		assertEquals(true, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());
		assertEquals(1, getWriteRegisterValue(sut, 200));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		test.deactivate();
	}

	@Test
	public void testStopIsRetriedIfWriteIsLost() throws Exception {
		final var sut = new EvseGoeImpl();
		final var clock = createDummyClock();
		final var test = prepareGoeTest(sut, createFixtureBridge(32), clock, config()) //
				.next(new TestCase(), 20);

		// first stop request: the write of ALLOW=0 is lost, ALLOW still reads 1
		sut.apply(actions(sut, 0));
		assertEquals(false, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());
		assertEquals(0, getWriteRegisterValue(sut, 200));

		// the next stop request retries ALLOW=0 immediately
		sut.apply(actions(sut, 0));
		assertEquals(false, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());
		assertEquals(0, getWriteRegisterValue(sut, 200));

		test.deactivate();
	}

	@Test
	public void testReadOnly() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, createFixtureBridge(32), createDummyClock(), config().setReadOnly(true)) //
				.next(new TestCase(), 20);

		assertTrue(sut.isReadOnly());

		// empty ApplySetPoint ability
		var abilities = sut.getChargePointAbilities();
		assertEquals(0, abilities.applySetPoint().min());
		assertEquals(0, abilities.applySetPoint().max());
		assertFalse(abilities.isEvConnected());
		assertFalse(abilities.isReadyForCharging());
		assertNull(abilities.phaseSwitch());

		// apply() is not executed in read-only mode: no writes are queued
		sut.apply(ChargePointActions.from(abilities) //
				.setApplySetPointInWatt(11000) //
				.build());
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"Read-only mode must not write the current register");
		assertFalse(sut.getSetAllowChargingChannel().getNextWriteValue().isPresent(),
				"Read-only mode must not write the ALLOW register");

		// reads still work
		test.next(new TestCase() //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				.output(EvseGoe.ChannelId.CAR_STATE, CarState.CHARGING));

		test.deactivate();
	}

	private static MyConfig.Builder config() {
		return MyConfig.create() //
				.setId("evseChargePoint0") //
				.setModbusId("modbus0");
	}

	private static ComponentTest prepareGoeTest(EvseGoeImpl sut, DummyModbusBridge bridge, TimeLeapClock clock,
			MyConfig.Builder config) throws Exception {
		return new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", bridge) //
				.activate(config.build());
	}

	private static DummyModbusBridge createFixtureBridge(int ampereMax) {
		return new DummyModbusBridge("modbus0") //
				// CAR_STATE (100): 2 = Charging
				.withInputRegisters(100, 2)
				// 101-104 are read as part of the block but not mapped
				.withInputRegisters(101, 0, 0, 0, 0)
				// FWV (105/106): "506" as ASCII, null-padded
				.withInputRegisters(105, 0x3530, 0x3600)
				// ERROR (107): 0 = no error
				.withInputRegisters(107, 0)
				// VOLT_L1/L2/L3 (108-113): 230, 231, 229 V
				.withInputRegisters(108, 230, 0)
				.withInputRegisters(110, 231, 0)
				.withInputRegisters(112, 229, 0)
				// AMP_L1/L2/L3 (114-119): 10.5, 10.3, 10.7 A in 0.1 A units
				.withInputRegisters(114, 105, 0)
				.withInputRegisters(116, 103, 0)
				.withInputRegisters(118, 107, 0)
				// POWER_TOTAL (120/121): 360000 in 0.01 W units = 3600 W
				.withInputRegisters(120, 360000 & 0xFFFF, 360000 >>> 16)
				// ENERGY_TOTAL (128/129): 360 in 0.1 kWh units = 36 kWh
				.withInputRegisters(128, 360, 0)
				// VOLT_N (144/145): 0 V
				.withInputRegisters(144, 0, 0)
				// POWER_L1/L2/L3 (146-151): 1200 W each in 0.1 kW units
				.withInputRegisters(146, 12, 0)
				.withInputRegisters(148, 12, 0)
				.withInputRegisters(150, 12, 0)
				// POWER_FACTOR_L1/L2/L3 (152-157): 98, 97, 99 %
				.withInputRegisters(152, 98, 0)
				.withInputRegisters(154, 97, 0)
				.withInputRegisters(156, 99, 0)
				// PHASES (205): all three phases present before/after contactor
				.withInputRegisters(205, 0b00111000)
				// SNR (304-309): "0123456789AB" as ASCII
				.withInputRegisters(304, 0x3031, 0x3233, 0x3435, 0x3637, 0x3839, 0x4142)
				// ALLOW (200): 1 = charging allowed
				.withRegisters(200, 1)
				// AMPERE_MAX (211)
				.withRegisters(211, ampereMax)
				// AMPERE_VOLATILE (299): 16 A
				.withRegisters(299, 16)
				// PHASE_SWITCH_MODE (332): 2 = three-phased
				.withRegisters(332, 2);
	}

	private static ChargePointActions actions(EvseGoeImpl sut, int setPointInMilliAmpere) {
		return ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInMilliAmpere(setPointInMilliAmpere) //
				.build();
	}

	private static ChargePointActions phaseSwitchActions(EvseGoeImpl sut, PhaseSwitchDirection direction) {
		return ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInMilliAmpere(16000) //
				.setPhaseSwitchManual(direction) //
				.build();
	}

	private static int getWriteRegisterValue(EvseGoeImpl sut, int address) throws Exception {
		ModbusProtocol protocol = getValueViaReflection(sut, "protocol");
		var task = protocol.getTaskManager().getTasks().stream() //
				.filter(t -> t instanceof FC16WriteRegistersTask && t.getStartAddress() == address) //
				.findFirst() //
				.orElseThrow();
		var registers = ((UnsignedWordElement) task.getElements()[0]).getNextWriteValueAndReset();
		assertNotNull(registers);
		return registers[0].getValue();
	}
}
