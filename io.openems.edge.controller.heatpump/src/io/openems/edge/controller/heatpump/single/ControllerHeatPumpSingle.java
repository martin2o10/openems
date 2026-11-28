package io.openems.edge.controller.heatpump.single;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;

public interface ControllerHeatPumpSingle extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ACTUAL_MODE(Doc.of(Mode.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ANTI_CYCLING_STATE(Doc.of(AntiCyclingState.values()) //
				.text("Current state of the anti-cycling protection") //
				.persistencePriority(PersistencePriority.HIGH)), //
		REQUESTED_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Power requested by the Cluster before anti-cycling protection") //
				.persistencePriority(PersistencePriority.HIGH)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Controller {@link Params}.
	 *
	 * @return the {@link Params}; null if the heat pump is not available
	 */
	public Params getParams();

	/**
	 * Applies the {@link HeatPumpAction} requested by the
	 * Controller.HeatPump.Cluster.
	 *
	 * @param mode   the actual {@link Mode}
	 * @param action the requested {@link HeatPumpAction}
	 */
	public void apply(Mode mode, HeatPumpAction action);
}
