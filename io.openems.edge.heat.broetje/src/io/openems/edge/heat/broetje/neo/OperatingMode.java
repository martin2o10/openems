package io.openems.edge.heat.broetje.neo;

import io.openems.common.types.OptionsEnum;

/**
 * Operating mode of the heat pump (NEO-RKM register 100). Values 8 to 10 are
 * only reported by the device and can not be written.
 */
public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	AUTOMATIC(1, "Automatic"), //
	COOLING(2, "Cooling"), //
	SUMMER(3, "Summer"), //
	CONTINUOUS(4, "Continuous"), //
	SETBACK(5, "Setback"), //
	HOLIDAY(6, "Holiday"), //
	PARTY(7, "Party"), //
	SCREED_DRYING(8, "Screed drying"), //
	GRID_OPERATOR_LOCK(9, "Grid operator lock"), //
	MAIN_SWITCH_OFF(10, "Main switch off"); //

	private final int value;
	private final String name;

	private OperatingMode(int value, String name) {
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
