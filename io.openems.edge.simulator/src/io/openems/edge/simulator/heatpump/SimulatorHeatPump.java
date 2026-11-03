package io.openems.edge.simulator.heatpump;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface SimulatorHeatPump extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current coefficient of performance of the simulation.
		 */
		COEFFICIENT_OF_PERFORMANCE(Doc.of(OpenemsType.DOUBLE)), //

		/**
		 * Number of compressor starts since activation.
		 */
		STARTS(Doc.of(OpenemsType.INTEGER)), //

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
