import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { getOperatingStateColor, HeatPumpOperatingState } from "../shared/shared";
import { ControllerHeatPumpComponent } from "./flat";

/**
 * Minimal unit tests for {@link ControllerHeatPumpComponent}. The component extends {@link AbstractFlatWidget} with a
 * heavy DI graph, so the pure logic is exercised via the prototype with a stub `this` context, like in the
 * Controller/Heat flat spec.
 */
describe("ControllerHeatPumpComponent", () => {
    function callGetChannelAddresses(component: EdgeConfig.Component | null): ChannelAddress[] {
        const stub = { component } as ControllerHeatPumpComponent;
        return (ControllerHeatPumpComponent.prototype as any).getChannelAddresses.call(stub);
    }

    function callOnCurrentData(
        component: EdgeConfig.Component,
        currentData: CurrentData,
    ): { state: HeatPumpOperatingState | null; color: string } {
        const stub: any = Object.create(ControllerHeatPumpComponent.prototype);
        stub.component = component;
        stub.operatingState = null;
        stub.stateColor = "";
        (ControllerHeatPumpComponent.prototype as any).onCurrentData.call(stub, currentData);
        return { state: stub.operatingState, color: stub.stateColor };
    }

    describe("#getChannelAddresses()", () => {
        it("returns an empty list when component is null", () => {
            expect(callGetChannelAddresses(null)).toEqual([]);
        });

        it("returns the generic channels for a non-Broetje ManagedHeatPump", () => {
            const component = new EdgeConfig.Component("heatPump0", "HeatPump", true, false, "Heat.Generic", {});
            expect(callGetChannelAddresses(component)).toEqual([
                new ChannelAddress("heatPump0", "OperatingState"),
                new ChannelAddress("heatPump0", "ThermalPower"),
                new ChannelAddress("heatPump0", "ActivePower"),
                new ChannelAddress("heatPump0", "Temperature"),
            ]);
        });

        it("adds the Broetje channels for Heat.Broetje.Neo", () => {
            const component = DummyConfig.from(DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0")).components[
                "heatPump0"
            ];
            const channels = callGetChannelAddresses(component).map((c) => c.toString());
            expect(channels).toContain("heatPump0/CoefficientOfPerformance");
            expect(channels).toContain("heatPump0/OperatingMode");
            expect(channels.length).toBe(10);
        });
    });

    describe("#onCurrentData()", () => {
        const component = DummyConfig.from(DummyConfig.Component.HEAT_BROETJE_NEO("heatPump0")).components["heatPump0"];

        function dataWith(values: CurrentData["allComponents"]): CurrentData {
            return { allComponents: values } as CurrentData;
        }

        it("stores the operating state and its color", () => {
            const { state, color } = callOnCurrentData(
                component,
                dataWith({ "heatPump0/OperatingState": HeatPumpOperatingState.SPACE_HEATING }),
            );
            expect(state).toBe(HeatPumpOperatingState.SPACE_HEATING);
            expect(color).toBe(ChartConstants.Colors.GREEN);
        });

        it("falls back to the undefined color when the channel is missing", () => {
            const { state, color } = callOnCurrentData(component, dataWith({}));
            expect(state).toBeNull();
            expect(color).toBe(getOperatingStateColor(null));
        });

        it("does nothing when component is null", () => {
            const stub: any = Object.create(ControllerHeatPumpComponent.prototype);
            stub.component = null;
            (ControllerHeatPumpComponent.prototype as any).onCurrentData.call(stub, dataWith({}));
            expect(stub.operatingState).toBeUndefined();
        });
    });
});
