import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ControllerHeatPumpModalComponent } from "../modal/modal";
import { CONVERT_COP, CONVERT_OPERATING_MODE_TO_LABEL, CONVERT_OPERATING_STATE_TO_LABEL, FACTORY_ID_HEAT_BROETJE_NEO, getFlatChannelAddresses, getOperatingStateColor, HeatPumpOperatingState, } from "../shared/shared";

@Component({
    selector: "oe-controller-heatpump",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerHeatPumpComponent extends AbstractFlatWidget {
    protected modalComponent: Modal | null = null;
    protected operatingState: HeatPumpOperatingState | null = null;
    protected stateColor: string = getOperatingStateColor(null);

    protected readonly FACTORY_ID_HEAT_BROETJE_NEO = FACTORY_ID_HEAT_BROETJE_NEO;
    protected readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
    protected readonly CONVERT_OPERATING_STATE_TO_LABEL = CONVERT_OPERATING_STATE_TO_LABEL(this.translate);
    protected readonly CONVERT_OPERATING_MODE_TO_LABEL = CONVERT_OPERATING_MODE_TO_LABEL(this.translate);
    protected readonly CONVERT_COP = CONVERT_COP;

    protected override afterIsInitialized(): void {
        this.modalComponent = {
            component: ControllerHeatPumpModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        };
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        if (this.component == null) {
            return [];
        }
        return getFlatChannelAddresses(this.component);
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            return;
        }
        const state = currentData.allComponents[this.component.id + "/OperatingState"];
        this.operatingState = state == null ? null : (state as HeatPumpOperatingState);
        this.stateColor = getOperatingStateColor(this.operatingState);
    }
}
