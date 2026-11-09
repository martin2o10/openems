package io.openems.edge.simulator.heatpump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.openems.edge.heat.api.HeatPumpOperatingState;

class HeatPumpModelTest {

	private static final Duration MINUTE = Duration.ofMinutes(1);

	private static HeatPumpModel createModel(double initialStorageTemperature) {
		return new HeatPumpModel(new HeatPumpModel.Parameters(//
				1200, 4500, // modulation range [W]
				1200, 600, // min run time, min off time [s]
				1000, // storage volume [l]
				40, 5, 55, // target, hysteresis, max storage temperature
				5, 20, // outdoor, room temperature
				200, // heat loss coefficient [W/K]
				0.45), // Carnot efficiency
				initialStorageTemperature);
	}

	private static void run(HeatPumpModel model, int minutes, Integer target, Integer limit) {
		for (var i = 0; i < minutes; i++) {
			model.step(MINUTE, target, limit);
		}
	}

	@Test
	void ownControlStartsBelowHysteresis() {
		var sut = createModel(34);

		sut.step(MINUTE, null, null);

		assertEquals(HeatPumpOperatingState.SPACE_HEATING, sut.getOperatingState());
		assertTrue(sut.getElectricPower() >= 1200);
		assertTrue(sut.getThermalPower() > sut.getElectricPower());
		assertEquals(1, sut.getStarts());
	}

	@Test
	void ownControlKeepsStorageInBand() {
		var sut = createModel(34);

		run(sut, 60, null, null);
		for (var i = 0; i < 6 * 60; i++) {
			sut.step(MINUTE, null, null);
			assertTrue(sut.getStorageTemperature() > 34.5, "too cold: " + sut.getStorageTemperature());
			assertTrue(sut.getStorageTemperature() < 40.5, "too hot: " + sut.getStorageTemperature());
		}
		assertTrue(sut.getStarts() >= 2);
	}

	@Test
	void ownControlStaysOffWithinBand() {
		var sut = createModel(38);

		sut.step(MINUTE, null, null);

		assertEquals(HeatPumpOperatingState.OFF, sut.getOperatingState());
		assertEquals(0, sut.getElectricPower());
	}

	@Test
	void targetActivePowerIsFollowed() {
		var sut = createModel(38);

		sut.step(MINUTE, 3000, null);

		assertEquals(3000, sut.getElectricPower());
		assertEquals(HeatPumpOperatingState.SPACE_HEATING, sut.getOperatingState());
	}

	@Test
	void targetActivePowerIsCappedAtMaxPower() {
		var sut = createModel(38);

		sut.step(MINUTE, 9000, null);

		assertEquals(4500, sut.getElectricPower());
	}

	@Test
	void targetBelowMinPowerDoesNotStart() {
		var sut = createModel(38);

		sut.step(MINUTE, 500, null);

		assertEquals(0, sut.getElectricPower());
	}

	@Test
	void comfortHasPrecedenceOverLowTarget() {
		var sut = createModel(34);

		sut.step(MINUTE, 0, null);

		assertTrue(sut.getElectricPower() >= 1200);
	}

	@Test
	void activePowerLimitIsHard() {
		var sut = createModel(34);

		sut.step(MINUTE, 4000, 2000);
		assertEquals(2000, sut.getElectricPower());

		sut.step(MINUTE, 4000, 1000);
		assertEquals(0, sut.getElectricPower());
	}

	@Test
	void minRunTimeKeepsCompressorRunning() {
		// Above target temperature: no comfort demand of the own control
		var sut = createModel(41);

		sut.step(MINUTE, 3000, null);
		run(sut, 10, 0, null);

		assertEquals(1200, sut.getElectricPower());

		run(sut, 10, 0, null);

		assertEquals(0, sut.getElectricPower());
	}

	@Test
	void minOffTimeBlocksRestart() {
		var sut = createModel(41);

		run(sut, 20, 3000, null);
		sut.step(MINUTE, 0, null);
		assertEquals(0, sut.getElectricPower());

		sut.step(MINUTE, 3000, null);
		assertEquals(0, sut.getElectricPower());
		assertEquals(HeatPumpOperatingState.BLOCKED, sut.getOperatingState());

		run(sut, 10, 3000, null);
		assertEquals(3000, sut.getElectricPower());
		assertEquals(2, sut.getStarts());
	}

	@Test
	void maxStorageTemperatureStopsHeating() {
		var sut = createModel(55);

		sut.step(MINUTE, 4000, null);

		assertEquals(0, sut.getElectricPower());
	}

	@Test
	void copDecreasesWithStorageTemperature() {
		var cold = createModel(30);
		var hot = createModel(50);

		cold.step(MINUTE, 3000, null);
		hot.step(MINUTE, 3000, null);

		assertTrue(cold.getCop() > hot.getCop());
	}
}
