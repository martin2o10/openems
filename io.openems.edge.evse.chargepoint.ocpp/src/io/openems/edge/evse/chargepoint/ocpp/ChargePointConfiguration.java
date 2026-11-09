package io.openems.edge.evse.chargepoint.ocpp;

import static io.openems.common.utils.FunctionUtils.doNothing;

import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.GetConfigurationRequest;

/**
 * Smart-Charging relevant configuration of an OCPP 1.6 charging station.
 *
 * @param phaseSwitchingSupported 'ConnectorSwitch3to1PhaseSupported'
 * @param stackLevel              'ChargeProfileMaxStackLevel'; the charging
 *                                profile is installed on the highest stack
 *                                level, so that it overrules other profiles
 * @param chargingProfileId       'MaxChargingProfilesInstalled'; used as fixed
 *                                id, so that every new profile replaces the
 *                                previous one
 */
public record ChargePointConfiguration(boolean phaseSwitchingSupported, int stackLevel, int chargingProfileId) {

	public static final String PHASE_SWITCHING_SUPPORTED = "ConnectorSwitch3to1PhaseSupported";
	public static final String MAX_STACK_LEVEL = "ChargeProfileMaxStackLevel";
	public static final String MAX_CHARGING_PROFILES_INSTALLED = "MaxChargingProfilesInstalled";

	/** Used until the configuration has been read from the charging station. */
	public static final ChargePointConfiguration DEFAULT = new ChargePointConfiguration(false, 0, 1);

	/**
	 * Creates the {@link GetConfigurationRequest} for all required keys.
	 *
	 * @return the {@link GetConfigurationRequest}
	 */
	public static GetConfigurationRequest createRequest() {
		var request = new GetConfigurationRequest();
		request.setKey(new String[] { PHASE_SWITCHING_SUPPORTED, MAX_STACK_LEVEL, MAX_CHARGING_PROFILES_INSTALLED });
		return request;
	}

	/**
	 * Parses a {@link GetConfigurationConfirmation}. Unknown keys and invalid
	 * values fall back to {@link #DEFAULT}.
	 *
	 * @param confirmation the {@link GetConfigurationConfirmation}
	 * @return the {@link ChargePointConfiguration}
	 */
	public static ChargePointConfiguration from(GetConfigurationConfirmation confirmation) {
		var phaseSwitchingSupported = DEFAULT.phaseSwitchingSupported;
		var stackLevel = DEFAULT.stackLevel;
		var chargingProfileId = DEFAULT.chargingProfileId;
		var keys = confirmation == null ? null : confirmation.getConfigurationKey();
		if (keys == null) {
			return DEFAULT;
		}
		for (var key : keys) {
			if (key == null || key.getKey() == null || key.getValue() == null) {
				continue;
			}
			var value = key.getValue().trim();
			switch (key.getKey()) {
			case PHASE_SWITCHING_SUPPORTED -> phaseSwitchingSupported = Boolean.parseBoolean(value);
			case MAX_STACK_LEVEL -> stackLevel = parseInt(value, 0, stackLevel);
			case MAX_CHARGING_PROFILES_INSTALLED -> chargingProfileId = parseInt(value, 1, chargingProfileId);
			default -> doNothing();
			}
		}
		return new ChargePointConfiguration(phaseSwitchingSupported, stackLevel, chargingProfileId);
	}

	private static int parseInt(String value, int min, int fallback) {
		try {
			return Math.max(min, Integer.parseInt(value));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
