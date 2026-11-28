import { TranslateService } from "@ngx-translate/core";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

/**
 * Chart data for the 'Temperatures' tab: outdoor, flow, return, hot water and buffer temperature.
 *
 * @param component The heat pump component
 * @param translate The TranslateService
 */
export function getTemperaturesChartData(
    component: EdgeConfig.Component,
    translate: TranslateService,
): HistoryUtils.ChartData {
    AssertionUtils.assertIsDefined(component);

    const input: HistoryUtils.InputChannel[] = [
        { name: "OutdoorTemperature", powerChannel: new ChannelAddress(component.id, "OutdoorTemperature") },
        { name: "FlowTemperature", powerChannel: new ChannelAddress(component.id, "FlowTemperature") },
        { name: "ReturnTemperature", powerChannel: new ChannelAddress(component.id, "ReturnTemperature") },
        {
            name: "DomesticHotWaterTemperature",
            powerChannel: new ChannelAddress(component.id, "DomesticHotWaterTemperature"),
        },
        { name: "Temperature", powerChannel: new ChannelAddress(component.id, "Temperature") },
    ];

    return {
        input: input,
        output: (data: HistoryUtils.ChannelData) => {
            // Historic data is delivered divided by 1000; DEZIDEGREE_CELSIUS needs x100 to show °C
            const toDegrees = (values: (number | null)[] | undefined) =>
                values?.map((value) => (value == null ? null : NumberUtils.multiplySafely(value, 100)));

            return [
                {
                    name: translate.instant("HEAT_PUMP.OUTDOOR_TEMPERATURE"),
                    converter: () => toDegrees(data["OutdoorTemperature"]),
                    color: ChartConstants.Colors.BLUE,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 1,
                },
                {
                    name: translate.instant("HEAT_PUMP.FLOW_TEMPERATURE"),
                    converter: () => toDegrees(data["FlowTemperature"]),
                    color: ChartConstants.Colors.RED,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 2,
                },
                {
                    name: translate.instant("HEAT_PUMP.RETURN_TEMPERATURE"),
                    converter: () => toDegrees(data["ReturnTemperature"]),
                    color: ChartConstants.Colors.ORANGE,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 3,
                },
                {
                    name: translate.instant("HEAT_PUMP.DOMESTIC_HOT_WATER_TEMPERATURE"),
                    converter: () => toDegrees(data["DomesticHotWaterTemperature"]),
                    color: ChartConstants.Colors.TURQUOISE,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 4,
                },
                {
                    name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.TEMPERATURE"),
                    converter: () => toDegrees(data["Temperature"]),
                    color: ChartConstants.Colors.GREEN,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 5,
                },
            ];
        },
        tooltip: {
            formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
        },
        yAxes: [
            {
                unit: YAxisType.TEMPERATURE,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            },
        ],
    };
}

/**
 * Chart data for the 'Power & energy' tab: electric/thermal power (line) and the energy split (bar).
 *
 * @param component The heat pump component
 * @param translate The TranslateService
 * @param chartType The chart type
 */
