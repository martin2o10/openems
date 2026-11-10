package io.openems.edge.evse.chargepoint.abl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVSE Charge-Point ABL eM4", //
		description = "Implementation for the ABL eM4 Single and Twin charging station in stand-alone operation")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this charge point is read only.", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 255;

	@AttributeDefinition(name = "Outlet", description = "Outlet of the charge point; only the eM4 Twin has two.")
	Outlet outlet() default Outlet.OUTLET_1;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Wiring", description = "Is the charge point wired with one or three phases?")
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point ABL eM4 [{id}]";

}
