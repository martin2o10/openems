# SPEZIFIKATION: Brötje BLW Neo Wärmepumpen-Treiber (io.openems.edge.heat.broetje)

Stand: 28.11.2026 (Martin Wagner, Projekt OpenEMS, privat, später Upstream)
Branch: `feature/broetje-blw-neo` (Fork martin2o10/openems; Basis develop b68bde5 + pr/heatpump-nature 514766f + pr/heatpump-controller 95ed688)

## 1. Ziel und Rahmen

Monitoring und später Steuerung der Brötje BLW Neo 12 B Wärmepumpe über das verbaute NEO-RKM-Modul (Modbus TCP). Zielbild: echte elektrische Leistungsvorgabe von außen (Anforderungsprofil 3), nicht nur SG-Ready. Erst privat im eigenen OpenEMS testen, dann als OpenEMS-Upstream-PR einreichen.

Nicht-Ziele: FEMS bleibt unangetastet (die WP wird vom FEMS nicht per Modbus gehalten, kein Client-Konflikt). Die AC THORs bleiben vollständig FEMS-Sache. Das auf Eis gelegte Monitor-UI (feature/heatpump-monitor) kann später als Statistikschicht über den Treiberkanälen reaktiviert werden.

## 2. Hardware und Kommunikation

- Gerät: Brötje BLW Neo 12 B mit NEO-RKM (Modbus TCP), Netz 192.168.178.38 (Web-UI "RemoteControl"), Port 502, Unit-ID 1
- Schreibintervall mindestens 5 s (RKM-Vorgabe, wird im Treiber erzwungen)
- Temperaturen: INT16, Faktor 0,1; Sentinel Rohwert -500 (= -50,0 °C) bedeutet "Fühler nicht angeschlossen"
- Referenzmessung: Shelly Pro 3EM (shellyWp0, Meter-Type Consumption), bleibt als unabhängige Validierungsmessung erhalten (27.11.: 1652 W extern vs. 1610 W intern)
- Registerquellen: NEO-RKM Montageanleitung 7734937 Kap. 7.1 Tabellen 4+5; abgeglichen mit github.com/AndyKausG/ha-broetje-neo (registers.py) und altem Treiberstand 1917bc8

## 3. Phasenmodell

**Phase 1 (read-only, Standard):** Treiber liest alle Pflicht-Register und stellt die Kanäle im OpenEMS bereit. WP bleibt komplett in ihrer Eigenregelung. `readOnly = true` ist der Konfigurations-Default.

**Phase 2 (Steuerung, nur nach Feldvalidierung):** Schreibpfad über Register 125 (Leistungsvorgabe) plus EVU-Sperre über Register 149/150. Die Kanäle und Strukturen dafür werden schon in Phase 1 angelegt, Phase 2 ist reine Verkabelung ohne Umbau am Grundgerüst.

## 4. Architektur

