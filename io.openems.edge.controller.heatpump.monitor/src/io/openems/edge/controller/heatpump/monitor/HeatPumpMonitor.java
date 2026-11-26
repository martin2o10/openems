package io.openems.edge.controller.heatpump.monitor;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.AccessMode;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface HeatPumpMonitor extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Is the heat pump currently running?
		 *
		 * <p>
		 * The running state is derived either from a configured running channel or
		 * from the electrical power, and it is debounced via the configured minimum
		 * run and pause times.
		 *
		 * <ul>
		 * <li>Type: Boolean
		 * </ul>
		 */
		HEAT_PUMP_RUNNING(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Total number of heat pump starts since activation of this Component.
		 *
		 * <p>
		 * The counter value is initialized from Timedata on startup and incremented
		 * continuously afterwards. Values for specific periods (e.g. per day) can be
		 * evaluated via the difference of the Timedata history of this channel.
		 *
		 * <ul>
		 * <li>Type: Integer
		 * </ul>
		 */
		HEAT_PUMP_STARTS_TOTAL(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Total running time of the heat pump in [s] since activation of this
		 * Component.
		 *
		 * <p>
		 * The value is initialized from Timedata on startup and incremented
		 * continuously afterwards. Values for specific periods (e.g. per day) can be
		 * evaluated via the difference of the Timedata history of this channel.
		 *
		 * <ul>
		 * <li>Type: Long
		 * <li>Unit: sec_Σ
		 * </ul>
		 */
		HEAT_PUMP_RUN_TIME_TOTAL(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Duration of the last completed run of the heat pump in [s].
		 *
		 * <ul>
		 * <li>Type: Long
		 * <li>Unit: sec
		 * </ul>
		 */
		HEAT_PUMP_LAST_RUN_DURATION(Doc.of(OpenemsType.LONG)//
				.unit(Unit.SECONDS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Duration of the last completed pause between two runs of the heat pump in
		 * [s].
		 *
		 * <ul>
		 * <li>Type: Long
		 * <li>Unit: sec
		 * </ul>
		 */
		HEAT_PUMP_LAST_PAUSE_DURATION(Doc.of(OpenemsType.LONG)//
				.unit(Unit.SECONDS)//
				.persistencePriority(PersistencePriority.HIGH)//
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

	/**
	 * Gets the Channel for {@link ChannelId#HEAT_PUMP_RUNNING}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getHeatPumpRunningChannel() {
		return this.channel(ChannelId.HEAT_PUMP_RUNNING);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HEAT_PUMP_RUNNING}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHeatPumpRunning(boolean value) {
		this.getHeatPumpRunningChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEAT_PUMP_STARTS_TOTAL}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getHeatPumpStartsTotalChannel() {
		return this.channel(ChannelId.HEAT_PUMP_STARTS_TOTAL);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEAT_PUMP_RUN_TIME_TOTAL}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getHeatPumpRunTimeTotalChannel() {
		return this.channel(ChannelId.HEAT_PUMP_RUN_TIME_TOTAL);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEAT_PUMP_LAST_RUN_DURATION}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getHeatPumpLastRunDurationChannel() {
		return this.channel(ChannelId.HEAT_PUMP_LAST_RUN_DURATION);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEAT_PUMP_LAST_PAUSE_DURATION}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getHeatPumpLastPauseDurationChannel() {
		return this.channel(ChannelId.HEAT_PUMP_LAST_PAUSE_DURATION);
	}
}
