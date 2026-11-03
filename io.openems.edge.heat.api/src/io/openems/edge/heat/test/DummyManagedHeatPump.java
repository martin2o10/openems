package io.openems.edge.heat.test;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.heat.api.HeatPumpControlCapability;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.ManagedHeatPump;

/**
 * Provides a simple, simulated {@link ManagedHeatPump} Component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyManagedHeatPump extends AbstractDummyOpenemsComponent<DummyManagedHeatPump>
		implements ManagedHeatPump, OpenemsComponent {

	public DummyManagedHeatPump(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ManagedHeatPump.ChannelId.values());
	}

	@Override
	protected DummyManagedHeatPump self() {
		return this;
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#MIN_ACTIVE_POWER} and
	 * {@link ManagedHeatPump.ChannelId#MAX_ACTIVE_POWER} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param min the minimum active power [W]
	 * @param max the maximum active power [W]
	 * @return myself
	 */
	public DummyManagedHeatPump withModulationRange(Integer min, Integer max) {
		withValue(this, ManagedHeatPump.ChannelId.MIN_ACTIVE_POWER, min);
		withValue(this, ManagedHeatPump.ChannelId.MAX_ACTIVE_POWER, max);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#MIN_RUN_TIME} and
	 * {@link ManagedHeatPump.ChannelId#MIN_OFF_TIME} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param minRunTime the minimum run time [s]
	 * @param minOffTime the minimum off time [s]
	 * @return myself
	 */
	public DummyManagedHeatPump withMinTimes(Integer minRunTime, Integer minOffTime) {
		withValue(this, ManagedHeatPump.ChannelId.MIN_RUN_TIME, minRunTime);
		withValue(this, ManagedHeatPump.ChannelId.MIN_OFF_TIME, minOffTime);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#CONTROL_CAPABILITY} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param value the {@link HeatPumpControlCapability}
	 * @return myself
	 */
	public DummyManagedHeatPump withControlCapability(HeatPumpControlCapability value) {
		withValue(this, ManagedHeatPump.ChannelId.CONTROL_CAPABILITY, value);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#OPERATING_STATE} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param value the {@link HeatPumpOperatingState}
	 * @return myself
	 */
	public DummyManagedHeatPump withOperatingState(HeatPumpOperatingState value) {
		withValue(this, ManagedHeatPump.ChannelId.OPERATING_STATE, value);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#FLOW_TEMPERATURE} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param value the flow temperature [dezidegree Celsius]
	 * @return myself
	 */
	public DummyManagedHeatPump withFlowTemperature(Integer value) {
		withValue(this, ManagedHeatPump.ChannelId.FLOW_TEMPERATURE, value);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#RETURN_TEMPERATURE} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param value the return temperature [dezidegree Celsius]
	 * @return myself
	 */
	public DummyManagedHeatPump withReturnTemperature(Integer value) {
		withValue(this, ManagedHeatPump.ChannelId.RETURN_TEMPERATURE, value);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#THERMAL_POWER} and
	 * {@link ManagedHeatPump.ChannelId#THERMAL_ENERGY} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param power  the thermal power [W]
	 * @param energy the cumulated thermal energy [Wh_Σ]
	 * @return myself
	 */
	public DummyManagedHeatPump withThermal(Integer power, Long energy) {
		withValue(this, ManagedHeatPump.ChannelId.THERMAL_POWER, power);
		withValue(this, ManagedHeatPump.ChannelId.THERMAL_ENERGY, energy);
		return this.self();
	}

	/**
	 * Set {@link ManagedHeatPump.ChannelId#MAX_TEMPERATURE_OFFSET} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param value the maximum temperature offset [dezidegree Celsius]
	 * @return myself
	 */
	public DummyManagedHeatPump withMaxTemperatureOffset(Integer value) {
		withValue(this, ManagedHeatPump.ChannelId.MAX_TEMPERATURE_OFFSET, value);
		return this.self();
	}
}