- Bundle: `io.openems.edge.heat.broetje` (upstream-Konvention `heat.<hersteller>`), Package `.neo`, Factory "Heat Broetje BLW NEO", FACTORY_ID `Heat.Broetje.Neo`, Component-ID `heatPump0`
- Interfaces: `ManagedHeatPump` (Nature, ersetzt die v1 des alten Treibers), `Heat` (Temperaturen), `ModbusComponent`, `TimedataProvider`, `OpenemsComponent`. `ElectricityMeter` ist über die Nature (extends ElectricityMeter) abgedeckt.
- Nature-Mechanik (aus pr/heatpump-nature): der Treiber deklariert `getHeatPumpAbilities()` und setzt Aktionen pro Cycle via `apply(HeatPumpAction)` um. Alle Entscheidungen treffen Controller, die WP-Eigenregelung (Frostschutz, Komfort) hat immer Vorrang.
- **Design-Entscheidung ManagedHeatElement: Empfehlung NEIN (vorerst).** Begründung: Die Nature löst die Rolle von `ManagedHeatElement`/`TARGET_ACTIVE_POWER` für Wärmepumpen ab (`apply(HeatPumpAction.Power)` mit `ACTION_POWER`-Kanal ist das passende Muster). Ein zusätzlicher `ManagedHeatElement`-Setpoint-Pfad würde zwei konkurrierende Schreibquellen auf Register 125 bedeuten. Alternative, im Upstream-Review zu diskutieren: doch zusätzlich implementieren, damit die bestehende EnergyScheduler-Statemachine (SurplusHandler wie bei heat.askoma/heat.mypv) direkt greift. Dann aber klar getrennt: `TARGET_ACTIVE_POWER` nur lesender Mirror, Quelle der Wahrheit bleibt `apply()`. Entscheidung bleibt offen bis zur Abstimmung mit dem Upstream-Maintainer im Nature-PR.
- Portierungsquellen: alter Treiber aus feature/heatpump-broetje-neo (ChannelIds, OperatingMode, OperatingStateMapper, SetpointHandler, Config) als Referenz; Muster für Modbus-Aufbau und readOnly-Default: heat.askoma (upstream).

## 5. Kanal- und Register-Zuordnung

### 5.1 Nature-Kanäle (Pflicht)

| Kanal | Quelle | Mapping |
|---|---|---|
| OPERATING_STATE | Reg 25/26/27/32/38/39/41 | Fehler (26 > 0) → FAULT; Abtau (27 > 0) → DEFROST; Bedarf 41 = 30 → DOMESTIC_HOT_WATER, 20 → SPACE_HEATING, 10 → COOLING; läuft (25 > 0) ohne Bedarfsdetail → SPACE_HEATING; EVU-Sperrzustand (32 ohne Freigabe) → BLOCKED; sonst OFF |
| THERMAL_POWER | Reg 74 | ×0,1 kW → W |
| ACTION / ACTION_POWER | intern | gesetzt durch apply(HeatPumpAction) |
| ACTIVE_POWER (ElectricityMeter) | Reg 70 | UINT32 [W] |
| ACTIVE_CONSUMPTION_ENERGY (ElectricityMeter) | Reg 68 | UINT32; Einheit (Wh vs. kWh) am Gerät klären, siehe Abschnitt 10 |
| Phasen (ElectricityMeter) | berechnet | wie heat.askoma: calculatePhasesFromActivePower |

### 5.2 Heat- und Treiberkanäle (Pflicht, aus altem Treiber)

| Kanal | Register | Typ/Faktor |
|---|---|---|
| OUTDOOR_TEMPERATURE | 10 | INT16 ×0,1 °C |
| DOMESTIC_HOT_WATER_TEMPERATURE | 11 | INT16 ×0,1 °C |
| FLOW_TEMPERATURE | 12 | INT16 ×0,1 °C |
| RETURN_TEMPERATURE | 13 | INT16 ×0,1 °C |
| Heat TEMPERATURE (Puffer/Speicher) | 14 | INT16 ×0,1 °C |
| COMPRESSOR_RUNNING | 25 | Flag > 0 |
| DEVICE_FAULT | 26 | Flag > 0, Level FAULT |
| DEFROST_VALVE_ACTIVE | 27 | Flag > 0 (4-Wege-Ventil/Abtau) |
| COMPRESSOR_SPEED | 29 | ‰ ×0,1 = % |
| COEFFICIENT_OF_PERFORMANCE | 30 | ×0,1 |
| GRID_OPERATOR_RELEASE | 32 | Flag > 0 |
| Soll-Temperatur (Rücklauf/Speicher) | 34 | INT16 ×0,1 °C |
| DEMAND | 41 | Enum 0/10/20/30 |
| OPERATING_MODE | FC3, Reg 100 | Enum 0-10 (8-10 nur lesend) |

### 5.3 Energie (Pflicht, neu gegenüber altem Treiber)

