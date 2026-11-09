package io.openems.edge.evse.chargepoint.ocpp;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVSE Charge-Point OCPP", //
		description = "Implementation for a charging station connected via OCPP 1.6J. Requires an 'EVCS OCPP Server' component.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this charge point is read only.", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "OCPP-ID", description = "The OCPP identifier the charging station uses to connect to the OCPP Server.")
	String ocppId() default "";

	@AttributeDefinition(name = "Connector-ID", description = "The connector of the charging station.")
	int connectorId() default 1;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Wiring", description = "Is the charge point wired with one or three phases?")
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Maximum Current", description = "Maximum charging current in [mA]")
	int maxCurrent() default 32_000;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point OCPP [{id}]";

}
