import { ChangeDetectionStrategy, Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

@Component({
    selector: "controller-heatpump-history-widget",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerHeatPumpHistoryFlatComponent extends AbstractFlatWidget {}
