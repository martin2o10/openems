import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerHeatPumpMonitor {
    /**
     * Converts a duration value in [s] to a string in [h] with one decimal.
     */
    export const secondsToHours = (value: number | null): string | null => {
        if (value == null) {
            return null;
        }
        return (Math.round((value / 3600) * 10) / 10).toFixed(1);
    };

    /**
     * Converts the 'HeatPumpRunning' channel value to a translated state text.
     */
    export const runningStateText = (translate: TranslateService) => {
        return (value: number | boolean | null): string | null => {
            if (value == null) {
                return null;
            }
            return value === true || value === 1
                ? translate.instant("HEAT_PUMP_MONITOR.RUNNING")
                : translate.instant("HEAT_PUMP_MONITOR.PAUSED");
        };
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
