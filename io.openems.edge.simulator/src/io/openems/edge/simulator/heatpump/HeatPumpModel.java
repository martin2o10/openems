package io.openems.edge.simulator.heatpump;

import java.time.Duration;

import io.openems.edge.heat.api.HeatPumpOperatingState;

/**
 * Deterministic model of a heat pump that heats a water storage.
 *
 * <p>
 * The model is intentionally simple: one fully mixed storage, a constant heat
 * demand of the building and a coefficient of performance derived from the
 * Carnot efficiency. It reacts to the setpoints of a
 * {@link io.openems.edge.heat.api.ManagedHeatPump} like a real device: its own
 * control keeps the storage at the target temperature, setpoints shift the
 * consumption, and minimum run and off times are always respected.
 */
class HeatPumpModel {

	/**
	 * Heat capacity of water [Wh/(l*K)].
	 */
	private static final double WATER_HEAT_CAPACITY = 1.163;

	/**
	 * Temperature difference between storage and condenser [K].
	 */
	private static final double SINK_TEMPERATURE_LIFT = 5.0;

	/**
	 * Temperature difference between outdoor air and evaporator [K].
	 */
	private static final double SOURCE_TEMPERATURE_DROP = 5.0;

	private static final double MIN_COP = 1.0;
	private static final double MAX_COP = 8.0;
	private static final double CELSIUS_TO_KELVIN = 273.15;

	/**
	 * The parameters of the model.
	 *
	 * @param minPower              minimum electric power while running [W]
	 * @param maxPower              maximum electric power [W]
	 * @param minRunTime            minimum run time [s]
	 * @param minOffTime            minimum off time [s]
	 * @param storageVolume         storage volume [l]
	 * @param targetTemperature     target storage temperature of the own control
	 *                              [°C]
	 * @param hysteresis            switch-on hysteresis of the own control [K]
	 * @param maxStorageTemperature maximum storage temperature [°C]
	 * @param outdoorTemperature    outdoor temperature [°C]
	 * @param roomTemperature       room temperature [°C]
	 * @param heatLossCoefficient   heat loss coefficient of the building [W/K]
	 * @param carnotEfficiency      share of the Carnot COP that is reached [0..1]
	 */
	record Parameters(int minPower, int maxPower, int minRunTime, int minOffTime, double storageVolume,
			double targetTemperature, double hysteresis, double maxStorageTemperature, double outdoorTemperature,
			double roomTemperature, double heatLossCoefficient, double carnotEfficiency) {
	}

	private final Parameters parameters;

	private double storageTemperature;
	private int electricPower = 0;
	private int thermalPower = 0;
	private double cop;
	private HeatPumpOperatingState operatingState = HeatPumpOperatingState.OFF;
	private boolean running = false;
	private long runTime = 0;
	private long offTime;
	private int starts = 0;

	HeatPumpModel(Parameters parameters, double initialStorageTemperature) {
		this.parameters = parameters;
		this.storageTemperature = initialStorageTemperature;
		this.offTime = parameters.minOffTime();
		this.cop = this.calculateCop();
	}

	/**
	 * Simulates the given duration.
	 *
	 * @param duration          the simulated duration
	 * @param targetActivePower the preferred electric power [W]; null for none
	 * @param activePowerLimit  the hard limit of the electric power [W]; null for
	 *                          none
	 */
	void step(Duration duration, Integer targetActivePower, Integer activePowerLimit) {
		var seconds = duration.toSeconds();
		if (seconds <= 0) {
			return;
		}

		this.cop = this.calculateCop();
		var power = this.decidePower(targetActivePower, activePowerLimit);
		this.applyPower(power, seconds);

		var heatLoss = this.getHeatLoss();
		var energy = (this.thermalPower - heatLoss) * seconds / 3600.0; // [Wh]
		this.storageTemperature += energy / this.getHeatCapacity();
	}

	private int decidePower(Integer targetActivePower, Integer activePowerLimit) {
		var p = this.parameters;
		var comfortDemand = this.running //
				? this.storageTemperature < p.targetTemperature() //
				: this.storageTemperature < p.targetTemperature() - p.hysteresis();

		int desired;
		if (targetActivePower != null) {
			desired = Math.max(0, targetActivePower);
			if (comfortDemand) {
				desired = Math.max(desired, this.getAutonomousPower());
			}
		} else {
			desired = comfortDemand ? this.getAutonomousPower() : 0;
		}

		if (this.storageTemperature >= p.maxStorageTemperature()) {
			desired = 0;
		}
		if (this.running && this.runTime < p.minRunTime() && desired < p.minPower()
				&& this.storageTemperature < p.maxStorageTemperature()) {
			desired = p.minPower();
		}
		if (activePowerLimit != null) {
			desired = Math.min(desired, Math.max(0, activePowerLimit));
		}
		if (desired < p.minPower()) {
			desired = 0;
		}
		if (desired > 0 && !this.running && this.offTime < p.minOffTime()) {
			this.operatingState = HeatPumpOperatingState.BLOCKED;
			return 0;
		}
		this.operatingState = desired > 0 //
				? HeatPumpOperatingState.SPACE_HEATING //
				: HeatPumpOperatingState.OFF;
		return Math.min(desired, p.maxPower());
	}

	private void applyPower(int power, long seconds) {
		if (power > 0) {
			if (!this.running) {
				this.running = true;
				this.runTime = 0;
				this.starts++;
			}
			this.runTime += seconds;
		} else {
			if (this.running) {
				this.running = false;
				this.offTime = 0;
			}
			this.offTime += seconds;
		}
		this.electricPower = power;
		this.thermalPower = (int) Math.round(power * this.cop);
	}

	/**
	 * Electric power the own control uses: covers the heat loss and reheats the
	 * hysteresis band within one hour.
	 *
	 * @return the power [W]
	 */
	private int getAutonomousPower() {
		var p = this.parameters;
		var thermal = this.getHeatLoss() + this.getHeatCapacity() * p.hysteresis();
		var electric = (int) Math.round(thermal / this.cop);
		return Math.max(p.minPower(), Math.min(p.maxPower(), electric));
	}

	private double calculateCop() {
		var p = this.parameters;
		var sink = this.storageTemperature + SINK_TEMPERATURE_LIFT;
		var source = p.outdoorTemperature() - SOURCE_TEMPERATURE_DROP;
		var lift = sink - source;
		if (lift <= 1.0) {
			return MAX_COP;
		}
		var cop = p.carnotEfficiency() * (sink + CELSIUS_TO_KELVIN) / lift;
		return Math.max(MIN_COP, Math.min(MAX_COP, cop));
	}

	/**
	 * Heat loss of the building [W].
	 *
	 * @return the heat loss
	 */
	double getHeatLoss() {
		var p = this.parameters;
		return Math.max(0, p.heatLossCoefficient() * (p.roomTemperature() - p.outdoorTemperature()));
	}

	/**
	 * Heat capacity of the storage [Wh/K].
	 *
	 * @return the heat capacity
	 */
	private double getHeatCapacity() {
		return this.parameters.storageVolume() * WATER_HEAT_CAPACITY;
	}

	double getStorageTemperature() {
		return this.storageTemperature;
	}

	int getElectricPower() {
		return this.electricPower;
	}

	int getThermalPower() {
		return this.thermalPower;
	}

	double getCop() {
		return this.cop;
	}

	HeatPumpOperatingState getOperatingState() {
		return this.operatingState;
	}

	int getStarts() {
		return this.starts;
	}
}
