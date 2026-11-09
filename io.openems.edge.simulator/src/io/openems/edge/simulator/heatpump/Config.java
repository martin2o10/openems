package io.openems.edge.simulator.heatpump;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Simulator Heat Pump", //
		description = "This simulates a heat pump that heats a water storage.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "heatPump0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Min Power", description = "Minimum electric power while running [W]")
	int minPower() default 1200;

	@AttributeDefinition(name = "Max Power", description = "Maximum electric power [W]")
	int maxPower() default 4500;

	@AttributeDefinition(name = "Min Run Time", description = "Minimum run time of the compressor [s]")
	int minRunTime() default 1200;

	@AttributeDefinition(name = "Min Off Time", description = "Minimum off time of the compressor [s]")
	int minOffTime() default 600;

	@AttributeDefinition(name = "Storage Volume", description = "Volume of the water storage [l]")
	int storageVolume() default 1000;

	@AttributeDefinition(name = "Initial Storage Temperature", description = "Storage temperature at start [dezidegree Celsius]")
	int initialStorageTemperature() default 380;

	@AttributeDefinition(name = "Target Temperature", description = "Target storage temperature of the own control [dezidegree Celsius]")
	int targetTemperature() default 400;

	@AttributeDefinition(name = "Hysteresis", description = "Switch-on hysteresis of the own control [dezidegree Celsius]")
	int hysteresis() default 50;

	@AttributeDefinition(name = "Max Storage Temperature", description = "Maximum storage temperature [dezidegree Celsius]")
	int maxStorageTemperature() default 550;

	@AttributeDefinition(name = "Outdoor Temperature", description = "Constant outdoor temperature [dezidegree Celsius]")
	int outdoorTemperature() default 50;

	@AttributeDefinition(name = "Room Temperature", description = "Room temperature of the building [dezidegree Celsius]")
	int roomTemperature() default 200;

	@AttributeDefinition(name = "Heat Loss Coefficient", description = "Heat loss coefficient of the building [W/K]")
	int heatLossCoefficient() default 200;

	@AttributeDefinition(name = "Carnot Efficiency", description = "Share of the Carnot coefficient of performance that is reached [%]")
	int carnotEfficiency() default 45;

	String webconsole_configurationFactory_nameHint() default "Simulator Heat Pump [{id}]";
}
