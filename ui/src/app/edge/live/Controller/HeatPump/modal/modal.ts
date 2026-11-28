import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, Utils } from "src/app/shared/shared";
import { CONVERT_BOOLEAN_TO_ON_OFF, CONVERT_COP, CONVERT_DEMAND_TO_LABEL, CONVERT_FAULT_LEVEL_TO_LABEL, CONVERT_OPERATING_MODE_TO_LABEL, CONVERT_OPERATING_STATE_TO_LABEL, CONVERT_THOUSANDTH_TO_PERCENT, FACTORY_ID_HEAT_BROETJE_NEO, getModalChannelAddresses, isHeatBroetjeNeo, } from "../shared/shared";

@Component({
    selector: "oe-controller-heatpump-modal",
    templateUrl: "./modal.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerHeatPumpModalComponent extends AbstractModal {
    protected readonly FACTORY_ID_HEAT_BROETJE_NEO = FACTORY_ID_HEAT_BROETJE_NEO;
    protected readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
    protected readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;
    protected readonly CONVERT_TO_WATTHOURS = Utils.CONVERT_TO_WATTHOURS;
    protected readonly CONVERT_OPERATING_STATE_TO_LABEL = CONVERT_OPERATING_STATE_TO_LABEL(this.translate);
    protected readonly CONVERT_OPERATING_MODE_TO_LABEL = CONVERT_OPERATING_MODE_TO_LABEL(this.translate);
    protected readonly CONVERT_DEMAND_TO_LABEL = CONVERT_DEMAND_TO_LABEL(this.translate);
    protected readonly CONVERT_FAULT_LEVEL_TO_LABEL = CONVERT_FAULT_LEVEL_TO_LABEL(this.translate);
    protected readonly CONVERT_BOOLEAN_TO_ON_OFF = CONVERT_BOOLEAN_TO_ON_OFF(this.translate);
    protected readonly CONVERT_THOUSANDTH_TO_PERCENT = CONVERT_THOUSANDTH_TO_PERCENT;
    protected readonly CONVERT_COP = CONVERT_COP;

    protected isBroetjeNeo(): boolean {
        return isHeatBroetjeNeo(this.component);
    }

    /** Whether the given optional Kür config switch of Heat.Broetje.Neo is enabled. */
    protected isKuerEnabled(property: string): boolean {
        return this.component?.properties?.[property] === true;
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this.component == null) {
            return [];
        }
        return getModalChannelAddresses(this.component);
    }
}
