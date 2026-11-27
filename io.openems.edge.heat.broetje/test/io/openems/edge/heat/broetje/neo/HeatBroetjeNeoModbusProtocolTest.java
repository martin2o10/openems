package io.openems.edge.heat.broetje.neo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.api.ChannelMetaInfo;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatPump;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Verifies the channel-to-register mapping of the {@link ModbusProtocol},
 * following the pattern of the heat.mypv ModbusProtocolTest.
 */
class HeatBroetjeNeoModbusProtocolTest {

	private HeatBroetjeNeoImpl sut;

	private static HeatBroetjeNeoImpl activatedSut(boolean readOnly, boolean readPressures, boolean readOperatingHours)
			throws Exception {
		var sut = new HeatBroetjeNeoImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("heatPump0") //
						.setModbusId("modbus0") //
						.setReadOnly(readOnly) //
						.setReadPressures(readPressures) //
						.setReadOperatingHours(readOperatingHours) //
						.build());
		return sut;
	}

	@BeforeEach
	void setUp() throws Exception {
		this.sut = activatedSut(true, false, false);
	}

	@AfterEach
	void tearDown() {
		this.sut.deactivate();
	}

	@Test
	void testOutdoorTemperatureMappedToRegister10() {
		assertEquals(new ChannelMetaInfo(10),
				this.sut.channel(HeatBroetjeNeo.ChannelId.OUTDOOR_TEMPERATURE).getMetaInfo());
	}

	@Test
	void testBufferTemperatureMappedToRegister14() {
		assertEquals(new ChannelMetaInfo(14), this.sut.channel(Heat.ChannelId.TEMPERATURE).getMetaInfo());
	}

	@Test
	void testCompressorRunningMappedToRegister25() {
		assertEquals(new ChannelMetaInfo(25),
				this.sut.channel(HeatBroetjeNeo.ChannelId.COMPRESSOR_RUNNING).getMetaInfo());
	}

	@Test
	void testDeviceFaultMappedToRegister26() {
		assertEquals(new ChannelMetaInfo(26), this.sut.channel(HeatBroetjeNeo.ChannelId.DEVICE_FAULT).getMetaInfo());
	}

	@Test
	void testGridOperatorReleaseMappedToRegister32() {
		assertEquals(new ChannelMetaInfo(32),
				this.sut.channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE).getMetaInfo());
	}

	@Test
	void testSetpointTemperatureMappedToRegister34() {
		assertEquals(new ChannelMetaInfo(34), this.sut.channel(Heat.ChannelId.TARGET_TEMPERATURE).getMetaInfo());
	}

	@Test
	void testDemandMappedToRegister41() {
		assertEquals(new ChannelMetaInfo(41), this.sut.channel(HeatBroetjeNeo.ChannelId.DEMAND).getMetaInfo());
	}

	@Test
	void testActivePowerMappedToRegister70() {
		assertEquals(new ChannelMetaInfo(70), this.sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getMetaInfo());
	}

	@Test
	void testActiveConsumptionEnergyMappedToRegister68() {
		assertEquals(new ChannelMetaInfo(68),
				this.sut.channel(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY).getMetaInfo());
	}

	@Test
	void testThermalPowerMappedToRegister74() {
		assertEquals(new ChannelMetaInfo(74), this.sut.channel(ManagedHeatPump.ChannelId.THERMAL_POWER).getMetaInfo());
	}

	@Test
	void testOperatingModeMappedToRegister100() {
		assertEquals(new ChannelMetaInfo(100), this.sut.channel(HeatBroetjeNeo.ChannelId.OPERATING_MODE).getMetaInfo());
	}

	@Test
	void testWriteChannelsNotMappedInReadOnly() {
		assertNull(this.sut.channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT).getMetaInfo());
		assertNull(this.sut.channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE).getMetaInfo());
		assertNull(this.sut.channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE).getMetaInfo());
	}

	@Test
	void testWriteChannelsMappedWhenWritable() throws Exception {
		var writableSut = activatedSut(false, false, false);
		try {
			assertNotNull(writableSut.channel(HeatBroetjeNeo.ChannelId.POWER_SETPOINT).getMetaInfo());
			assertNotNull(writableSut.channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_VALUE).getMetaInfo());
			assertNotNull(writableSut.channel(HeatBroetjeNeo.ChannelId.GRID_OPERATOR_RELEASE_ACTIVATE).getMetaInfo());
		} finally {
			writableSut.deactivate();
		}
	}

	@Test
	void testOptionalGroupsNotMappedByDefault() {
		assertNull(this.sut.channel(HeatBroetjeNeo.ChannelId.PRESSURE_1).getMetaInfo());
		assertNull(this.sut.channel(HeatBroetjeNeo.ChannelId.OPERATING_HOURS_1).getMetaInfo());
	}

	@Test
	void testOptionalGroupsMappedWhenEnabled() throws Exception {
		var extendedSut = activatedSut(true, true, true);
		try {
			assertEquals(new ChannelMetaInfo(21),
					extendedSut.channel(HeatBroetjeNeo.ChannelId.PRESSURE_1).getMetaInfo());
			assertEquals(new ChannelMetaInfo(22),
					extendedSut.channel(HeatBroetjeNeo.ChannelId.PRESSURE_2).getMetaInfo());
			assertEquals(new ChannelMetaInfo(42),
					extendedSut.channel(HeatBroetjeNeo.ChannelId.OPERATING_HOURS_1).getMetaInfo());
			assertEquals(new ChannelMetaInfo(44),
					extendedSut.channel(HeatBroetjeNeo.ChannelId.OPERATING_HOURS_2).getMetaInfo());
		} finally {
			extendedSut.deactivate();
		}
	}
}
