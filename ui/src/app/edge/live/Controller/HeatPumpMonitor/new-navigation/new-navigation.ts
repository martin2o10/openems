import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerHeatPumpMonitor } from "../shared/shared";

@Component({
    selector: "oe-controller-heat-pump-monitor-home",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [CommonModule, IonicModule, ReactiveFormsModule, FormlyModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class ControllerHeatPumpMonitorHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private route: ActivatedRoute = inject(ActivatedRoute);

    public static getFormlyGeneralView(translate: TranslateService, component: EdgeConfig.Component): OeFormlyView {
        const lines: OeFormlyField[] = [
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.OPERATING_STATUS"),
                channel: component.id + "/HeatPumpRunning",
                converter: SharedControllerHeatPumpMonitor.runningStateText(translate),
            },
        ];

        if (SharedControllerHeatPumpMonitor.isConfigured(component, "heatPumpPowerChannelAddress")) {
            lines.push({
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.ACTIVE_POWER"),
                channel: component.id + "/HeatPumpActivePower",
            });
        }
        if (SharedControllerHeatPumpMonitor.isConfigured(component, "bufferTemperatureChannelAddress")) {
            lines.push({
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.BUFFER_TEMPERATURE"),
                channel: component.id + "/HeatPumpBufferTemperature",
                converter: SharedControllerHeatPumpMonitor.temperatureOneDecimal,
            });
        }
        if (SharedControllerHeatPumpMonitor.isConfigured(component, "outsideTemperatureChannelAddress")) {
            lines.push({
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.OUTSIDE_TEMPERATURE"),
                channel: component.id + "/HeatPumpOutsideTemperature",
                converter: SharedControllerHeatPumpMonitor.temperatureOneDecimal,
            });
        }

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.TOTAL_STARTS"),
                channel: component.id + "/HeatPumpStartsTotal",
            },
            {
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.TOTAL_RUN_TIME"),
                channel: component.id + "/HeatPumpRunTimeTotal",
                converter: SharedControllerHeatPumpMonitor.secondsToHours,
            },
            {
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.LAST_RUN_DURATION"),
                channel: component.id + "/HeatPumpLastRunDuration",
                converter: SharedControllerHeatPumpMonitor.secondsToHours,
            },
            {
                type: "channel-line",
                name: translate.instant("HEAT_PUMP_MONITOR.LAST_PAUSE_DURATION"),
                channel: component.id + "/HeatPumpLastPauseDuration",
                converter: SharedControllerHeatPumpMonitor.secondsToHours,
            },
        );

        return {
            title: component.alias,
            lines: lines,
            icon: { name: "oe-heatpump", color: "normal", size: "normal" },
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);

        return ControllerHeatPumpMonitorHomeComponent.getFormlyGeneralView(this.translate, component);
    }
}
