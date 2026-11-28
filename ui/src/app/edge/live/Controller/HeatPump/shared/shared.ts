import { TranslateService } from "@ngx-translate/core";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

/** FactoryId of the Brötje BLW Neo driver ({@code Heat.Broetje.Neo}). */
export const FACTORY_ID_HEAT_BROETJE_NEO = "Heat.Broetje.Neo";

/** Mirrors {@code io.openems.edge.heat.api.HeatPumpOperatingState}. */
export enum HeatPumpOperatingState {
    UNDEFINED = -1,
    OFF = 0,
    SPACE_HEATING = 1,
    DOMESTIC_HOT_WATER = 2,
    COOLING = 3,
    DEFROST = 4,
    BLOCKED = 5,
    FAULT = 6,
}

/** Mirrors {@code io.openems.edge.heat.broetje.neo.OperatingMode}. */
export enum OperatingMode {
    UNDEFINED = -1,
    OFF = 0,
    AUTOMATIC = 1,
    COOLING = 2,
    SUMMER = 3,
    CONTINUOUS = 4,
    SETBACK = 5,
    HOLIDAY = 6,
    PARTY = 7,
    SCREED_DRYING = 8,
    GRID_OPERATOR_LOCK = 9,
    MAIN_SWITCH_OFF = 10,
}

/** Mirrors {@code io.openems.edge.heat.broetje.neo.Demand}. */
export enum Demand {
    UNDEFINED = -1,
    NONE = 0,
    COOLING = 10,
    SPACE_HEATING = 20,
    DOMESTIC_HOT_WATER = 30,
}

/** Mirrors {@code io.openems.common.channel.Level} (channel DEVICE_FAULT). */
export enum FaultLevel {
    OK = 0,
    INFO = 1,
    WARNING = 2,
    FAULT = 3,
}

/** Whether the given component is the Brötje BLW Neo driver. */
export function isHeatBroetjeNeo(component: EdgeConfig.Component | null): boolean {
    return component?.factoryId === FACTORY_ID_HEAT_BROETJE_NEO;
}

/**
 * Converts the OPERATING_STATE channel value to a translated label.
 *
 * @param translate The TranslateService
 */
export const CONVERT_OPERATING_STATE_TO_LABEL = (translate: TranslateService) => {
    return (value: number | null): string => {
        switch (value) {
            case HeatPumpOperatingState.OFF:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.OFF");
            case HeatPumpOperatingState.SPACE_HEATING:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.SPACE_HEATING");
            case HeatPumpOperatingState.DOMESTIC_HOT_WATER:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.DOMESTIC_HOT_WATER");
            case HeatPumpOperatingState.COOLING:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.COOLING");
            case HeatPumpOperatingState.DEFROST:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.DEFROST");
            case HeatPumpOperatingState.BLOCKED:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.BLOCKED");
            case HeatPumpOperatingState.FAULT:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.FAULT");
            case HeatPumpOperatingState.UNDEFINED:
            default:
                return translate.instant("HEAT_PUMP.OPERATING_STATE.UNDEFINED");
        }
    };
};

/**
 * Gets the display color for an {@link HeatPumpOperatingState}, using the shared chart color palette.
 *
 * @param state The operating state
 */
export function getOperatingStateColor(state: HeatPumpOperatingState | null): string {
    switch (state) {
        case HeatPumpOperatingState.SPACE_HEATING:
            return ChartConstants.Colors.GREEN;
        case HeatPumpOperatingState.DOMESTIC_HOT_WATER:
            return ChartConstants.Colors.BLUE;
        case HeatPumpOperatingState.COOLING:
            return ChartConstants.Colors.TURQUOISE;
        case HeatPumpOperatingState.DEFROST:
            return ChartConstants.Colors.YELLOW;
        case HeatPumpOperatingState.BLOCKED:
            return ChartConstants.Colors.ORANGE;
        case HeatPumpOperatingState.FAULT:
            return ChartConstants.Colors.RED;
        case HeatPumpOperatingState.OFF:
        case HeatPumpOperatingState.UNDEFINED:
        default:
            return ChartConstants.Colors.GREY;
    }
}

