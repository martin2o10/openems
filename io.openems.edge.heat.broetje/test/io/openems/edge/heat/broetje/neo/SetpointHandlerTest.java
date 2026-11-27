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

	@Test
	void powerIsWrittenToRegister125() {
		var result = createHandler().calculate(new HeatPumpAction.Power(2000, false));

		assertEquals(2000, result.powerSetpoint());
		// a Power action explicitly releases a previous lock: 149 = 1, 150 = 0
		assertEquals(1, result.gridOperatorReleaseValue());
		assertEquals(0, result.gridOperatorReleaseActivate());
		assertFalse(result.isLocked());
	}

	@Test
	void powerAfterLockReleasesTheLock() {
		var sut = createHandler();

		var lock = sut.calculate(HeatPumpAction.LOCK);
		assertTrue(lock.isLocked());

		var power = sut.calculate(new HeatPumpAction.Power(2000, false));
		assertEquals(2000, power.powerSetpoint());
		assertEquals(1, power.gridOperatorReleaseValue());
		assertEquals(0, power.gridOperatorReleaseActivate());
		assertFalse(power.isLocked());
	}

	@Test
	void powerIsClampedToMaxPower() {
		assertEquals(3500, createHandler().calculate(new HeatPumpAction.Power(9000, false)).powerSetpoint());
	}

	@Test
	void powerBelowMinPowerIsZero() {
		assertEquals(0, createHandler().calculate(new HeatPumpAction.Power(500, false)).powerSetpoint());
		assertEquals(0, createHandler().calculate(new HeatPumpAction.Power(-1000, false)).powerSetpoint());
	}

	@Test
	void forceHasNoRegisterEquivalent() {
		// 'force' is request semantics only (maintainer feedback; no register
		// equivalent on the Broetje), so both variants map to the same registers
		var forced = createHandler().calculate(new HeatPumpAction.Power(2000, true));
		var normal = createHandler().calculate(new HeatPumpAction.Power(2000, false));

		assertEquals(normal.powerSetpoint(), forced.powerSetpoint());
		assertEquals(normal.gridOperatorReleaseValue(), forced.gridOperatorReleaseValue());
		assertEquals(normal.gridOperatorReleaseActivate(), forced.gridOperatorReleaseActivate());
	}

	@Test
	void lockWritesRegisters149And150() {
		var result = createHandler().calculate(HeatPumpAction.LOCK);

		assertNull(result.powerSetpoint());
		assertEquals(0, result.gridOperatorReleaseValue());
		assertEquals(1, result.gridOperatorReleaseActivate());
		assertTrue(result.isLocked());
	}

	@Test
	void ownControlDeactivatesExternalRequest() {
		var result = createHandler().calculate(HeatPumpAction.OWN_CONTROL);

		assertNull(result.powerSetpoint());
		assertNull(result.gridOperatorReleaseValue());
		assertEquals(0, result.gridOperatorReleaseActivate());
		assertFalse(result.isLocked());
	}

	@Test
	void nullActionFallsBackToOwnControl() {
		var result = createHandler().calculate(null);

		assertNull(result.powerSetpoint());
		assertEquals(0, result.gridOperatorReleaseActivate());
		assertFalse(result.isLocked());
	}

	@Test
	void writesAreRateLimited() {
		var sut = createHandler();

		assertNotNull(sut.apply(START, new HeatPumpAction.Power(2000, false)));
		assertNull(sut.apply(START.plusSeconds(5), new HeatPumpAction.Power(3000, false)));
		assertNull(sut.apply(START.plusSeconds(9), HeatPumpAction.LOCK));
		assertEquals(3000, sut.apply(START.plusSeconds(10), new HeatPumpAction.Power(3000, false)).powerSetpoint());
	}

	@Test
	void writeIntervalHasLowerBound() {
		var sut = new SetpointHandler(800, 3500, Duration.ofSeconds(1));

		assertEquals(SetpointHandler.MIN_WRITE_INTERVAL, sut.getWriteInterval());
	}
}
