package io.openems.edge.controller.heatpump.single;

import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;

/**
 * Parameters of one Controller.HeatPump.Single, used by the
 * Controller.HeatPump.Cluster to distribute power.
 *
 * @param ctrlSingleId the Component-ID of the Controller.HeatPump.Single
 * @param heatPumpId   the Component-ID of the ManagedHeatPump
 * @param mode         the effective {@link Mode}; for {@link Mode#SMART} the
 *                     Mode of the current Energy Scheduler period
 * @param activePower  the measured electric power [W]; possibly null
 * @param isRunning    is the compressor currently running?
 * @param abilities    the {@link HeatPumpAbilities}
 */
public record Params(String ctrlSingleId, String heatPumpId, Mode mode, Integer activePower, boolean isRunning,
		HeatPumpAbilities abilities) {

	/**
	 * Can this heat pump take surplus power?.
	 *
	 * @return true if the mode is {@link Mode#SURPLUS} and the power can be
	 *         applied
	 */
	public boolean takesSurplus() {
		return this.mode.isSurplus() && this.abilities != null && this.abilities.canApplyPower();
	}
}
