package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Interface for the ABL eM4 charging station.
 *
 * <p>
 * Values that have a matching {@link ElectricityMeter} Channel - voltages,
 * currents, active power and energy - are mapped directly to that Channel and
 * are therefore intentionally missing here.
 */
public interface EvseAblEm4 extends EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		OUTLET_STATUS(Doc.of(OutletStatus.values())//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Outlet status")), //
		FIRMWARE_VERSION(Doc.of(STRING)//
				.text("Firmware version")), //
		SERIAL_NUMBER(Doc.of(STRING)//
				.text("Serial number")), //
		HARDWARE_MAX_CURRENT(Doc.of(INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Maximum current of the installation (Idefault)")), //
		ACTUAL_APPLIED_MAX_CURRENT(Doc.of(INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Maximum current the EV is allowed to draw (Ic)")), //
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)//
				.text("Charging current (Icmax); 0 stops charging")) //
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
	 * Gets the Channel for {@link ChannelId#OUTLET_STATUS}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getOutletStatusChannel() {
		return this.channel(ChannelId.OUTLET_STATUS);
	}

	/**
	 * Gets the {@link OutletStatus}.
	 *
	 * @return the {@link OutletStatus}
	 */
	public default OutletStatus getOutletStatus() {
		return this.getOutletStatusChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HARDWARE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getHardwareMaxCurrentChannel() {
		return this.channel(ChannelId.HARDWARE_MAX_CURRENT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_CHARGING_CURRENT}.
	 *
	 * @param value the value in [mA]
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetChargingCurrent(int value) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(value);
	}
}
