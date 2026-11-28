import { TranslateService } from "@ngx-translate/core";
import heatPumpEn from "src/app/edge/live/Controller/HeatPump/i18n/en.json";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { HistoryUtils } from "src/app/shared/utils/utils";
import { getCopChartData, getPowerEnergyChartData, getTemperaturesChartData } from "./chart-data";

describe("HeatPump history chart data", () => {
    let translate: TranslateService;

    beforeEach(async () => {
        const testContext = await TestingUtils.sharedSetup();
        translate = testContext.translate;
        translate.setTranslation("en", heatPumpEn, true);
        translate.use("en");
    });

    const component = DummyConfig.from(DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0", "Wärmepumpe")).components[
        "heatPump0"
    ];

    function outputNames(chartData: HistoryUtils.ChartData, data: HistoryUtils.ChannelData) {
        return chartData.output(data).map((d) => d.name);
    }

    describe("getTemperaturesChartData", () => {
        it("queries the five temperature channels", () => {
            const chartData = getTemperaturesChartData(component, translate);
            expect(chartData.input.map((c) => c.powerChannel?.toString() ?? c.energyChannel?.toString())).toEqual([
                "heatPump0/OutdoorTemperature",
                "heatPump0/FlowTemperature",
                "heatPump0/ReturnTemperature",
                "heatPump0/DomesticHotWaterTemperature",
                "heatPump0/Temperature",
            ]);
        });

        it("converts dezidegree values to degrees", () => {
            const chartData = getTemperaturesChartData(component, translate);
            const output = chartData.output({
                OutdoorTemperature: [50, null],
                FlowTemperature: [480, null],
                ReturnTemperature: [350, null],
                DomesticHotWaterTemperature: [420, null],
                Temperature: [300, null],
            });
            expect(output[0].converter()).toEqual([5, null]);
            expect(output[1].converter()).toEqual([48, null]);
            expect(outputNames(chartData, {})).toContain("Outdoor temperature");
            expect(chartData.yAxes[0].unit).toBeDefined();
        });
    });

    describe("getPowerEnergyChartData", () => {
        it("shows power in kW in the line chart", () => {
            const chartData = getPowerEnergyChartData(component, translate, "line");
            const output = chartData.output({ ActivePower: [1500, null], ThermalPower: [5200, null] });
            expect(output.map((d) => d.name)).toEqual(["Electric power", "Thermal power"]);
            expect(output[0].converter()).toEqual([1.5, null]);
            expect(output[1].converter()).toEqual([5.2, null]);
            expect(output[0].color).toBe(ChartConstants.Colors.BLUE);
            expect(output[1].color).toBe(ChartConstants.Colors.GREEN);
        });

        it("shows the energy split as stacked bars in the bar chart", () => {
            const chartData = getPowerEnergyChartData(component, translate, "bar");
            const data: HistoryUtils.ChannelData = {
                ThermalEnergyHeating: [1000],
                ElectricEnergyHeating: [500],
                ThermalEnergyDhws: [200],
                ElectricEnergyDhws: [100],
            };
            const output = chartData.output(data);
            expect(output.map((d) => d.name)).toEqual([
                "Thermal energy - heating",
                "Electric energy - heating",
                "Thermal energy - hot water",
                "Electric energy - hot water",
            ]);
            expect(output.map((d) => d.converter())).toEqual([[1000], [500], [200], [100]]);
            expect(output.every((d) => d.stack === 0)).toBeTrue();
        });
    });

    describe("getCopChartData", () => {
        it("passes the COP values through", () => {
            const chartData = getCopChartData(component, translate);
            expect(chartData.input[0].powerChannel?.toString()).toBe("heatPump0/CoefficientOfPerformance");
            const output = chartData.output({ CoefficientOfPerformance: [3.5, null] });
            expect(output[0].name).toBe("Coefficient of performance (COP)");
            expect(output[0].converter()).toEqual([3.5, null]);
        });
    });
});
