package io.openems.edge.evse.chargepoint.goe;

import io.openems.common.types.OptionsEnum;

/**
 * State as reported by the go-e charge point in Modbus input register 100
 * ("CAR_STATE").
 *
 * <p>
 * Note that state 1 ("Ready, no EV") is explicitly NOT considered as
 * EV-connected, unlike some other charge points, where "ready" implies an EV
 * is plugged in.
 */
public enum CarState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", false, false), //
	DEFECT(0, "Defective", false, false), //
	READY_NO_EV(1, "Ready, no EV connected", false, false), //
	CHARGING(2, "Charging", true, true), //
	WAITING_FOR_VEHICLE(3, "Waiting for vehicle", true, true), //
	FINISHED_EV_CONNECTED(4, "Charging finished, EV connected", true, false) //
	;

	public final boolean isEvConnected;
	public final boolean isReadyForCharging;

	private final int value;
	private final String name;

	private CarState(int value, String name, boolean isEvConnected, boolean isReadyForCharging) {
		this.value = value;
		this.name = name;
		this.isEvConnected = isEvConnected;
		this.isReadyForCharging = isReadyForCharging;
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
	 * Parses the raw value of Modbus input register 100.
	 *
	 * @param value the raw register value
	 * @return the {@link CarState}; {@link #UNDEFINED} for unknown values
	 */
	public static CarState fromValue(int value) {
		return switch (value) {
		case 0 -> DEFECT;
		case 1 -> READY_NO_EV;
		case 2 -> CHARGING;
		case 3 -> WAITING_FOR_VEHICLE;
		case 4 -> FINISHED_EV_CONNECTED;
		default -> UNDEFINED;
		};
	}
}
