package io.openems.edge.heat.broetje.neo;

import static io.openems.edge.heat.broetje.neo.OperatingStateMapper.map;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.heat.api.HeatPumpOperatingState;

class OperatingStateMapperTest {

	@Test
	void faultHasPrecedence() {
		assertEquals(HeatPumpOperatingState.FAULT,
				map(true, false, OperatingMode.AUTOMATIC, true, false, Demand.SPACE_HEATING));
	}

	@Test
	void gridOperatorLockIsBlocked() {
		assertEquals(HeatPumpOperatingState.BLOCKED,
				map(false, false, OperatingMode.AUTOMATIC, false, false, Demand.NONE));
		assertEquals(HeatPumpOperatingState.BLOCKED,
				map(false, true, OperatingMode.GRID_OPERATOR_LOCK, false, false, Demand.NONE));
	}

	@Test
	void notRunningIsOff() {
		assertEquals(HeatPumpOperatingState.OFF,
				map(false, true, OperatingMode.AUTOMATIC, false, false, Demand.NONE));
	}

	@Test
	void unknownRunningStateIsUndefined() {
		assertEquals(HeatPumpOperatingState.UNDEFINED, map(null, null, null, null, null, null));
	}

	@Test
	void runningStateFollowsDemand() {
		assertEquals(HeatPumpOperatingState.SPACE_HEATING,
				map(false, true, OperatingMode.AUTOMATIC, true, false, Demand.SPACE_HEATING));
		assertEquals(HeatPumpOperatingState.DOMESTIC_HOT_WATER,
				map(false, true, OperatingMode.AUTOMATIC, true, false, Demand.DOMESTIC_HOT_WATER));
		assertEquals(HeatPumpOperatingState.COOLING,
				map(false, true, OperatingMode.COOLING, true, true, Demand.COOLING));
	}

	@Test
	void reversingValveWhileHeatingIsDefrost() {
		assertEquals(HeatPumpOperatingState.DEFROST,
				map(false, true, OperatingMode.AUTOMATIC, true, true, Demand.SPACE_HEATING));
	}
}
