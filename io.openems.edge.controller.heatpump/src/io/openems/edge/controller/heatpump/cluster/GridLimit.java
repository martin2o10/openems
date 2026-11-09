package io.openems.edge.controller.heatpump.cluster;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import io.openems.edge.controller.heatpump.single.Params;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

/**
 * Limits the grid buy power by reducing the power of heat pumps that the
 * Controller increased (e.g. in {@code FORCE} mode in a cheap period).
 *
 * <p>
 * The own control of the heat pumps is never restricted; the heat pumps are not
 * locked. Heat pumps are reduced in reverse order of priority.
 *
 * <p>
 * This class has no dependencies on other goals and can be removed together
 * with its call in {@link PowerDistribution}.
 */
public final class GridLimit {

	private GridLimit() {
	}

	/**
	 * Reduces the actions if the grid buy power exceeds the limit.
	 *
	 * @param params          the {@link Params} of all heat pumps, in order of
	 *                        priority
	 * @param gridActivePower the grid power [W]; positive for buy from grid
	 * @param maxGridBuyPower the maximum grid buy power [W]; 0 or negative to
	 *                        disable
	 * @param actions         the map of actions per Controller-ID; updated in
	 *                        place
	 * @return true if the limit was active
	 */
	public static boolean apply(List<Params> params, int gridActivePower, int maxGridBuyPower,
			Map<String, HeatPumpAction> actions) {
		if (maxGridBuyPower <= 0 || gridActivePower <= maxGridBuyPower) {
			return false;
		}
		var excess = gridActivePower - maxGridBuyPower;
		for (var p : Lists.reverse(params)) {
			if (excess <= 0) {
				break;
			}
			if (!(actions.get(p.ctrlSingleId()) instanceof HeatPumpAction.Power power)) {
				continue;
			}
			var reduced = power.power() - excess;
			if (reduced >= p.abilities().applyPower().minPower()) {
				actions.put(p.ctrlSingleId(), new HeatPumpAction.Power(reduced, power.force()));
				excess = 0;
			} else {
				actions.put(p.ctrlSingleId(), HeatPumpAction.OWN_CONTROL);
				excess -= power.power();
			}
		}
		return true;
	}
}
