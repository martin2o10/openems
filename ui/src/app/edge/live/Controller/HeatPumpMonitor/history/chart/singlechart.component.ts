import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Name } from "src/app/shared/components/shared/name";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-heat-pump-monitor-single-chart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class ControllerHeatPumpMonitorSingleChartComponent extends AbstractHistoryChart {
    public static getChartData(_config: EdgeConfig, component: EdgeConfig.Component): HistoryUtils.ChartData {
        const controllerId = component.id;

        return {
            input: [
                {
                    name: controllerId,
                    powerChannel: new ChannelAddress(controllerId, "HeatPumpRunning"),
                },
            ],
            output: (data: HistoryUtils.ChannelData) => {
                const output: HistoryUtils.DisplayValue[] = [
                    {
                        name: Name.METER_ALIAS_OR_ID(component),
                        converter: () => {
                            return data[controllerId].map((val) => val);
                        },
                        color: ChartConstants.Colors.SHADES_OF_YELLOW[0],
                    },
                ];

                return output;
            },
            tooltip: {
                formatNumber: "1.0-0",
            },
            yAxes: [
                {
                    unit: YAxisType.RELAY,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
            ],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return ControllerHeatPumpMonitorSingleChartComponent.getChartData(this.config, this.component);
    }
}
