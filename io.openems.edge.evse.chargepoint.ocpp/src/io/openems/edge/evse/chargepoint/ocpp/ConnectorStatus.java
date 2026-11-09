package io.openems.edge.evse.chargepoint.ocpp;

import eu.chargetime.ocpp.model.core.ChargePointStatus;
import io.openems.common.types.OptionsEnum;

/**
 * Connector status as reported by the OCPP 1.6 StatusNotification.
 */
public enum ConnectorStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined", false, false), //
	AVAILABLE(0, "Available", false, false), //
	PREPARING(1, "Preparing", true, true), //
	CHARGING(2, "Charging", true, true), //
	SUSPENDED_EVSE(3, "Suspended by the charging station", true, true), //
	SUSPENDED_EV(4, "Suspended by the EV", true, true), //
	FINISHING(5, "Finishing", true, false), //
	RESERVED(6, "Reserved", false, false), //
	UNAVAILABLE(7, "Unavailable", false, false), //
	FAULTED(8, "Faulted", false, false) //
	;

	public final boolean isEvConnected;
	public final boolean isReadyForCharging;

	private final int value;
	private final String name;

	private ConnectorStatus(int value, String name, boolean isEvConnected, boolean isReadyForCharging) {
		this.value = value;
		this.name = name;
		this.isEvConnected = isEvConnected;
		this.isReadyForCharging = isReadyForCharging;
	}

	/**
	 * Maps an OCPP {@link ChargePointStatus} to a {@link ConnectorStatus}.
	 *
	 * @param status the {@link ChargePointStatus}
	 * @return the {@link ConnectorStatus}; UNDEFINED for null
	 */
	public static ConnectorStatus from(ChargePointStatus status) {
		return switch (status) {
		case null -> UNDEFINED;
		case Available -> AVAILABLE;
		case Preparing -> PREPARING;
		case Charging -> CHARGING;
		case SuspendedEVSE -> SUSPENDED_EVSE;
		case SuspendedEV -> SUSPENDED_EV;
		case Finishing -> FINISHING;
		case Reserved -> RESERVED;
		case Unavailable -> UNAVAILABLE;
		case Faulted -> FAULTED;
		};
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