| Kanal | Register | Typ |
|---|---|---|
| THERMAL_ENERGY_HEATING / ELECTRIC_ENERGY_HEATING | 60 / 62 | UINT32 |
| THERMAL_ENERGY_DHWS / ELECTRIC_ENERGY_DHWS | 64 / 66 | UINT32 |
| THERMAL_ENERGY_TOTAL | 72 | UINT32 |
| (ACTIVE_CONSUMPTION_ENERGY) | 68 | UINT32, siehe 5.1 |

### 5.4 Kür-Register (Struktur vorbereiten, einzeln nachrüstbar)

Drücke (21/22), Quellentemperaturen (15/16), Sauggas/Verdampfung/Kondensation/Heißgas (17-20), Pumpen-Flags (23/24), Volumenstrom (28), Frischwasser (31), Außentemp. verzögert (33), MKR-Sollwerte (35/36), Expansionsventil (40), Betriebsstunden (42/44), Holding 101-106, externe Sensor-Overrides (129-134), 2. Stufe (147/148), Drehzahl-Soll (126), ext. Anforderung (117/127), Fehler-Reset (128). Jede Nachrüstung ist ein lokaler Kanal-Zusatz ohne Berührung der Nature-Logik.

### 5.5 Schreibkanäle (ab Tag 1 angelegt, in Phase 1 ohne Modbus-Task dahinter)

| Kanal | Register | Modus |
|---|---|---|
| POWER_SETPOINT | 125 | WRITE_ONLY, UINT16 [W], Anforderungsprofil 3 |
| GRID_OPERATOR_RELEASE_VALUE | 149 | WRITE_ONLY, 0 = WP gesperrt |
| GRID_OPERATOR_RELEASE_ACTIVATE | 150 | WRITE_ONLY |

## 6. Schreibpfad (Phase 2)

Mapping der Nature-Aktionen auf Register (Portierung des bestehenden SetpointHandler aus dem alten Treiber):

| HeatPumpAction | Register-Schreibvorgang |
|---|---|
| OwnControl | Rückfall in Eigenregelung: externen Request deaktivieren (Reg 150 = 0) |
| Lock | EVU-Sperre: Reg 149 = 0, Reg 150 = 1 |
| Power(p, force) | Reg 125 = clamp(p, minPower..maxPower), begrenzt durch aktive Sperre; p < minPower → 0 schreiben. force hat beim Brötje keine direkte Entsprechung: offen, ob force ignoriert oder über Reg 127 (ext. Anforderung) abgebildet wird |

Abilities: `ApplyPower.Continuous(minPower, maxPower)` (Config 800/3500 W, für BLW 12 B verifizieren), `minRunTime`/`minOffTime` aus Config (alt 1200/600 s, verifizieren). Der SetpointHandler erzwingt das 5s-Minimum im Code, unabhängig von der Config.

**Offener Punkt Fallback:** Verhalten des RKM bei Kommunikationsabbruch klären (Probe): bleibt der letzte Register-125-Wert aktiv? Schutzoptionen: OwnControl als Default-Aktion im Deactivate einmalig schreiben, oder bewusst dokumentieren, dass der letzte Wert gilt und die WP-Eigenregelung den Bedarf ohnehin begrenzt.

## 7. Schutzkonzept

1. `readOnly = true` als Default, Schreibpfad nur aktiv nach bewusster Konfiguration.
2. Write-Intervall mindestens 5 s, im SetpointHandler erzwungen.
3. Mindestlauf- und Pausenzeiten als Abilities deklariert; Controller verpflichtet sich, sie zu respektieren. Zusätzlich prüfen, ob der Treiber einen lokalen Schutz ergänzt.
4. Rückfall in die WP-Eigenregelung bei: kein apply() im Cycle (Nature-Konzept), Treiber-Deactivate (OwnControl), Kommunikationsabbruch (siehe offener Punkt 6).
5. WP-interne Sicherheitssysteme (Frostschutz, Maximaltemperaturen, EVU-Logik des RKM) haben immer Vorrang; der Treiber schreibt nur Vorgaben, keine Verbote an die Eigenregelung.

