package io.openems.edge.simulator.heatpump;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private int initialStorageTemperature = 380;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setInitialStorageTemperature(int initialStorageTemperature) {
			this.initialStorageTemperature = initialStorageTemperature;
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
	public int minPower() {
		return 1200;
	}

	@Override
	public int maxPower() {
		return 4500;
	}

	@Override
	public int minRunTime() {
		return 1200;
	}

	@Override
	public int minOffTime() {
		return 600;
	}

	@Override
	public int storageVolume() {
		return 1000;
	}

	@Override
	public int initialStorageTemperature() {
		return this.builder.initialStorageTemperature;
	}

	@Override
	public int targetTemperature() {
		return 400;
	}

	@Override
	public int hysteresis() {
		return 50;
	}

	@Override
	public int maxStorageTemperature() {
		return 550;
	}

	@Override
	public int outdoorTemperature() {
		return 50;
	}

	@Override
	public int roomTemperature() {
		return 200;
	}

	@Override
	public int heatLossCoefficient() {
		return 200;
	}

	@Override
	public int carnotEfficiency() {
		return 45;
	}
}
