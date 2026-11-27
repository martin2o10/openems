package io.openems.edge.controller.heatpump.monitor;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a simple, simulated temperature sensor component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyTemperatureSensor extends AbstractOpenemsComponent implements OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/** Temperature in [°C]. */
		TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_ONLY)), //

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

	public DummyTemperatureSensor(String id) {
		super(OpenemsComponent.ChannelId.values(), DummyTemperatureSensor.ChannelId.values());
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}
}
