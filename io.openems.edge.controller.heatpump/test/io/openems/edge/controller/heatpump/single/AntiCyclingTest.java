package io.openems.edge.controller.heatpump.single;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

class AntiCyclingTest {

	private static final Instant T0 = Instant.parse("2026-11-09T12:00:00Z");
	private static final HeatPumpAbilities ABILITIES = new HeatPumpAbilities(new ApplyPower.Continuous(800, 3500),
			1200, 600, true);
	private static final HeatPumpAction POWER = new HeatPumpAction.Power(2000, false);

	private static AntiCycling.Result apply(AntiCycling sut, long seconds, HeatPumpAction action, boolean running) {
		return sut.apply(T0.plusSeconds(seconds), action, running, ABILITIES);
	}

	@Test
	void powerIsAppliedAfterStartDelay() {
		var sut = new AntiCycling();

		assertEquals(AntiCyclingState.START_DELAY, apply(sut, 0, POWER, false).state());
		assertEquals(HeatPumpAction.OWN_CONTROL, apply(sut, 59, POWER, false).action());
		var result = apply(sut, 60, POWER, false);
		assertEquals(AntiCyclingState.POWER, result.state());
		assertEquals(POWER, result.action());
	}

	@Test
	void shortSurplusPeakIsIgnored() {
		var sut = new AntiCycling();

		apply(sut, 0, POWER, false);
		assertEquals(AntiCyclingState.IDLE, apply(sut, 30, HeatPumpAction.OWN_CONTROL, false).state());
		assertEquals(AntiCyclingState.START_DELAY, apply(sut, 61, POWER, false).state());
	}

	@Test
	void forceHasNoStartDelay() {
		var sut = new AntiCycling();

		var force = new HeatPumpAction.Power(3500, true);
		assertEquals(force, apply(sut, 0, force, false).action());
	}

	@Test
	void minRunTimeHoldsMinPower() {
		var sut = new AntiCycling();

		apply(sut, 0, POWER, false);
		apply(sut, 60, POWER, true);

		var result = apply(sut, 600, HeatPumpAction.OWN_CONTROL, true);
		assertEquals(AntiCyclingState.MIN_RUN_TIME, result.state());
		assertEquals(new HeatPumpAction.Power(800, false), result.action());

		assertEquals(AntiCyclingState.IDLE, apply(sut, 60 + 1200, HeatPumpAction.OWN_CONTROL, true).state());
	}

	@Test
	void minOffTimeBlocksRestart() {
		var sut = new AntiCycling();

		apply(sut, 0, HeatPumpAction.OWN_CONTROL, true);
		apply(sut, 100, HeatPumpAction.OWN_CONTROL, false); // compressor stopped

		assertEquals(AntiCyclingState.MIN_OFF_TIME, apply(sut, 200, POWER, false).state());
		assertEquals(AntiCyclingState.START_DELAY, apply(sut, 700, POWER, false).state());
		assertEquals(AntiCyclingState.POWER, apply(sut, 760, POWER, false).state());
	}

	@Test
	void runningCompressorIgnoresMinOffTime() {
		var sut = new AntiCycling();

		apply(sut, 0, HeatPumpAction.OWN_CONTROL, true);
		apply(sut, 100, HeatPumpAction.OWN_CONTROL, false);
		apply(sut, 150, HeatPumpAction.OWN_CONTROL, true); // own control restarted

		assertEquals(AntiCyclingState.START_DELAY, apply(sut, 200, POWER, true).state());
	}

	@Test
	void lockIsImmediate() {
		var sut = new AntiCycling();

		apply(sut, 0, POWER, false);
		apply(sut, 60, POWER, true);

		var result = apply(sut, 120, HeatPumpAction.LOCK, true);
		assertEquals(AntiCyclingState.LOCKED, result.state());
		assertEquals(HeatPumpAction.LOCK, result.action());
	}

	@Test
	void nullIsOwnControl() {
		assertEquals(HeatPumpAction.OWN_CONTROL, apply(new AntiCycling(), 0, null, false).action());
	}
}
