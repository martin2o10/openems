package io.openems.edge.controller.heatpump.cluster;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerHeatPumpCluster extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SURPLUS_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Surplus power available for heat pumps") //
				.persistencePriority(PersistencePriority.HIGH)), //
		GRID_LIMIT_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Heat pump power is reduced to limit the grid buy power") //
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
}
