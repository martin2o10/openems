import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ControllerHeatPumpModalComponent } from "./modal";

/** Minimal unit tests for {@link ControllerHeatPumpModalComponent}, exercising the pure logic via the prototype. */
describe("ControllerHeatPumpModalComponent", () => {
    function callGetChannelAddresses(component: EdgeConfig.Component | null): string[] {
        const stub = { component } as ControllerHeatPumpModalComponent;
        return (ControllerHeatPumpModalComponent.prototype as any).getChannelAddresses
            .call(stub)
            .map((c: ChannelAddress) => c.toString());
    }

    function callIsKuerEnabled(component: EdgeConfig.Component | null, property: string): boolean {
        const stub = { component } as ControllerHeatPumpModalComponent;
        return (ControllerHeatPumpModalComponent.prototype as any).isKuerEnabled.call(stub, property);
    }

    it("returns an empty list when component is null", () => {
        expect(callGetChannelAddresses(null)).toEqual([]);
    });

    it("subscribes the generic channels for any ManagedHeatPump", () => {
        const component = new EdgeConfig.Component("heatPump0", "HeatPump", true, false, "Heat.Generic", {});
        const channels = callGetChannelAddresses(component);
        expect(channels).toContain("heatPump0/OperatingState");
        expect(channels).toContain("heatPump0/ActiveConsumptionEnergy");
        expect(channels).toContain("heatPump0/TargetTemperature");
        expect(channels).not.toContain("heatPump0/DeviceFault");
    });

    it("subscribes the Broetje detail channels for Heat.Broetje.Neo", () => {
        const component = DummyConfig.from(DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0")).components["heatPump0"];
        const channels = callGetChannelAddresses(component);
        expect(channels).toContain("heatPump0/DeviceFault");
        expect(channels).toContain("heatPump0/GridOperatorRelease");
        expect(channels).toContain("heatPump0/ThermalEnergyHeating");
        expect(channels).toContain("heatPump0/ElectricEnergyDhws");
    });

    it("includes Kür channels only for enabled config switches", () => {
        const component = DummyConfig.from(
            DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0", "Wärmepumpe", {
                readPressures: true,
                readSourceTemperatures: true,
            }),
        ).components["heatPump0"];
        const channels = callGetChannelAddresses(component);
        expect(channels).toContain("heatPump0/Pressure1");
        expect(channels).toContain("heatPump0/SourceFlowTemperature");
        expect(channels).not.toContain("heatPump0/OperatingHours1");
    });

    describe("#isKuerEnabled()", () => {
        const component = DummyConfig.from(
            DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0", "Wärmepumpe", { readOperatingHours: true }),
        ).components["heatPump0"];

        it("is true only for explicitly enabled switches", () => {
            expect(callIsKuerEnabled(component, "readOperatingHours")).toBeTrue();
            expect(callIsKuerEnabled(component, "readPressures")).toBeFalse();
        });

        it("is false when component is null", () => {
            expect(callIsKuerEnabled(null, "readPressures")).toBeFalse();
        });
    });
});
