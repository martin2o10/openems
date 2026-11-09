package io.openems.edge.controller.heatpump.cluster;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatpump.single.ControllerHeatPumpSingle;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.HeatPump.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("ctrls")
public class ControllerHeatPumpClusterImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, ControllerHeatPumpCluster, Controller {

	@Reference
	private Sum sum;

	private Config config;

	private List<ControllerHeatPumpSingle> ctrls = new CopyOnWriteArrayList<>();

	@Reference(name = "ctrls", cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY, //
			target = "(&(id=${config.ctrl_ids})(enabled=true))")
	private void bindController(ControllerHeatPumpSingle ctrl) {
		this.ctrls.add(ctrl);
	}

	@SuppressWarnings("unused")
	private void unbindController(ControllerHeatPumpSingle ctrl) {
		this.ctrls.remove(ctrl);
	}

	public ControllerHeatPumpClusterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerHeatPumpCluster.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		var order = Arrays.asList(this.config.ctrl_ids());
		var ctrls = this.ctrls.stream() //
				.sorted(Comparator.comparingInt(c -> order.indexOf(c.id()))) //
				.toList();
		var params = ctrls.stream() //
				.map(ControllerHeatPumpSingle::getParams) //
				.filter(Objects::nonNull) //
				.toList();

		var distribution = PowerDistribution.calculate(params, //
				this.sum.getGridActivePower().orElse(0), //
				this.sum.getEssDischargePower().orElse(0));

		setValue(this, ControllerHeatPumpCluster.ChannelId.SURPLUS_POWER, distribution.surplus());

		for (var p : params) {
			var ctrl = ctrls.stream().filter(c -> c.id().equals(p.ctrlSingleId())).findFirst().orElse(null);
			if (ctrl != null) {
				ctrl.apply(p.mode(), distribution.actions().get(p.ctrlSingleId()));
			}
		}
	}
}
