package io.openems.edge.controller.heatpump.cluster;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.openems.edge.controller.heatpump.single.Params;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

/**
 * Calculates the {@link HeatPumpAction} for every heat pump of a
 * Controller.HeatPump.Cluster.
 *
 * <ol>
 * <li>Base action per Mode: {@code OWN_CONTROL} → own control, {@code FORCE} →
 * maximum power
 * <li>{@link SurplusDistribution}: heat pumps in {@code SURPLUS} mode
 * </ol>
 *
 * @param actions the {@link HeatPumpAction} per Controller-ID
 * @param surplus the calculated surplus power [W]
 */
public record PowerDistribution(Map<String, HeatPumpAction> actions, int surplus) {

	/**
	 * Calculates the {@link PowerDistribution}.
	 *
	 * @param params            the {@link Params} of all heat pumps, in order of
	 *                          priority
	 * @param gridActivePower   the grid power [W]; positive for buy from grid
	 * @param essDischargePower the storage discharge power [W]; positive for
	 *                          discharge
	 * @return the {@link PowerDistribution}
	 */
	public static PowerDistribution calculate(List<Params> params, int gridActivePower, int essDischargePower) {
		var actions = new LinkedHashMap<String, HeatPumpAction>();
		for (var p : params) {
			actions.put(p.ctrlSingleId(), switch (p.mode()) {
			case FORCE -> p.abilities().canApplyPower() //
					? new HeatPumpAction.Power(p.abilities().applyPower().maxPower(), true) //
					: HeatPumpAction.OWN_CONTROL;
			case OWN_CONTROL, SURPLUS, SMART -> HeatPumpAction.OWN_CONTROL;
			});
		}

		var surplus = SurplusDistribution.calculateSurplus(params, gridActivePower, essDischargePower);
		SurplusDistribution.distribute(params, surplus, actions);

		return new PowerDistribution(actions, surplus);
	}
}
