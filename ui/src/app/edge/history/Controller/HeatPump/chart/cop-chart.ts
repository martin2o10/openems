import { ChangeDetectionStrategy, Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { HistoryUtils } from "src/app/shared/utils/utils";
import { getCopChartData } from "./chart-data";

@Component({
    selector: "controller-heatpump-cop-chart",
    templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CopChartComponent extends AbstractHistoryChart {
    protected static getChartData(
        component: EdgeConfig.Component | undefined,
        translate: TranslateService,
    ): HistoryUtils.ChartData {
        AssertionUtils.assertIsDefined(component);
        return getCopChartData(component, translate);
    }

    protected override getChartData(): HistoryUtils.ChartData {
        const edge = this.edge ?? this.service.currentEdge();
        AssertionUtils.assertIsDefined(edge);

        const config = this.config ?? edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = this.component ?? config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);

        return CopChartComponent.getChartData(component, this.translate);
    }
}
