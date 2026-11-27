package io.openems.edge.heat.broetje.neo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the annotation defaults of the OSGi {@link Config}; in particular
 * that read-only is the safe default.
 */
class ConfigTest {

	@Test
	void readOnlyDefaultsToTrue() throws Exception {
		var defaultValue = Config.class.getMethod("readOnly").getDefaultValue();
		assertEquals(Boolean.TRUE, defaultValue);
	}

	@Test
	void powerAndTimingDefaults() throws Exception {
		assertEquals(800, Config.class.getMethod("minPower").getDefaultValue());
		assertEquals(3500, Config.class.getMethod("maxPower").getDefaultValue());
		assertEquals(1200, Config.class.getMethod("minRunTimeSeconds").getDefaultValue());
		assertEquals(600, Config.class.getMethod("minOffTimeSeconds").getDefaultValue());
		assertEquals(10, Config.class.getMethod("writeIntervalSeconds").getDefaultValue());
	}

	@Test
	void optionalRegisterGroupsDefaultToOff() throws Exception {
		for (var method : Config.class.getDeclaredMethods()) {
			var name = method.getName();
			if (name.startsWith("read") && !name.equals("readOnly") && method.getReturnType() == boolean.class) {
				assertTrue(Boolean.FALSE.equals(method.getDefaultValue()), name + " must default to false");
			}
		}
	}
}
