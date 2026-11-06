package io.openems.edge.heat.broetje.neo;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpControlCapability;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;

class HeatBroetjeNeoImplTest {

	private static DummyModbusBridge createBridge() {
		return new DummyModbusBridge("modbus0") //
				.withInputRegisters(10, //
						50, 480, 350, 300, 420, // outdoor, DHW, flow, return, buffer [0.1 °C]
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 15-24
						1, 0, 0, // running, fault, 4-way valve
						0, // flow rate
						450, 42, // compressor speed [‰], COP [0.1]
						0, // fresh water temperature
						1, // grid operator release
						0, // delayed outdoor temperature
						450, // return/storage setpoint [0.1 °C]
						0, 0, 0, 0, 0, 0, // 35-40
						20) // demand: space heating
				.withInputRegisters(68, //
						0, 1234, // total electric energy [kWh]
						0, 1500, // electric power [W]
						0, 0, // total thermal energy
						0, 52) // thermal power [0.1 kW]
				.withRegister(100, 1); // operating mode: automatic
	}

	@Test
	void testReadOnly() throws Exception {
		new ComponentTest(new HeatBroetjeNeoImpl()) //
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
						.output(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE, true) //
						.output(HeatBroetjeNeo.ChannelId.COMPRESSOR_SPEED, 450) //
						.output(HeatBroetjeNeo.ChannelId.DEMAND, Demand.SPACE_HEATING) //
						.output(HeatBroetjeNeo.ChannelId.OPERATING_MODE, OperatingMode.AUTOMATIC) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1500) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 1_234_000L) //
						.output(ManagedHeatPump.ChannelId.THERMAL_POWER, 5200) //
						.output(ManagedHeatPump.ChannelId.CONTROL_CAPABILITY, HeatPumpControlCapability.UNDEFINED) //
						.output(ManagedHeatPump.ChannelId.MIN_ACTIVE_POWER, 800) //
						.output(ManagedHeatPump.ChannelId.MAX_ACTIVE_POWER, 3500)) //
				.next(new TestCase("Operating state is derived") //
						.output(ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.SPACE_HEATING)) //
				.deactivate();
	}

	@Test
	void testWrite() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
		new ComponentTest(new HeatBroetjeNeoImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", createBridge().withClock(clock)) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.build()) //
				.next(new TestCase("Setpoints are taken over") //
						.input(ManagedHeatPump.ChannelId.TARGET_ACTIVE_POWER, 2000) //
						.output(ManagedHeatPump.ChannelId.CONTROL_CAPABILITY, HeatPumpControlCapability.CONTINUOUS) //
						.output(ManagedHeatPump.ChannelId.DEBUG_TARGET_ACTIVE_POWER, 2000)) //
				.deactivate();
	}
}
