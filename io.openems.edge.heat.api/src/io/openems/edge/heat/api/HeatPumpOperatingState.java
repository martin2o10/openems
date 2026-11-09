package io.openems.edge.heat.api;

import io.openems.common.types.OptionsEnum;

/**
 * The current operating state of a {@link ManagedHeatPump}.
 */
public enum HeatPumpOperatingState implements OptionsEnum {

	/**
	 * Unknown state.
	 */
	UNDEFINED(-1, "Undefined"),

	/**
	 * The compressor is off and no request is pending.
	 */
	OFF(0, "Off"),

	/**
	 * The heat pump is producing heat for space heating.
	 */
	SPACE_HEATING(1, "Space heating"),

	/**
	 * The heat pump is producing heat for domestic hot water.
	 */
	DOMESTIC_HOT_WATER(2, "Domestic hot water"),

	/**
	 * The heat pump is cooling.
	 */
	COOLING(3, "Cooling"),

	/**
	 * The heat pump is defrosting its evaporator. Power consumption is not
	 * controllable during this state.
	 */
	DEFROST(4, "Defrost"),

	/**
	 * The heat pump is blocked, e.g. by an internal minimum off-time or by an
	 * external lock signal of the grid operator.
	 */
	BLOCKED(5, "Blocked"),

	/**
	 * An error occurred on the device.
	 */
	FAULT(6, "Fault");

	private final int value;
	private final String name;

	private HeatPumpOperatingState(int value, String name) {
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

	/**
	 * Is the compressor running in this state?.
	 *
	 * @return true for heating, domestic hot water, cooling and defrost
	 */
	public boolean isRunning() {
		return switch (this) {
		case SPACE_HEATING, DOMESTIC_HOT_WATER, COOLING, DEFROST -> true;
		case UNDEFINED, OFF, BLOCKED, FAULT -> false;
		};
	}
}
