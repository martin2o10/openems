import { TranslateService } from "@ngx-translate/core";
import { TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ControllerHeatPumpModule } from "./HeatPump";
import heatPumpDe from "./i18n/de.json";
import heatPumpEn from "./i18n/en.json";

describe("ControllerHeatPumpModule", () => {
    let translate: TranslateService;

    beforeEach(async () => {
        const testContext = await TestingUtils.sharedSetup();
        translate = testContext.translate;
    });

    it("should define the module", () => {
        expect(ControllerHeatPumpModule).toBeDefined();
    });

    it("should support translation initialization", async () => {
        await expect(() => {
            translate.setTranslation("de", heatPumpDe, true);
            translate.setTranslation("en", heatPumpEn, true);
        }).not.toThrow();
    });

    it("should translate the operating states", () => {
        translate.setTranslation("en", heatPumpEn, true);
        translate.use("en");
        expect(translate.instant("HEAT_PUMP.OPERATING_STATE.SPACE_HEATING")).toBe("Space heating");
        expect(translate.instant("HEAT_PUMP.HISTORY.POWER_AND_ENERGY")).toBe("Power & energy");
    });
});
