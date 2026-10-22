package io.openems.edge.evse.chargepoint.goe;

import io.openems.common.types.OptionsEnum;

/**
 * Error as reported by the go-e charge point in Modbus input register 107
 * ("ERROR").
 *
 * <p>
 * Per go-e Modbus API v2: "10, default: INTERNAL" - i.e. unknown values map
 * to {@link #INTERNAL}. Value 0 is not documented explicitly; it is interpreted
 * as "no error", consistent with the go-e HTTP API "err" key.
 */
public enum ErrorCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ERROR(0, "No error"), //
	RCCB(1, "Residual current operated device error"), //
	PHASE(3, "Phase error"), //
	NO_GROUND(8, "No ground"), //
	INTERNAL(10, "Internal error") //
	;

	private final int value;
	private final String name;

	private ErrorCode(int value, String name) {
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
	 * Parses the raw value of Modbus input register 107.
	 *
	 * @param value the raw register value
	 * @return the {@link ErrorCode}; unknown values map to {@link #INTERNAL}
	 */
	public static ErrorCode fromValue(int value) {
		return switch (value) {
		case 0 -> NO_ERROR;
		case 1 -> RCCB;
		case 3 -> PHASE;
		case 8 -> NO_GROUND;
		case 10 -> INTERNAL;
		default -> INTERNAL;
		};
	}
}
