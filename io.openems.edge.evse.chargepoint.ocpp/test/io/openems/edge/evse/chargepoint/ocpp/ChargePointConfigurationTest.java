package io.openems.edge.evse.chargepoint.ocpp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.KeyValueType;

public class ChargePointConfigurationTest {

	@Test
	public void testCreateRequest() {
		assertArrayEquals(new String[] { "ConnectorSwitch3to1PhaseSupported", "ChargeProfileMaxStackLevel",
				"MaxChargingProfilesInstalled" }, ChargePointConfiguration.createRequest().getKey());
	}

	@Test
	public void testFrom() {
		assertEquals(new ChargePointConfiguration(true, 9, 16), ChargePointConfiguration.from(confirmation(//
				keyValue("ConnectorSwitch3to1PhaseSupported", "true"), //
				keyValue("ChargeProfileMaxStackLevel", " 9 "), //
				keyValue("MaxChargingProfilesInstalled", "16"), //
				keyValue("HeartbeatInterval", "100"))));
	}

	@Test
	public void testFromInvalidValues() {
		assertEquals(ChargePointConfiguration.DEFAULT, ChargePointConfiguration.from(confirmation(//
				keyValue("ConnectorSwitch3to1PhaseSupported", "yes"), //
				keyValue("ChargeProfileMaxStackLevel", "-1"), //
				keyValue("MaxChargingProfilesInstalled", "0"), //
				keyValue("MeterValueSampleInterval", null), //
				null)));
		assertEquals(new ChargePointConfiguration(false, 3, 1), ChargePointConfiguration.from(confirmation(//
				keyValue("ChargeProfileMaxStackLevel", "3"), //
				keyValue("MaxChargingProfilesInstalled", "many"))));
	}

	@Test
	public void testFromMissingConfiguration() {
		assertEquals(ChargePointConfiguration.DEFAULT, ChargePointConfiguration.from(null));
		assertEquals(ChargePointConfiguration.DEFAULT,
				ChargePointConfiguration.from(new GetConfigurationConfirmation()));
	}

	private static GetConfigurationConfirmation confirmation(KeyValueType... keys) {
		var result = new GetConfigurationConfirmation();
		result.setConfigurationKey(keys);
		return result;
	}

	private static KeyValueType keyValue(String key, String value) {
		var result = new KeyValueType(key, true);
		if (value != null) {
			result.setValue(value);
		}
		return result;
	}
}
