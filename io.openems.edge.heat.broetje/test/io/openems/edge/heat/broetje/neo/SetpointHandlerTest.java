package io.openems.edge.heat.broetje.neo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class SetpointHandlerTest {

	private static final Instant START = Instant.parse("2026-11-06T12:00:00Z");

	private static SetpointHandler createHandler() {
		return new SetpointHandler(800, 3500, Duration.ofSeconds(10));
	}

	@Test
	void targetIsWrittenAsPowerSetpoint() {
		var result = createHandler().calculate(2000, null);

		assertEquals(2000, result.powerSetpoint());
		assertFalse(result.isLocked());
		assertEquals(0, result.gridOperatorReleaseActivate());
	}

	@Test
	void noTargetDoesNotWritePowerSetpoint() {
		var result = createHandler().calculate(null, null);

		assertNull(result.powerSetpoint());
		assertFalse(result.isLocked());
	}

	@Test
	void targetIsCappedAtMaxPowerAndLimit() {
		var sut = createHandler();

		assertEquals(3500, sut.calculate(9000, null).powerSetpoint());
		assertEquals(1500, sut.calculate(3000, 1500).powerSetpoint());
	}

	@Test
	void targetBelowMinPowerIsZero() {
		assertEquals(0, createHandler().calculate(500, null).powerSetpoint());
		assertEquals(0, createHandler().calculate(-1000, null).powerSetpoint());
	}

	@Test
	void limitBelowMinPowerLocksHeatPump() {
		var result = createHandler().calculate(3000, 0);

		assertTrue(result.isLocked());
		assertEquals(0, result.powerSetpoint());
		assertEquals(0, result.gridOperatorReleaseValue());
		assertEquals(1, result.gridOperatorReleaseActivate());
	}

	@Test
	void writesAreRateLimited() {
		var sut = createHandler();

		assertNotNull(sut.apply(START, 2000, null));
		assertNull(sut.apply(START.plusSeconds(5), 3000, null));
		assertNull(sut.apply(START.plusSeconds(9), 3000, null));
		assertEquals(3000, sut.apply(START.plusSeconds(10), 3000, null).powerSetpoint());
	}

	@Test
	void writeIntervalHasLowerBound() {
		var sut = new SetpointHandler(800, 3500, Duration.ofSeconds(1));

		assertEquals(SetpointHandler.MIN_WRITE_INTERVAL, sut.getWriteInterval());
	}
}
