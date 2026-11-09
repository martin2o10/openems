package io.openems.edge.controller.heatpump.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.edge.controller.heatpump.single.Mode;
import io.openems.edge.controller.heatpump.single.Params;
import io.openems.edge.heat.api.HeatPumpProfile.ApplyPower;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

class PowerDistributionTest {

	private static Params params(String id, Mode mode, Integer activePower, ApplyPower applyPower) {
		return new Params(id, mode, activePower, new HeatPumpAbilities(applyPower, 1200, 600));
	}

	private static final ApplyPower CONTINUOUS = new ApplyPower.Continuous(800, 3500);

	@Test
	void surplusIsCalculatedLikeEvse() {
		var params = List.of(params("0", Mode.SURPLUS, 1000, CONTINUOUS),
				params("1", Mode.OWN_CONTROL, 2000, CONTINUOUS));

		// Feed-in 1500 W, battery charges with 500 W
		assertEquals(3000, SurplusDistribution.calculateSurplus(params, -1500, -500));
		// Own-control consumption of heat pump 1 is not taken as surplus
		assertEquals(0, SurplusDistribution.calculateSurplus(params, 2500, 0));
	}

	@Test
	void ownControlByDefault() {
		var result = PowerDistribution.calculate(List.of(params("0", Mode.OWN_CONTROL, 0, CONTINUOUS)), -5000, 0);

		assertEquals(HeatPumpAction.OWN_CONTROL, result.actions().get("0"));
	}

	@Test
	void surplusIsDistributedInOrder() {
		var params = List.of(params("0", Mode.SURPLUS, 0, CONTINUOUS),
				params("1", Mode.SURPLUS, 0, new ApplyPower.SgReady(1500, 3000)));

		var result = PowerDistribution.calculate(params, -4500, 0);

		assertEquals(4500, result.surplus());
		assertEquals(new HeatPumpAction.Power(3500, false), result.actions().get("0"));
		assertEquals(HeatPumpAction.OWN_CONTROL, result.actions().get("1")); // only 1000 W left

		result = PowerDistribution.calculate(params, -5000, 0);
		assertEquals(new HeatPumpAction.Power(1500, false), result.actions().get("1"));
	}

	@Test
	void surplusBelowMinPowerIsOwnControl() {
		var result = PowerDistribution.calculate(List.of(params("0", Mode.SURPLUS, 0, CONTINUOUS)), -500, 0);

		assertEquals(HeatPumpAction.OWN_CONTROL, result.actions().get("0"));
	}

	@Test
	void forceUsesMaxPower() {
		var result = PowerDistribution.calculate(List.of(params("0", Mode.FORCE, 0, CONTINUOUS)), 0, 0);

		assertEquals(new HeatPumpAction.Power(3500, true), result.actions().get("0"));
	}
}
