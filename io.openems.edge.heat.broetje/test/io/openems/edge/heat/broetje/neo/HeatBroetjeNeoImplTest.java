package io.openems.edge.heat.broetje.neo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpActionType;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;

class HeatBroetjeNeoImplTest {

	private static DummyModbusBridge createBridge() {
		return new DummyModbusBridge("modbus0") //
				.withInputRegisters(10, //
						50, 480, 350, 300, 420, // outdoor, DHW, flow, return, buffer [0.1 °C]
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 15-24
						1, 0, 0, // running, fault, 4-way valve
						0, // volume flow
						450, 42, // compressor speed [‰], COP [0.1]
						0, // fresh water temperature
						1, // grid operator release
						0, // delayed outdoor temperature
						450, // return/storage setpoint [0.1 °C]
						0, 0, 0, 0, 0, 0, // 35-40
						20) // demand: space heating
				.withInputRegisters(60, //
						0, 10, // thermal energy heating [kWh]
						0, 20, // electric energy heating [kWh]
						0, 5, // thermal energy DHWS [kWh]
						0, 6, // electric energy DHWS [kWh]
						0, 1234, // total electric energy [kWh]
						0, 1500, // electric power [W]
						0, 7, // total thermal energy [kWh]
						0, 52) // thermal power [0.1 kW]
				.withRegister(100, 1); // operating mode: automatic
	}

	@Test
	void testReadOnly() throws Exception {
		var sut = new HeatBroetjeNeoImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", createBridge()) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase("Values are read from the NEO-RKM") //
						.output(HeatBroetjeNeo.ChannelId.OUTDOOR_TEMPERATURE, 50) //
						.output(HeatBroetjeNeo.ChannelId.DOMESTIC_HOT_WATER_TEMPERATURE, 480) //
						.output(HeatBroetjeNeo.ChannelId.FLOW_TEMPERATURE, 350) //
						.output(HeatBroetjeNeo.ChannelId.RETURN_TEMPERATURE, 300) //
						.output(Heat.ChannelId.TEMPERATURE, 420) //
						.output(Heat.ChannelId.TARGET_TEMPERATURE, 450) //
						.output(HeatBroetjeNeo.ChannelId.COMPRESSOR_RUNNING, true) //
						.output(HeatBroetjeNeo.ChannelId.DEVICE_FAULT, false) //
						.output(HeatBroetjeNeo.ChannelId.DEFROST_VALVE_ACTIVE, false) //
						.output(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE, true) //
						.output(HeatBroetjeNeo.ChannelId.COMPRESSOR_SPEED, 450) //
						.output(HeatBroetjeNeo.ChannelId.COEFFICIENT_OF_PERFORMANCE, 4.2) //
						.output(HeatBroetjeNeo.ChannelId.DEMAND, Demand.SPACE_HEATING) //
						.output(HeatBroetjeNeo.ChannelId.OPERATING_MODE, OperatingMode.AUTOMATIC) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1500) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 1_234_000L) //
						.output(HeatBroetjeNeo.ChannelId.THERMAL_ENERGY_HEATING, 10_000L) //
						.output(HeatBroetjeNeo.ChannelId.ELECTRIC_ENERGY_HEATING, 20_000L) //
						.output(HeatBroetjeNeo.ChannelId.THERMAL_ENERGY_DHWS, 5_000L) //
						.output(HeatBroetjeNeo.ChannelId.ELECTRIC_ENERGY_DHWS, 6_000L) //
						.output(HeatBroetjeNeo.ChannelId.THERMAL_ENERGY_TOTAL, 7_000L) //
						.output(ManagedHeatPump.ChannelId.THERMAL_POWER, 5200)) //
				.next(new TestCase("Operating state is derived") //
						.output(ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.SPACE_HEATING)) //
				.deactivate();

		assertTrue(sut.isReadOnly());
		var abilities = sut.getHeatPumpAbilities();
		assertNull(abilities.applyPower(), "read-only: no ApplyPower ability");
		assertEquals(1200, abilities.minRunTime());
		assertEquals(600, abilities.minOffTime());
	}

	@Test
	void testSentinelIsNotWrittenToChannel() throws Exception {
		new ComponentTest(new HeatBroetjeNeoImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", createBridge().withInputRegister(10, -500)) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase("Sentinel -500 stays undefined") //
						.output(HeatBroetjeNeo.ChannelId.OUTDOOR_TEMPERATURE, null) //
						.output(HeatBroetjeNeo.ChannelId.FLOW_TEMPERATURE, 350)) //
				.deactivate();
	}

	@Test
	void testApplyDoesNotWriteInReadOnly() throws Exception {
		var sut = new HeatBroetjeNeoImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", createBridge()) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase());

		sut.apply(new HeatPumpAction.Power(2000, false));

		// ACTION channels reflect the applied action ...
		assertEquals(HeatPumpActionType.POWER, sut.channel(ManagedHeatPump.ChannelId.ACTION).getNextValue().asEnum());
		assertEquals(2000, sut.channel(ManagedHeatPump.ChannelId.ACTION_POWER).getNextValue().get());
		// ... but no Modbus write value is queued in read-only mode
		assertEquals(Optional.empty(),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT).getNextWriteValue());
		assertEquals(Optional.empty(),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE)
						.getNextWriteValue());
		assertEquals(Optional.empty(),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE)
						.getNextWriteValue());

		sut.deactivate();
	}

	@Test
	void testApplyWritesPowerSetpointWhenWritable() throws Exception {
		var clock = new TimeLeapClock(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
		var sut = new HeatBroetjeNeoImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", createBridge().withClock(clock)) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.build()) //
				.next(new TestCase());

		var abilities = sut.getHeatPumpAbilities();
		assertEquals(new ApplyPower.Continuous(800, 3500), abilities.applyPower());
		assertEquals(1200, abilities.minRunTime());
		assertEquals(600, abilities.minOffTime());

		// first cycle already wrote the OwnControl fallback; leap past the
		// write interval so the next apply() is not rate-limited
		clock.leap(10, ChronoUnit.SECONDS);
		sut.apply(new HeatPumpAction.Power(2000, false));
		assertEquals(Optional.of(2000),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT).getNextWriteValue());

		clock.leap(10, ChronoUnit.SECONDS);
		sut.apply(HeatPumpAction.LOCK);
		assertEquals(Optional.of(0),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE)
						.getNextWriteValue());
		assertEquals(Optional.of(1),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE)
						.getNextWriteValue());

		sut.deactivate();
	}

	@Test
	void testApplyLockThenPowerReleasesLock() throws Exception {
		var clock = new TimeLeapClock(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
		var sut = new HeatBroetjeNeoImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", createBridge().withClock(clock)) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.build()) //
				.next(new TestCase());

		// Lock: registers 149 = 0, 150 = 1
		clock.leap(10, ChronoUnit.SECONDS);
		sut.apply(HeatPumpAction.LOCK);
		assertEquals(Optional.of(0),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE)
						.getNextWriteValue());
		assertEquals(Optional.of(1),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE)
						.getNextWriteValue());

		// Power after Lock must explicitly release the lock: (power, 1, 0)
		clock.leap(10, ChronoUnit.SECONDS);
		sut.apply(new HeatPumpAction.Power(2000, false));
		assertEquals(Optional.of(2000),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT).getNextWriteValue());
		assertEquals(Optional.of(1),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE)
						.getNextWriteValue());
		assertEquals(Optional.of(0),
				sut.<IntegerWriteChannel>channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE)
						.getNextWriteValue());

		sut.deactivate();
	}
}
