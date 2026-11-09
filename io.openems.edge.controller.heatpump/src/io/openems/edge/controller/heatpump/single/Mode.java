package io.openems.edge.controller.heatpump.single;

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
	/**
	 * No intervention; the heat pump follows its own control.
	 */
	OWN_CONTROL(0, "Own control"), //
	/**
	 * Use surplus power (e.g. from PV) to heat up the storage.
	 */
	SURPLUS(1, "Surplus"), //
	/**
	 * Heat up the storage with maximum power.
	 */
	FORCE(2, "Force"), //
	/**
	 * The Energy Scheduler decides per period between {@link #SURPLUS} and
	 * {@link #FORCE}, e.g. to use cheap periods of a dynamic tariff.
	 */
	SMART(3, "Smart"); //

	private final int value;
	private final String name;

	private Mode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return OWN_CONTROL;
	}

	/**
	 * Does this Mode use surplus power?.
	 *
	 * @return true for {@link #SURPLUS}
	 */
	public boolean isSurplus() {
		return this == SURPLUS;
	}
}
