package io.openems.edge.heat.broetje.neo;

import io.openems.edge.heat.api.HeatPumpOperatingState;

/**
 * Derives the {@link HeatPumpOperatingState} from the NEO-RKM status
 * registers.
 */
public final class OperatingStateMapper {

	private OperatingStateMapper() {
	}

	/**
	 * Maps the device status to a {@link HeatPumpOperatingState}.
	 *
	 * @param fault         fault active (register 26)
	 * @param released      grid operator release (register 32)
	 * @param mode          operating mode (register 100)
	 * @param running       heat pump running (register 25)
	 * @param defrostValve  4-way valve active (register 27)
	 * @param demand        current demand (register 41)
	 * @return the {@link HeatPumpOperatingState}
	 */
	public static HeatPumpOperatingState map(Boolean fault, Boolean released, OperatingMode mode, Boolean running,
			Boolean defrostValve, Demand demand) {
		if (Boolean.TRUE.equals(fault)) {
			return HeatPumpOperatingState.FAULT;
		}
		if (Boolean.FALSE.equals(released) || mode == OperatingMode.GRID_OPERATOR_LOCK) {
			return HeatPumpOperatingState.BLOCKED;
		}
		if (running == null) {
			return HeatPumpOperatingState.UNDEFINED;
		}
		if (!running) {
			return HeatPumpOperatingState.OFF;
		}
		if (Boolean.TRUE.equals(defrostValve) && demand != Demand.COOLING) {
			return HeatPumpOperatingState.DEFROST;
		}
		if (demand == null) {
			return HeatPumpOperatingState.SPACE_HEATING;
		}
		return switch (demand) {
		case COOLING -> HeatPumpOperatingState.COOLING;
		case DOMESTIC_HOT_WATER -> HeatPumpOperatingState.DOMESTIC_HOT_WATER;
		case SPACE_HEATING, NONE, UNDEFINED -> HeatPumpOperatingState.SPACE_HEATING;
		};
	}
}
