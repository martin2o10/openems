package io.openems.edge.evse.chargepoint.ocpp;

import static io.openems.edge.evse.chargepoint.ocpp.ChargePointConfiguration.MAX_CHARGING_PROFILES_INSTALLED;
import static io.openems.edge.evse.chargepoint.ocpp.ChargePointConfiguration.MAX_STACK_LEVEL;
import static io.openems.edge.evse.chargepoint.ocpp.ChargePointConfiguration.PHASE_SWITCHING_SUPPORTED;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChangeConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.ConfigurationStatus;
import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.GetConfigurationRequest;
import eu.chargetime.ocpp.model.core.KeyValueType;
import eu.chargetime.ocpp.model.smartcharging.ChargingProfileStatus;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileConfirmation;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import io.openems.edge.evcs.ocpp.common.OcppServer;

/**
 * Records every sent {@link Request} and answers like an OCPP 1.6 charging
 * station.
 */
public class DummyOcppServer implements OcppServer {

	public final UUID sessionId = UUID.randomUUID();

	private final List<Request> requests = new ArrayList<>();
	private final KeyValueType[] configuration;

	public DummyOcppServer(boolean phaseSwitchingSupported) {
		this.configuration = new KeyValueType[] { //
				keyValue(PHASE_SWITCHING_SUPPORTED, String.valueOf(phaseSwitchingSupported)), //
				keyValue(MAX_STACK_LEVEL, "9"), //
				keyValue(MAX_CHARGING_PROFILES_INSTALLED, "16") //
		};
	}

	@Override
	public CompletionStage<Confirmation> send(UUID session, Request request) {
		this.requests.add(request);
		final Confirmation confirmation = switch (request) {
		case GetConfigurationRequest r -> {
			var c = new GetConfigurationConfirmation();
			c.setConfigurationKey(this.configuration);
			yield c;
		}
		case SetChargingProfileRequest r -> new SetChargingProfileConfirmation(ChargingProfileStatus.Accepted);
		default -> new ChangeConfigurationConfirmation(ConfigurationStatus.Accepted);
		};
		return CompletableFuture.completedFuture(confirmation);
	}

	/**
	 * Gets all recorded {@link Request}s of the given type and clears the record.
	 *
	 * @param <T>  the type of the {@link Request}
	 * @param type the class of the {@link Request}
	 * @return a list of {@link Request}s
	 */
	public <T extends Request> List<T> getAndClearRequests(Class<T> type) {
		var result = this.requests.stream() //
				.filter(type::isInstance) //
				.map(type::cast) //
				.toList();
		this.requests.clear();
		return result;
	}

	private static KeyValueType keyValue(String key, String value) {
		var result = new KeyValueType(key, true);
		result.setValue(value);
		return result;
	}
}
