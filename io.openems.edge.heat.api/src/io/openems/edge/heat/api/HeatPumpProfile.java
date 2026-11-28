package io.openems.edge.heat.api;

/**
 * Declares what a {@link ManagedHeatPump} can do ({@link HeatPumpAbilities})
 * and what a Controller wants it to do ({@link HeatPumpAction}).
 *
 * <p>
 * The structure follows the EVSE Profile of Charge-Points: the device only
 * describes its abilities and applies actions; all decisions are taken by
 * Controllers.
 */
public final class HeatPumpProfile {

	private HeatPumpProfile() {
	}

	/**
	 * Describes how the electric power consumption of a heat pump can be
	 * increased.
	 */
	public sealed interface ApplyPower {

		/**
		 * The lowest electric power [W] that can be requested.
		 *
		 * @return the power
		 */
		public int minPower();

		/**
		 * The highest electric power [W] that can be requested.
		 *
		 * @return the power
		 */
		public int maxPower();

		/**
		 * Fits the given available power to the abilities of the heat pump.
		 *
		 * @param availablePower the available power [W]
		 * @return the power [W] that can be requested; 0 if the available power is
		 *         not sufficient
		 */
		public int fit(int availablePower);

		/**
		 * The electric power can be set continuously, e.g. via Modbus.
		 *
		 * @param minPower the lowest electric power [W]
		 * @param maxPower the highest electric power [W]
		 */
		public static record Continuous(int minPower, int maxPower) implements ApplyPower {

			@Override
			public int fit(int availablePower) {
				if (availablePower < this.minPower) {
					return 0;
				}
				return Math.min(availablePower, this.maxPower);
			}
		}

		/**
		 * The heat pump is controlled via SG-Ready states. The electric power per
		 * state is only an estimate.
		 *
		 * @param recommendationPower the expected electric power [W] in SG-Ready
		 *                            state 3 (switch-on recommendation)
		 * @param forcePower          the expected electric power [W] in SG-Ready
		 *                            state 4 (switch-on command)
		 */
		public static record SgReady(int recommendationPower, int forcePower) implements ApplyPower {

			@Override
			public int minPower() {
				return this.recommendationPower;
			}

			@Override
			public int maxPower() {
				return Math.max(this.recommendationPower, this.forcePower);
			}

			@Override
			public int fit(int availablePower) {
				return availablePower < this.recommendationPower ? 0 : this.recommendationPower;
			}
		}
	}

	/**
	 * Declares the Abilities of a {@link ManagedHeatPump}.
	 *
	 * @param applyPower the {@link ApplyPower}; null if the power can not be
	 *                   influenced
	 * @param minRunTime the minimum compressor run time [s] after a start
	 * @param minOffTime the minimum compressor off time [s] before a restart
	 */
	public static record HeatPumpAbilities(ApplyPower applyPower, int minRunTime, int minOffTime) {

		/**
		 * Can the electric power of the heat pump be increased?.
		 *
		 * @return true if {@link #applyPower()} is available
		 */
		public boolean canApplyPower() {
			return this.applyPower != null;
		}
	}

	/**
	 * An Action for a {@link ManagedHeatPump}.
	 */
	public sealed interface HeatPumpAction {

		/**
		 * Gets the {@link HeatPumpActionType} for monitoring.
		 *
		 * @return the type
		 */
		public HeatPumpActionType type();

		/**
		 * The heat pump follows its own control; no intervention (SG-Ready state
		 * 2).
		 */
		public static record OwnControl() implements HeatPumpAction {

			@Override
			public HeatPumpActionType type() {
				return HeatPumpActionType.OWN_CONTROL;
			}
		}

		/**
		 * The heat pump is locked (e.g. grid operator lock, SG-Ready state 1).
		 */
		public static record Lock() implements HeatPumpAction {

			@Override
			public HeatPumpActionType type() {
				return HeatPumpActionType.LOCK;
			}
		}

		/**
		 * The heat pump should consume the given electric power.
		 *
		 * <p>
		 * For {@link ApplyPower.SgReady} devices this maps to SG-Ready state 3
		 * (recommendation) or 4 (command, if {@code force} is set).
		 *
		 * @param power the requested electric power [W]
		 * @param force true to request the power even if the own control of the
		 *              heat pump would not heat (up to its maximum storage
		 *              temperature)
		 */
		public static record Power(int power, boolean force) implements HeatPumpAction {

			@Override
			public HeatPumpActionType type() {
				return this.force //
						? HeatPumpActionType.FORCE_POWER //
						: HeatPumpActionType.POWER;
			}
		}

		/** Singleton for {@link OwnControl}. */
		public static final HeatPumpAction OWN_CONTROL = new OwnControl();

		/** Singleton for {@link Lock}. */
		public static final HeatPumpAction LOCK = new Lock();
	}
}
