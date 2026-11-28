import { CommonModule } from "@angular/common";
import { NgModule, inject, provideEnvironmentInitializer } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { Language } from "src/app/shared/type/language";
import { ControllerHeatPumpComponent } from "./flat/flat";
import cz from "./i18n/cz.json";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import es from "./i18n/es.json";
import fr from "./i18n/fr.json";
import ja from "./i18n/ja.json";
import lt from "./i18n/lt.json";
import nl from "./i18n/nl.json";
import sv from "./i18n/sv.json";
import { ControllerHeatPumpModalComponent } from "./modal/modal";

function initializeHeatPumpTranslations(translate: TranslateService): void {
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
    imports: [BrowserModule, SharedModule, CommonModule, IonicModule, TranslateModule],
    declarations: [ControllerHeatPumpComponent, ControllerHeatPumpModalComponent],
    providers: [provideEnvironmentInitializer(() => initializeHeatPumpTranslations(inject(TranslateService)))],
    exports: [ControllerHeatPumpComponent, ControllerHeatPumpModalComponent],
})
export class ControllerHeatPumpModule {}
