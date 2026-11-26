package io.openems.edge.controller.heatpump.monitor;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String heatPumpRunningChannelAddress = "";
		private String heatPumpPowerChannelAddress = "";
		private String bufferTemperatureChannelAddress = "";
		private String outsideTemperatureChannelAddress = "";
		private int powerStartThreshold = 300;
		private int powerStopThreshold = 100;
		private int minimumRunTimeSeconds = 60;
		private int minimumPauseTimeSeconds = 60;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setHeatPumpRunningChannelAddress(String heatPumpRunningChannelAddress) {
			this.heatPumpRunningChannelAddress = heatPumpRunningChannelAddress;
			return this;
		}

		public Builder setHeatPumpPowerChannelAddress(String heatPumpPowerChannelAddress) {
			this.heatPumpPowerChannelAddress = heatPumpPowerChannelAddress;
			return this;
		}

		public Builder setBufferTemperatureChannelAddress(String bufferTemperatureChannelAddress) {
			this.bufferTemperatureChannelAddress = bufferTemperatureChannelAddress;
			return this;
		}

		public Builder setOutsideTemperatureChannelAddress(String outsideTemperatureChannelAddress) {
			this.outsideTemperatureChannelAddress = outsideTemperatureChannelAddress;
			return this;
		}

		public Builder setPowerStartThreshold(int powerStartThreshold) {
			this.powerStartThreshold = powerStartThreshold;
			return this;
		}

		public Builder setPowerStopThreshold(int powerStopThreshold) {
			this.powerStopThreshold = powerStopThreshold;
			return this;
		}

		public Builder setMinimumRunTimeSeconds(int minimumRunTimeSeconds) {
			this.minimumRunTimeSeconds = minimumRunTimeSeconds;
			return this;
		}

		public Builder setMinimumPauseTimeSeconds(int minimumPauseTimeSeconds) {
			this.minimumPauseTimeSeconds = minimumPauseTimeSeconds;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
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
	public String heatPumpRunningChannelAddress() {
		return this.builder.heatPumpRunningChannelAddress;
	}

	@Override
	public String heatPumpPowerChannelAddress() {
		return this.builder.heatPumpPowerChannelAddress;
	}

	@Override
	public String bufferTemperatureChannelAddress() {
		return this.builder.bufferTemperatureChannelAddress;
	}

	@Override
	public String outsideTemperatureChannelAddress() {
		return this.builder.outsideTemperatureChannelAddress;
	}

	@Override
	public int powerStartThreshold() {
		return this.builder.powerStartThreshold;
	}

	@Override
	public int powerStopThreshold() {
		return this.builder.powerStopThreshold;
	}

	@Override
	public int minimumRunTimeSeconds() {
		return this.builder.minimumRunTimeSeconds;
	}

	@Override
	public int minimumPauseTimeSeconds() {
		return this.builder.minimumPauseTimeSeconds;
	}
}
