package io.openems.edge.heat.broetje.neo;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatBroetjeNeo extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		OUTDOOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		DOMESTIC_HOT_WATER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		COMPRESSOR_RUNNING(Doc.of(OpenemsType.BOOLEAN)), //
		DEFROST_VALVE_ACTIVE(Doc.of(OpenemsType.BOOLEAN)), //
		COMPRESSOR_SPEED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH)), //
		COEFFICIENT_OF_PERFORMANCE(Doc.of(OpenemsType.DOUBLE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		GRID_OPERATOR_RELEASE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Grid operator (EVU) release; false means the heat pump is locked")), //
		DEMAND(Doc.of(Demand.values())), //
		OPERATING_MODE(Doc.of(OperatingMode.values())), //
		DEVICE_FAULT(Doc.of(Level.FAULT) //
				.text("The heat pump reports a fault")), //

		/**
		 * Electric power setpoint (register 125, demand profile 3). Written only in
		 * non-read-only mode and only every write interval.
		 */
		POWER_SETPOINT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		/**
		 * Grid operator release value (register 149); 0 locks the heat pump.
		 */
		GRID_OPERATOR_RELEASE_VALUE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		/**
		 * Activates {@link #GRID_OPERATOR_RELEASE_VALUE} (register 150).
		 */
		GRID_OPERATOR_RELEASE_ACTIVATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
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
