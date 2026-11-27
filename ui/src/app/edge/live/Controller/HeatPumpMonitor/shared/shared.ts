import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerHeatPumpMonitor {
    /**
     * Converts a duration value in [s] to a string in [h] with one decimal.
     */
    export const secondsToHours = (value: number | null): string => {
        if (value == null) {
            return "-";
        }
        return (Math.round((value / 3600) * 10) / 10).toFixed(1);
    };

    /**
     * Converts the 'HeatPumpRunning' channel value to a translated ON/OFF state text (GENERAL.ON/OFF).
     */
    export const runningStateText = (translate: TranslateService) => {
        return (value: number | boolean | null): string => {
            if (value == null) {
                return "-";
            }
            return value === true || value === 1
                ? translate.instant("GENERAL.ON")
                : translate.instant("GENERAL.OFF");
        };
    };

    /**
     * Converts a temperature value to a string with one decimal.
     */
    export const temperatureOneDecimal = (value: number | null): string => {
        if (value == null) {
            return "-";
        }
        return (Math.round(value * 10) / 10).toString();
    };

    /**
     * Checks whether a configuration property is set on the Component (not null and not empty).
     */
    export const isConfigured = (component: EdgeConfig.Component, propertyName: string): boolean => {
        const value = component.properties[propertyName];
        return value != null && value !== "";
    };

    /**
     * Normalizes a configuration property to a list of channel addresses.
     * Handles arrays as well as comma-separated strings.
     */
    const parseChannelAddresses = (raw: unknown): string[] => {
        if (raw == null) {
            return [];
        }
        if (Array.isArray(raw)) {
            return raw.filter((entry): entry is string => typeof entry === "string" && entry !== "");
        }
        if (typeof raw === "string") {
            return raw
                .split(",")
                .map((entry) => entry.trim())
                .filter((entry) => entry !== "");
        }
        return [];
    };

    /**
     * Gets the configured heating element power channel addresses of the Component.
     */
    export const getHeatingElementChannelAddresses = (component: EdgeConfig.Component): string[] => {
        return parseChannelAddresses(component.properties["heatingElementPowerChannelAddresses"]);
    };

    /**
     * Builds one entry (label + channel address) per configured heating element.
     * The label is the alias of the source component, falling back to its component id.
     */
    export const getHeatingElements = (
        config: EdgeConfig | null,
        component: EdgeConfig.Component,
    ): Array<{ label: string; channelAddress: string }> => {
        return getHeatingElementChannelAddresses(component).map((channelAddress) => {
            const componentId = channelAddress.slice(0, channelAddress.indexOf("/"));
            const sourceComponent = config?.components[componentId];
            return {
                label: sourceComponent?.alias || componentId,
                channelAddress: channelAddress,
            };
        });
    };

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "controller/heatpump-monitor/" + component.id },
            { name: "oe-heatpump", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [NavigationConstants.CommonNodes.HISTORY(translate, component.id)],
            null,
        ).toConstructorParams();
    }
}
