import { TranslateService } from "@ngx-translate/core";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import heatPumpEn from "../i18n/en.json";
import { CONVERT_BOOLEAN_TO_ON_OFF, CONVERT_COP, CONVERT_DEMAND_TO_LABEL, CONVERT_FAULT_LEVEL_TO_LABEL, CONVERT_OPERATING_MODE_TO_LABEL, CONVERT_OPERATING_STATE_TO_LABEL, CONVERT_THOUSANDTH_TO_PERCENT, Demand, FACTORY_ID_HEAT_BROETJE_NEO, FaultLevel, getFlatChannelAddresses, getModalChannelAddresses, getOperatingStateColor, HeatPumpOperatingState, isHeatBroetjeNeo, OperatingMode, } from "./shared";

describe("SharedControllerHeatPump", () => {
    let translate: TranslateService;

    beforeEach(async () => {
        const testContext = await TestingUtils.sharedSetup();
        translate = testContext.translate;
        translate.setTranslation("en", heatPumpEn, true);
        translate.use("en");
    });

    const broetje = DummyConfig.from(DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0", "Wärmepumpe")).components[
        "heatPump0"
    ];
    const generic = new EdgeConfig.Component("heatPump0", "HeatPump", true, false, "Heat.Generic", {});

    describe("isHeatBroetjeNeo", () => {
        it("matches only the Heat.Broetje.Neo factoryId", () => {
            expect(isHeatBroetjeNeo(broetje)).toBeTrue();
            expect(broetje.factoryId).toBe(FACTORY_ID_HEAT_BROETJE_NEO);
            expect(isHeatBroetjeNeo(generic)).toBeFalse();
            expect(isHeatBroetjeNeo(null)).toBeFalse();
        });
    });

    describe("CONVERT_OPERATING_STATE_TO_LABEL", () => {
        const convert = (value: number | null): string => CONVERT_OPERATING_STATE_TO_LABEL(translate)(value);

        it("maps every HeatPumpOperatingState to its label", () => {
            expect(convert(HeatPumpOperatingState.OFF)).toBe("Off");
            expect(convert(HeatPumpOperatingState.SPACE_HEATING)).toBe("Space heating");
            expect(convert(HeatPumpOperatingState.DOMESTIC_HOT_WATER)).toBe("Domestic hot water");
            expect(convert(HeatPumpOperatingState.COOLING)).toBe("Cooling");
            expect(convert(HeatPumpOperatingState.DEFROST)).toBe("Defrost");
            expect(convert(HeatPumpOperatingState.BLOCKED)).toBe("Blocked");
            expect(convert(HeatPumpOperatingState.FAULT)).toBe("Fault");
            expect(convert(HeatPumpOperatingState.UNDEFINED)).toBe("Undefined");
            expect(convert(99)).toBe("Undefined");
        });
    });

    describe("getOperatingStateColor", () => {
        it("maps states to distinct palette colors", () => {
            expect(getOperatingStateColor(HeatPumpOperatingState.OFF)).toBe(ChartConstants.Colors.GREY);
            expect(getOperatingStateColor(HeatPumpOperatingState.UNDEFINED)).toBe(ChartConstants.Colors.GREY);
            expect(getOperatingStateColor(HeatPumpOperatingState.SPACE_HEATING)).toBe(ChartConstants.Colors.GREEN);
            expect(getOperatingStateColor(HeatPumpOperatingState.DOMESTIC_HOT_WATER)).toBe(ChartConstants.Colors.BLUE);
            expect(getOperatingStateColor(HeatPumpOperatingState.COOLING)).toBe(ChartConstants.Colors.TURQUOISE);
            expect(getOperatingStateColor(HeatPumpOperatingState.DEFROST)).toBe(ChartConstants.Colors.YELLOW);
            expect(getOperatingStateColor(HeatPumpOperatingState.BLOCKED)).toBe(ChartConstants.Colors.ORANGE);
            expect(getOperatingStateColor(HeatPumpOperatingState.FAULT)).toBe(ChartConstants.Colors.RED);
            expect(getOperatingStateColor(null)).toBe(ChartConstants.Colors.GREY);
        });
    });

    describe("CONVERT_OPERATING_MODE_TO_LABEL", () => {
        const convert = (value: number | null): string => CONVERT_OPERATING_MODE_TO_LABEL(translate)(value);

        it("maps modes to labels", () => {
            expect(convert(OperatingMode.AUTOMATIC)).toBe("Automatic");
            expect(convert(OperatingMode.GRID_OPERATOR_LOCK)).toBe("Grid operator lock");
            expect(convert(OperatingMode.MAIN_SWITCH_OFF)).toBe("Main switch off");
            expect(convert(OperatingMode.UNDEFINED)).toBe("Undefined");
            expect(convert(42)).toBe("Undefined");
        });
    });

    describe("CONVERT_DEMAND_TO_LABEL", () => {
        const convert = (value: number | null): string => CONVERT_DEMAND_TO_LABEL(translate)(value);

        it("maps demand values to labels", () => {
            expect(convert(Demand.NONE)).toBe("No demand");
            expect(convert(Demand.SPACE_HEATING)).toBe("Space heating");
            expect(convert(Demand.DOMESTIC_HOT_WATER)).toBe("Domestic hot water");
            expect(convert(Demand.COOLING)).toBe("Cooling");
            expect(convert(Demand.UNDEFINED)).toBe("Undefined");
        });
    });

    describe("CONVERT_FAULT_LEVEL_TO_LABEL", () => {
        const convert = (value: number | null): string => CONVERT_FAULT_LEVEL_TO_LABEL(translate)(value);

        it("maps levels to labels", () => {
            expect(convert(FaultLevel.OK)).toBe("Ok");
            expect(convert(FaultLevel.INFO)).toBe("Info");
            expect(convert(FaultLevel.WARNING)).toBe("Warning");
            expect(convert(FaultLevel.FAULT)).toBe("Fault");
        });
    });

    describe("scalar converters", () => {
        it("CONVERT_BOOLEAN_TO_ON_OFF handles boolean and numeric values", () => {
            const convert = CONVERT_BOOLEAN_TO_ON_OFF(translate);
            expect(convert(true)).toBe("On");
            expect(convert(1)).toBe("On");
            expect(convert(false)).toBe("Off");
            expect(convert(0)).toBe("Off");
        });

        it("CONVERT_THOUSANDTH_TO_PERCENT converts thousandths to percent", () => {
            expect(CONVERT_THOUSANDTH_TO_PERCENT(520)).toBe("52 %");
            expect(CONVERT_THOUSANDTH_TO_PERCENT(0)).toBe("0 %");
            expect(CONVERT_THOUSANDTH_TO_PERCENT(null)).toBe("-");
        });

        it("CONVERT_COP formats the already-scaled COP value", () => {
            expect(CONVERT_COP(3.5)).toBe("3.5");
            expect(CONVERT_COP(3.47)).toBe("3.5");
            expect(CONVERT_COP(null)).toBe("-");
        });
    });

    describe("getFlatChannelAddresses", () => {
        it("returns the nature-generic channels for any ManagedHeatPump", () => {
            expect(getFlatChannelAddresses(generic)).toEqual([
                new ChannelAddress("heatPump0", "OperatingState"),
                new ChannelAddress("heatPump0", "ThermalPower"),
                new ChannelAddress("heatPump0", "ActivePower"),
                new ChannelAddress("heatPump0", "Temperature"),
            ]);
        });

        it("adds the Heat.Broetje.Neo channels only for the Broetje factory", () => {
            const channels = getFlatChannelAddresses(broetje).map((c) => c.toString());
            expect(channels).toContain("heatPump0/CoefficientOfPerformance");
            expect(channels).toContain("heatPump0/OutdoorTemperature");
            expect(channels).toContain("heatPump0/FlowTemperature");
            expect(channels).toContain("heatPump0/ReturnTemperature");
            expect(channels).toContain("heatPump0/DomesticHotWaterTemperature");
            expect(channels).toContain("heatPump0/OperatingMode");
            expect(channels.length).toBe(10);
        });
    });

    describe("getModalChannelAddresses", () => {
        it("returns only generic channels for non-Broetje components", () => {
            const channels = getModalChannelAddresses(generic).map((c) => c.toString());
            expect(channels).toContain("heatPump0/ActiveConsumptionEnergy");
            expect(channels).toContain("heatPump0/RemainingHeatEnergy");
            expect(channels).not.toContain("heatPump0/CoefficientOfPerformance");
            expect(channels).not.toContain("heatPump0/Pressure1");
        });

        it("includes the Broetje base channels but no Kür channels by default", () => {
            const channels = getModalChannelAddresses(broetje).map((c) => c.toString());
            expect(channels).toContain("heatPump0/CoefficientOfPerformance");
            expect(channels).toContain("heatPump0/CompressorSpeed");
            expect(channels).toContain("heatPump0/GridOperatorRelease");
            expect(channels).toContain("heatPump0/DeviceFault");
            expect(channels).toContain("heatPump0/ThermalEnergyTotal");
            expect(channels).not.toContain("heatPump0/Pressure1");
            expect(channels).not.toContain("heatPump0/OperatingHours1");
            expect(channels).not.toContain("heatPump0/VolumeFlow");
        });

        it("includes Kür channels only when the config switch is enabled", () => {
            const withKuer = DummyConfig.from(
                DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0", "Wärmepumpe", {
                    readPressures: true,
                    readOperatingHours: true,
                    readFlowRate: true,
                }),
            ).components["heatPump0"];
            const channels = getModalChannelAddresses(withKuer).map((c) => c.toString());
            expect(channels).toContain("heatPump0/Pressure1");
            expect(channels).toContain("heatPump0/Pressure2");
            expect(channels).toContain("heatPump0/OperatingHours1");
            expect(channels).toContain("heatPump0/OperatingHours2");
            expect(channels).toContain("heatPump0/VolumeFlow");
            expect(channels).not.toContain("heatPump0/SourceFlowTemperature");
        });
    });
});
