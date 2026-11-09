package io.openems.edge.controller.heatpump.cluster;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TestUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.heatpump.single.AntiCyclingState;
import io.openems.edge.controller.heatpump.single.ControllerHeatPumpSingle;
import io.openems.edge.controller.heatpump.single.ControllerHeatPumpSingleImpl;
import io.openems.edge.controller.heatpump.single.Mode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.test.DummyManagedHeatPump;

class ControllerHeatPumpClusterImplTest {

	@Test
	void surplusWithAntiCycling() throws Exception {
		final var clock = TestUtils.createDummyClock();
		final var heatPump = new DummyManagedHeatPump("heatPump0") //
				.withAbilities(new HeatPumpAbilities(new ApplyPower.Continuous(800, 3500), 1200, 600)) //
				.withOperatingState(HeatPumpOperatingState.OFF) //
				.withActivePower(0);
		final var single = new ControllerHeatPumpSingleImpl();
		new ControllerTest(single) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("heatPump", heatPump) //
				.activate(io.openems.edge.controller.heatpump.single.MyConfig.create() //
						.setId("ctrlHeatPumpSingle0") //
						.setMode(Mode.SURPLUS) //
						.build());

		final var sum = new DummySum().withGridActivePower(-3000);
		new ControllerTest(new ControllerHeatPumpClusterImpl()) //
				.addReference("sum", sum) //
				.addReference("ctrls", List.of(single)) //
				.addComponent(heatPump) //
				.activate(MyConfig.create() //
						.setId("ctrlHeatPumpCluster0") //
						.setCtrlIds("ctrlHeatPumpSingle0") //
						.build()) //
				.next(new TestCase("Surplus is detected, start delay") //
						.output(ControllerHeatPumpCluster.ChannelId.SURPLUS_POWER, 3000) //
						.onAfterControllersCallbacks(() -> {
							assertEquals(HeatPumpAction.OWN_CONTROL, heatPump.getLastAction());
							assertEquals(AntiCyclingState.START_DELAY, single
									.channel(ControllerHeatPumpSingle.ChannelId.ANTI_CYCLING_STATE).getNextValue()
									.asEnum());
						})) //
				.next(new TestCase("Power is applied after the start delay") //
						.timeleap(clock, 61, SECONDS) //
						.onAfterControllersCallbacks(() -> {
							assertEquals(new HeatPumpAction.Power(3000, false), heatPump.getLastAction());
						})) //
				.deactivate();
	}

	@Test
	void readOnlyHeatPumpIsNotControlled() throws Exception {
		final var clock = TestUtils.createDummyClock();
		final var heatPump = new DummyManagedHeatPump("heatPump0") //
				.withReadOnly(true) //
				.withAbilities(new HeatPumpAbilities(new ApplyPower.Continuous(800, 3500), 1200, 600)) //
				.withOperatingState(HeatPumpOperatingState.OFF) //
				.withActivePower(0);
		final var single = new ControllerHeatPumpSingleImpl();
		new ControllerTest(single) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("heatPump", heatPump) //
				.activate(io.openems.edge.controller.heatpump.single.MyConfig.create() //
						.setId("ctrlHeatPumpSingle0") //
						.setMode(Mode.FORCE) //
						.build());

		new ControllerTest(new ControllerHeatPumpClusterImpl()) //
				.addReference("sum", new DummySum()) //
				.addReference("ctrls", List.of(single)) //
				.addComponent(heatPump) //
				.activate(MyConfig.create() //
						.setId("ctrlHeatPumpCluster0") //
						.setCtrlIds("ctrlHeatPumpSingle0") //
						.build()) //
				.next(new TestCase("Decision is visible, but not applied") //
						.onAfterControllersCallbacks(() -> {
							assertEquals(null, heatPump.getLastAction());
							assertEquals(3500, single.channel(ControllerHeatPumpSingle.ChannelId.REQUESTED_POWER)
									.getNextValue().get());
						})) //
				.deactivate();
	}
}
