import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
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
    public static getChartData(
        _config: EdgeConfig,
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): HistoryUtils.ChartData {
        const controllerId = component.id;

        return {
            input: [
                {
                    name: controllerId + "/HeatPumpActivePower",
                    powerChannel: new ChannelAddress(controllerId, "HeatPumpActivePower"),
                },
                {
                    name: controllerId + "/HeatingElementsActivePower",
                    powerChannel: new ChannelAddress(controllerId, "HeatingElementsActivePower"),
                },
                {
                    name: controllerId + "/HeatPumpBufferTemperature",
                    powerChannel: new ChannelAddress(controllerId, "HeatPumpBufferTemperature"),
                },
                {
                    name: controllerId + "/HeatPumpOutsideTemperature",
                    powerChannel: new ChannelAddress(controllerId, "HeatPumpOutsideTemperature"),
                },
            ],
            output: (data: HistoryUtils.ChannelData) => {
                const output: HistoryUtils.DisplayValue[] = [
                    {
                        name: Name.METER_ALIAS_OR_ID(component),
                        converter: () => {
                            return HistoryUtils.CONVERT_WATT_TO_KILOWATT_OR_KILOWATTHOURS(
                                data[controllerId + "/HeatPumpActivePower"] ?? [],
                            );
                        },
                        yAxisId: ChartAxis.RIGHT,
                        color: ChartConstants.Colors.RED,
                    },
                    {
                        name: translate.instant("HEAT_PUMP_MONITOR.HEATING_ELEMENTS"),
                        converter: () => {
                            return HistoryUtils.CONVERT_WATT_TO_KILOWATT_OR_KILOWATTHOURS(
                                data[controllerId + "/HeatingElementsActivePower"] ?? [],
                            );
                        },
                        yAxisId: ChartAxis.RIGHT,
                        color: ChartConstants.Colors.YELLOW,
                    },
                    {
                        name: translate.instant("HEAT_PUMP_MONITOR.BUFFER_TEMPERATURE"),
                        converter: () => {
                            return data[controllerId + "/HeatPumpBufferTemperature"]?.map((val) => val);
                        },
                        yAxisId: ChartAxis.LEFT,
                        color: ChartConstants.Colors.BLUE,
                    },
                    {
                        name: translate.instant("HEAT_PUMP_MONITOR.OUTSIDE_TEMPERATURE"),
                        converter: () => {
                            return data[controllerId + "/HeatPumpOutsideTemperature"]?.map((val) => val);
                        },
                        yAxisId: ChartAxis.LEFT,
                        color: ChartConstants.Colors.BLUE_GREY,
                    },
                ];

                return output;
            },
            tooltip: {
                formatNumber: "1.0-0",
            },
            yAxes: [
                {
                    unit: YAxisType.TEMPERATURE,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
                {
                    unit: YAxisType.POWER,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT,
                    displayGrid: false,
                },
            ],
        };
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return ControllerHeatPumpMonitorSingleChartComponent.getChartData(this.config, this.translate, this.component);
    }
}
