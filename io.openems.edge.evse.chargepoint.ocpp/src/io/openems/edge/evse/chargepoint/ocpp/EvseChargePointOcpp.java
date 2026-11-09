package io.openems.edge.evse.chargepoint.ocpp;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Interface for a charging station connected via OCPP 1.6J.
 *
 * <p>
 * Meter values are mapped directly to the {@link ElectricityMeter} Channels and
 * are therefore intentionally missing here.
 */
public interface EvseChargePointOcpp extends EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		COMMUNICATION_FAILED(Doc.of(Level.FAULT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Communication to wallbox Failed "//
						+ "| Keine Verbindung zur Ladestation "//
						+ "| Bitte überprüfen Sie die OCPP-Verbindung der Ladestation zum OCPP-Server")), //
		STATUS(Doc.of(ConnectorStatus.values())//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Connector status")), //
		PHASE_SWITCHING_SUPPORTED(Doc.of(BOOLEAN)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Charging station supports switching from three to one phase")), //
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.unit(Unit.AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)//
				.text("Charging current")), //
		DEBUG_SET_NUMBER_OF_PHASES(Doc.of(INTEGER)), //
		SET_NUMBER_OF_PHASES(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.persistencePriority(PersistencePriority.LOW)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_NUMBER_OF_PHASES)//
				.text("Number of phases: 1 or 3")) //
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
	 * Gets the Channel for {@link ChannelId#STATUS}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getStatusChannel() {
		return this.channel(ChannelId.STATUS);
	}

	/**
	 * Gets the {@link ConnectorStatus}.
	 *
	 * @return the {@link ConnectorStatus}
	 */
	public default ConnectorStatus getStatus() {
		return this.getStatusChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCHING_SUPPORTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getPhaseSwitchingSupportedChannel() {
		return this.channel(ChannelId.PHASE_SWITCHING_SUPPORTED);
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
	 * @param value the value in [A]
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetChargingCurrent(int value) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_NUMBER_OF_PHASES}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetNumberOfPhasesChannel() {
		return this.channel(ChannelId.SET_NUMBER_OF_PHASES);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_NUMBER_OF_PHASES}.
	 *
	 * @param value 1 or 3
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetNumberOfPhases(int value) throws OpenemsNamedException {
		this.getSetNumberOfPhasesChannel().setNextWriteValue(value);
	}
}
