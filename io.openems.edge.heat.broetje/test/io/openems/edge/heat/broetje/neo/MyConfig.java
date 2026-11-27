package io.openems.edge.heat.broetje.neo;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private boolean readOnly = true;
		private boolean readPressures = false;
		private boolean readOperatingHours = false;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setReadPressures(boolean readPressures) {
			this.readPressures = readPressures;
			return this;
		}

		public Builder setReadOperatingHours(boolean readOperatingHours) {
			this.readOperatingHours = readOperatingHours;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link MyConfig.Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public int modbusUnitId() {
		return 1;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public int minPower() {
		return 800;
	}

	@Override
	public int maxPower() {
		return 3500;
	}

	@Override
	public int minRunTimeSeconds() {
		return 1200;
	}

	@Override
	public int minOffTimeSeconds() {
		return 600;
	}

	@Override
	public int writeIntervalSeconds() {
		return 10;
	}

	@Override
	public boolean readPressures() {
		return this.builder.readPressures;
	}

	@Override
	public boolean readOperatingHours() {
		return this.builder.readOperatingHours;
	}

	@Override
	public boolean readSourceTemperatures() {
		return false;
	}

	@Override
	public boolean readRefrigerantTemperatures() {
		return false;
	}

	@Override
	public boolean readPumpFlags() {
		return false;
	}

	@Override
	public boolean readFlowRate() {
		return false;
	}

	@Override
	public boolean readFreshWaterTemperature() {
		return false;
	}

	@Override
	public boolean readDelayedOutdoorTemperature() {
		return false;
	}

	@Override
	public boolean readMixerCircuitSetpoints() {
		return false;
	}

	@Override
	public boolean readExpansionValve() {
		return false;
	}

	@Override
	public boolean readHoldingRegisters() {
		return false;
	}

	@Override
	public boolean readSpeedSetpoint() {
		return false;
	}

	@Override
	public boolean readExternalRequest() {
		return false;
	}

	@Override
	public boolean readExternalSensorOverrides() {
		return false;
	}

	@Override
	public boolean readSecondStage() {
		return false;
	}
}
