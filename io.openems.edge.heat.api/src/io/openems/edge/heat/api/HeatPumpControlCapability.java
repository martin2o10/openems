package io.openems.edge.heat.api;

import io.openems.common.types.OptionsEnum;

/**
 * Describes how precisely the electric power consumption of a
 * {@link ManagedHeatPump} can be controlled via its interface.
 *
 * <p>
 * The setpoints of a {@link ManagedHeatPump} are always given in watt. An
 * implementation maps them to the capability of its interface.
 */
public enum HeatPumpControlCapability implements OptionsEnum {

	/**
	 * Unknown capability.
	 */
	UNDEFINED(-1, "Undefined"),

	/**
	 * The power consumption can be set continuously within the modulation range,
	 * e.g. via a power setpoint register or EEBUS.
	 */
	CONTINUOUS(0, "Continuous"),

	/**
	 * The power consumption can be influenced in a few discrete steps, e.g. the
	 * four states of SG-Ready.
	 */
	DISCRETE_STEPS(1, "Discrete steps"),

	/**
	 * The heat pump can only be released or blocked.
	 */
	ON_OFF(2, "On/Off");

	private final int value;
	private final String name;

	private HeatPumpControlCapability(int value, String name) {
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
