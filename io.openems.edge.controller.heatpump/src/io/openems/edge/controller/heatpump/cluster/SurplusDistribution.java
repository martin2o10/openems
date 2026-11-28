package io.openems.edge.controller.heatpump.cluster;

import java.util.List;
import java.util.Map;

import io.openems.edge.controller.heatpump.single.Params;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

/**
 * Distributes surplus power to heat pumps in {@code SURPLUS} mode.
 *
 * <p>
 * The surplus is calculated like in the EVSE Cluster: the current power of all
 * heat pumps that take surplus, minus grid buy and storage discharge. The
 * surplus is then distributed in the configured order of the heat pumps.
 *
 * <p>
 * This class has no dependencies on other goals and can be removed together
 * with its call in {@link PowerDistribution}.
 */
public final class SurplusDistribution {

	private SurplusDistribution() {
	}

	/**
	 * Calculates the available surplus power.
	 *
	 * @param params            the {@link Params} of all heat pumps
	 * @param gridActivePower   the grid power [W]; positive for buy from grid
	 * @param essDischargePower the storage discharge power [W]; positive for
	 *                          discharge
	 * @return the surplus power [W]; never negative
	 */
	public static int calculateSurplus(List<Params> params, int gridActivePower, int essDischargePower) {
		var heatPumpPower = params.stream() //
				.filter(Params::takesSurplus) //
				.mapToInt(p -> p.activePower() == null ? 0 : p.activePower()) //
				.sum();
		return Math.max(0, heatPumpPower - gridActivePower - essDischargePower);
	}

	/**
	 * Distributes the surplus power and stores the resulting actions.
	 *
	 * @param params  the {@link Params} of all heat pumps, in order of priority
	 * @param surplus the available surplus power [W]
	 * @param actions the map of actions per Controller-ID; updated in place
	 */
	public static void distribute(List<Params> params, int surplus, Map<String, HeatPumpAction> actions) {
		var remaining = surplus;
		for (var p : params) {
			if (!p.takesSurplus()) {
				continue;
			}
			var power = p.abilities().applyPower().fit(remaining);
			if (power > 0) {
				actions.put(p.ctrlSingleId(), new HeatPumpAction.Power(power, false));
				remaining -= power;
			} else {
				actions.put(p.ctrlSingleId(), HeatPumpAction.OWN_CONTROL);
			}
		}
	}
}
