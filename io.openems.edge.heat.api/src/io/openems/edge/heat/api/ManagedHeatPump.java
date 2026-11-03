package io.openems.edge.heat.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a heat pump whose electric power consumption can be controlled.
 *
 * <p>
 * The nature is independent of the device interface (SG-Ready, Modbus, EEBUS,
 * ...). It offers two separate setpoints, because real interfaces distinguish
 * between them:
 *
 * <ul>
 * <li>{@link ChannelId#ACTIVE_POWER_LIMIT}: a hard upper limit ("must not
 * consume more than"), e.g. for §14a EnWG or EEBUS LPC
 * <li>{@link ChannelId#TARGET_ACTIVE_POWER}: a preferred consumption ("should
 * consume"), e.g. to use PV surplus
 * </ul>
 *
 * <p>
 * Both setpoints are optional. If no setpoint is given, the heat pump runs
 * autonomously. The internal control of the heat pump (frost protection,
 * minimum run and off times, comfort) always has precedence.
 *
 * <p>
 * The measured electric power consumption is provided by implementing
 * {@code ElectricityMeter}; thermal values can be provided by implementing
 * {@link Heat}.
 */
@ProviderType
public interface ManagedHeatPump extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Debug value of the last written {@link ChannelId#ACTIVE_POWER_LIMIT}.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		DEBUG_ACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)), //

		/**
		 * Hard upper limit for the electric power consumption.
		 *
		 * <p>
		 * The heat pump must not consume more than this value. 'null' means no
		 * limit.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		ACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_ACTIVE_POWER_LIMIT)//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Debug value of the last written {@link ChannelId#TARGET_ACTIVE_POWER}.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		DEBUG_TARGET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)), //

		/**
		 * Preferred electric power consumption.
		 *
		 * <p>
		 * The heat pump should consume this value, as far as its internal control
		 * allows. The value is always limited by {@link ChannelId#ACTIVE_POWER_LIMIT}.
		 * 'null' means no preference.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		TARGET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_TARGET_ACTIVE_POWER)//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Minimum electric power consumption while the compressor is running (lower
		 * end of the modulation range).
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MIN_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.LOW)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Maximum electric power consumption (upper end of the modulation range).
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.LOW)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Minimum run time of the compressor after a start.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: s
		 * </ul>
		 */
		MIN_RUN_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.persistencePriority(PersistencePriority.LOW)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Minimum off time of the compressor after a stop.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: s
		 * </ul>
		 */
		MIN_OFF_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.persistencePriority(PersistencePriority.LOW)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * How precisely the power consumption can be controlled.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: {@link HeatPumpControlCapability}
		 * </ul>
		 */
		CONTROL_CAPABILITY(Doc.of(HeatPumpControlCapability.values())//
				.persistencePriority(PersistencePriority.LOW)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Current operating state.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: {@link HeatPumpOperatingState}
		 * </ul>
		 */
		OPERATING_STATE(Doc.of(HeatPumpOperatingState.values())//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Current flow temperature. Optional.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: dezidegree Celsius
		 * </ul>
		 */
		FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Debug value of the last written
		 * {@link ChannelId#TARGET_TEMPERATURE_OFFSET}.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: dezidegree Celsius (temperature difference)
		 * </ul>
		 */
		DEBUG_TARGET_TEMPERATURE_OFFSET(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		/**
		 * Requested increase of the storage target temperature, e.g. to store heat
		 * from PV surplus or cheap energy. Optional.
		 *
		 * <p>
		 * The offset is added by the heat pump to its own target temperature. It
		 * never replaces the comfort setpoints of the heat pump. It is limited by
		 * {@link ChannelId#MAX_TEMPERATURE_OFFSET}. 'null' or '0' means no
		 * increase.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: dezidegree Celsius (temperature difference)
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		TARGET_TEMPERATURE_OFFSET(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_TARGET_TEMPERATURE_OFFSET)//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximum allowed {@link ChannelId#TARGET_TEMPERATURE_OFFSET}. Typically
		 * configured by the user to protect efficiency and comfort. 'null' means
		 * that the heat pump does not support a temperature offset.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: dezidegree Celsius (temperature difference)
		 * </ul>
		 */
		MAX_TEMPERATURE_OFFSET(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.LOW)//
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getActivePowerLimitChannel() {
		return this.channel(ChannelId.ACTIVE_POWER_LIMIT);
	}

	/**
	 * Gets the Active Power Limit in [W]. See {@link ChannelId#ACTIVE_POWER_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerLimit() {
		return this.getActivePowerLimitChannel().value();
	}

	/**
	 * Sets the Active Power Limit in [W]. See {@link ChannelId#ACTIVE_POWER_LIMIT}.
	 *
	 * @param value the Integer value; 'null' for no limit
	 * @throws OpenemsNamedException on error
	 */
	public default void setActivePowerLimit(Integer value) throws OpenemsNamedException {
		this.getActivePowerLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_ACTIVE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugActivePowerLimitChannel() {
		return this.channel(ChannelId.DEBUG_ACTIVE_POWER_LIMIT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TARGET_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getTargetActivePowerChannel() {
		return this.channel(ChannelId.TARGET_ACTIVE_POWER);
	}

	/**
	 * Gets the Target Active Power in [W]. See
	 * {@link ChannelId#TARGET_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTargetActivePower() {
		return this.getTargetActivePowerChannel().value();
	}

	/**
	 * Sets the Target Active Power in [W]. See
	 * {@link ChannelId#TARGET_ACTIVE_POWER}.
	 *
	 * @param value the Integer value; 'null' for no preference
	 * @throws OpenemsNamedException on error
	 */
	public default void setTargetActivePower(Integer value) throws OpenemsNamedException {
		this.getTargetActivePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_TARGET_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugTargetActivePowerChannel() {
		return this.channel(ChannelId.DEBUG_TARGET_ACTIVE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinActivePowerChannel() {
		return this.channel(ChannelId.MIN_ACTIVE_POWER);
	}

	/**
	 * Gets the Minimum Active Power in [W]. See {@link ChannelId#MIN_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinActivePower() {
		return this.getMinActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinActivePower(Integer value) {
		this.getMinActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxActivePowerChannel() {
		return this.channel(ChannelId.MAX_ACTIVE_POWER);
	}

	/**
	 * Gets the Maximum Active Power in [W]. See {@link ChannelId#MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxActivePower() {
		return this.getMaxActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxActivePower(Integer value) {
		this.getMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_RUN_TIME}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinRunTimeChannel() {
		return this.channel(ChannelId.MIN_RUN_TIME);
	}

	/**
	 * Gets the Minimum Run Time in [s]. See {@link ChannelId#MIN_RUN_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinRunTime() {
		return this.getMinRunTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_RUN_TIME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinRunTime(Integer value) {
		this.getMinRunTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_OFF_TIME}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinOffTimeChannel() {
		return this.channel(ChannelId.MIN_OFF_TIME);
	}

	/**
	 * Gets the Minimum Off Time in [s]. See {@link ChannelId#MIN_OFF_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinOffTime() {
		return this.getMinOffTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_OFF_TIME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinOffTime(Integer value) {
		this.getMinOffTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_CAPABILITY}.
	 *
	 * @return the Channel
	 */
	public default Channel<HeatPumpControlCapability> getControlCapabilityChannel() {
		return this.channel(ChannelId.CONTROL_CAPABILITY);
	}

	/**
	 * Gets the Control Capability. See {@link ChannelId#CONTROL_CAPABILITY}.
	 *
	 * @return the {@link HeatPumpControlCapability}
	 */
	public default HeatPumpControlCapability getControlCapability() {
		return this.getControlCapabilityChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONTROL_CAPABILITY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setControlCapability(HeatPumpControlCapability value) {
		this.getControlCapabilityChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<HeatPumpOperatingState> getOperatingStateChannel() {
		return this.channel(ChannelId.OPERATING_STATE);
	}

	/**
	 * Gets the Operating State. See {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the {@link HeatPumpOperatingState}
	 */
	public default HeatPumpOperatingState getOperatingState() {
		return this.getOperatingStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OPERATING_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOperatingState(HeatPumpOperatingState value) {
		this.getOperatingStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FLOW_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFlowTemperatureChannel() {
		return this.channel(ChannelId.FLOW_TEMPERATURE);
	}

	/**
	 * Gets the Flow Temperature in [dezidegree Celsius]. See
	 * {@link ChannelId#FLOW_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFlowTemperature() {
		return this.getFlowTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FLOW_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFlowTemperature(Integer value) {
		this.getFlowTemperatureChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TARGET_TEMPERATURE_OFFSET}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getTargetTemperatureOffsetChannel() {
		return this.channel(ChannelId.TARGET_TEMPERATURE_OFFSET);
	}

	/**
	 * Gets the Target Temperature Offset in [dezidegree Celsius]. See
	 * {@link ChannelId#TARGET_TEMPERATURE_OFFSET}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTargetTemperatureOffset() {
		return this.getTargetTemperatureOffsetChannel().value();
	}

	/**
	 * Sets the Target Temperature Offset in [dezidegree Celsius]. See
	 * {@link ChannelId#TARGET_TEMPERATURE_OFFSET}.
	 *
	 * @param value the Integer value; 'null' for no increase
	 * @throws OpenemsNamedException on error
	 */
	public default void setTargetTemperatureOffset(Integer value) throws OpenemsNamedException {
		this.getTargetTemperatureOffsetChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_TARGET_TEMPERATURE_OFFSET}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugTargetTemperatureOffsetChannel() {
		return this.channel(ChannelId.DEBUG_TARGET_TEMPERATURE_OFFSET);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_TEMPERATURE_OFFSET}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxTemperatureOffsetChannel() {
		return this.channel(ChannelId.MAX_TEMPERATURE_OFFSET);
	}

	/**
	 * Gets the Maximum Temperature Offset in [dezidegree Celsius]. See
	 * {@link ChannelId#MAX_TEMPERATURE_OFFSET}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxTemperatureOffset() {
		return this.getMaxTemperatureOffsetChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_TEMPERATURE_OFFSET} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxTemperatureOffset(Integer value) {
		this.getMaxTemperatureOffsetChannel().setNextValue(value);
	}
}
