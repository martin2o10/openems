package io.openems.edge.evse.chargepoint.abl;

import io.openems.common.types.OptionsEnum;

/**
 * Status of the outlet as reported by the ABL eM4 in register 0x..31.
 *
 * <p>
 * See ABL "Modbus TCP - Schnittstellenbeschreibung Wallbox eM4 Controller /
 * Extender", R 01, 19.12.2023, chapter 3.4. All values 0xF0..0xFF are errors
 * and are mapped to {@link #ERROR}.
 */
public enum OutletStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined", false, false), //
	A0(0xA0, "A0: Outlet blocked", false, false), //
	A1(0xA1, "A1: Outlet waiting for EV", false, false), //
	A2(0xA2, "A2: Outlet reserved", false, false), //
	B0(0xB0, "B0: EV connected, authentication failed", true, false), //
	B1(0xB1, "B1: EV connected, authentication", true, false), //
	B2(0xB2, "B2: EV connected, ready to provide energy", true, true), //
	B3(0xB3, "B3: EV connected, charging finished or paused by EV", true, true), //
	C2(0xC2, "C2: Charging", true, true), //
	E0(0xE0, "E0: Outlet blocked, EV not detected", false, false), //
	E2(0xE2, "E2: Outlet booting", false, false), //
	ERROR(0xF0, "Error", false, false) //
	;

	public final boolean isEvConnected;
	public final boolean isReadyForCharging;

	private final int value;
	private final String name;

	private OutletStatus(int value, String name, boolean isEvConnected, boolean isReadyForCharging) {
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
}
