package io.openems.edge.controller.heatpump.single;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Heat Pump Single", //
		description = "Controls a single heat pump; power is distributed by Controller.HeatPump.Cluster")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHeatPumpSingle0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the mode")
	Mode mode() default Mode.OWN_CONTROL;

	@AttributeDefinition(name = "Heat-Pump-ID", description = "ID of ManagedHeatPump", required = true)
	String heatPump_id() default "heatPump0";

	@AttributeDefinition(name = "Storage volume [l]", description = "Volume of the heat storage (Smart mode)")
	int storageVolume() default 500;

	@AttributeDefinition(name = "Min. storage temperature [°C]", description = "Lowest comfort temperature of the storage (Smart mode)")
	int minStorageTemperature() default 35;

	@AttributeDefinition(name = "Max. storage temperature [°C]", description = "Highest allowed storage temperature (Smart mode)")
	int maxStorageTemperature() default 55;

	@AttributeDefinition(name = "Average heat demand [W]", description = "Average thermal power taken from the storage (Smart mode)")
	int averageHeatDemand() default 2000;

	@AttributeDefinition(name = "Estimated COP [1/10]", description = "Estimated coefficient of performance times ten, e.g. 35 for 3.5 (Smart mode)")
	int estimatedCop() default 35;

	String webconsole_configurationFactory_nameHint() default "Controller Heat Pump Single [{id}]";
}
