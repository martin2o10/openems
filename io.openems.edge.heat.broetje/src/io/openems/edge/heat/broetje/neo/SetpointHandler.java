package io.openems.edge.heat.broetje.neo;

import java.time.Duration;
import java.time.Instant;

import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

/**
 * Translates a {@link HeatPumpAction} into NEO-RKM register values and limits
 * the write rate, as the NEO-RKM must not be written faster than every 5
 * seconds.
 *
 * <ul>
 * <li>{@link HeatPumpAction.Power}: the power is written to the electric power
 * setpoint (register 125, demand profile 3), limited to the configured maximum
 * power; values below the minimum power are written as 0.
 * <li>{@link HeatPumpAction.OwnControl}: the power setpoint is set to 0 and the
 * grid operator release is handed back to the device (register 150 = 0).
 * <li>{@link HeatPumpAction.Lock}: the heat pump is locked via the grid
 * operator release (registers 149 = 0, 150 = 1).
 * </ul>
 */
public class SetpointHandler {

	/**
	 * Register values to write.
	 *
	 * @param powerSetpoint               register 125 [W]
	 * @param gridOperatorReleaseValue    register 149
	 * @param gridOperatorReleaseActivate register 150
	 */
	public record Result(int powerSetpoint, int gridOperatorReleaseValue, int gridOperatorReleaseActivate) {

		/**
		 * Is the heat pump locked by this result?.
		 *
		 * @return true if locked
		 */
		public boolean isLocked() {
			return this.gridOperatorReleaseActivate == 1 && this.gridOperatorReleaseValue == 0;
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
	 * Calculates the register values for the given {@link HeatPumpAction}.
	 *
	 * @param action the {@link HeatPumpAction}; null is handled like
	 *               {@link HeatPumpAction.OwnControl}
	 * @return the {@link Result}
	 */
	public Result calculate(HeatPumpAction action) {
		return switch (action) {
		case null -> new Result(0, 1, 0);
		case HeatPumpAction.OwnControl o -> new Result(0, 1, 0);
		case HeatPumpAction.Lock l -> new Result(0, 0, 1);
		case HeatPumpAction.Power p -> {
			var power = Math.max(0, Math.min(p.power(), this.maxPower));
			yield new Result(power < this.minPower ? 0 : power, 1, 0);
		}
		};
	}

	/**
	 * Calculates the register values, but only if the write interval has passed.
	 *
	 * @param now    the current {@link Instant}
	 * @param action the {@link HeatPumpAction}
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
