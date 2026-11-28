package io.openems.edge.controller.heatpump.single;

import java.time.Duration;
import java.time.Instant;

import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

/**
 * Protects the compressor against frequent starts and stops ("Takten").
 *
 * <ul>
 * <li>A requested {@link HeatPumpAction.Power} is only applied after a start
 * delay (to ignore short surplus peaks, e.g. from clouds) and after the minimum
 * off time of the compressor.
 * <li>Once applied, the power is held at least at minimum power until the
 * minimum run time is over.
 * <li>A {@link HeatPumpAction.Lock} is always applied immediately.
 * </ul>
 *
 * <p>
 * This class has no dependencies on other goals and can be removed together
 * with its call in {@link ControllerHeatPumpSingleImpl}.
 */
public class AntiCycling {

	/** Delay before a requested power is applied. */
	public static final Duration START_DELAY = Duration.ofSeconds(60);

	/**
	 * Result of
	 * {@link AntiCycling#apply(Instant, HeatPumpAction, boolean, HeatPumpAbilities)}.
	 *
	 * @param action the effective {@link HeatPumpAction}
	 * @param state  the {@link AntiCyclingState}
	 */
	public record Result(HeatPumpAction action, AntiCyclingState state) {
	}

	private boolean powerApplied = false;
	private Instant powerAppliedSince = null;
	private Instant powerRequestedSince = null;
	private Instant compressorStoppedAt = null;
	private boolean wasRunning = false;

	/**
	 * Calculates the effective {@link HeatPumpAction}.
	 *
	 * @param now       the current {@link Instant}
	 * @param requested the requested {@link HeatPumpAction}
	 * @param isRunning is the compressor currently running?
	 * @param abilities the {@link HeatPumpAbilities} of the heat pump
	 * @return the {@link Result}
	 */
	public Result apply(Instant now, HeatPumpAction requested, boolean isRunning, HeatPumpAbilities abilities) {
		this.trackCompressor(now, isRunning);

		return switch (requested) {
		case HeatPumpAction.Lock l -> {
			this.stopPower();
			this.powerRequestedSince = null;
			yield new Result(requested, AntiCyclingState.LOCKED);
		}
		case HeatPumpAction.Power p -> this.applyPower(now, p, isRunning, abilities);
		case HeatPumpAction.OwnControl o -> this.applyOwnControl(now, abilities);
		case null -> this.applyOwnControl(now, abilities);
		};
	}

	private Result applyPower(Instant now, HeatPumpAction.Power power, boolean isRunning,
			HeatPumpAbilities abilities) {
		if (this.powerApplied) {
			return new Result(power, AntiCyclingState.POWER);
		}
		if (!isRunning && this.compressorStoppedAt != null
				&& now.isBefore(this.compressorStoppedAt.plusSeconds(abilities.minOffTime()))) {
			return new Result(HeatPumpAction.OWN_CONTROL, AntiCyclingState.MIN_OFF_TIME);
		}
		if (this.powerRequestedSince == null) {
			this.powerRequestedSince = now;
		}
		if (!power.force() && now.isBefore(this.powerRequestedSince.plus(START_DELAY))) {
			return new Result(HeatPumpAction.OWN_CONTROL, AntiCyclingState.START_DELAY);
		}
		this.powerApplied = true;
		this.powerAppliedSince = now;
		return new Result(power, AntiCyclingState.POWER);
	}

	private Result applyOwnControl(Instant now, HeatPumpAbilities abilities) {
		this.powerRequestedSince = null;
		if (this.powerApplied && abilities.canApplyPower()
				&& now.isBefore(this.powerAppliedSince.plusSeconds(abilities.minRunTime()))) {
			return new Result(new HeatPumpAction.Power(abilities.applyPower().minPower(), false),
					AntiCyclingState.MIN_RUN_TIME);
		}
		this.stopPower();
		return new Result(HeatPumpAction.OWN_CONTROL, AntiCyclingState.IDLE);
	}

	private void stopPower() {
		this.powerApplied = false;
		this.powerAppliedSince = null;
	}

	private void trackCompressor(Instant now, boolean isRunning) {
		if (this.wasRunning && !isRunning) {
			this.compressorStoppedAt = now;
		}
		this.wasRunning = isRunning;
	}
}
