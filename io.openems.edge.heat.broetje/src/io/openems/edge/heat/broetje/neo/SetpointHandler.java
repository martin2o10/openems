package io.openems.edge.heat.broetje.neo;

import java.time.Duration;
import java.time.Instant;

/**
 * Translates the {@code ManagedHeatPump} setpoints into NEO-RKM register
 * values and limits the write rate, as the NEO-RKM must not be written faster
 * than every 5 seconds.
 *
 * <ul>
 * <li>TARGET_ACTIVE_POWER is written to the electric power setpoint (register
 * 125, demand profile 3), limited by ACTIVE_POWER_LIMIT and MAX_ACTIVE_POWER.
 * <li>An ACTIVE_POWER_LIMIT below MIN_ACTIVE_POWER locks the heat pump via the
 * grid operator release (registers 149/150). Without a limit, the lock is
 * handed back to the device (register 150 = 0).
 * </ul>
 */
public class SetpointHandler {

	/**
	 * Register values to write.
	 *
	 * @param powerSetpoint               register 125 [W]; null means "do not
	 *                                    write"
	 * @param gridOperatorReleaseValue    register 149
	 * @param gridOperatorReleaseActivate register 150
	 */
	public record Result(Integer powerSetpoint, int gridOperatorReleaseValue, int gridOperatorReleaseActivate) {

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
	 * Calculates the register values for the given setpoints.
	 *
	 * @param target the TARGET_ACTIVE_POWER [W] or null
	 * @param limit  the ACTIVE_POWER_LIMIT [W] or null
	 * @return the {@link Result}
	 */
	public Result calculate(Integer target, Integer limit) {
		if (limit != null && limit < this.minPower) {
			return new Result(0, 0, 1);
		}

		Integer power = null;
		if (target != null) {
			var p = Math.max(0, Math.min(target, this.maxPower));
			if (limit != null) {
				p = Math.min(p, limit);
			}
			power = p < this.minPower ? 0 : p;
		}
		return new Result(power, 1, 0);
	}

	/**
	 * Calculates the register values, but only if the write interval has passed.
	 *
	 * @param now    the current {@link Instant}
	 * @param target the TARGET_ACTIVE_POWER [W] or null
	 * @param limit  the ACTIVE_POWER_LIMIT [W] or null
	 * @return the {@link Result} or null if nothing should be written now
	 */
	public Result apply(Instant now, Integer target, Integer limit) {
		if (this.lastWrite != null && now.isBefore(this.lastWrite.plus(this.writeInterval))) {
			return null;
		}
		this.lastWrite = now;
		return this.calculate(target, limit);
	}

	public Duration getWriteInterval() {
		return this.writeInterval;
	}
}
