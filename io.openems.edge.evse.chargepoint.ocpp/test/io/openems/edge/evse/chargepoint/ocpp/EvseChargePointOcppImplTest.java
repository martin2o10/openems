package io.openems.edge.evse.chargepoint.ocpp;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import eu.chargetime.ocpp.model.core.ChargingProfileKindType;
import eu.chargetime.ocpp.model.core.ChargingProfilePurposeType;
import eu.chargetime.ocpp.model.core.ChargingRateUnitType;
import eu.chargetime.ocpp.model.core.GetConfigurationRequest;
import eu.chargetime.ocpp.model.core.MeterValue;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.SampledValue;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

public class EvseChargePointOcppImplTest {

	@Test
	public void testConnect() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var test = prepareTest(sut, false, THREE_PHASE, createDummyClock()) //
				.next(new TestCase("Not connected") //
						.output(EvseChargePointOcpp.ChannelId.COMMUNICATION_FAILED, true));

		assertEquals("goe1", sut.getConfiguredOcppId());
		assertEquals(1, sut.getConfiguredConnectorId());

		sut.onConnect(server, server.sessionId);
		test.next(new TestCase("Connected; configuration has been read") //
				.output(EvseChargePointOcpp.ChannelId.COMMUNICATION_FAILED, false) //
				.output(EvseChargePointOcpp.ChannelId.PHASE_SWITCHING_SUPPORTED, true));

		final var changeConfigurations = server.getAndClearRequests(ChangeConfigurationRequest.class);
		assertEquals(2, changeConfigurations.size());
		assertEquals("MeterValuesSampledData", changeConfigurations.get(0).getKey());
		assertEquals("Energy.Active.Import.Register,Power.Active.Import,Current.Import,Voltage",
				changeConfigurations.get(0).getValue());
		assertEquals("MeterValueSampleInterval", changeConfigurations.get(1).getKey());
		assertEquals("10", changeConfigurations.get(1).getValue());

		sut.onConnect(server, server.sessionId);
		assertEquals(1, server.getAndClearRequests(GetConfigurationRequest.class).size());

