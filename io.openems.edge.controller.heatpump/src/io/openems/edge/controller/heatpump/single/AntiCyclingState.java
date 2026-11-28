package io.openems.edge.controller.heatpump.single;

import io.openems.common.types.OptionsEnum;

/**
 * State of the {@link AntiCycling} of a {@link ControllerHeatPumpSingle}.
 */
public enum AntiCyclingState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	/** No power is requested; the heat pump follows its own control. */
	IDLE(0, "Idle"), //
	/** Power is requested, but the start delay is not yet over. */
	START_DELAY(1, "Start delay"), //
	/** Power is requested, but the minimum off time is not yet over. */
	MIN_OFF_TIME(2, "Minimum off time"), //
	/** Power is applied. */
	POWER(3, "Power"), //
	/** No more power is requested, but the minimum run time is not yet over. */
	MIN_RUN_TIME(4, "Minimum run time"), //
	/** The heat pump is locked. */
	LOCKED(5, "Locked"); //

	private final int value;
	private final String name;

	private AntiCyclingState(int value, String name) {
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
		return UNDEFINED;
	}
}