/**
 * Converts the OPERATING_MODE channel value (Heat.Broetje.Neo) to a translated label.
 *
 * @param translate The TranslateService
 */
export const CONVERT_OPERATING_MODE_TO_LABEL = (translate: TranslateService) => {
    return (value: number | null): string => {
        switch (value) {
            case OperatingMode.OFF:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.OFF");
            case OperatingMode.AUTOMATIC:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.AUTOMATIC");
            case OperatingMode.COOLING:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.COOLING");
            case OperatingMode.SUMMER:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.SUMMER");
            case OperatingMode.CONTINUOUS:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.CONTINUOUS");
            case OperatingMode.SETBACK:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.SETBACK");
            case OperatingMode.HOLIDAY:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.HOLIDAY");
            case OperatingMode.PARTY:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.PARTY");
            case OperatingMode.SCREED_DRYING:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.SCREED_DRYING");
            case OperatingMode.GRID_OPERATOR_LOCK:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.GRID_OPERATOR_LOCK");
            case OperatingMode.MAIN_SWITCH_OFF:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.MAIN_SWITCH_OFF");
            case OperatingMode.UNDEFINED:
            default:
                return translate.instant("HEAT_PUMP.OPERATING_MODE_VALUES.UNDEFINED");
        }
    };
};

/**
 * Converts the DEMAND channel value (Heat.Broetje.Neo) to a translated label.
 *
 * @param translate The TranslateService
 */
export const CONVERT_DEMAND_TO_LABEL = (translate: TranslateService) => {
    return (value: number | null): string => {
        switch (value) {
            case Demand.NONE:
                return translate.instant("HEAT_PUMP.DEMAND_VALUES.NONE");
            case Demand.COOLING:
                return translate.instant("HEAT_PUMP.DEMAND_VALUES.COOLING");
            case Demand.SPACE_HEATING:
                return translate.instant("HEAT_PUMP.DEMAND_VALUES.SPACE_HEATING");
            case Demand.DOMESTIC_HOT_WATER:
                return translate.instant("HEAT_PUMP.DEMAND_VALUES.DOMESTIC_HOT_WATER");
            case Demand.UNDEFINED:
            default:
                return translate.instant("HEAT_PUMP.DEMAND_VALUES.UNDEFINED");
        }
    };
};

/**
 * Converts the DEVICE_FAULT channel value ({@link FaultLevel}) to a translated label.
 *
 * @param translate The TranslateService
 */
export const CONVERT_FAULT_LEVEL_TO_LABEL = (translate: TranslateService) => {
    return (value: number | null): string => {
        switch (value) {
            case FaultLevel.OK:
                return translate.instant("HEAT_PUMP.FAULT_LEVEL.OK");
            case FaultLevel.INFO:
                return translate.instant("HEAT_PUMP.FAULT_LEVEL.INFO");
            case FaultLevel.WARNING:
                return translate.instant("HEAT_PUMP.FAULT_LEVEL.WARNING");
            case FaultLevel.FAULT:
                return translate.instant("HEAT_PUMP.FAULT_LEVEL.FAULT");
            default:
                return Converter.HIDE_VALUE(value);
        }
    };
};

/** Converts a BOOLEAN channel value to a translated On/Off label. */
export const CONVERT_BOOLEAN_TO_ON_OFF = (translate: TranslateService) => {
    return (value: boolean | number | null): string => {
        return translate.instant(value === true || value === 1 ? "GENERAL.ON" : "GENERAL.OFF");
    };
};

/** Converts a THOUSANDTH channel value (e.g. COMPRESSOR_SPEED) to percent. */
export const CONVERT_THOUSANDTH_TO_PERCENT: Converter = (value): string => {
    return Converter.IF_NUMBER(value, (v) => `${v / 10} %`);
};

/** Formats the COEFFICIENT_OF_PERFORMANCE channel value (already scaled by the driver). */
export const CONVERT_COP: Converter = (value): string => {
    return Converter.IF_NUMBER(value, (v) => v.toFixed(1));
};

