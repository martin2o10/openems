package io.openems.edge.simulator.heatpump;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;

class SimulatorHeatPumpImplTest {

	@Test
	void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
		final var sut = new SimulatorHeatPumpImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setInitialStorageTemperature(380) //
						.build()) //
				.next(new TestCase("Device properties and initial state") //
						.onBeforeControllersCallbacks(() -> {
							var abilities = sut.getHeatPumpAbilities();
							assertEquals(new ApplyPower.Continuous(1200, 4500), abilities.applyPower());
							assertEquals(1200, abilities.minRunTime());
							assertEquals(600, abilities.minOffTime());
						}) //
						.onAfterControllersCallbacks(() -> sut.apply(new HeatPumpAction.Power(3000, false))) //
						.output(Heat.ChannelId.TARGET_TEMPERATURE, 400) //
						.output(Heat.ChannelId.TEMPERATURE, 380) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0)) //
				.next(new TestCase("Follows the requested power") //
						.timeleap(clock, 1, MINUTES) //
						.onAfterControllersCallbacks(() -> sut.apply(HeatPumpAction.LOCK)) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1000) //
						.output(ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.SPACE_HEATING)) //
				.next(new TestCase("Lock stops the compressor") //
						.timeleap(clock, 1, MINUTES) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(ManagedHeatPump.ChannelId.THERMAL_POWER, 0) //
						.output(ManagedHeatPump.ChannelId.OPERATING_STATE, HeatPumpOperatingState.OFF) //
						.output(SimulatorHeatPump.ChannelId.STARTS, 1)) //
				.deactivate();
	}
}
