import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ControllerHeatPumpMonitorSingleChartComponent } from "../chart/singlechart.component";

@Component({
    selector: "oe-controller-heat-pump-monitor-overview",
    templateUrl: "./new-navigation.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
        ControllerHeatPumpMonitorSingleChartComponent,
    ],
})
export class ControllerHeatPumpMonitorHistoryComponent extends AbstractHistoryChartOverview {}
