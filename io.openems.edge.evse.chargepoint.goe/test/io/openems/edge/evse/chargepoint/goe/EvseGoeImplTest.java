package io.openems.edge.evse.chargepoint.goe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseGoeImplTest {

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseGoe} and realistic Modbus
	 * register values of a go-e Gemini charging a three-phased EV.
	 *
	 * @param sut      the system under test
	 * @param readOnly true if the charge point is configured read-only
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareGoeTest(EvseGoeImpl sut, boolean readOnly) throws Exception {
		return prepareGoeTest(sut, readOnly, SingleOrThreePhase.THREE_PHASE);
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseGoe} and realistic Modbus
	 * register values of a go-e Gemini charging a three-phased EV.
	 *
	 * @param sut      the system under test
	 * @param readOnly true if the charge point is configured read-only
	 * @param wiring   the configured {@link SingleOrThreePhase} wiring
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareGoeTest(EvseGoeImpl sut, boolean readOnly, SingleOrThreePhase wiring)
			throws Exception {
		return prepareGoeTest(sut, readOnly, wiring, 32, 32_000);
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseGoe} and realistic Modbus
	 * register values of a go-e Gemini charging a three-phased EV.
	 *
	 * @param sut        the system under test
	 * @param readOnly   true if the charge point is configured read-only
	 * @param wiring     the configured {@link SingleOrThreePhase} wiring
	 * @param ampereMax  the hardware maximum current in [A] (register 211)
	 * @param maxCurrent the configured maximum current in [mA]
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareGoeTest(EvseGoeImpl sut, boolean readOnly, SingleOrThreePhase wiring,
			int ampereMax, int maxCurrent) throws Exception {
		return new ComponentTest(sut) //
				.addReference("setModbus", createFixtureBridge(ampereMax)) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setWiring(wiring) //
						.setMaxCurrent(maxCurrent) //
						.setReadOnly(readOnly) //
						.build());
	}

	/**
	 * Creates a {@link DummyModbusBridge} with realistic register values of a
	 * go-e Gemini charging a three-phased EV.
	 *
	 * @param ampereMax the hardware maximum current in [A] (register 211)
	 * @return the DummyModbusBridge
	 */
	private static DummyModbusBridge createFixtureBridge(int ampereMax) {
		return new DummyModbusBridge("modbus0") //
						// CAR_STATE (100): 2 = Charging
						.withInputRegisters(100, 2)
						// 101-106 are read as part of the block but not mapped
						.withInputRegisters(101, 0, 0, 0, 0, 0, 0)
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
						// 130-131 are read as part of the block but not mapped
						.withInputRegisters(130, 0, 0)
						// ENERGY_CHARGE (132/133): 360000 Deka-Watt-seconds = 1000 Wh
						.withInputRegisters(132, 360000 & 0xFFFF, 360000 >>> 16)
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

	/**
	 * Builds {@link ChargePointActions} applying the given Set-Point in [mA].
	 *
	 * @param sut               the system under test
	 * @param setPointInMilliAmpere the Set-Point in [mA]
	 * @return the {@link ChargePointActions}
	 */
	private static ChargePointActions actions(EvseGoeImpl sut, int setPointInMilliAmpere) {
		return ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInMilliAmpere(setPointInMilliAmpere) //
				.build();
	}

	@Test
	public void testActivation() throws Exception {
		final var sut = new EvseGoeImpl();
		prepareGoeTest(sut, false) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Component activates without Modbus communication failure") //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.CHARGING) //
						.output(EvseGoe.ChannelId.ERROR, ErrorCode.NO_ERROR))

				.next(new TestCase("Read-once registers are filled and their Tasks removed") //
						.output(EvseGoe.ChannelId.FWV, "506") //
						.output(EvseGoe.ChannelId.SNR, "0123456789AB") //
						.output(EvseGoe.ChannelId.HARDWARE_MAX_CURRENT, 32.f))

				.deactivate();
	}

	@Test
	public void testReadMeterValues() throws Exception {
		final var sut = new EvseGoeImpl();
		prepareGoeTest(sut, false) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Voltages are read in V and scaled to mV") //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230000))

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

				.next(new TestCase("Energies are read in 0.1 kWh units and scaled to Wh") //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 36000L) //
						.output(EvseGoe.ChannelId.ENERGY_SESSION, 1000))

				.next(new TestCase("go-e specific values") //
						.output(EvseGoe.ChannelId.VOLTAGE_N, 0.f) //
						.output(EvseGoe.ChannelId.POWER_FACTOR_L1, 98) //
						.output(EvseGoe.ChannelId.POWER_FACTOR_L2, 97) //
						.output(EvseGoe.ChannelId.POWER_FACTOR_L3, 99) //
						.output(EvseGoe.ChannelId.PHASES, 0b00111000) //
						.output(EvseGoe.ChannelId.PHASE_SWITCH_MODE, 2))

				.deactivate();

		// readbacks of the READ_WRITE channels: register 299 reads 16 A,
		// register 200 reads 1 = allowed
		assertEquals(16000, sut.getSetChargingCurrentChannel().value().get());
		assertEquals(true, sut.getSetAllowChargingChannel().value().get());
	}

	@Test
	public void testCarStateMapping() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, false);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		// 2 = Charging
		test.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.CHARGING) //
						.output(EvseGoe.ChannelId.IS_EV_CONNECTED, true) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true));

		// 1 = Ready, no EV connected - NOT considered as EV connected
		test.next(new TestCase("Change to 1 = Ready, no EV connected") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(100, 1)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.READY_NO_EV) //
						.output(EvseGoe.ChannelId.IS_EV_CONNECTED, false) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		// 0 = Defective
		test.next(new TestCase("Change to 0 = Defective") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(100, 0)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.DEFECT) //
						.output(EvseGoe.ChannelId.IS_EV_CONNECTED, false) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		// 3 = Waiting for vehicle
		test.next(new TestCase("Change to 3 = Waiting for vehicle") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(100, 3)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.WAITING_FOR_VEHICLE) //
						.output(EvseGoe.ChannelId.IS_EV_CONNECTED, true) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true));

		// 4 = Charging finished, EV connected - connected, but not ready for charging
		test.next(new TestCase("Change to 4 = Charging finished, EV connected") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(100, 4)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.FINISHED_EV_CONNECTED) //
						.output(EvseGoe.ChannelId.IS_EV_CONNECTED, true) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		// unknown values are not guessed
		test.next(new TestCase("Change to an unknown value") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(100, 99)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.CAR_STATE, CarState.UNDEFINED) //
						.output(EvseGoe.ChannelId.IS_EV_CONNECTED, false) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		test.deactivate();
	}

	@Test
	public void testErrorMapping() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, false);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		test.next(new TestCase(), 20);

		test.next(new TestCase("Change to 8 = NO_GROUND") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(107, 8)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.ERROR, ErrorCode.NO_GROUND));

		// Unknown values map to INTERNAL per go-e Modbus API v2
		test.next(new TestCase("Change to an unknown value") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(107, 4711)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.ERROR, ErrorCode.INTERNAL));

		test.deactivate();
	}

	@Test
	public void testEnergySessionConverter() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, false);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		// 360000 Deka-Watt-seconds = 1000 Wh
		test.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.ENERGY_SESSION, 1000));

		// 100000 Deka-Watt-seconds = 277 Wh (example from the go-e Modbus API v2)
		test.next(new TestCase("Change to 100000 Deka-Watt-seconds") //
				.onBeforeProcessImage(() -> bridge //
						.withInputRegisters(132, 100000 & 0xFFFF, 100000 >>> 16)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.ENERGY_SESSION, 277));

		// 0 Deka-Watt-seconds = 0 Wh
		test.next(new TestCase("Change to 0 Deka-Watt-seconds") //
				.onBeforeProcessImage(() -> bridge.withInputRegisters(132, 0, 0)));
		test.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseGoe.ChannelId.ENERGY_SESSION, 0));

		test.deactivate();
	}

	@Test
	public void testChargePointAbilities() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, false) //
				.next(new TestCase(), 20);

		// all three phases carry current and AMPERE_MAX (211) reads 32 A
		var abilities = sut.getChargePointAbilities();
		var applySetPoint = (ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint();
		assertEquals(SingleOrThreePhase.THREE_PHASE, applySetPoint.phase());
		assertEquals(ApplySetPoint.MIN_CURRENT, applySetPoint.min());
		assertEquals(32000, applySetPoint.max());
		assertTrue(abilities.isEvConnected());
		assertTrue(abilities.isReadyForCharging());
		// phase switching is not supported
		assertNull(abilities.phaseSwitch());

		// the configured maximum current limits the ability, here below the
		// hardware maximum of 32 A
		final var sut2 = new EvseGoeImpl();
		final var test2 = prepareGoeTest(sut2, false, SingleOrThreePhase.THREE_PHASE, 32, 16000) //
				.next(new TestCase(), 20);
		abilities = sut2.getChargePointAbilities();
		applySetPoint = (ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint();
		assertEquals(16000, applySetPoint.max());

		// the hardware maximum current limits the configured value: AMPERE_MAX
		// (211) reads 16 A, like the 11 kW variant of the go-e Gemini
		final var sut4 = new EvseGoeImpl();
		final var test4 = prepareGoeTest(sut4, false, SingleOrThreePhase.THREE_PHASE, 16, 32_000) //
				.next(new TestCase(), 20);
		abilities = sut4.getChargePointAbilities();
		applySetPoint = (ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint();
		assertEquals(16000, applySetPoint.max());

		// single-phase wiring
		final var sut3 = new EvseGoeImpl();
		final var test3 = prepareGoeTest(sut3, false, SingleOrThreePhase.SINGLE_PHASE) //
				.next(new TestCase(), 20);
		abilities = sut3.getChargePointAbilities();
		applySetPoint = (ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, applySetPoint.phase());

		test2.deactivate();
		test3.deactivate();
		test4.deactivate();
	}

	@Test
	public void testWriteSetPointOnChange() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, false) //
				.next(new TestCase(), 20);

		// first Set-Point is queued for writing
		sut.apply(actions(sut, 16000));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		// the identical Set-Point is not queued again (write-on-change)
		sut.apply(actions(sut, 16000));
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"Identical Set-Point must not be queued again");

		// a changed Set-Point is queued again
		sut.apply(actions(sut, 20000));
		assertEquals(20000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		// the Set-Point is written in whole Amperes: 16500 mA -> 17 A register value
		sut.apply(actions(sut, 16500));
		assertEquals(16500, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		test.deactivate();
	}

	@Test
	public void testEnableOnCharge() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, false) //
				.next(new TestCase(), 20);

		// transition 0 -> >0 writes ALLOW=1 exactly once
		sut.apply(actions(sut, 16000));
		assertEquals(true, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());

		// a changed Set-Point while charging does not re-write ALLOW; consume
		// the queued current write to simulate the executed Modbus write
		sut.apply(actions(sut, 20000));
		assertEquals(20000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertFalse(sut.getSetAllowChargingChannel().getNextWriteValue().isPresent(),
				"ALLOW must not be re-written while the charging session continues");

		// returning to 0 writes ALLOW=0; the current register is not written at
		// all, because it accepts only 6..32 A
		sut.apply(actions(sut, 0));
		assertEquals(false, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"A zero Set-Point must not write the current register");

		// restarting the charging session writes ALLOW=1 and the current again
		sut.apply(actions(sut, 16000));
		assertEquals(true, sut.getSetAllowChargingChannel().getNextWriteValueAndReset().get());
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		test.deactivate();
	}

	@Test
	public void testReadOnly() throws Exception {
		final var sut = new EvseGoeImpl();
		final var test = prepareGoeTest(sut, true) //
				.next(new TestCase(), 20);

		assertTrue(sut.isReadOnly());

		// empty ApplySetPoint ability
		var abilities = sut.getChargePointAbilities();
		assertEquals(0, abilities.applySetPoint().min());
		assertEquals(0, abilities.applySetPoint().max());
		assertFalse(abilities.isEvConnected());
		assertFalse(abilities.isReadyForCharging());

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
}
