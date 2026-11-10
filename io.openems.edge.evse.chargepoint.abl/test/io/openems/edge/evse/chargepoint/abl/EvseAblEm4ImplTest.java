package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
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
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseAblEm4ImplTest {

	@Test
	public void testActivation() throws Exception {
		final var sut = new EvseAblEm4Impl();
		prepareAblTest(sut, createFixtureBridge(0x3000, 0xC2, 320), createDummyClock(), config()) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Component activates without Modbus communication failure") //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(EvseAblEm4.ChannelId.OUTLET_STATUS, OutletStatus.C2))

				.next(new TestCase("Device information is read") //
						.output(EvseAblEm4.ChannelId.FIRMWARE_VERSION, "1.2.5") //
						.output(EvseAblEm4.ChannelId.SERIAL_NUMBER, "ABCDEF012345") //
						.output(EvseAblEm4.ChannelId.HARDWARE_MAX_CURRENT, 32000))

				.deactivate();
	}

	@Test
	public void testReadMeterValues() throws Exception {
		final var sut = new EvseAblEm4Impl();
		prepareAblTest(sut, createFixtureBridge(0x3000, 0xC2, 320), createDummyClock(), config()) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Currents are read in 0.1 A and scaled to mA") //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 10300) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 10700) //
						.output(ElectricityMeter.ChannelId.CURRENT, 31500))

				.next(new TestCase("Voltages are read in 0.1 V and scaled to mV") //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230000))

				.next(new TestCase("Power is read in W, energy in 0.01 kWh and scaled to Wh") //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 7200) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 1234560L) //
						.output(EvseAblEm4.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, 16000))

				.deactivate();

		// READ_WRITE Channel: TestCase.output() checks the write value, so the
		// readback is asserted directly
		assertEquals(16000, sut.getSetChargingCurrentChannel().value().get());
	}

	@Test
	public void testOutlet2() throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var test = prepareAblTest(sut, createFixtureBridge(0x3100, 0xB2, 320), createDummyClock(),
				config().setOutlet(Outlet.OUTLET_2)) //
				.next(new TestCase(), 20) //
				.next(new TestCase("Registers of the right outlet are read") //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(EvseAblEm4.ChannelId.OUTLET_STATUS, OutletStatus.B2) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500));

		sut.apply(actions(sut, 10000));
		assertEquals(10000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(100, getWriteRegisterValue(sut, 0x3132));

		test.deactivate();
	}

	@ParameterizedTest(name = "Outlet status {0} -> {1}")
	@CsvSource({ //
			"161, A1, false, false", // 0xA1
			"176, B0, false, true", // 0xB0
			"177, B1, false, true", // 0xB1
			"178, B2, true, true", // 0xB2
			"179, B3, true, true", // 0xB3: paused by the EV must not block a restart
			"194, C2, true, true", // 0xC2
			"226, E2, false, false", // 0xE2
			"243, ERROR, false, false", // 0xF3: all values 0xF0..0xFF are errors
			"153, UNDEFINED, false, false" // 0x99: not documented
	})
	public void testOutletStatusMapping(int outletStatusRegister, OutletStatus expectedOutletStatus,
			boolean expectedReadyForCharging, boolean expectedEvConnected) throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var bridge = createFixtureBridge(0x3000, outletStatusRegister, 320);
		final var test = prepareAblTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(EvseAblEm4.ChannelId.OUTLET_STATUS, expectedOutletStatus) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, expectedReadyForCharging));

		assertEquals(expectedEvConnected, sut.getChargePointAbilities().isEvConnected());
		test.deactivate();
	}

	@ParameterizedTest(name = "wiring={0}, Idefault={1} -> {2}, {3} mA")
	@CsvSource({ //
			"THREE_PHASE, 320, THREE_PHASE, 32000", //
			"THREE_PHASE, 160, THREE_PHASE, 16000", // installation limited to 16 A
			"SINGLE_PHASE, 320, SINGLE_PHASE, 32000" //
	})
	public void testApplySetPointAbility(SingleOrThreePhase wiring, int idefault, SingleOrThreePhase expectedPhase,
			int expectedMax) throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var test = prepareAblTest(sut, createFixtureBridge(0x3000, 0xC2, idefault), createDummyClock(),
				config().setWiring(wiring)) //
				.next(new TestCase(), 20);

		final var abilities = sut.getChargePointAbilities();
		final var applySetPoint = (ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint();
		assertEquals(expectedPhase, applySetPoint.phase());
		assertEquals(ApplySetPoint.MIN_CURRENT, applySetPoint.min());
		assertEquals(expectedMax, applySetPoint.max());
		assertTrue(abilities.isEvConnected());
		assertTrue(abilities.isReadyForCharging());
		assertNull(abilities.phaseSwitch(), "The eM4 does not support phase switching");
		test.deactivate();
	}

	@Test
	public void testMaxCurrentWithoutIdefault() throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var bridge = createOutletBridge(0x3000, 0xC2); // product registers are not available
		final var test = prepareAblTest(sut, bridge, createDummyClock(), config()) //
				.next(new TestCase(), 20);

		final var applySetPoint = sut.getChargePointAbilities().applySetPoint();
		assertEquals(ApplySetPoint.MIN_CURRENT, applySetPoint.max(),
				"Without Idefault the maximum must fall back to the minimum current");
		test.deactivate();
	}

	@Test
	public void testSetPointWriteInterval() throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var clock = createDummyClock();
		final var test = prepareAblTest(sut, createFixtureBridge(0x3000, 0xC2, 320), clock, config()) //
				.next(new TestCase(), 20);

		// first Set-Point is written in 0.1 A
		sut.apply(actions(sut, 16000));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(160, getWriteRegisterValue(sut, 0x3032));

		// a changed Set-Point is delayed for 5 seconds
		sut.apply(actions(sut, 20000));
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"A changed Set-Point must not be written within 5 seconds");

		clock.leap(6, SECONDS);
		sut.apply(actions(sut, 20000));
		assertEquals(20000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(200, getWriteRegisterValue(sut, 0x3032));

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

		// the Set-Point is never written above the requested current: 16550 mA -> 165
		clock.leap(6, SECONDS);
		sut.apply(actions(sut, 16550));
		assertEquals(16550, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(165, getWriteRegisterValue(sut, 0x3032));

		test.deactivate();
	}

	@Test
	public void testStop() throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var clock = createDummyClock();
		final var bridge = createFixtureBridge(0x3000, 0xC2, 320);
		final var test = prepareAblTest(sut, bridge, clock, config()) //
				.next(new TestCase(), 20);

		sut.apply(actions(sut, 16000));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());

		// stopping is applied immediately, even within the 5 seconds throttle
		sut.apply(actions(sut, 0));
		assertEquals(0, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(0, getWriteRegisterValue(sut, 0x3032));

		// Icmax reads 0: no further write
		test.next(new TestCase().onBeforeProcessImage(() -> bridge.withRegisters(0x3032, 0)));
		test.next(new TestCase(), 20);
		sut.apply(actions(sut, 0));
		assertFalse(sut.getSetChargingCurrentChannel().getNextWriteValue().isPresent(),
				"A stop must not be re-written if the charge point already reads 0 A");

		// restarting writes the current immediately
		sut.apply(actions(sut, 16000));
		assertEquals(16000, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(160, getWriteRegisterValue(sut, 0x3032));

		test.deactivate();
	}

	@Test
	public void testStopIsRetriedIfWriteIsLost() throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var test = prepareAblTest(sut, createFixtureBridge(0x3000, 0xC2, 320), createDummyClock(), config()) //
				.next(new TestCase(), 20);

		// first stop request: the write is lost, Icmax still reads 16 A
		sut.apply(actions(sut, 0));
		assertEquals(0, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(0, getWriteRegisterValue(sut, 0x3032));

		// the next stop request retries immediately
		sut.apply(actions(sut, 0));
		assertEquals(0, sut.getSetChargingCurrentChannel().getNextWriteValueAndReset().get());
		assertEquals(0, getWriteRegisterValue(sut, 0x3032));

		test.deactivate();
	}

	@Test
	public void testReadOnly() throws Exception {
		final var sut = new EvseAblEm4Impl();
		final var test = prepareAblTest(sut, createFixtureBridge(0x3000, 0xC2, 320), createDummyClock(),
				config().setReadOnly(true)) //
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
				"Read-only mode must not write Icmax");

		// reads still work
		test.next(new TestCase() //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				.output(EvseAblEm4.ChannelId.OUTLET_STATUS, OutletStatus.C2));

		test.deactivate();
	}

	private static MyConfig.Builder config() {
		return MyConfig.create() //
				.setId("evseChargePoint0") //
				.setModbusId("modbus0");
	}

	private static ComponentTest prepareAblTest(EvseAblEm4Impl sut, DummyModbusBridge bridge, TimeLeapClock clock,
			MyConfig.Builder config) throws Exception {
		return new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", bridge) //
				.activate(config.build());
	}

	private static DummyModbusBridge createFixtureBridge(int outletAddress, int outletStatus, int idefault) {
		return createOutletBridge(outletAddress, outletStatus) //
				// S/N (0x0110-0x011F): "ABCDEF012345" as ASCII, null-padded
				.withRegisters(0x0110, 0x4142, 0x4344, 0x4546, 0x3031, 0x3233, 0x3435, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				// FW-Revision (0x0122): 1.2.5
				.withRegisters(0x0122, 0x1205)
				// Irated (0x0123): 32 A in 0.1 A
				.withRegisters(0x0123, 320)
				// Idefault (0x0124) in 0.1 A
				.withRegisters(0x0124, idefault);
	}

	private static DummyModbusBridge createOutletBridge(int outletAddress, int outletStatus) {
		return new DummyModbusBridge("modbus0") //
				// Currents L1/L2/L3 (0x..01-0x..06): 10.5, 10.3, 10.7 A in 0.1 A
				.withRegisters(outletAddress + 0x01, 0, 105, 0, 103, 0, 107)
				// Voltages L1/L2/L3 (0x..07-0x..0C): 230.0, 231.0, 229.0 V in 0.1 V
				.withRegisters(outletAddress + 0x07, 0, 2300, 0, 2310, 0, 2290)
				// Active power (0x..0D/0x..0E): 7200 W
				.withRegisters(outletAddress + 0x0D, 0, 7200)
				// Energy (0x..0F/0x..10): 123456 in 0.01 kWh = 1234.56 kWh
				.withRegisters(outletAddress + 0x0F, 123456 >>> 16, 123456 & 0xFFFF)
				// Status outlet (0x..31)
				.withRegisters(outletAddress + 0x31, outletStatus)
				// Icmax (0x..32): 16 A in 0.1 A
				.withRegisters(outletAddress + 0x32, 160)
				// Ic (0x..33): 16 A in 0.1 A
				.withRegisters(outletAddress + 0x33, 160);
	}

	private static ChargePointActions actions(EvseAblEm4Impl sut, int setPointInMilliAmpere) {
		return ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInMilliAmpere(setPointInMilliAmpere) //
				.build();
	}

	private static int getWriteRegisterValue(EvseAblEm4Impl sut, int address) throws Exception {
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