/**
 * Gets the ChannelAddresses shown on the flat live widget.
 *
 * @param component The heat pump component
 */
export function getFlatChannelAddresses(component: EdgeConfig.Component): ChannelAddress[] {
    const channels: ChannelAddress[] = [
        new ChannelAddress(component.id, "OperatingState"),
        new ChannelAddress(component.id, "ThermalPower"),
        new ChannelAddress(component.id, "ActivePower"),
        new ChannelAddress(component.id, "Temperature"),
    ];

    if (isHeatBroetjeNeo(component)) {
        channels.push(
            new ChannelAddress(component.id, "CoefficientOfPerformance"),
            new ChannelAddress(component.id, "OutdoorTemperature"),
            new ChannelAddress(component.id, "FlowTemperature"),
            new ChannelAddress(component.id, "ReturnTemperature"),
            new ChannelAddress(component.id, "DomesticHotWaterTemperature"),
            new ChannelAddress(component.id, "OperatingMode"),
        );
    }

    return channels;
}

/**
 * Gets the ChannelAddresses shown in the modal. Optional Kür channels of Heat.Broetje.Neo are only included if the
 * corresponding config switch is enabled.
 *
 * @param component The heat pump component
 */
export function getModalChannelAddresses(component: EdgeConfig.Component): ChannelAddress[] {
    const channels: ChannelAddress[] = [
        new ChannelAddress(component.id, "OperatingState"),
        new ChannelAddress(component.id, "ThermalPower"),
        new ChannelAddress(component.id, "ActivePower"),
        new ChannelAddress(component.id, "ActiveConsumptionEnergy"),
        new ChannelAddress(component.id, "Temperature"),
        new ChannelAddress(component.id, "TargetTemperature"),
        new ChannelAddress(component.id, "RemainingHeatEnergy"),
    ];

    if (!isHeatBroetjeNeo(component)) {
        return channels;
    }

    channels.push(
        new ChannelAddress(component.id, "CoefficientOfPerformance"),
        new ChannelAddress(component.id, "OperatingMode"),
        new ChannelAddress(component.id, "Demand"),
        new ChannelAddress(component.id, "CompressorRunning"),
        new ChannelAddress(component.id, "CompressorSpeed"),
        new ChannelAddress(component.id, "GridOperatorRelease"),
        new ChannelAddress(component.id, "DeviceFault"),
        new ChannelAddress(component.id, "DefrostValveActive"),
        new ChannelAddress(component.id, "ThermalEnergyHeating"),
        new ChannelAddress(component.id, "ElectricEnergyHeating"),
        new ChannelAddress(component.id, "ThermalEnergyDhws"),
        new ChannelAddress(component.id, "ElectricEnergyDhws"),
        new ChannelAddress(component.id, "ThermalEnergyTotal"),
    );

    const properties = component.properties ?? {};
    if (properties.readPressures === true) {
        channels.push(new ChannelAddress(component.id, "Pressure1"), new ChannelAddress(component.id, "Pressure2"));
    }
    if (properties.readSourceTemperatures === true) {
        channels.push(
            new ChannelAddress(component.id, "SourceFlowTemperature"),
            new ChannelAddress(component.id, "SourceReturnTemperature"),
        );
    }
    if (properties.readRefrigerantTemperatures === true) {
        channels.push(
            new ChannelAddress(component.id, "SuctionGasTemperature"),
            new ChannelAddress(component.id, "EvaporationTemperature"),
            new ChannelAddress(component.id, "CondensationTemperature"),
            new ChannelAddress(component.id, "HotGasTemperature"),
        );
    }
    if (properties.readPumpFlags === true) {
        channels.push(new ChannelAddress(component.id, "Pump1Active"), new ChannelAddress(component.id, "Pump2Active"));
    }
    if (properties.readFlowRate === true) {
        channels.push(new ChannelAddress(component.id, "VolumeFlow"));
    }
    if (properties.readOperatingHours === true) {
        channels.push(
            new ChannelAddress(component.id, "OperatingHours1"),
            new ChannelAddress(component.id, "OperatingHours2"),
        );
    }

    return channels;
}
