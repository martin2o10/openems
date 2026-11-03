package io.openems.edge.simulator.heatpump;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpControlCapability;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;

class SimulatorHeatPumpImplTest {

	@Test
	void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
		new ComponentTest(new SimulatorHeatPumpImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setInitialStorageTemperature(380) //
						.build()) //
				.next(new TestCase("Device properties and initial state") //
						.input(ManagedHeatPump.ChannelId.TARGET_ACTIVE_POWER, 3000) //
						.output(ManagedHeatPump.ChannelId.MIN_ACTIVE_POWER, 1200) //
						.output(ManagedHeatPump.ChannelId.MAX_ACTIVE_POWER, 4500) //
						.output(ManagedHeatPump.ChannelId.MIN_RUN_TIME, 1200) //
						.output(ManagedHeatPump.ChannelId.MIN_OFF_TIME, 600) //
						.output(ManagedHeatPump.ChannelId.CONTROL_CAPABILITY, HeatPumpControlCapability.CONTINUOUS) //
						.output(Heat.ChannelId.TARGET_TEMPERATURE, 400) //
						.output(Heat.ChannelId.TEMPERATURE, 380) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(ManagedHeatPump.ChannelId.TARGET_ACTIVE_POWER, 3000) //
						.output(ManagedHeatPump.ChannelId.DEBUG_TARGET_ACTIVE_POWER, 3000)) //
				.next(new TestCase("Follows the target active power") //
						.timeleap(clock, 1, MINUTES) //
						.input(ManagedHeatPump.ChannelId.TARGET_ACTIVE_POWER, 3000) //
						.input(ManagedHeatPump.ChannelId.ACTIVE_POWER_LIMIT, 1000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1000) //
						.output(ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.SPACE_HEATING)) //
				.next(new TestCase("Active power limit below min power stops the compressor") //
						.timeleap(clock, 1, MINUTES) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(ManagedHeatPump.ChannelId.THERMAL_POWER, 0) //
						.output(ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.OFF) //
						.output(SimulatorHeatPump.ChannelId.STARTS, 1)) //
				.deactivate();
	}
}