		test.deactivate();
	}

	@Test
	public void testDisconnect() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var test = prepareConnectedTest(sut, server, false) //
				.next(new TestCase("Values are present while connected") //
						.onBeforeProcessImage(() -> {
							sut.onStatusNotification(statusNotification(ChargePointStatus.Charging));
							sut.onMeterValues(meterValues(sampledValue("Power.Active.Import", null, "W", "3680")));
						}) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3680) //
						.output(EvseChargePointOcpp.ChannelId.STATUS, ConnectorStatus.CHARGING));

		sut.onDisconnect();
		test.next(new TestCase("Values are reset after disconnect") //
				.output(EvseChargePointOcpp.ChannelId.COMMUNICATION_FAILED, true) //
				.output(EvseChargePointOcpp.ChannelId.STATUS, ConnectorStatus.UNDEFINED) //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null));

		server.getAndClearRequests(SetChargingProfileRequest.class);
		test.next(new TestCase("Set-Point is not sent without connection") //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 16))));
		assertTrue(server.getAndClearRequests(SetChargingProfileRequest.class).isEmpty());

		test.deactivate();
	}

	@Test
	public void testMeterValues() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		prepareConnectedTest(sut, server, false) //
				.next(new TestCase("Meter values are converted to OpenEMS units") //
						.onBeforeProcessImage(() -> sut.onMeterValues(meterValues(//
								sampledValue("Current.Import", "L1", "A", "10.5"), //
								sampledValue("Current.Import", "L2", "A", "10.3"), //
								sampledValue("Current.Import", "L3", "A", "10.7"), //
								sampledValue("Voltage", "L1-N", "V", "230.0"), //
								sampledValue("Voltage", "L2-N", "V", "231.0"), //
								sampledValue("Voltage", "L3-N", "V", "229.0"), //
								sampledValue("Power.Active.Import", null, "kW", "7.36"), //
								sampledValue("Power.Active.Import", "L1", "W", "2450"), //
								sampledValue("Energy.Active.Import.Register", null, "kWh", "1234.5"), //
								sampledValue("Temperature", null, "Celsius", "25")))) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 10300) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 10700) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 7360) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2450) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 1234500L)) //
				.next(new TestCase("Sums are calculated from the phases") //
						.output(ElectricityMeter.ChannelId.CURRENT, 31500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230000)) //
				.next(new TestCase("Values in W and Wh are taken as they are") //
						.onBeforeProcessImage(() -> sut.onMeterValues(meterValues(//
								sampledValue("Power.Active.Import", null, "W", "1380.4"), //
								sampledValue("Energy.Active.Import.Register", null, "Wh", "500"), //
								sampledValue("Energy.Active.Import.Register", "L1", "Wh", "100"), //
								sampledValue("Current.Import", null, "A", "99"), //
								sampledValue("Current.Import", "L1", "A", "not a number")))) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1380) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 500L) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500)) //
				.deactivate();
	}

	@Test
	public void testMeterValuesWithPhaseRotation() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var test = new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setOcppId("goe1") //
						.setPhaseRotation(PhaseRotation.L2_L3_L1) //
						.build());
		sut.onConnect(server, server.sessionId);
		test.next(new TestCase("L1 of the charge point is L2 of the grid") //
				.onBeforeProcessImage(() -> sut.onMeterValues(meterValues(//
						sampledValue("Current.Import", "L1", "A", "16"), //
						sampledValue("Current.Import", "L2", "A", "0"), //
						sampledValue("Current.Import", "L3", "A", "0")))) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 0) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 16000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 0)) //
				.deactivate();
	}

	@Test
	public void testStatus() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var test = prepareConnectedTest(sut, server, false);

		assertStatus(test, sut, ChargePointStatus.Available, ConnectorStatus.AVAILABLE, false, false);
		assertStatus(test, sut, ChargePointStatus.Preparing, ConnectorStatus.PREPARING, true, true);
		assertStatus(test, sut, ChargePointStatus.Charging, ConnectorStatus.CHARGING, true, true);
		assertStatus(test, sut, ChargePointStatus.SuspendedEVSE, ConnectorStatus.SUSPENDED_EVSE, true, true);
		assertStatus(test, sut, ChargePointStatus.SuspendedEV, ConnectorStatus.SUSPENDED_EV, true, true);
		assertStatus(test, sut, ChargePointStatus.Finishing, ConnectorStatus.FINISHING, true, false);
		assertStatus(test, sut, ChargePointStatus.Reserved, ConnectorStatus.RESERVED, false, false);
		assertStatus(test, sut, ChargePointStatus.Unavailable, ConnectorStatus.UNAVAILABLE, false, false);
		assertStatus(test, sut, ChargePointStatus.Faulted, ConnectorStatus.FAULTED, false, false);
		assertEquals(ConnectorStatus.UNDEFINED, ConnectorStatus.from(null));

		test.deactivate();
	}

	@Test
	public void testAbilities() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var test = prepareConnectedTest(sut, new DummyOcppServer(true), false, THREE_PHASE, 16_000);

		final var abilities = sut.getChargePointAbilities();
		assertEquals(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16), abilities.applySetPoint());
		assertEquals(TO_SINGLE_PHASE, abilities.phaseSwitch().direction());
		assertEquals(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680),
				abilities.phaseSwitch().oppositePhaseApplySetPoint());
		assertFalse(abilities.isEvConnected());
		assertFalse(abilities.isReadyForCharging());

		test.deactivate();
	}

	@Test
	public void testNoPhaseSwitchAbility() throws Exception {
		final var notSupported = new EvseChargePointOcppImpl();
		final var test1 = prepareConnectedTest(notSupported, new DummyOcppServer(false), false);
		assertNull(notSupported.getChargePointAbilities().phaseSwitch());

		final var singlePhaseWiring = new EvseChargePointOcppImpl();
		final var test2 = prepareConnectedTest(singlePhaseWiring, new DummyOcppServer(true), false, SINGLE_PHASE,
				32_000);
		final var abilities = singlePhaseWiring.getChargePointAbilities();
		assertNull(abilities.phaseSwitch());
		assertEquals(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 32), abilities.applySetPoint());

		test1.deactivate();
		test2.deactivate();
	}

	@Test
	public void testApplySetPoint() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var clock = createDummyClock();
		final var test = prepareConnectedTest(sut, server, false, THREE_PHASE, 32_000, clock) //
				.next(new TestCase("Set-Point is sent as charging profile") //
						.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 16))));

		var requests = server.getAndClearRequests(SetChargingProfileRequest.class);
		assertEquals(1, requests.size());
		final var request = requests.get(0);
		assertEquals(1, request.getConnectorId());
		final var profile = request.getCsChargingProfiles();
		assertEquals(16, profile.getChargingProfileId());
		assertEquals(9, profile.getStackLevel());
		assertEquals(ChargingProfilePurposeType.TxDefaultProfile, profile.getChargingProfilePurpose());
		assertEquals(ChargingProfileKindType.Absolute, profile.getChargingProfileKind());
		final var schedule = profile.getChargingSchedule();
		assertEquals(ChargingRateUnitType.A, schedule.getChargingRateUnit());
		assertEquals(ZonedDateTime.now(clock).minusMinutes(1), schedule.getStartSchedule());
		assertNull(schedule.getDuration());
		final var period = schedule.getChargingSchedulePeriod()[0];
		assertEquals(0, period.getStartPeriod());
		assertEquals(16., period.getLimit());
		assertEquals(3, period.getNumberPhases());
		assertTrue(request.validate());

		test.next(new TestCase("Same Set-Point is not repeated within 30 seconds") //
				.timeleap(clock, 29, ChronoUnit.SECONDS) //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 16))));
		assertTrue(server.getAndClearRequests(SetChargingProfileRequest.class).isEmpty());

		test.next(new TestCase("Same Set-Point is repeated after 30 seconds") //
				.timeleap(clock, 1, ChronoUnit.SECONDS) //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 16))));
		assertEquals(1, server.getAndClearRequests(SetChargingProfileRequest.class).size());

		test.next(new TestCase("New Set-Point is not sent within 5 seconds") //
				.timeleap(clock, 4, ChronoUnit.SECONDS) //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 10))));
		assertTrue(server.getAndClearRequests(SetChargingProfileRequest.class).isEmpty());

		test.next(new TestCase("New Set-Point is sent after 5 seconds") //
				.timeleap(clock, 1, ChronoUnit.SECONDS) //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 10))) //
				.output(EvseChargePointOcpp.ChannelId.DEBUG_SET_CHARGING_CURRENT, 10));
		requests = server.getAndClearRequests(SetChargingProfileRequest.class);
		assertEquals(1, requests.size());
		assertEquals(10., getLimit(requests.get(0)));

		test.next(new TestCase("Zero Set-Point pauses charging") //
				.timeleap(clock, 5, ChronoUnit.SECONDS) //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 0))));
		assertEquals("L:UNDEFINED|SetCurrent:10", sut.debugLog());
		assertEquals(0., getLimit(server.getAndClearRequests(SetChargingProfileRequest.class).get(0)));

		sut.onConnect(server, server.sessionId);
		server.getAndClearRequests(ChangeConfigurationRequest.class);
		test.next(new TestCase("Set-Point is sent again immediately after reconnect") //
				.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 0))));
		assertEquals(1, server.getAndClearRequests(SetChargingProfileRequest.class).size());

		test.deactivate();
	}

	@Test
	public void testPhaseSwitch() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var test = prepareConnectedTest(sut, server, false) //
				.next(new TestCase("Switch to single phase") //
						.onExecuteControllersCallbacks(() -> sut.apply(phaseSwitchActions(sut, 0, TO_SINGLE_PHASE))) //
						.output(EvseChargePointOcpp.ChannelId.DEBUG_SET_NUMBER_OF_PHASES, 1));

		var requests = server.getAndClearRequests(SetChargingProfileRequest.class);
		assertEquals(1, requests.size());
		assertEquals(1, getNumberPhases(requests.get(0)));
		assertEquals(0., getLimit(requests.get(0)));

		var abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());
		assertEquals(TO_THREE_PHASE, abilities.phaseSwitch().direction());

		test.next(new TestCase("Switch back to three phases") //
				.onExecuteControllersCallbacks(() -> sut.apply(phaseSwitchActions(sut, 0, TO_THREE_PHASE))));
		requests = server.getAndClearRequests(SetChargingProfileRequest.class);
		assertEquals(1, requests.size());
		assertEquals(3, getNumberPhases(requests.get(0)));
		assertEquals(THREE_PHASE, sut.getChargePointAbilities().applySetPoint().phase());

		test.deactivate();
	}

	@Test
	public void testDefaultNumberPhasesWithoutPhaseSwitching() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(false);
		final var test = prepareConnectedTest(sut, server, false, SINGLE_PHASE, 32_000) //
				.next(new TestCase() //
						.onExecuteControllersCallbacks(() -> sut.apply(actions(sut, 8))));

		// Wiring is not sent; the library default equals the OCPP default of 3
		final var requests = server.getAndClearRequests(SetChargingProfileRequest.class);
		assertEquals(1, requests.size());
		assertEquals(3, getNumberPhases(requests.get(0)));

		test.deactivate();
	}

	@Test
	public void testReadOnly() throws Exception {
		final var sut = new EvseChargePointOcppImpl();
		final var server = new DummyOcppServer(true);
		final var test = prepareConnectedTest(sut, server, true);

		final var abilities = sut.getChargePointAbilities();
		assertEquals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY, abilities.applySetPoint());
		assertNull(abilities.phaseSwitch());
		assertTrue(sut.isReadOnly());

		test.next(new TestCase("Read-only charge point never sends a charging profile") //
				.onExecuteControllersCallbacks(
						() -> sut.apply(ChargePointActions.from(abilities).setApplySetPointInWatt(0).build())));
		assertTrue(server.getAndClearRequests(SetChargingProfileRequest.class).isEmpty());
		assertEquals("L:UNDEFINED", sut.debugLog());

		test.deactivate();
	}

	private static ComponentTest prepareTest(EvseChargePointOcppImpl sut, boolean readOnly, SingleOrThreePhase wiring,
			TimeLeapClock clock) throws Exception {
		return prepareTest(sut, readOnly, wiring, 32_000, clock);
	}

	private static ComponentTest prepareTest(EvseChargePointOcppImpl sut, boolean readOnly, SingleOrThreePhase wiring,
			int maxCurrent, TimeLeapClock clock) throws Exception {
		return new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setOcppId("goe1") //
						.setWiring(wiring) //
						.setMaxCurrent(maxCurrent) //
						.setReadOnly(readOnly) //
						.build());
	}

	private static ComponentTest prepareConnectedTest(EvseChargePointOcppImpl sut, DummyOcppServer server,
			boolean readOnly) throws Exception {
		return prepareConnectedTest(sut, server, readOnly, THREE_PHASE, 32_000);
	}

	private static ComponentTest prepareConnectedTest(EvseChargePointOcppImpl sut, DummyOcppServer server,
			boolean readOnly, SingleOrThreePhase wiring, int maxCurrent) throws Exception {
		return prepareConnectedTest(sut, server, readOnly, wiring, maxCurrent, createDummyClock());
	}

	private static ComponentTest prepareConnectedTest(EvseChargePointOcppImpl sut, DummyOcppServer server,
			boolean readOnly, SingleOrThreePhase wiring, int maxCurrent, TimeLeapClock clock) throws Exception {
		final var test = prepareTest(sut, readOnly, wiring, maxCurrent, clock);
		sut.onConnect(server, server.sessionId);
		server.getAndClearRequests(ChangeConfigurationRequest.class);
		return test;
	}

	private static void assertStatus(ComponentTest test, EvseChargePointOcppImpl sut, ChargePointStatus ocppStatus,
			ConnectorStatus status, boolean isEvConnected, boolean isReadyForCharging) throws Exception {
		test.next(new TestCase(ocppStatus.name()) //
				.onBeforeProcessImage(() -> sut.onStatusNotification(statusNotification(ocppStatus))) //
				.output(EvseChargePointOcpp.ChannelId.STATUS, status) //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReadyForCharging));
		assertEquals(isEvConnected, sut.getChargePointAbilities().isEvConnected(), ocppStatus.name());
	}

	private static ChargePointActions actions(EvseChargePointOcppImpl sut, int setPointInAmpere) {
		return ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(setPointInAmpere) //
				.build();
	}

	private static ChargePointActions phaseSwitchActions(EvseChargePointOcppImpl sut, int setPointInAmpere,
			PhaseSwitchDirection direction) {
		return ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(setPointInAmpere) //
				.setPhaseSwitchManual(direction) //
				.build();
	}

	private static StatusNotificationRequest statusNotification(ChargePointStatus status) {
		return new StatusNotificationRequest(1, ChargePointErrorCode.NoError, status);
	}

	private static MeterValuesRequest meterValues(SampledValue... sampledValues) {
		final var request = new MeterValuesRequest(1);
		request.setMeterValue(new MeterValue[] { new MeterValue(ZonedDateTime.now(), sampledValues) });
		return request;
	}

	private static SampledValue sampledValue(String measurand, String phase, String unit, String value) {
		final var result = new SampledValue(value);
		result.setMeasurand(measurand);
		result.setPhase(phase);
		result.setUnit(unit);
		return result;
	}

	private static Double getLimit(SetChargingProfileRequest request) {
		return request.getCsChargingProfiles().getChargingSchedule().getChargingSchedulePeriod()[0].getLimit();
	}

	private static Integer getNumberPhases(SetChargingProfileRequest request) {
		return request.getCsChargingProfiles().getChargingSchedule().getChargingSchedulePeriod()[0]
				.getNumberPhases();
	}
}
