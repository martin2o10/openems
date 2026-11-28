import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

type HeatPumpHistoryTab = "temperatures" | "powerEnergy" | "cop";

@Component({
    selector: "controller-heatpump-overview",
    templateUrl: "./overview.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerHeatPumpOverviewComponent extends AbstractHistoryChartOverview {
    protected selectedTab: HeatPumpHistoryTab = "temperatures";

    protected setTab(event: CustomEvent): void {
        const value = event.detail?.value;
        if (value === "temperatures" || value === "powerEnergy" || value === "cop") {
            this.selectedTab = value;
        }
    }
}
