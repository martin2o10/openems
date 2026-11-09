package io.openems.edge.heat.broetje.neo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

class SetpointHandlerTest {

	private static final Instant START = Instant.parse("2026-11-06T12:00:00Z");

	private static SetpointHandler createHandler() {
		return new SetpointHandler(800, 3500, Duration.ofSeconds(10));
	}

	private static HeatPumpAction power(int power) {
		return new HeatPumpAction.Power(power, false);
	}

	@Test
	void powerIsWrittenAsPowerSetpoint() {
		var result = createHandler().calculate(power(2000));

		assertEquals(2000, result.powerSetpoint());
		assertFalse(result.isLocked());
		assertEquals(0, result.gridOperatorReleaseActivate());
	}

	@Test
	void ownControlWritesZeroAndReleases() {
		var result = createHandler().calculate(HeatPumpAction.OWN_CONTROL);

		assertEquals(0, result.powerSetpoint());
		assertFalse(result.isLocked());
		assertEquals(0, result.gridOperatorReleaseActivate());
		assertEquals(result, createHandler().calculate(null));
	}

	@Test
	void powerIsCappedAtMaxPower() {
		assertEquals(3500, createHandler().calculate(power(9000)).powerSetpoint());
	}

	@Test
	void powerBelowMinPowerIsZero() {
		assertEquals(0, createHandler().calculate(power(500)).powerSetpoint());
		assertEquals(0, createHandler().calculate(power(-1000)).powerSetpoint());
	}

	@Test
	void lockLocksHeatPump() {
		var result = createHandler().calculate(HeatPumpAction.LOCK);

		assertTrue(result.isLocked());
		assertEquals(0, result.powerSetpoint());
		assertEquals(0, result.gridOperatorReleaseValue());
		assertEquals(1, result.gridOperatorReleaseActivate());
	}

	@Test
	void writesAreRateLimited() {
		var sut = createHandler();

		assertNotNull(sut.apply(START, power(2000)));
		assertNull(sut.apply(START.plusSeconds(5), power(3000)));
		assertNull(sut.apply(START.plusSeconds(9), power(3000)));
		assertEquals(3000, sut.apply(START.plusSeconds(10), power(3000)).powerSetpoint());
	}

	@Test
	void writeIntervalHasLowerBound() {
		var sut = new SetpointHandler(800, 3500, Duration.ofSeconds(1));

		assertEquals(SetpointHandler.MIN_WRITE_INTERVAL, sut.getWriteInterval());
	}
}
