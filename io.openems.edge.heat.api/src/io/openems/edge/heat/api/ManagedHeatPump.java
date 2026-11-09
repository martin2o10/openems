package io.openems.edge.heat.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.MeterType;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAbilities;
import io.openems.edge.heat.api.HeatPumpProfile.HeatPumpAction;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Represents a heat pump whose electric power consumption can be influenced.
 *
 * <p>
 * The nature is independent of the device interface (SG-Ready, Modbus, EEBUS,
 * ...). Like an EVSE Charge-Point, the device only declares its
 * {@link HeatPumpAbilities} and applies {@link HeatPumpAction}s; all decisions
 * are taken by Controllers. The internal control of the heat pump (frost
 * protection, comfort) always has precedence.
 *
 * <p>
 * The measured electric power consumption is provided via
 * {@link ElectricityMeter}; thermal values can be provided by implementing
 * {@link Heat}.
 */
@ProviderType
public interface ManagedHeatPump extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current operating state.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: {@link HeatPumpOperatingState}
		 * </ul>
		 */
		OPERATING_STATE(Doc.of(HeatPumpOperatingState.values())//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Current thermal power output; optional.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		THERMAL_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Type of the last applied {@link HeatPumpAction}.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: {@link HeatPumpActionType}
		 * </ul>
		 */
		ACTION(Doc.of(HeatPumpActionType.values())//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Electric power of the last applied
		 * {@link HeatPumpAction.Power}; null for other actions.
		 *
		 * <ul>
		 * <li>Interface: ManagedHeatPump
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTION_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
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
	 * Gets the {@link HeatPumpAbilities}.
	 *
	 * @return the {@link HeatPumpAbilities}; null if not available (e.g. no
	 *         communication)
	 */
	public HeatPumpAbilities getHeatPumpAbilities();

	/**
	 * Applies a {@link HeatPumpAction}. The action is only valid for the current
	 * Cycle; Controllers have to apply it in every Cycle.
	 *
	 * @param action the {@link HeatPumpAction}
	 */
	public void apply(HeatPumpAction action);

	/**
	 * Is this {@link ManagedHeatPump} read-only or read-write?.
	 *
	 * @return true for read-only
	 */
	public boolean isReadOnly();

	/**
	 * Gets the {@link MeterType} of a {@link ManagedHeatPump}
	 * {@link ElectricityMeter}.
	 *
	 * @return the {@link MeterType}
	 */
	@Override
	public default MeterType getMeterType() {
		return this.isReadOnly() //
				? MeterType.CONSUMPTION_METERED //
				: MeterType.MANAGED_CONSUMPTION_METERED;
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
	 * Gets the {@link HeatPumpOperatingState}. See
	 * {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the operating state
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
	 * Gets the Channel for {@link ChannelId#THERMAL_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getThermalPowerChannel() {
		return this.channel(ChannelId.THERMAL_POWER);
	}

	/**
	 * Gets the thermal power output in [W]. See {@link ChannelId#THERMAL_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getThermalPower() {
		return this.getThermalPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#THERMAL_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setThermalPower(Integer value) {
		this.getThermalPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set {@link ChannelId#ACTION} and
	 * {@link ChannelId#ACTION_POWER} for the given {@link HeatPumpAction}.
	 *
	 * @param action the applied {@link HeatPumpAction}
	 */
	public default void _setAction(HeatPumpAction action) {
		this.channel(ChannelId.ACTION).setNextValue(action == null ? null : action.type());
		this.channel(ChannelId.ACTION_POWER).setNextValue(//
				action instanceof HeatPumpAction.Power p ? p.power() : null);
	}
}
