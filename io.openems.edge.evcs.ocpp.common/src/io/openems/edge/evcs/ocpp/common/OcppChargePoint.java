package io.openems.edge.evcs.ocpp.common;

import java.util.UUID;

import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;

/**
 * A Charge-Point connector that is served by an {@link OcppServer}.
 *
 * <p>
 * In contrast to {@link AbstractManagedOcppEvcsComponent} this interface is
 * independent of the EVCS API, so that e.g. EVSE Charge-Points can use the OCPP
 * Server. The server forwards the relevant requests of the matching OCPP-ID and
 * Connector-ID; interpreting them is up to the implementation.
 */
public interface OcppChargePoint {

	/**
	 * Gets the configured OCPP-ID, i.e. the identifier the Charge-Point uses to
	 * connect to the server.
	 *
	 * @return the OCPP-ID
	 */
	public String getConfiguredOcppId();

	/**
	 * Gets the configured Connector-ID.
	 *
	 * @return the Connector-ID
	 */
	public int getConfiguredConnectorId();

	/**
	 * Called when the Charge-Point connected to the server.
	 *
	 * @param server    the {@link OcppServer}
	 * @param sessionId the session {@link UUID}
	 */
	public void onConnect(OcppServer server, UUID sessionId);

	/**
	 * Called when the Charge-Point disconnected from the server.
	 */
	public void onDisconnect();

	/**
	 * Called on a {@link MeterValuesRequest} for this Connector.
	 *
	 * @param request the {@link MeterValuesRequest}
	 */
	public void onMeterValues(MeterValuesRequest request);

	/**
	 * Called on a {@link StatusNotificationRequest} for this Connector.
	 *
	 * @param request the {@link StatusNotificationRequest}
	 */
	public void onStatusNotification(StatusNotificationRequest request);
}
