package io.openems.edge.controller.heatpump.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.lang.Math.clamp;

import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.SingleModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Energy Scheduler of a Controller.HeatPump.Single.
 *
 * <p>
 * Simulates the heat storage of the heat pump with a simple model: the storage
 * is charged by the heat pump and discharged by a constant heat demand. If the
 * storage is empty, the own control of the heat pump has to cover the demand.
 * In {@link Mode#SMART} the optimizer chooses per period between
 * {@link Mode#SURPLUS} and {@link Mode#FORCE}, i.e. it pre-heats the storage in
 * cheap periods.
 *
 * <p>
 * This class has no dependencies on other goals and can be removed together
 * with its use in {@link ControllerHeatPumpSingleImpl}.
 */
public class EnergyScheduler {

	/** Specific heat capacity of water [Wh/(l*K)]. */
	public static final double WATER_HEAT_CAPACITY = 1.163;

	/**
	 * Serializable configuration snapshot for the energy schedule handler.
	 *
	 * @param mode                  the configured {@link Mode}
	 * @param maxPower              the maximum electric power [W]
	 * @param storageCapacity       the usable storage capacity [Wh]
	 * @param initialStorageEnergy  the currently stored energy [Wh]
	 * @param averageHeatDemand     the average heat demand [W]
	 * @param coefficientOfPerformance the estimated COP
	 */
	public record Config(Mode mode, int maxPower, int storageCapacity, int initialStorageEnergy,
			int averageHeatDemand, double coefficientOfPerformance) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			return jsonObjectSerializer(Config.class, //
					json -> new Config(//
							json.getEnum("mode", Mode.class), //
							json.getInt("maxPower"), //
							json.getInt("storageCapacity"), //
							json.getInt("initialStorageEnergy"), //
							json.getInt("averageHeatDemand"), //
							json.getDouble("coefficientOfPerformance")), //
					obj -> buildJsonObject() //
							.addProperty("mode", obj.mode()) //
							.addProperty("maxPower", obj.maxPower()) //
							.addProperty("storageCapacity", obj.storageCapacity()) //
							.addProperty("initialStorageEnergy", obj.initialStorageEnergy()) //
							.addProperty("averageHeatDemand", obj.averageHeatDemand()) //
							.addProperty("coefficientOfPerformance", obj.coefficientOfPerformance()) //
							.build());
		}
	}

	/**
	 * Tracks the stored heat energy during a simulated schedule.
	 */
	public static class ScheduleContext {
		private final int capacity;
		private int energy;

		public ScheduleContext(int capacity, int initialEnergy) {
			this.capacity = capacity;
			this.energy = clamp(initialEnergy, 0, capacity);
		}

		public int getEnergy() {
			return this.energy;
		}

		public int getFreeCapacity() {
			return this.capacity - this.energy;
		}
	}

	/**
	 * Calculates the usable storage capacity.
	 *
	 * @param volume         the storage volume [l]
	 * @param minTemperature the minimum (comfort) temperature [°C]
	 * @param maxTemperature the maximum storage temperature [°C]
	 * @return the capacity [Wh]
	 */
	public static int calculateStorageCapacity(int volume, int minTemperature, int maxTemperature) {
		return (int) Math.round(volume * WATER_HEAT_CAPACITY * Math.max(0, maxTemperature - minTemperature));
	}

	/**
	 * Calculates the stored energy above the minimum temperature.
	 *
	 * @param volume         the storage volume [l]
	 * @param minTemperature the minimum (comfort) temperature [°C]
	 * @param temperature    the current temperature [dezi °C]; possibly null
	 * @param capacity       the usable storage capacity [Wh]
	 * @return the stored energy [Wh]; half the capacity if the temperature is
	 *         unknown
	 */
	public static int calculateStorageEnergy(int volume, int minTemperature, Integer temperature, int capacity) {
		if (temperature == null) {
			return capacity / 2;
		}
		var energy = (int) Math.round(volume * WATER_HEAT_CAPACITY * (temperature / 10.0 - minTemperature));
		return clamp(energy, 0, capacity);
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 *
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<Mode, Config, ScheduleContext> buildEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode, Config, ScheduleContext>create(parent) //
				.setSerializer(Config.serializer(), configSupplier) //

				.setModes(() -> {
					var config = configSupplier.get();
					var modes = config == null || config.mode() != Mode.SMART //
							? ImmutableList.of(config == null ? Mode.OWN_CONTROL : config.mode()) //
							: ImmutableList.of(Mode.SURPLUS, Mode.FORCE);
					return new SingleModes<>(//
							new Modes.Channels(//
									new ChannelAddress(parent.id(), ControllerHeatPumpSingle.ChannelId.ACTUAL_MODE.id()),
									new ChannelAddress(parent.id(),
											ElectricityMeter.ChannelId.ACTIVE_POWER.id())),
							modes.stream() //
									.map(mode -> new SingleModes.SingleMode<>(mode, modes.size() > 1, null)) //
									.collect(ImmutableList.toImmutableList()));
				})

				.setOptimizationContext(goc -> configSupplier.get())

				.setScheduleContext(coc -> coc == null //
						? null //
						: new ScheduleContext(coc.storageCapacity(), coc.initialStorageEnergy()))

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					if (coc == null) {
						return mode;
					}
					var cop = coc.coefficientOfPerformance();
					var maxEnergy = period.duration().convertPowerToEnergy(coc.maxPower());
					var demand = period.duration().convertPowerToEnergy(coc.averageHeatDemand());
					var maxForStorage = (int) Math.max(0, (csc.getFreeCapacity() + demand) / cop);

					var electricEnergy = switch (mode) {
					case FORCE -> Math.min(maxEnergy, maxForStorage);
					case SURPLUS -> clamp(ef.getSurplus(), 0, Math.min(maxEnergy, maxForStorage));
					case OWN_CONTROL, SMART -> 0;
					};

					// Own control covers the demand if the storage is empty
					var balance = csc.energy + (int) Math.round(electricEnergy * cop) - demand;
					if (balance < 0) {
						electricEnergy += (int) Math.ceil(-balance / cop);
						balance = 0;
					}
					csc.energy = clamp(balance, 0, csc.capacity);

					ef.addManagedConsumption(id, electricEnergy);
					return mode;
				})

				.build();
	}
}
