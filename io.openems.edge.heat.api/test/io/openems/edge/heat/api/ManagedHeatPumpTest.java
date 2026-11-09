package io.openems.edge.heat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.test.DummyManagedHeatPump;

class ManagedHeatPumpTest {

	@Test
	void applyStoresActionInChannels() {
		var sut = new DummyManagedHeatPump("heatPump0");

		sut.apply(new HeatPumpAction.Power(2500, false));
		assertEquals(HeatPumpActionType.POWER, sut.channel(ManagedHeatPump.ChannelId.ACTION).getNextValue().asEnum());
		assertEquals(2500, sut.channel(ManagedHeatPump.ChannelId.ACTION_POWER).getNextValue().get());

		sut.apply(HeatPumpAction.LOCK);
		assertEquals(HeatPumpActionType.LOCK, sut.channel(ManagedHeatPump.ChannelId.ACTION).getNextValue().asEnum());
		assertNull(sut.channel(ManagedHeatPump.ChannelId.ACTION_POWER).getNextValue().get());
	}

	@Test
	void meterTypeDependsOnReadOnly() {
		assertEquals(MeterType.MANAGED_CONSUMPTION_METERED, new DummyManagedHeatPump("heatPump0").getMeterType());
		assertEquals(MeterType.CONSUMPTION_METERED,
				new DummyManagedHeatPump("heatPump0").withReadOnly(true).getMeterType());
	}

	@Test
	void abilities() {
		var sut = new HeatPumpAbilities(new ApplyPower.Continuous(800, 3500), 1200, 600, true);
		assertTrue(sut.canApplyPower());
		assertFalse(new HeatPumpAbilities(null, 0, 0, false).canApplyPower());
	}

	@Test
	void continuousFit() {
		var sut = new ApplyPower.Continuous(800, 3500);
		assertEquals(0, sut.fit(500));
		assertEquals(800, sut.fit(800));
		assertEquals(2000, sut.fit(2000));
		assertEquals(3500, sut.fit(9000));
	}

	@Test
	void sgReadyFit() {
		var sut = new ApplyPower.SgReady(1500, 3000);
		assertEquals(1500, sut.minPower());
		assertEquals(3000, sut.maxPower());
		assertEquals(0, sut.fit(1000));
		assertEquals(1500, sut.fit(5000));
	}

	@Test
	void onOffFit() {
		var sut = new ApplyPower.OnOff(2000);
		assertEquals(0, sut.fit(1999));
		assertEquals(2000, sut.fit(2000));
	}

	@Test
	void actionTypes() {
		assertEquals(HeatPumpActionType.OWN_CONTROL, HeatPumpAction.OWN_CONTROL.type());
		assertEquals(HeatPumpActionType.FORCE_POWER, new HeatPumpAction.Power(3000, true).type());
	}

	@Test
	void operatingStateIsRunning() {
		assertTrue(HeatPumpOperatingState.SPACE_HEATING.isRunning());
		assertTrue(HeatPumpOperatingState.DEFROST.isRunning());
		assertFalse(HeatPumpOperatingState.OFF.isRunning());
		assertFalse(HeatPumpOperatingState.BLOCKED.isRunning());
	}
}
