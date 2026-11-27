package io.openems.edge.controller.heatpump.monitor;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_ACTIVE_POWER;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_BUFFER_TEMPERATURE;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_LAST_PAUSE_DURATION;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_LAST_RUN_DURATION;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_OUTSIDE_TEMPERATURE;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_RUN_TIME_TOTAL;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_RUNNING;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEAT_PUMP_STARTS_TOTAL;
import static io.openems.edge.controller.heatpump.monitor.HeatPumpMonitor.ChannelId.HEATING_ELEMENTS_ACTIVE_POWER;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;

public class HeatPumpMonitorImplTest {

	private static final String COMPONENT_ID = "ctrl0";
	private static final ChannelAddress POWER = new ChannelAddress("meter0", "ActivePower");
	private static final ChannelAddress BUFFER_TEMPERATURE = new ChannelAddress("tempBuffer0", "Temperature");
	private static final ChannelAddress OUTSIDE_TEMPERATURE = new ChannelAddress("tempOutside0", "Temperature");
	private static final ChannelAddress HEATING_ELEMENT_0 = new ChannelAddress("heatingElement0", "ActivePower");
	private static final ChannelAddress HEATING_ELEMENT_1 = new ChannelAddress("heatingElement1", "ActivePower");

	/**
	 * Builds a {@link ControllerTest} with a Dummy Timedata and a Dummy
	 * Electricity-Meter as heat pump power source.
	 *
	 * @param clock    the {@link TimeLeapClock} to use; or null for the default clock
	 * @param timedata the {@link DummyTimedata} to use; or null for none
	 * @return the {@link ControllerTest}
	 * @throws Exception on error
	 */
	private ControllerTest createDefaultControllerTest(TimeLeapClock clock, DummyTimedata timedata) throws Exception {
		var test = new ControllerTest(new HeatPumpMonitorImpl()) //
				.addReference("componentManager", clock == null //
						? new DummyComponentManager() //
						: new DummyComponentManager(clock));
		if (timedata != null) {
			test = test.addReference("timedata", timedata);
		}
		return test.addComponent(new DummyElectricityMeter("meter0"));
	}