## 8. Fehlerbehandlung

- Sentinel -500: Kanal nicht beschreiben (Fühler fehlt), nicht als -50 °C in die Kanäle schreiben
- Modbus-Aussetzer: betroffene Kanäle undefined, OPERATING_STATE → UNDEFINED, getHeatPumpAbilities() → null
- DEVICE_FAULT (Reg 26) als Level-FAULT-Kanal
- EVU-Freigabe (Reg 32) nur lesend abbilden, nie selbst verändern (außer über dokumentierte Lock-Aktion 149/150)

## 9. Konfiguration

id, alias, enabled, modbus_id (default modbus0), modbusUnitId (default 1), readOnly (default TRUE), minPower (800), maxPower (3500), minRunTimeSeconds (1200), minOffTimeSeconds (600), writeIntervalSeconds (10), plus je ein Schalter pro Kür-Registergruppe (z. B. readPressures, readSourceTemperatures, readOperatingHours), damit das Poll-Volumen konfigurierbar bleibt.

## 10. Validierungsfahrplan (am PC, in dieser Reihenfolge)

1. Branch auschecken: `git fetch && git checkout feature/broetje-blw-neo`
2. Portierung implementieren (diese Spec als Vorlage für den Code-Agenten)
3. Modbus-Probe: Bridge Modbus/TCP auf 192.168.178.38:502, Treiber readOnly, Edge-Log beobachten
4. Register verifizieren: Werte gegen RemoteControl-Web-UI und Shelly kreuzchecken (Anker: 1652 W extern vs. 1610 W intern vom 27.11.)
5. Einheiten klären: Energie-Register 42-72 (Wh vs. kWh), Leistung 70/74
6. Optional: RemoteControl-Web-UI F12-JSON-Auswertung als zweite Referenz
7. Erst danach: Phase 2 Feldtest mit Schutzleitungen aus Abschnitt 7

## 11. Upstream-Strategie

- PR-Reihenfolge: 1. Nature (pr/heatpump-nature, Discussion mit Maintainer, dort auch ManagedHeatElement-Frage klären), 2. Controller.HeatPump.Single/Cluster (pr/heatpump-controller), 3. Treiber (feature/broetje-blw-neo auf Basis der angenommenen PRs)
- Muster und Qualitätsmaßstab: heat.askoma/heat.mypv (readme.adoc, Unit-Tests, OSGi-Config, bnd-Build)
- Positionierung: kein öffentlicher OpenEMS-Wärmepumpen-Modbus-Treiber existiert (Stand 28.11., GitHub-Code-Suche); SESAM (Smart Energy Solutions) und clever-PV arbeiten bei WP nur mit SG-Ready bzw. Cloud. Der Treiber füllt die meistgefragte Lücke der Community (Foren-Threads seit 2020)
- Lizenz: OpenEMS steht unter EPL-2.0/AGPL-3.0-Dual-Lizenz, eigene Contribution unter denselben Bedingungen

## 12. Offene Punkte

1. Energie-Einheit der Register 42-72 (Wh vs. kWh): am Gerät klären
2. ManagedHeatElement: ja/nein, Entscheidung im Nature-PR-Review
3. force-Semantik bei Power(p, force): ignorieren oder über Reg 127 abbilden
4. RKM-Verhalten bei Kommunikationsabbruch: Probe (Abschnitt 6)
5. minRunTime/minOffTime des BLW 12 B: verifizieren (alt 1200/600 s)
6. Schreibintervall in der Praxis: 5 s Minimum vs. 10 s Default, Feldtest
7. Lese-Umfang: Pflicht/Kür-Aufteilung nach dieser Spec bestätigen oder anpassen
