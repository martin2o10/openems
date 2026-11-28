import { CommonModule } from "@angular/common";
import { NgModule, inject, provideEnvironmentInitializer } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { ControllerHeatPumpModule } from "src/app/edge/live/Controller/HeatPump/HeatPump";
import cz from "src/app/edge/live/Controller/HeatPump/i18n/cz.json";
import de from "src/app/edge/live/Controller/HeatPump/i18n/de.json";
import en from "src/app/edge/live/Controller/HeatPump/i18n/en.json";
import es from "src/app/edge/live/Controller/HeatPump/i18n/es.json";
import fr from "src/app/edge/live/Controller/HeatPump/i18n/fr.json";
import ja from "src/app/edge/live/Controller/HeatPump/i18n/ja.json";
import lt from "src/app/edge/live/Controller/HeatPump/i18n/lt.json";
import nl from "src/app/edge/live/Controller/HeatPump/i18n/nl.json";
import sv from "src/app/edge/live/Controller/HeatPump/i18n/sv.json";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { CopChartComponent } from "./chart/cop-chart";
import { PowerEnergyChartComponent } from "./chart/power-energy-chart";
import { TemperaturesChartComponent } from "./chart/temperatures-chart";
import { ControllerHeatPumpHistoryFlatComponent } from "./flat/flat";
import { ControllerHeatPumpOverviewComponent } from "./overview/overview";

function initializeHeatPumpHistoryTranslations(translate: TranslateService): void {
    void Language.normalizeAdditionalTranslationFiles({
        cs: cz,
        de: de,
        en: en,
        es: es,
        fr: fr,
        ja: ja,
        lt: lt,
        nl: nl,
        sv: sv,
    }).then((translations) => {
        for (const { lang, translation, shouldMerge } of translations) {
            translate.setTranslation(lang, translation, shouldMerge);
        }
    });
}

@NgModule({
    imports: [BrowserModule, CommonModule, IonicModule, SharedModule, TranslateModule, ControllerHeatPumpModule],
    declarations: [
        TemperaturesChartComponent,
        PowerEnergyChartComponent,
        CopChartComponent,
        ControllerHeatPumpOverviewComponent,
        ControllerHeatPumpHistoryFlatComponent,
    ],
    providers: [provideEnvironmentInitializer(() => initializeHeatPumpHistoryTranslations(inject(TranslateService)))],
    exports: [
        TemperaturesChartComponent,
        PowerEnergyChartComponent,
        CopChartComponent,
        ControllerHeatPumpOverviewComponent,
        ControllerHeatPumpHistoryFlatComponent,
    ],
})
export class ControllerHeatPumpHistoryModule {}