export function getPowerEnergyChartData(
    component: EdgeConfig.Component,
    translate: TranslateService,
    chartType: "line" | "bar",
): HistoryUtils.ChartData {
    AssertionUtils.assertIsDefined(component);

    const input: HistoryUtils.InputChannel[] = [
        { name: "ActivePower", powerChannel: new ChannelAddress(component.id, "ActivePower") },
        { name: "ThermalPower", powerChannel: new ChannelAddress(component.id, "ThermalPower") },
        {
            name: "ThermalEnergyHeating",
            energyChannel: new ChannelAddress(component.id, "ThermalEnergyHeating"),
        },
        {
            name: "ElectricEnergyHeating",
            energyChannel: new ChannelAddress(component.id, "ElectricEnergyHeating"),
        },
        {
            name: "ThermalEnergyDhws",
            energyChannel: new ChannelAddress(component.id, "ThermalEnergyDhws"),
        },
        {
            name: "ElectricEnergyDhws",
            energyChannel: new ChannelAddress(component.id, "ElectricEnergyDhws"),
        },
        {
            name: "ThermalEnergyTotal",
            energyChannel: new ChannelAddress(component.id, "ThermalEnergyTotal"),
        },
    ];

    return {
        input: input,
        output: (data: HistoryUtils.ChannelData) => {
            if (chartType === "bar") {
                return [
                    {
                        name: translate.instant("HEAT_PUMP.ENERGY.THERMAL_HEATING"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/ThermalEnergyHeating"] ?? null,
                        converter: () => data["ThermalEnergyHeating"],
                        color: ChartConstants.Colors.RED,
                        yAxisId: ChartAxis.LEFT,
                        stack: 0,
                    },
                    {
                        name: translate.instant("HEAT_PUMP.ENERGY.ELECTRIC_HEATING"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/ElectricEnergyHeating"] ?? null,
                        converter: () => data["ElectricEnergyHeating"],
                        color: ChartConstants.Colors.ORANGE,
                        yAxisId: ChartAxis.LEFT,
                        stack: 0,
                    },
                    {
                        name: translate.instant("HEAT_PUMP.ENERGY.THERMAL_DHWS"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/ThermalEnergyDhws"] ?? null,
                        converter: () => data["ThermalEnergyDhws"],
                        color: ChartConstants.Colors.TURQUOISE,
                        yAxisId: ChartAxis.LEFT,
                        stack: 0,
                    },
                    {
                        name: translate.instant("HEAT_PUMP.ENERGY.ELECTRIC_DHWS"),
                        nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) =>
                            energyValues?.result.data[component.id + "/ElectricEnergyDhws"] ?? null,
                        converter: () => data["ElectricEnergyDhws"],
                        color: ChartConstants.Colors.YELLOW,
                        yAxisId: ChartAxis.LEFT,
                        stack: 0,
                    },
                ];
            }

            return [
                {
                    // Historic data is delivered in kW already (raw W / 1000)
                    name: translate.instant("HEAT_PUMP.ELECTRIC_POWER"),
                    converter: () => data["ActivePower"]?.map((value) => value),
                    color: ChartConstants.Colors.BLUE,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 1,
                },
                {
                    name: translate.instant("HEAT_PUMP.THERMAL_POWER"),
                    converter: () => data["ThermalPower"]?.map((value) => value),
                    color: ChartConstants.Colors.GREEN,
                    yAxisId: ChartAxis.LEFT,
                    stack: 0,
                    order: 2,
                },
            ];
        },
        tooltip: {
            formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
        },
        yAxes: [
            {
                unit: chartType === "bar" ? YAxisType.ENERGY : YAxisType.POWER,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            },
        ],
    };
}

/**
 * Chart data for the 'COP' tab.
 *
 * @param component The heat pump component
 * @param translate The TranslateService
 */
export function getCopChartData(component: EdgeConfig.Component, translate: TranslateService): HistoryUtils.ChartData {
    AssertionUtils.assertIsDefined(component);

    const input: HistoryUtils.InputChannel[] = [
        {
            name: "CoefficientOfPerformance",
            powerChannel: new ChannelAddress(component.id, "CoefficientOfPerformance"),
        },
    ];

    return {
        input: input,
        output: (data: HistoryUtils.ChannelData) => [
            {
                name: translate.instant("HEAT_PUMP.COP"),
                // Historic data is delivered divided by 1000; COP is dimensionless -> x1000
                converter: () =>
                    data["CoefficientOfPerformance"]?.map((value) =>
                        value == null ? null : NumberUtils.multiplySafely(value, 1000),
                    ),
                color: ChartConstants.Colors.GREEN,
                yAxisId: ChartAxis.LEFT,
                stack: 0,
                order: 1,
            },
        ],
        tooltip: {
            formatNumber: ChartConstants.NumberFormat.ONE_TO_TWO,
        },
        yAxes: [
            {
                unit: YAxisType.NONE,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            },
        ],
    };
}
