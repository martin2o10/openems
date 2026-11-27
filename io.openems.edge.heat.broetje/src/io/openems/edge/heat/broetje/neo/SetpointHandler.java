package io.openems.edge.heat.broetje.neo;

import java.time.Duration;
import java.time.Instant;

import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

/**
 * Translates {@link HeatPumpAction}s into NEO-RKM register values and limits
 * the write rate, as the NEO-RKM must not be written faster than every 5
 * seconds.
 *
 * <ul>
 * <li>{@link HeatPumpAction.OwnControl} (or no action) deactivates the external
 * request (register 150 = 0).
 * <li>{@link HeatPumpAction.Lock} locks the heat pump via the grid operator
 * release (register 149 = 0, register 150 = 1).
 * <li>{@link HeatPumpAction.Power} writes the clamped electric power setpoint
 * to register 125; a power below the minimum is written as 0.
 * </ul>
 */
public class SetpointHandler {

	/**
	 * Register values to write.
	 *
	 * @param powerSetpoint               register 125 [W]; null means "do not
	 *                                    write"
	 * @param gridOperatorReleaseValue    register 149; null means "do not write"
	 * @param gridOperatorReleaseActivate register 150; null means "do not write"
	 */
	public record Result(Integer powerSetpoint, Integer gridOperatorReleaseValue, Integer gridOperatorReleaseActivate) {

		/**
		 * Is the heat pump locked by this result?.
		 *
		 * @return true if locked
		 */
		public boolean isLocked() {
			return this.gridOperatorReleaseActivate != null && this.gridOperatorReleaseActivate == 1
					&& this.gridOperatorReleaseValue != null && this.gridOperatorReleaseValue == 0;
		}
	}

	/** Minimum write interval required by the NEO-RKM. */
	public static final Duration MIN_WRITE_INTERVAL = Duration.ofSeconds(5);

	private final int minPower;
	private final int maxPower;
	private final Duration writeInterval;

	private Instant lastWrite = null;

	public SetpointHandler(int minPower, int maxPower, Duration writeInterval) {
		this.minPower = minPower;
		this.maxPower = maxPower;
		this.writeInterval = writeInterval.compareTo(MIN_WRITE_INTERVAL) < 0 //
				? MIN_WRITE_INTERVAL //
				: writeInterval;
	}

	/**
	 * Calculates the register values for the given action.
	 *
	 * @param action the {@link HeatPumpAction}; null is treated as
	 *               {@link HeatPumpAction.OwnControl}
	 * @return the {@link Result}
	 */
	public Result calculate(HeatPumpAction action) {
		if (action instanceof HeatPumpAction.Lock) {
			// EVU lock: register 149 = 0, register 150 = 1
			return new Result(null, 0, 1);
		}
		if (action instanceof HeatPumpAction.Power p) {
			// 'force' is a request semantic per maintainer feedback: the heat pump is asked
			// to run at maximum power, whether it does so remains up to its own control;
			// there is no register equivalent on the Broetje.
			var clamped = Math.max(0, Math.min(p.power(), this.maxPower));
			var power = clamped < this.minPower ? 0 : clamped;
			// explicitly release a previous lock: 149 = 1 (grid operator release value),
			// 150 = 0 (lock inactive)
			return new Result(power, 1, 0);
		}
		// OwnControl or no action: deactivate the external request
		return new Result(null, null, 0);
	}

	/**
	 * Calculates the register values, but only if the write interval has passed.
	 *
	 * @param now    the current {@link Instant}
	 * @param action the {@link HeatPumpAction}; null is treated as
	 *               {@link HeatPumpAction.OwnControl}
	 * @return the {@link Result} or null if nothing should be written now
	 */
	public Result apply(Instant now, HeatPumpAction action) {
		if (this.lastWrite != null && now.isBefore(this.lastWrite.plus(this.writeInterval))) {
			return null;
		}
		this.lastWrite = now;
		return this.calculate(action);
	}

	public Duration getWriteInterval() {
		return this.writeInterval;
	}
}
