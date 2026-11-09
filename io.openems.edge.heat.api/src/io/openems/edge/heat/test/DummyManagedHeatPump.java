package io.openems.edge.heat.test;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.heat.api.HeatPumpOperatingState;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Provides a simple, simulated {@link ManagedHeatPump} Component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyManagedHeatPump extends AbstractDummyOpenemsComponent<DummyManagedHeatPump>
		implements ManagedHeatPump, ElectricityMeter, OpenemsComponent {

	private HeatPumpAbilities abilities = null;
	private boolean readOnly = false;
	private HeatPumpAction lastAction = null;

	public DummyManagedHeatPump(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedHeatPump.ChannelId.values());
	}

	@Override
	protected DummyManagedHeatPump self() {
		return this;
	}

	/**
	 * Set the {@link HeatPumpAbilities} of this {@link DummyManagedHeatPump}.
	 *
	 * @param abilities the {@link HeatPumpAbilities}
	 * @return myself
	 */
	public DummyManagedHeatPump withAbilities(HeatPumpAbilities abilities) {
		this.abilities = abilities;
		return this.self();
	}

	/**
	 * Set read-only mode of this {@link DummyManagedHeatPump}.
	 *
	 * @param readOnly true for read-only
	 * @return myself
	 */
	public DummyManagedHeatPump withReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
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
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_POWER} of this
	 * {@link DummyManagedHeatPump}.
	 *
	 * @param value the active power [W]
	 * @return myself
	 */
	public DummyManagedHeatPump withActivePower(Integer value) {
		withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, value);
		return this.self();
	}

	@Override
	public HeatPumpAbilities getHeatPumpAbilities() {
		return this.abilities;
	}

	@Override
	public void apply(HeatPumpAction action) {
		this.lastAction = action;
		this._setAction(action);
	}

	@Override
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Gets the last applied {@link HeatPumpAction}.
	 *
	 * @return the {@link HeatPumpAction}; null if none was applied
	 */
	public HeatPumpAction getLastAction() {
		return this.lastAction;
	}
}
