package io.openems.edge.evse.chargepoint.goe;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Interface for the go-e Gemini charging station.
 *
 * <p>
 * Values that have a matching {@link ElectricityMeter} Channel - voltages,
 * currents, active power and energy - are mapped directly to that Channel and
 * are therefore intentionally missing here.
 */
public interface EvseGoe extends EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		CAR_STATE(Doc.of(CarState.values())//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Car state")), //
		ERROR(Doc.of(ErrorCode.values())//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Error")), //
		VOLTAGE_N(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage N")), //
		POWER_FACTOR_L1(Doc.of(INTEGER)//
				.unit(Unit.PERCENT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor L1")), //
		POWER_FACTOR_L2(Doc.of(INTEGER)//
				.unit(Unit.PERCENT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor L2")), //
		POWER_FACTOR_L3(Doc.of(INTEGER)//
				.unit(Unit.PERCENT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor L3")), //
		FIRMWARE_VERSION(Doc.of(STRING)//
				.text("Firmware version")), //
		SERIAL_NUMBER(Doc.of(STRING)//
				.text("Serial number")), //
		PHASES(Doc.of(INTEGER)//
				.text("Phase states before/after contactor")), //
		HARDWARE_MAX_CURRENT(Doc.of(FLOAT)//
				.unit(Unit.AMPERE)//
				.text("Maximum hardware current")), //
		PHASE_SWITCH_MODE(Doc.of(INTEGER)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Phase switch mode")), //
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.MEDIUM)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)//
				.text("Charging current")), //
		DEBUG_SET_ALLOW_CHARGING(Doc.of(BOOLEAN)), //
		SET_ALLOW_CHARGING(Doc.of(BOOLEAN)//
				.accessMode(READ_WRITE)//
				.persistencePriority(PersistencePriority.MEDIUM)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_ALLOW_CHARGING)//
				.text("Allow charging")) //
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
	 * Gets the Channel for {@link ChannelId#CAR_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getCarStateChannel() {
		return this.channel(ChannelId.CAR_STATE);
	}

	/**
	 * Gets the {@link CarState}.
	 *
	 * @return the {@link CarState}
	 */
	public default CarState getCarState() {
		return this.getCarStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HARDWARE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getHardwareMaxCurrentChannel() {
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

	/**
	 * Gets the Channel for {@link ChannelId#SET_ALLOW_CHARGING}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetAllowChargingChannel() {
		return this.channel(ChannelId.SET_ALLOW_CHARGING);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_ALLOW_CHARGING}.
	 *
	 * @param value true to allow charging
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetAllowCharging(boolean value) throws OpenemsNamedException {
		this.getSetAllowChargingChannel().setNextWriteValue(value);
	}
}
