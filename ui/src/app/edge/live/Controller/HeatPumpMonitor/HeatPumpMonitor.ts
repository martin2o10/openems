import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Icon } from "src/app/shared/type/widget";

import { ChannelAddress, CurrentData } from "../../../../shared/shared";

@Component({
    selector: "Controller_HeatPumpMonitor",
    templateUrl: "./HeatPumpMonitor.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class Controller_HeatPumpMonitorComponent extends AbstractFlatWidget {
    public icon: Icon = {
        name: "oe-heatpump",
        size: "large",
        color: "normal",
    };
    public state: string = "?";
    public startsTotal: number | null = null;
    public runTimeTotalHours: number | null = null;
    public lastRunDurationHours: number | null = null;
    public lastPauseDurationHours: number | null = null;

    protected override getChannelAddresses() {
        if (this.component == null) {
            return [];
        }

        return [
            new ChannelAddress(this.component.id, "HeatPumpRunning"),
            new ChannelAddress(this.component.id, "HeatPumpStartsTotal"),
            new ChannelAddress(this.component.id, "HeatPumpRunTimeTotal"),
            new ChannelAddress(this.component.id, "HeatPumpLastRunDuration"),
            new ChannelAddress(this.component.id, "HeatPumpLastPauseDuration"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this.component == null) {
            return;
        }

        const running = currentData.allComponents[this.component.id + "/HeatPumpRunning"];
        if (running == null) {
            this.state = "?";
        } else {
            this.state = this.translate.instant(
                running === true || running === 1 ? "HEAT_PUMP_MONITOR.RUNNING" : "HEAT_PUMP_MONITOR.PAUSED",
            );
        }

        this.startsTotal = currentData.allComponents[this.component.id + "/HeatPumpStartsTotal"];
        this.runTimeTotalHours = this.secondsToHours(currentData.allComponents[this.component.id + "/HeatPumpRunTimeTotal"]);
        this.lastRunDurationHours = this.secondsToHours(currentData.allComponents[this.component.id + "/HeatPumpLastRunDuration"]);
        this.lastPauseDurationHours = this.secondsToHours(currentData.allComponents[this.component.id + "/HeatPumpLastPauseDuration"]);
    }

    private secondsToHours(value: number | null): number | null {
        if (value == null) {
            return null;
        }
        return Math.round((value / 3600) * 10) / 10;
    }
}
