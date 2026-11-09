package io.openems.edge.evse.chargepoint.ocpp;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.MIN_CURRENT;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertAmpereToWatt;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import eu.chargetime.ocpp.model.core.ChargingProfile;
import eu.chargetime.ocpp.model.core.ChargingProfileKindType;
import eu.chargetime.ocpp.model.core.ChargingProfilePurposeType;
import eu.chargetime.ocpp.model.core.ChargingRateUnitType;
import eu.chargetime.ocpp.model.core.ChargingSchedule;
import eu.chargetime.ocpp.model.core.ChargingSchedulePeriod;
import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.SampledValue;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import eu.chargetime.ocpp.model.core.ValueFormat;
import eu.chargetime.ocpp.model.smartcharging.ChargingProfileStatus;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileConfirmation;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evcs.ocpp.common.OcppChargePoint;
import io.openems.edge.evcs.ocpp.common.OcppServer;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Ocpp", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvseChargePointOcppImpl extends AbstractOpenemsComponent implements EvseChargePointOcpp, OcppChargePoint,
		EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private static final long SAME_SETPOINT_INTERVAL_SECONDS = 30;
	private static final long NEW_SETPOINT_INTERVAL_SECONDS = 5;
	private static final int METER_VALUE_SAMPLE_INTERVAL_SECONDS = 10;
	private static final String METER_VALUES_SAMPLED_DATA = "Energy.Active.Import.Register,Power.Active.Import,Current.Import,Voltage";

	private final Logger log = LoggerFactory.getLogger(EvseChargePointOcppImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	private volatile OcppServer server = null;
	private volatile UUID sessionId = null;
	private volatile ChargePointConfiguration chargePointConfiguration = ChargePointConfiguration.DEFAULT;

	/** Charging current in [A] of the last charging profile. */
	private int chargingCurrent = 0;
	/** Number of phases of the last charging profile. */
	private SingleOrThreePhase phases = THREE_PHASE;

	private Instant previousSetPointTime = null;
	private Integer previousSetPoint = null;

	public EvseChargePointOcppImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointOcpp.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	// No @Modified: a changed OCPP-ID has to be re-registered at the OCPP Server
	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		this.phases = config.wiring();
		super.activate(context, config.id(), config.alias(), config.enabled());
		setValue(this, EvseChargePointOcpp.ChannelId.COMMUNICATION_FAILED, true);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			final ConnectorStatus status = this.getStatusChannel().getNextValue().asEnum();
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, status.isReadyForCharging);
		}
		case TOPIC_CYCLE_EXECUTE_WRITE -> {
			if (this.config.readOnly()) {
				return;
			}
			final var current = this.getSetChargingCurrentChannel().getNextWriteValueAndReset();
			final var numberOfPhases = this.getSetNumberOfPhasesChannel().getNextWriteValueAndReset();
			if (current.isEmpty() && numberOfPhases.isEmpty()) {
				return;
			}
			current.ifPresent(value -> this.chargingCurrent = value);
			numberOfPhases.ifPresent(value -> this.phases = value == 1 ? SINGLE_PHASE : THREE_PHASE);
			this.send(this.createSetChargingProfileRequest());
		}
		}
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config.readOnly()) {
			return ChargePointAbilities.create().build();
		}

		final var phases = this.getPhases();
		final var maxCurrent = this.config.maxCurrent() / 1000;
		final var phaseSwitchDirection = this.getPhaseSwitchAbility(phases);

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(phases, MIN_CURRENT / 1000, maxCurrent)) //
				.setPhaseSwitchManual(phaseSwitchDirection,
						getOppositePhaseApplySetPointAbility(phaseSwitchDirection, maxCurrent)) //
				.setIsEvConnected(this.getStatus().isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}
		final var phaseSwitch = actions.phaseSwitch();
		if (phaseSwitch != null && phaseSwitch.direction() != null) {
			this.applyPhaseSwitch(phaseSwitch.direction());
		}
		this.applySetPoint(actions.getApplySetPointInAmpere().value());
	}

	@Override
	public String getConfiguredOcppId() {
		return this.config.ocppId();
	}

	@Override
	public int getConfiguredConnectorId() {
		return this.config.connectorId();
	}

	@Override
	public void onConnect(OcppServer server, UUID sessionId) {
		this.server = server;
		this.sessionId = sessionId;
		this.previousSetPointTime = null; // re-send the current set-point to the charging station
		setValue(this, EvseChargePointOcpp.ChannelId.COMMUNICATION_FAILED, false);

		this.send(new ChangeConfigurationRequest("MeterValuesSampledData", METER_VALUES_SAMPLED_DATA));
		this.send(new ChangeConfigurationRequest("MeterValueSampleInterval",
				String.valueOf(METER_VALUE_SAMPLE_INTERVAL_SECONDS)));
		this.send(ChargePointConfiguration.createRequest(), confirmation -> {
			if (confirmation instanceof GetConfigurationConfirmation c) {
				this.chargePointConfiguration = ChargePointConfiguration.from(c);
				setValue(this, EvseChargePointOcpp.ChannelId.PHASE_SWITCHING_SUPPORTED,
						this.chargePointConfiguration.phaseSwitchingSupported());
			}
		});
	}

	@Override
	public void onDisconnect() {
		this.server = null;
		this.sessionId = null;
		setValue(this, EvseChargePointOcpp.ChannelId.COMMUNICATION_FAILED, true);
		setValue(this, EvseChargePointOcpp.ChannelId.STATUS, ConnectorStatus.UNDEFINED);
		for (var channelId : ElectricityMeter.ChannelId.values()) {
			setValue(this, channelId, null);
		}
	}

	@Override
	public void onMeterValues(MeterValuesRequest request) {
		final var meterValues = request.getMeterValue();
		if (meterValues == null) {
			return;
		}
		final var phaseRotation = this.getPhaseRotation();
		for (var meterValue : meterValues) {
			if (meterValue == null || meterValue.getSampledValue() == null) {
				continue;
			}
			for (var sampledValue : meterValue.getSampledValue()) {
				this.applySampledValue(phaseRotation, sampledValue);
			}
		}
	}

	@Override
	public void onStatusNotification(StatusNotificationRequest request) {
		setValue(this, EvseChargePointOcpp.ChannelId.STATUS, ConnectorStatus.from(request.getStatus()));
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointOcpp.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString());
		}
		return b.toString();
	}

	private SingleOrThreePhase getPhases() {
		// A single-phase wired charge point can never charge three-phased
		if (this.config.wiring() == SINGLE_PHASE) {
			return SINGLE_PHASE;
		}
		return this.phases;
	}

	private PhaseSwitchDirection getPhaseSwitchAbility(SingleOrThreePhase phases) {
		if (this.config.wiring() == SINGLE_PHASE //
				|| !this.chargePointConfiguration.phaseSwitchingSupported()) {
			return null;
		}
		return phases == THREE_PHASE //
				? PhaseSwitchDirection.TO_SINGLE_PHASE //
				: PhaseSwitchDirection.TO_THREE_PHASE;
	}

	private static ApplySetPoint.Ability.Watt getOppositePhaseApplySetPointAbility(
			PhaseSwitchDirection phaseSwitchDirection, int maxCurrent) {
		if (phaseSwitchDirection == null) {
			return null;
		}
		final var oppositePhase = phaseSwitchDirection.getTargetPhase();
		return new ApplySetPoint.Ability.Watt(oppositePhase, //
				convertAmpereToWatt(oppositePhase, MIN_CURRENT / 1000), //
				convertAmpereToWatt(oppositePhase, maxCurrent));
	}

	private void applyPhaseSwitch(PhaseSwitchDirection direction) {
		final var targetPhase = direction.getTargetPhase();
		if (targetPhase == this.getPhases()) {
			return;
		}
		try {
			this.setSetNumberOfPhases(targetPhase.count);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply phase switch: " + e.getMessage());
		}
	}

	private void applySetPoint(int setPointInAmpere) {
		final var now = Instant.now(this.componentManager.getClock());
		if (this.previousSetPointTime != null && this.previousSetPoint != null) {
			final var elapsedSeconds = Duration.between(this.previousSetPointTime, now).getSeconds();
			if (this.previousSetPoint == setPointInAmpere) {
				if (elapsedSeconds < SAME_SETPOINT_INTERVAL_SECONDS) {
					return;
				}
			} else if (elapsedSeconds < NEW_SETPOINT_INTERVAL_SECONDS) {
				return;
			}
		}
		this.previousSetPointTime = now;
		this.previousSetPoint = setPointInAmpere;
		try {
			this.setSetChargingCurrent(setPointInAmpere);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply set-point: " + e.getMessage());
		}
	}

	private SetChargingProfileRequest createSetChargingProfileRequest() {
		final var configuration = this.chargePointConfiguration;
		final var period = new ChargingSchedulePeriod(0, (double) this.chargingCurrent);
		if (configuration.phaseSwitchingSupported()) {
			period.setNumberPhases(this.getPhases().count);
		}
		final var schedule = new ChargingSchedule(ChargingRateUnitType.A, new ChargingSchedulePeriod[] { period });
		// Absolute profile without duration, started in the past to tolerate clock
		// deviations of the charging station
		schedule.setStartSchedule(ZonedDateTime.now(this.componentManager.getClock()).minusMinutes(1));
		final var profile = new ChargingProfile(configuration.chargingProfileId(), configuration.stackLevel(),
				ChargingProfilePurposeType.TxDefaultProfile, ChargingProfileKindType.Absolute, schedule);
		return new SetChargingProfileRequest(this.config.connectorId(), profile);
	}

	private void applySampledValue(PhaseRotation phaseRotation, SampledValue sampledValue) {
		if (sampledValue == null || sampledValue.getValue() == null
				|| sampledValue.getFormat() != ValueFormat.Raw) {
			return;
		}
		final double value;
		try {
			value = Double.parseDouble(sampledValue.getValue());
		} catch (NumberFormatException e) {
			return;
		}
		final var phase = getPhaseIndex(sampledValue.getPhase());
		final var isKilo = sampledValue.getUnit() != null && sampledValue.getUnit().startsWith("k");
		switch (sampledValue.getMeasurand()) {
		case "Current.Import" -> {
			final var channelId = switch (phase) {
			case 1 -> phaseRotation.channelCurrentL1();
			case 2 -> phaseRotation.channelCurrentL2();
			case 3 -> phaseRotation.channelCurrentL3();
			default -> null;
			};
			this.setMeterValue(channelId, (int) Math.round(value * 1000)); // [A] to [mA]
		}
		case "Voltage" -> {
			final var channelId = switch (phase) {
			case 1 -> phaseRotation.channelVoltageL1();
			case 2 -> phaseRotation.channelVoltageL2();
			case 3 -> phaseRotation.channelVoltageL3();
			default -> null;
			};
			this.setMeterValue(channelId, (int) Math.round(value * 1000)); // [V] to [mV]
		}
		case "Power.Active.Import" -> {
			final var channelId = switch (phase) {
			case 0 -> ElectricityMeter.ChannelId.ACTIVE_POWER;
			case 1 -> phaseRotation.channelActivePowerL1();
			case 2 -> phaseRotation.channelActivePowerL2();
			case 3 -> phaseRotation.channelActivePowerL3();
			default -> null;
			};
			this.setMeterValue(channelId, (int) Math.round(isKilo ? value * 1000 : value)); // [W]
		}
		case "Energy.Active.Import.Register" -> {
			if (phase == 0) {
				this.setMeterValue(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
						Math.round(isKilo ? value * 1000 : value)); // [Wh]
			}
		}
		default -> this.log.debug("Ignoring measurand [" + sampledValue.getMeasurand() + "]");
		}
	}

	private void setMeterValue(ElectricityMeter.ChannelId channelId, Object value) {
		if (channelId != null) {
			setValue(this, channelId, value);
		}
	}

	// 0 = total; 1-3 = L1-L3 (also for phase-to-neutral voltages); -1 = others
	private static int getPhaseIndex(String phase) {
		if (phase == null) {
			return 0;
		}
		return switch (phase) {
		case "L1", "L1-N" -> 1;
		case "L2", "L2-N" -> 2;
		case "L3", "L3-N" -> 3;
		default -> -1;
		};
	}

	private void send(Request request) {
		this.send(request, confirmation -> {
			if (confirmation instanceof SetChargingProfileConfirmation c
					&& c.getStatus() != ChargingProfileStatus.Accepted) {
				this.logWarn(this.log, "Charging profile not accepted: " + c.getStatus());
			}
		});
	}

	private void send(Request request, Consumer<Confirmation> onConfirmation) {
		final var server = this.server;
		final var sessionId = this.sessionId;
		if (server == null || sessionId == null) {
			return;
		}
		try {
			server.send(sessionId, request).whenComplete((confirmation, throwable) -> {
				if (throwable != null) {
					this.logWarn(this.log, "OCPP request [" + request.getClass().getSimpleName() + "] failed: "
							+ throwable.getMessage());
					return;
				}
				onConfirmation.accept(confirmation);
			});
		} catch (OccurenceConstraintException | UnsupportedFeatureException | NotConnectedException e) {
			this.logWarn(this.log,
					"Unable to send OCPP request [" + request.getClass().getSimpleName() + "]: " + e.getMessage());
		}
	}
}
