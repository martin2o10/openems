package io.openems.edge.evse.chargepoint.abl;

/**
 * Charging outlet of an ABL eM4 in stand-alone operation.
 *
 * <p>
 * See ABL "Modbus TCP - Schnittstellenbeschreibung Wallbox eM4 Controller /
 * Extender", R 01, 19.12.2023, chapter 3.1: outlet 1 (left) starts at 0x3000,
 * outlet 2 (right, eM4 Twin only) at 0x3100.
 */
public enum Outlet {
	OUTLET_1(0x3000), //
	OUTLET_2(0x3100);

	public final int startAddress;

	private Outlet(int startAddress) {
		this.startAddress = startAddress;
	}
}
