package io.openems.edge.evcs.ocpp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import io.openems.edge.evcs.ocpp.common.OcppChargePoint;
import io.openems.edge.evcs.ocpp.common.OcppServer;

public class EvcsOcppServerTest {

	@Test
	public void testOcppChargePointOnExistingSession() {
		final var server = new EvcsOcppServer();
		final var sessionId = UUID.randomUUID();
		server.ocppSessions.put("goe1", sessionId);

		final var chargePoint = new RecordingChargePoint("goe1", 1);
		server.addOcppChargePoint(chargePoint);

		assertSame(server, chargePoint.server);
		assertEquals(sessionId, chargePoint.sessionId);
		assertEquals(List.of(chargePoint), server.getOcppChargePoints(sessionId, 1));
		assertTrue(server.getOcppChargePoints(sessionId, 2).isEmpty());
		assertTrue(server.getOcppChargePoints(sessionId, null).isEmpty());
		assertTrue(server.getOcppChargePoints(UUID.randomUUID(), 1).isEmpty());

		server.removeOcppChargePoint(chargePoint);
		assertNull(chargePoint.sessionId);
		assertTrue(server.getOcppChargePoints("goe1").isEmpty());
	}

	@Test
	public void testOcppChargePointWithoutSession() {
		final var server = new EvcsOcppServer();
		final var connector1 = new RecordingChargePoint("goe1", 1);
		final var connector2 = new RecordingChargePoint("goe1", 2);
		server.addOcppChargePoint(connector1);
		server.addOcppChargePoint(connector2);

		assertNull(connector1.sessionId);
		assertEquals(List.of(connector1, connector2), server.getOcppChargePoints("goe1"));
		assertTrue(server.getOcppChargePoints("unknown").isEmpty());

		final var sessionId = UUID.randomUUID();
		server.ocppSessions.put("goe1", sessionId);
		assertEquals(List.of(connector2), server.getOcppChargePoints(sessionId, 2));
	}

	private static class RecordingChargePoint implements OcppChargePoint {

		private final String ocppId;
		private final int connectorId;

		private OcppServer server;
		private UUID sessionId;

		private RecordingChargePoint(String ocppId, int connectorId) {
			this.ocppId = ocppId;
			this.connectorId = connectorId;
		}

		@Override
		public String getConfiguredOcppId() {
			return this.ocppId;
		}

		@Override
		public int getConfiguredConnectorId() {
			return this.connectorId;
		}

		@Override
		public void onConnect(OcppServer server, UUID sessionId) {
			this.server = server;
			this.sessionId = sessionId;
		}

		@Override
		public void onDisconnect() {
			this.server = null;
			this.sessionId = null;
		}

		@Override
		public void onMeterValues(MeterValuesRequest request) {
		}

		@Override
		public void onStatusNotification(StatusNotificationRequest request) {
		}
	}
}
