package io.openems.edge.evse.chargepoint.goe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVSE Charge-Point go-e", //
		description = "Implementation for the go-e Gemini charging station")
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
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	// Physical wiring of the charge point. SINGLE_PHASE wiring can never do
	// phase switching.
	@AttributeDefinition(name = "Wiring", description = "Is the charge point wired with one or three phases?")
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Maximum Current", description = "Maximum charging current in [mA]; internally limited by the charge point hardware (register 211 AMPERE_MAX).")
	int maxCurrent() default 32_000;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point go-e [{id}]";

}
