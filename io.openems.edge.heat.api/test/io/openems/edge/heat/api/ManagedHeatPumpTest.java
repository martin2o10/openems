package io.openems.edge.heat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.heat.test.DummyManagedHeatPump;

class ManagedHeatPumpTest {

	@Test
	void setActivePowerLimitMirrorsToDebugChannel() throws OpenemsNamedException {
		var sut = new DummyManagedHeatPump("heatPump0");

		sut.setActivePowerLimit(4200);

		assertEquals(4200, sut.getActivePowerLimitChannel().getNextWriteValue().get());
		assertEquals(4200, sut.getDebugActivePowerLimitChannel().getNextValue().get());
	}

	@Test
	void setTargetActivePowerMirrorsToDebugChannel() throws OpenemsNamedException {
		var sut = new DummyManagedHeatPump("heatPump0");

		sut.setTargetActivePower(2500);

		assertEquals(2500, sut.getTargetActivePowerChannel().getNextWriteValue().get());
		assertEquals(2500, sut.getDebugTargetActivePowerChannel().getNextValue().get());
	}

	@Test
	void nullSetpointsMeanNoLimitAndNoPreference() throws OpenemsNamedException {
		var sut = new DummyManagedHeatPump("heatPump0");

		sut.setActivePowerLimit(null);
		sut.setTargetActivePower(null);

		assertFalse(sut.getActivePowerLimitChannel().getNextWriteValue().isPresent());
		assertFalse(sut.getTargetActivePowerChannel().getNextWriteValue().isPresent());
	}

	@Test
	void setTargetTemperatureOffsetMirrorsToDebugChannel() throws OpenemsNamedException {
		var sut = new DummyManagedHeatPump("heatPump0") //
				.withControlCapability(HeatPumpControlCapability.TEMPERATURE_OFFSET) //
				.withMaxTemperatureOffset(50);

		sut.setTargetTemperatureOffset(30);

		assertEquals(30, sut.getTargetTemperatureOffsetChannel().getNextWriteValue().get());
		assertEquals(30, sut.getDebugTargetTemperatureOffsetChannel().getNextValue().get());
		assertEquals(50, sut.getMaxTemperatureOffset().get());
		assertEquals(HeatPumpControlCapability.TEMPERATURE_OFFSET, sut.getControlCapability());
	}

	@Test
	void dummyProvidesDeviceProperties() {
		var sut = new DummyManagedHeatPump("heatPump0") //
				.withModulationRange(1200, 4500) //
				.withMinTimes(1200, 600) //
				.withControlCapability(HeatPumpControlCapability.CONTINUOUS) //
				.withOperatingState(HeatPumpOperatingState.SPACE_HEATING) //
				.withFlowTemperature(352);

		assertEquals(1200, sut.getMinActivePower().get());
		assertEquals(4500, sut.getMaxActivePower().get());
		assertEquals(1200, sut.getMinRunTime().get());
		assertEquals(600, sut.getMinOffTime().get());
		assertEquals(HeatPumpControlCapability.CONTINUOUS, sut.getControlCapability());
		assertEquals(HeatPumpOperatingState.SPACE_HEATING, sut.getOperatingState());
		assertEquals(352, sut.getFlowTemperature().get());
	}

	@Test
	void undefinedValuesByDefault() {
		var sut = new DummyManagedHeatPump("heatPump0");

		assertFalse(sut.getMinActivePower().isDefined());
		assertEquals(HeatPumpControlCapability.UNDEFINED, sut.getControlCapability());
		assertEquals(HeatPumpOperatingState.UNDEFINED, sut.getOperatingState());
	}
}
