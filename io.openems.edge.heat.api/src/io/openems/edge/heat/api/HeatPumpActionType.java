package io.openems.edge.heat.api;

import io.openems.common.types.OptionsEnum;

/**
 * Type of the last {@link HeatPumpProfile.HeatPumpAction} applied to a
 * {@link ManagedHeatPump}.
 */
public enum HeatPumpActionType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OWN_CONTROL(0, "Own control"), //
	LOCK(1, "Lock"), //
	POWER(2, "Power"), //
	FORCE_POWER(3, "Force power"); //

	private final int value;
	private final String name;

	private HeatPumpActionType(int value, String name) {
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
