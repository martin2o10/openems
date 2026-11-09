package io.openems.edge.controller.heatpump.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

class EnergySchedulerTest {

	private static final String COMPONENT_ID = "ctrlHeatPumpSingle0";

	private static EnergyScheduleTester createTester(Mode mode, int initialStorageEnergy) {
		var config = new EnergyScheduler.Config(mode, 3500, 10_000, initialStorageEnergy, 2000, 4.0);
		var energySchedulable = new DummyEnergySchedulable<>("Controller.HeatPump.Single", COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> config));
		return EnergyScheduleTester.from(energySchedulable.getEnergyScheduleHandler());
	}

	@Test
	void storageCapacity() {
		assertEquals(11630, EnergyScheduler.calculateStorageCapacity(500, 35, 55));
		assertEquals(0, EnergyScheduler.calculateStorageCapacity(500, 55, 35));
		assertEquals(5815, EnergyScheduler.calculateStorageEnergy(500, 35, 450, 11630));
		assertEquals(0, EnergyScheduler.calculateStorageEnergy(500, 35, 300, 11630));
		assertEquals(5815, EnergyScheduler.calculateStorageEnergy(500, 35, null, 11630));
	}

	@Test
	void forceUsesMaxPower() {
		var t = createTester(Mode.FORCE, 5000);

		// 3500 W for 15 minutes
		assertEquals(875, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void ownControlCoversDemandWhenStorageIsEmpty() {
		var t = createTester(Mode.OWN_CONTROL, 0);

		// 2000 W heat demand for 15 minutes with COP 4 -> 125 Wh
		assertEquals(125, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void ownControlUsesStorageFirst() {
		var t = createTester(Mode.OWN_CONTROL, 5000);

		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void smartOffersSurplusAndForce() {
		var t = createTester(Mode.SMART, 5000);

		// Mode index 1 = FORCE
		assertEquals(875, t.simulatePeriod(1).ef().getManagedConsumption(COMPONENT_ID));
		assertTrue(t.simulatePeriod(0).ef().getManagedConsumption(COMPONENT_ID) <= 875);
	}
}