	@Test
	void powerDetection_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock, new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setHeatPumpPowerChannelAddress(POWER.toString()) //
						.setPowerStartThreshold(300) //
						.setPowerStopThreshold(100) //
						.setMinimumRunTimeSeconds(60) //
						.setMinimumPauseTimeSeconds(60) //
						.build()) //
				.next(new TestCase("Power above start threshold; debounce not yet finished") //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, false) //
						.output(HEAT_PUMP_STARTS_TOTAL, 0)) //
				.next(new TestCase("Run time is initialized; start is counted after Minimum Run Time") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 0L)) //
				.next(new TestCase("Still running") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("Run time is accumulated") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 60L) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("Short power dropout; stop debounce is running") //
						.timeleap(clock, 10, SECONDS) //
						.input(POWER, 50) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 70L)) //
				.next(new TestCase("Dropout over before Minimum Pause Time") //
						.timeleap(clock, 10, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 80L) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("Power within the hysteresis keeps the heat pump running") //
						.timeleap(clock, 120, SECONDS) //
						.input(POWER, 150) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 200L)) //
				.next(new TestCase("Power below stop threshold; stop debounce is running") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 50) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 260L)) //
				.next(new TestCase("Stop is completed after Minimum Pause Time") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 50) //
						.output(HEAT_PUMP_RUNNING, false) //
						.output(HEAT_PUMP_LAST_RUN_DURATION, 320L) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("New start candidate; debounce not yet finished") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, false)) //
				.next(new TestCase("Second start; the pause duration is evaluated") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 2) //
						.output(HEAT_PUMP_LAST_PAUSE_DURATION, 120L)) //
				.deactivate();
	}

	@Test
	void runningChannelTakesPrecedenceOverPowerChannel_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock, new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setHeatPumpRunningChannelAddress(POWER.toString()) //
						.setHeatPumpPowerChannelAddress(POWER.toString()) //
						.setMinimumRunTimeSeconds(30) //
						.setMinimumPauseTimeSeconds(30) //
						.build()) //
				.next(new TestCase("Running signal [1]") //
						.input(POWER, 1) //
						.output(HEAT_PUMP_RUNNING, false) //
						.output(HEAT_PUMP_ACTIVE_POWER, 1)) //
				.next(new TestCase("Start is counted") //
						.timeleap(clock, 30, SECONDS) //
						.input(POWER, 1) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("Running signal [0]; stop debounce is running") //
						.timeleap(clock, 30, SECONDS) //
						.input(POWER, 0) //
						.output(HEAT_PUMP_RUNNING, true)) //
				.next(new TestCase("Stop is counted") //
						.timeleap(clock, 30, SECONDS) //
						.input(POWER, 0) //
						.output(HEAT_PUMP_RUNNING, false) //
						.output(HEAT_PUMP_LAST_RUN_DURATION, 30L) //
						.output(HEAT_PUMP_ACTIVE_POWER, 0)) //
				.deactivate();
	}

	@Test
	void missingPowerChannelIsRejected_test() {
		final var clock = createDummyClock();
		final var exception = assertThrows(ReflectionException.class, () -> {
			this.createDefaultControllerTest(clock, new DummyTimedata("timedata0")) //
					.activate(MyConfig.create() //
							.setId(COMPONENT_ID) //
							.setHeatPumpRunningChannelAddress(POWER.toString()) //
							.build());
		});
		assertInstanceOf(IllegalArgumentException.class, exception.getCause().getCause());
	}

	@Test
	void unavailableSignalKeepsState_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock, new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setHeatPumpPowerChannelAddress(POWER.toString()) //
						.setMinimumRunTimeSeconds(60) //
						.setMinimumPauseTimeSeconds(60) //
						.build()) //
				.next(new TestCase("Start candidate") //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, false)) //
				.next(new TestCase("Start is counted") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("Unavailable signal keeps the running state") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, null) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.next(new TestCase("Signal is available again") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 1)) //
				.deactivate();
	}

	@Test
	void countersAreInitializedFromTimedata_test() throws Exception {
		final var clock = createDummyClock();
		final var timedata = new DummyTimedata("timedata0");
		final var timestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1577836800 - 3600), ZoneOffset.UTC);
		timedata.add(timestamp, new ChannelAddress(COMPONENT_ID, "HeatPumpStartsTotal"), 5);
		timedata.add(timestamp, new ChannelAddress(COMPONENT_ID, "HeatPumpRunTimeTotal"), 1000);

		this.createDefaultControllerTest(clock, timedata) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setHeatPumpPowerChannelAddress(POWER.toString()) //
						.setMinimumRunTimeSeconds(60) //
						.setMinimumPauseTimeSeconds(60) //
						.build()) //
				.next(new TestCase("Starts are initialized from Timedata") //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, false) //
						.output(HEAT_PUMP_STARTS_TOTAL, 5)) //
				.next(new TestCase("Run time is initialized from Timedata; start is counted") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 6) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 1000L)) //
				.next(new TestCase("Still running") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_STARTS_TOTAL, 6)) //
				.next(new TestCase("Run time continues from the stored value") //
						.timeleap(clock, 60, SECONDS) //
						.input(POWER, 500) //
						.output(HEAT_PUMP_RUNNING, true) //
						.output(HEAT_PUMP_RUN_TIME_TOTAL, 1060L)) //
				.deactivate();
	}

	@Test
	void mirroredValues_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock, new DummyTimedata("timedata0")) //
				.addComponent(new DummyTemperatureSensor("tempBuffer0")) //
				.addComponent(new DummyTemperatureSensor("tempOutside0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setHeatPumpPowerChannelAddress(POWER.toString()) //
						.setBufferTemperatureChannelAddress(BUFFER_TEMPERATURE.toString()) //
						.setOutsideTemperatureChannelAddress(OUTSIDE_TEMPERATURE.toString()) //
						.build()) //
				.next(new TestCase("Power and temperatures are mirrored to the Channels") //
						.input(POWER, 500) //
						.input(BUFFER_TEMPERATURE, 45) //
						.input(OUTSIDE_TEMPERATURE, -7) //
						.output(HEAT_PUMP_ACTIVE_POWER, 500) //
						.output(HEAT_PUMP_BUFFER_TEMPERATURE, 45.0f) //
						.output(HEAT_PUMP_OUTSIDE_TEMPERATURE, -7.0f)) //
				.next(new TestCase("Unavailable values are mirrored as null") //
						.input(POWER, null) //
						.input(BUFFER_TEMPERATURE, null) //
						.input(OUTSIDE_TEMPERATURE, null) //
						.output(HEAT_PUMP_ACTIVE_POWER, null) //
						.output(HEAT_PUMP_BUFFER_TEMPERATURE, null) //
						.output(HEAT_PUMP_OUTSIDE_TEMPERATURE, null)) //
				.deactivate();
	}

	@Test
	void heatingElementsSum_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock, new DummyTimedata("timedata0")) //
				.addComponent(new DummyElectricityMeter("heatingElement0")) //
				.addComponent(new DummyElectricityMeter("heatingElement1")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setHeatPumpPowerChannelAddress(POWER.toString()) //
						.setHeatingElementPowerChannelAddresses(new String[] { //
								HEATING_ELEMENT_0.toString(), HEATING_ELEMENT_1.toString() }) //
						.build()) //
				.next(new TestCase("Sum of all heating elements") //
						.input(POWER, 500) //
						.input(HEATING_ELEMENT_0, 300) //
						.input(HEATING_ELEMENT_1, 200) //
						.output(HEAT_PUMP_ACTIVE_POWER, 500) //
						.output(HEATING_ELEMENTS_ACTIVE_POWER, 500)) //
				.next(new TestCase("Unavailable heating elements are excluded from the sum") //
						.input(HEATING_ELEMENT_0, 300) //
						.input(HEATING_ELEMENT_1, null) //
						.output(HEATING_ELEMENTS_ACTIVE_POWER, 300)) //
				.next(new TestCase("Sum is null if all values are unavailable") //
						.input(HEATING_ELEMENT_0, null) //
						.input(HEATING_ELEMENT_1, null) //
						.output(HEATING_ELEMENTS_ACTIVE_POWER, null)) //
				.deactivate();
	}
}
