package io.openems.edge.controller.heatpump.cluster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Heat Pump Cluster", //
		description = "Distributes power to multiple Controller.HeatPump.Single")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHeatPumpCluster0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Heat-Pump-Controller-IDs", description = "IDs of Controller.HeatPump.Single, in order of priority", required = true)
	String[] ctrl_ids() default { "ctrlHeatPumpSingle0" };

	String webconsole_configurationFactory_nameHint() default "Controller Heat Pump Cluster [{id}]";
}
