package io.openems.edge.heat.broetje.neo;

import io.openems.common.types.OptionsEnum;

/**
 * Current demand of the heat pump (NEO-RKM register 41).
 */
public enum Demand implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NONE(0, "No demand"), //
	COOLING(10, "Cooling"), //
	SPACE_HEATING(20, "Space heating"), //
	DOMESTIC_HOT_WATER(30, "Domestic hot water"); //

	private final int value;
	private final String name;

	private Demand(int value, String name) {
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
