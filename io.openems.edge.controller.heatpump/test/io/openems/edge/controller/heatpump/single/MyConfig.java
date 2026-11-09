package io.openems.edge.controller.heatpump.single;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private Mode mode = Mode.OWN_CONTROL;
		private String heatPumpId = "heatPump0";

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setHeatPumpId(String heatPumpId) {
			this.heatPumpId = heatPumpId;
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
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String heatPump_id() {
		return this.builder.heatPumpId;
	}

	@Override
	public int storageVolume() {
		return 500;
	}

	@Override
	public int minStorageTemperature() {
		return 35;
	}

	@Override
	public int maxStorageTemperature() {
		return 55;
	}

	@Override
	public int averageHeatDemand() {
		return 2000;
	}

	@Override
	public int estimatedCop() {
		return 35;
	}
}
