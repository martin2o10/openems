package io.openems.edge.heat.broetje.neo;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Channels of the Broetje BLW NEO heat pump (NEO-RKM). Register mapping follows
 * the NEO-RKM installation manual 7734937 chapter 7.1 tables 4+5.
 */
public interface HeatBroetjeNeo extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// 5.2 Heat and driver channels (mandatory)
		/**
		 * Outdoor temperature; register 10, INT16 x 0.1 °C.
		 */
		OUTDOOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Domestic hot water temperature; register 11, INT16 x 0.1 °C.
		 */
		DOMESTIC_HOT_WATER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Flow temperature; register 12, INT16 x 0.1 °C.
		 */
		FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Return temperature; register 13, INT16 x 0.1 °C.
		 */
		RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Compressor running; register 25, flag (value > 0).
		 */
		COMPRESSOR_RUNNING(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Device fault; register 26, flag (value > 0).
		 */
		DEVICE_FAULT(Doc.of(Level.FAULT) //
				.text("The heat pump reports a fault (register 26)")), //
		/**
		 * Defrost (4-way valve) active; register 27, flag (value > 0).
		 */
		DEFROST_VALVE_ACTIVE(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Compressor speed; register 29, per mille.
		 */
		COMPRESSOR_SPEED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH)), //
		/**
		 * Coefficient of performance; register 30, x 0.1.
		 */
		COEFFICIENT_OF_PERFORMANCE(Doc.of(OpenemsType.DOUBLE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Grid operator (EVU) release; register 32, flag (value > 0). Read-only: never
		 * changed by this driver except via the documented Lock action (registers
		 * 149/150).
		 */
		GRID_OPERATOR_RELEASE(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Grid operator (EVU) release; false means the heat pump is locked")), //
		/**
		 * Current demand; register 41.
		 */
		DEMAND(Doc.of(Demand.values())), //
		/**
		 * Operating mode; register 100 (FC3). Values 8-10 are read-only on the device.
		 */
		OPERATING_MODE(Doc.of(OperatingMode.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //

		// 5.3 Energy (mandatory)
		/**
		 * Thermal energy heating; register 60, UINT32. Unit (Wh vs. kWh) to be
		 * clarified on the device.
		 */
		THERMAL_ENERGY_HEATING(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		/**
		 * Electric energy heating; register 62, UINT32. Unit (Wh vs. kWh) to be
		 * clarified on the device.
		 */
		ELECTRIC_ENERGY_HEATING(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		/**
		 * Thermal energy domestic hot water; register 64, UINT32. Unit (Wh vs. kWh) to
		 * be clarified on the device.
		 */
		THERMAL_ENERGY_DHWS(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		/**
		 * Electric energy domestic hot water; register 66, UINT32. Unit (Wh vs. kWh) to
		 * be clarified on the device.
		 */
		ELECTRIC_ENERGY_DHWS(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		/**
		 * Total thermal energy; register 72, UINT32. Unit (Wh vs. kWh) to be clarified
		 * on the device.
		 */
		THERMAL_ENERGY_TOTAL(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		// 5.4 Optional registers ("Kür"); each group is polled only if the
		// matching Config switch is enabled
		/**
		 * Source flow temperature; register 15, INT16 x 0.1 °C. Group:
		 * readSourceTemperatures.
		 */
		SOURCE_FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Source return temperature; register 16, INT16 x 0.1 °C. Group:
		 * readSourceTemperatures.
		 */
		SOURCE_RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Suction gas temperature; register 17, INT16 x 0.1 °C. Group:
		 * readRefrigerantTemperatures.
		 */
		SUCTION_GAS_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Evaporation temperature; register 18, INT16 x 0.1 °C. Group:
		 * readRefrigerantTemperatures.
		 */
		EVAPORATION_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Condensation temperature; register 19, INT16 x 0.1 °C. Group:
		 * readRefrigerantTemperatures.
		 */
		CONDENSATION_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Hot gas temperature; register 20, INT16 x 0.1 °C. Group:
		 * readRefrigerantTemperatures.
		 */
		HOT_GAS_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Pressure register 21. Unit to be clarified on the device. Group:
		 * readPressures.
		 */
		PRESSURE_1(Doc.of(OpenemsType.INTEGER) //
				.text("Pressure register 21; designation/unit to be verified on the device")), //
		/**
		 * Pressure register 22. Unit to be clarified on the device. Group:
		 * readPressures.
		 */
		PRESSURE_2(Doc.of(OpenemsType.INTEGER) //
				.text("Pressure register 22; designation/unit to be verified on the device")), //
		/**
		 * Pump flag 1; register 23, flag (value > 0). Group: readPumpFlags.
		 */
		PUMP_1_ACTIVE(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Pump flag 2; register 24, flag (value > 0). Group: readPumpFlags.
		 */
		PUMP_2_ACTIVE(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Volume flow; register 28. Unit to be clarified on the device. Group:
		 * readFlowRate.
		 */
		VOLUME_FLOW(Doc.of(OpenemsType.INTEGER) //
				.text("Volume flow register 28; unit to be verified on the device")), //
		/**
		 * Fresh water temperature; register 31, INT16 x 0.1 °C. Group:
		 * readFreshWaterTemperature.
		 */
		FRESH_WATER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Delayed outdoor temperature; register 33, INT16 x 0.1 °C. Group:
		 * readDelayedOutdoorTemperature.
		 */
		OUTDOOR_TEMPERATURE_DELAYED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Mixer circuit setpoint 1; register 35, INT16 x 0.1 °C. Group:
		 * readMixerCircuitSetpoints.
		 */
		MIXER_CIRCUIT_SETPOINT_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Mixer circuit setpoint 2; register 36, INT16 x 0.1 °C. Group:
		 * readMixerCircuitSetpoints.
		 */
		MIXER_CIRCUIT_SETPOINT_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		/**
		 * Expansion valve; register 40. Group: readExpansionValve.
		 */
		EXPANSION_VALVE(Doc.of(OpenemsType.INTEGER) //
				.text("Expansion valve register 40; unit to be verified on the device")), //
		/**
		 * Operating hours 1; register 42, UINT32. Group: readOperatingHours.
		 */
		OPERATING_HOURS_1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.HOUR)), //
		/**
		 * Operating hours 2; register 44, UINT32. Group: readOperatingHours.
		 */
		OPERATING_HOURS_2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.HOUR)), //
		/**
		 * Holding register 101. Group: readHoldingRegisters.
		 */
		HOLDING_101(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Holding register 102. Group: readHoldingRegisters.
		 */
		HOLDING_102(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Holding register 103. Group: readHoldingRegisters.
		 */
		HOLDING_103(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Holding register 104. Group: readHoldingRegisters.
		 */
		HOLDING_104(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Holding register 105. Group: readHoldingRegisters.
		 */
		HOLDING_105(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Holding register 106. Group: readHoldingRegisters.
		 */
		HOLDING_106(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * External request register 117. Group: readExternalRequest.
		 */
		EXTERNAL_REQUEST_117(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Compressor speed setpoint; register 126, per mille. Group: readSpeedSetpoint.
		 */
		COMPRESSOR_SPEED_SETPOINT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH)), //
		/**
		 * External request register 127. Group: readExternalRequest.
		 */
		EXTERNAL_REQUEST_127(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * External sensor override register 129. Group: readExternalSensorOverrides.
		 */
		SENSOR_OVERRIDE_129(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * External sensor override register 130. Group: readExternalSensorOverrides.
		 */
		SENSOR_OVERRIDE_130(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * External sensor override register 131. Group: readExternalSensorOverrides.
		 */
		SENSOR_OVERRIDE_131(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * External sensor override register 132. Group: readExternalSensorOverrides.
		 */
		SENSOR_OVERRIDE_132(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * External sensor override register 133. Group: readExternalSensorOverrides.
		 */
		SENSOR_OVERRIDE_133(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * External sensor override register 134. Group: readExternalSensorOverrides.
		 */
		SENSOR_OVERRIDE_134(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Second stage register 147. Group: readSecondStage.
		 */
		SECOND_STAGE_147(Doc.of(OpenemsType.INTEGER)), //
		/**
		 * Second stage register 148. Group: readSecondStage.
		 */
		SECOND_STAGE_148(Doc.of(OpenemsType.INTEGER)), //

		// 5.5 Write channels; without a Modbus write task in read-only mode
		/**
		 * Electric power setpoint (register 125, demand profile 3). Written only in
		 * non-read-only mode and only every write interval.
		 */
		POWER_SETPOINT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		/**
		 * Grid operator release value (register 149); 0 locks the heat pump.
		 */
		GRID_OPERATOR_RELEASE_VALUE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		/**
		 * Activates {@link #GRID_OPERATOR_RELEASE_VALUE} (register 150).
		 */
		GRID_OPERATOR_RELEASE_ACTIVATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
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
}
