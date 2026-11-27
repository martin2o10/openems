# SPEZIFIKATION: UI für Brötje BLW Neo / ManagedHeatPump

Stand: 28.11.2026 (Martin Wagner, Projekt OpenEMS)
Bezug: SPEZIFIKATION-broetje-blw-neo.md (Treiber), Branch feature/broetje-blw-neo

## 1. Ziel und Grundsatz

Die UI folgt der Treiber-Architektur: Der Treiber liefert Kanäle, Controller treffen Entscheidungen. Die UI zeigt Kanäle an und löst Modals auf; im Phase-2-Modus zeigt sie die letzte Controller-Vorgabe (ACTION/ACTION_POWER), ohne eigene Steuerlogik. Wiederverwendung vor Neubau: alle fertigen Bausteine aus dem bestehenden UI nutzen.

## 2. Fertige Bausteine im bestehenden UI

- **oe-flat-widget / oe-flat-widget-line** (shared): Live-Karten mit ChannelAddress-Bindung (`component.id + '/ChannelId'`), Konvertern (CONVERT_WATT_TO_KILOWATT, DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS), Modal-Popup und Icons. Der universelle Baustein für die WP-Karte.
- **Modulmuster live/Controller/Heat** (upstream develop, für ManagedHeatElement): vollständiges Angular-Modul mit flat, modal, history (chart/overview/flat), schedule (JSCalendar-Tasks mit add/edit), settings, new-navigation-Home, i18n in 9 Sprachen, EnergyScheduler-Anbindung mit Legacy-Fallback und factoryId-Verzweigungen (Heat.Askoma, Heat.MyPv.AcThor9s). Das ist das Haupt-Vorbild.
- **Modulmuster live/Controller/Io/Heatpump** (SG-Ready, im lokalen Baum; auf aktuellem upstream develop nicht gefunden, Provenienz vor Umsetzung klären): basemode, flat, history, modal, schedule, settings.
- **History-Framework**: abstracthistorychart/abstracthistorywidget plus Kategorien-Module (chart/overview/flat) wie edge/history/Controller/Ess.
- **Widget-Registry** (shared/type/widget.ts) für die Index-Seite und **edge/live/energymonitor** als Übersichtsmuster.
- **Monitor-Widget vom Eisstrang** (feature/heatpump-monitor): gebaute und echtgetestete Karte für die WP-Leistung (basierte auf dem Shelly), später auf Treiberkanäle umstellbar.

## 3. UI-Modul für die Wärmepumpe (Neu, Phase 1 read-only)

Neues Angular-Modul für ManagedHeatPump-Komponenten, nature-generisch aufgebaut, mit factoryId-Verzweigung für `Heat.Broetje.Neo`, damit später weitere WP-Treiber folgen können. Ablageort und Modulname nach ui/AGENTS.md-Konvention mit der bestehenden Struktur abstimmen (live-Views sind nach Komponententyp gruppiert).

**Live-Karte (flat):**

| Zeile | Kanal | Darstellung |
|---|---|---|
| Status | OPERATING_STATE | Icon + Farbe: OFF grau, SPACE_HEATING grün, DOMESTIC_HOT_WATER blau, COOLING cyan, DEFROST gelb, BLOCKED orange, FAULT rot |
| Thermische Leistung | THERMAL_POWER | kW-Konverter |
| Elektrische Leistung | ACTIVE_POWER | kW-Konverter |
| COP | COEFFICIENT_OF_PERFORMANCE | ×0,1 |
| Temperaturen | OUTDOOR_TEMPERATURE, FLOW_TEMPERATURE, RETURN_TEMPERATURE, DOMESTIC_HOT_WATER_TEMPERATURE, Puffer | °C-Konverter (Dezidegree) |
| Betriebsmodus | OPERATING_MODE (Reg 100) | Enum-Label |

**Modal (Detail):** Energie-Split (thermisch und elektrisch, je Heizen/Warmwasser/gesamt), Verdichterdrehzahl, EVU-Freigabe, DEVICE_FAULT, Kür-Kanäle (Drücke, Quellentemperaturen, Betriebsstunden) nur wenn aktiviert.

**Konventionen:** @if/@for-Template-Syntax (keine *ngIf), Prettier-clean, strictTemplates-strict, private Member, i18n-Keys in allen 9 Sprachen (cz, de, en, es, fr, ja, lt, nl, sv), keine OEM-Texte, spec-Dateien nach oe-ui-test-Skill.

## 4. History (Phase 1)

Kategorien-Modul nach dem Ess-Muster (chart/overview/flat) mit drei Tabs:

1. **Temperaturen**: Außen, Vorlauf, Rücklauf, Warmwasser, Puffer über Zeit
2. **Leistung & Energie**: elektrische und thermische Leistung, kumuliert Energie-Split Heizen/Warmwasser
3. **COP**: Verlauf (Tages-/Wochenaggregation)

Datenbasis Timedata/Influx; die Persistenz-Prioritäten (HIGH für Status, Leistungen, Temperaturen) kommen aus der Treiber-Spec.

## 5. Phase 2 (Steuerung)

- UI zeigt ACTION und ACTION_POWER (was der Controller zuletzt vorgegeben hat): Transparenz statt Steuerung.
- Optional ein Modal "manuelle Anforderung" (Debug) analog den Evcs-Modus-Buttons, nur für Testbetrieb, mit deutlicher Anzeige und Rückfall auf OwnControl beim Schließen.
- Fahrplan-UI (Muster HeatScheduleComponent mit JSCalendar-Tasks) erst wenn Controller.HeatPump.Single/Cluster Schedule-Quellen haben.

## 6. Umsetzungsreihenfolge

1. Live-Karte (flat) + i18n + Tests
2. Modal
3. History-Modul (3 Tabs)
4. Registrierung als Widget auf der Index-Seite
5. Phase-2-Elemente erst nach Feldvalidierung der Steuerung
6. Prüfen, ob das Monitor-Widget (Eisstrang) als zusätzliche Karte reaktiviert wird oder im neuen Modul aufgeht

## 7. Offene Punkte

1. Ablageort/Modulname: nature-generisch ("HeatPump") vs. treiberspezifisch, mit Upstream-Konvention und Maintainer abstimmen
2. Provenienz des SG-Ready-Heatpump-UI (lokal vorhanden, auf upstream develop nicht gefunden): klären, damit der UI-PR nicht auf entferntem Code aufbaut
3. Monitor-Widget: Reaktivierung als eigene Karte oder Integration ins neue Modul
4. Persistenz-Prioritäten der Kür-Kanäle für History/Influx festlegen
5. Farbschema/Icons für OPERATING_STATE mit bestehendem Theming (src/themes) abstimmen, nichts hart codieren
