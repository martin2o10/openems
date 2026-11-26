package io.openems.edge.controller.heatpump.monitor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Heat Pump Monitor", //
		description = "Monitors a heat pump via a running signal or via its electrical power and provides cycle statistics (starts, run time). This Controller is read-only; it does not control the heat pump.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHeatPumpMonitor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Running Channel", description = "Channel address of a signal that indicates whether the heat pump is running. Accepts Boolean (true = running) or Integer (not 0 = running). Takes precedence over the 'Power Channel'.")
	String heatPumpRunningChannelAddress() default "";

	@AttributeDefinition(name = "Power Channel", description = "Channel address of the electrical power of the heat pump in [W]. Ignored if 'Running Channel' is set.")
	String heatPumpPowerChannelAddress() default "";

	@AttributeDefinition(name = "Power Start Threshold", description = "Minimum electrical power in [W], above which the heat pump is considered running.")
	int powerStartThreshold() default 300;

	@AttributeDefinition(name = "Power Stop Threshold", description = "Electrical power in [W], below which the heat pump is considered paused.")
	int powerStopThreshold() default 100;

	@AttributeDefinition(name = "Minimum Run Time [s]", description = "The running signal must be continuously active for at least this many seconds, before a start is counted.")
	int minimumRunTimeSeconds() default 60;

	@AttributeDefinition(name = "Minimum Pause Time [s]", description = "The running signal must be continuously inactive for at least this many seconds, before the heat pump is considered paused.")
	int minimumPauseTimeSeconds() default 60;

	String webconsole_configurationFactory_nameHint() default "Heat Pump Monitor [{id}]";
}
