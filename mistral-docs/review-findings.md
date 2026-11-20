---
title: Review-Findings (KI-gestuetzte Entwicklung)
file: mistral-docs/review-findings.md
branch: mistral-docs (Basis: feature/go-e)
created: "2026-11-20"
last_updated: "2026-11-20"
language: de
schema_version: 1
id_format: "REV-<NNN>, fortlaufend, niemals wiederverwenden"
severity: [HIGH, MEDIUM, LOW, NIT]
status: [OPEN, ACCEPTED, IN_PROGRESS, FIXED, VERIFIED, WONT_FIX, DEFERRED]
purpose: >
  Laufende, maschinenlesbare Findings-Sammlung der KI-Reviews (Vibe/Mistral).
  Internes Arbeitsdokument. Gehoert niemals in OpenEMS- oder FEMS-PRs.
---

# Review-Findings

## 1. Ziel-Objekt des aktuellen Reviews

| Feld | Wert |
|---|---|
| Repo | martin2o10/openems (Fork von OpenEMS/openems) |
| Bundle | io.openems.edge.evse.chargepoint.goe |
| Komponente | Evse.ChargePoint.GoE (go-e Gemini, Modbus/TCP) |
| Basis-Branch | feature/go-e |
| Gepruefter Commit | 5013e2ee3c3a1c292e8d9590a3a8e4c8650adec3 (Stand 2026-11-20) |

Gepruefte Dateien: EvseGoeImpl.java, EvseGoe.java, Config.java, CarState.java,
ErrorCode.java, translation_de.properties, translation_en.properties, bnd.bnd,
readme.adoc, test/EvseGoeImplTest.java, test/MyConfig.java,
EdgeApp.bndrun (Registrierung in -runrequires und -runbundles).

Quellen des Reviews: go-e Modbus-Doku
(https://github.com/goecharger/go-eCharger-API-v2/blob/main/modbus-de.md),
Alfen-Implementierung als Struktur-Blaupause
(https://github.com/OpenEMS/openems/blob/develop/io.openems.edge.evse.chargepoint.alfen),
OpenEMS AGENTS.md, Runde-1/Runde-2-Vorgaben aus FEMS-PR 3707 (Sebastian Asen),
Hardware-Messungen an go-e Gemini 11 kW, FW 059.4 (QModMaster, 22./23.10.2026).

## 2. Regeln für KI-Agenten (Umgang mit dieser Datei)

1. Neue Findings erhalten die naechste freie ID (maximale vorhandene Nummer + 1).
   IDs werden nie umbenannt, neu nummeriert oder wiederverwendet.
2. Pflichtfelder je Finding: id, severity, status, found, component, area,
   requirements, evidence, recommendation. resolution bleibt leer bis zur Bearbeitung.
3. Statuswechsel nur mit Begruendung und Datum im Feld resolution:
   OPEN -> ACCEPTED | IN_PROGRESS -> FIXED -> VERIFIED (nach Test/Review)
   oder WONT_FIX/DEFERRED (mit Begruendung).
4. Evidence muss belegt sein: Datei + Methode, Doku-Abschnitt oder Messung.
   Keine unbelegten Behauptungen. Vermutungen erhalten den Zusatz confidence: low.
5. Nach jedem Update dieser Datei: Abschnitt 8 (Changelog) ergaenzen (Datum + Was).
6. findings nur zu Komponenten des eigenen Entwicklungs-Zweigs; externe Bugs
   (z. B. prepare-commit.sh) als eigene Datei in mistral-docs/ fuehren.
7. Ordner mistral-docs/ niemals in OpenEMS-/FEMS-PRs mergen.

## 3. Anforderungen (Pruefkriterien dieses Bundles)

| ID | Anforderung (Kurzform) |
|---|---|
| REQ-01 | EVSE-Vertrag: Abilities/apply gemaess EVSE-API, kein EVCS; readOnly = leere Abilities + No-op-apply |
| REQ-02 | Register korrekt lt. modbus-de.md: Wire-Adressen, Datentypen, Skalierungen, Word-Order |
| REQ-03 | Write-Disziplin: FN16-only; nur AMPERE_VOLATILE (299); EEPROM (301) nie; Wertebereich 6..32 A |
| REQ-04 | Start/Stopp ueber ALLOW (200/201): Readback-Abgleich je Zyklus, Stopp sofort |
| REQ-05 | Fehlverhalten: Kommunikationsverlust erzeugt keinen Fehlalarm; Error-Mapping exakt wie Doku |
| REQ-06 | Phasenwechsel: psm=1|2 (Register 332/333); Ability nur bei lesbarem Register; SINGLE_PHASE-Wiring nie |
| REQ-07 | Framework-Konformanz: Standard-Konverter (SCALE_FACTOR_*), readElementOnce, keine deprecated APIs |
| REQ-08 | Abwaertskompatibilitaet: evcs.goe unberuehrt; EdgeApp.bndrun beidseitig registriert |
| REQ-09 | Doku: readme high-level, Kanal-Doku via Doc-Annotationen, Uebersetzungen de/en |
| REQ-10 | Tests: Anforderungen + Randfaelle; Tests verhalten sich wie das Produkt |
| REQ-11 | Hardware-Nachweis: gemessenes Verhalten am realen Geraet (FEMS-Eintrittskriterium), im readme |

## 4. Index

| ID | Severity | Status | Component | Titel |
|---|---|---|---|---|
| REV-001 | HIGH | OPEN | goe | HW-Verifikation lief ueber QModMaster, nicht ueber die OpenEMS-Komponente |
| REV-002 | HIGH | OPEN | goe | FW-Anforderung unvollstaendig: 060.3 Byte-Order-Bug nicht abgedeckt |
| REV-003 | MEDIUM | OPEN | goe | Sub-Minimum-Setpoints werden out-of-spec geschrieben |
| REV-004 | MEDIUM | OPEN | goe | Throttle 5/30 s undokumentiert und abweichend vom Design |
| REV-005 | LOW | OPEN | goe | readme: Geraetefamilie unvollstaendig |
| REV-006 | LOW | OPEN | goe | PHASES-Register (205) ungenutzt (nur Kanal, keine Logik) |
| REV-007 | NIT | OPEN | goe | Toter Null-Check carState in handleEvent |
| REV-008 | NIT | OPEN | goe | Inkonsistente Null-Wertefassung (orElse(0) vs value().get() == null) |
| REV-009 | LOW | OPEN | goe | 332-Readback Priority.LOW, andere Readbacks HIGH |
| REV-010 | NIT | OPEN | goe | Test greift per Reflection auf internes protocol-Feld zu |
| REV-011 | LOW | OPEN | goe | Kommunikationsverlust-Verhalten nicht im readme dokumentiert |

## 5. Findings im Detail

### REV-001
- id: REV-001
- severity: HIGH
- status: OPEN
- found: 2026-11-20
- component: goe
- area: readme.adoc, Abschnitt Verification status; gesamte Komponente
- requirements: REQ-11
- evidence: >
  Die readme schreibt Verified on a go-e Gemini 11 kW. Die Hardware-Messungen
  vom 22./23.10.2026 (CAR_STATE, FC16 auf 299, ALLOW 200, ERROR, FW 059.4)
  wurden aber mit QModMaster durchgefuehrt, nicht mit dieser OpenEMS-Komponente.
  Die Base-Addr-Frage (Doku-Wire-Adresse vs. +1 im Tool) ist nicht abschliessend
  geklaert; der Code nutzt die Doku-Wire-Adressen, was der OpenEMS-Konvention entspricht.
- recommendation: >
  Einen echten Testlauf der OpenEMS-Komponente gegen die Gemini machen
  (Kanaele pruefen, Setpoint setzen). Danach readme praezisieren:
  verified with this component, firmware 059.4, Adressen 0-basiertes Wire-Format.
- references: modbus-de.md; FEMS-Qualitaetsgate Did you actually test this with hardware
- resolution:

### REV-002
- id: REV-002
- severity: HIGH
- status: OPEN
- found: 2026-11-20
- component: goe
- area: readme.adoc, Abschnitt Requirements; EvseGoeImpl (Annahme LSWMSW, FWV als ASCII)
- requirements: REQ-02, REQ-09
- evidence: >
  modbus-de.md: In Firmware 060.3 wurde die Byte-Reihenfolge getauscht,
  mit 060.4 behoben. Der Code nimmt durchgehend LSWMSW an und liest
  FIRMWARE_VERSION als plain ASCII. Auf einer 060.3-Box wuerden uint32-Werte
  still falsch interpretiert. Die readme fordert nur firmware 057.1 or newer,
  schliesst 060.3 also nicht aus. Sebastian Runde-1-Punkt (SemanticVersion
  pruefen) ist nur halb umgesetzt (Kanal ist STRING, kein FW-Check im Code).
- recommendation: >
  Mindestens readme-Anforderung praezisieren: 057.1 oder neuer, ausser 060.3.
  Besser: FIRMWARE_VERSION parsen und DEVICE_SOFTWARE_OUTDATED setzen
  (Muster: KEBA/Mennekes, DeviceID.minVersion).
- references: modbus-de.md Abschnitt Byte-Reihenfolge; EvseMennekes (getOutdatedVersion)
- resolution:

### REV-003
- id: REV-003
- severity: MEDIUM
- status: OPEN
- found: 2026-11-20
- component: goe
- area: EvseGoeImpl.applySetPoint() (Truncation requestedSetPointInMilliAmpere / 1000 * 1000)
- requirements: REQ-03
- evidence: >
  Setpoints 1000..5900 mA werden nach Truncation als 1..5 A auf Register 299
  geschrieben. Doku: 299 akzeptiert nur 6..32 A; Verhalten bei 5 A ist
  undokumentiert. Werte unter 500 mA werden korrekt als 0 (Stopp ueber ALLOW)
  behandelt, Werte ueber 6500 korrekt trunciert; nur der Bereich 1000..5900 ist
  lueckenhaft. Der Controller wuenscht normalerweise >= 6 A (Ability-Minimum),
  der Fall ist also defensiv zu schliessen.
- recommendation: >
  Bei value > 0 auf MIN_CURRENT clampen: Math.max(MIN_CURRENT, truncated).
  Testfall ergaenzen (z. B. 5000 mA -> 6 A).
- references: modbus-de.md Register 40300 (299)
- resolution:

### REV-004
- id: REV-004
- severity: MEDIUM
- status: OPEN
- found: 2026-11-20
- component: goe
- area: EvseGoeImpl.applySetPoint()/isSetPointWriteThrottled() (Konstanten 5 s / 30 s)
- requirements: REQ-04, REQ-09
- evidence: >
  Designentscheidung war write-on-change (Throttle bewusst verworfen);
  Alfen throttlet pauschal 5 s und refresht identische Werte alle 5 s.
  Implementiert ist: neue Setpoints bis 5 s verzögert, identische nur alle 30 s
  (Begruendung im Test: Restore nach Box-Neustart wegen RAM-Register 299).
  Verhalten ist ok, aber weder im readme erwaehnt noch gegen die Referenz
  begruendet. Risiko: im Review als KI-Eigeninterpretation ohne Abstimmung gewertet.
- recommendation: >
  Einen Satz in readme (Communication): Schreibintervalle 5 s neue / 30 s
  identische Setpoints, Begruendung RAM-Volatile-Restore. In der
  PR-Beschreibung aktiv begruenden und mit Sebastian abstimmen.
- resolution:

### REV-005
- id: REV-005
- severity: LOW
- status: OPEN
- found: 2026-11-20
- component: goe
- area: readme.adoc, Abschnitt Supported devices
- requirements: REQ-09
- evidence: >
  readme nennt nur Gemini und Gemini flex. Der existierende evcs.goe und die
  Herstellerauskunft decken die identische API-Familie ab: Gemini 2.0,
  Gemini 2.0 flex, Charger Pro.
- recommendation: >
  Familie vollstaendig nennen oder Einschraenkung mit Begruendung dokumentieren.
- resolution:

### REV-006
- id: REV-006
- severity: LOW
- status: OPEN
- found: 2026-11-20
- component: goe
- area: EvseGoeImpl.defineModbusProtocol() (PHASES-Kanal, Register 205) und getPhases()
- requirements: REQ-02, REQ-06
- evidence: >
  Register 205 (PHASES, Bits vor/nach Schuetz) wird gelesen und als Kanal
  angeboten, aber getPhases() nutzt den psm-Readback (332) plus Strom-Heuristik.
  HW-Messung 22.10.: 56 = 3 Phasen, konsistent mit Spannungsmessung; 205 zeigt
  real anliegende Phasen statt Konfiguration und waere die robustere Quelle.
- recommendation: >
  Kurzfristig: Kommentar an den Kanal, dass 205 als kuenftiger Upgrade-Pfad
  der Phasenerkennung dient (HW-Teil-2 pending). Spaeter: getPhases() auf 205
  umstellen, sobald unter Last verifiziert.
- resolution:

### REV-007
- id: REV-007
- severity: NIT
- status: OPEN
- found: 2026-11-20
- component: goe
- area: EvseGoeImpl.handleEvent() (carState != null)
- requirements: REQ-07
- evidence: >
  getNextValue().asEnum() liefert nie null, sondern UNDEFINED. Der Null-Check
  ist toter Code.
- recommendation: entfernen.
- resolution:

### REV-008
- id: REV-008
- severity: NIT
- status: OPEN
- found: 2026-11-20
- component: goe
- area: EvseGoeImpl.getPhases() vs getPhaseSwitchAbility()
- requirements: REQ-07
- evidence: >
  Beide Methoden pruefen denselben Kanalwert auf zwei verschiedene Weisen:
  value().orElse(0) vs value().get() == null. Beide Varianten sind korrekt
  (Channel.value() liefert ein Value-Objekt, get() kann null sein),
  der Stil ist aber uneinheitlich.
- recommendation: vereinheitlichen (orElse/Optional-Idiom bevorzugt).
- resolution:

### REV-009
- id: REV-009
- severity: LOW
- status: OPEN
- found: 2026-11-20
- component: goe
- area: EvseGoeImpl.defineModbusProtocol() (FC3ReadRegistersTask 332, Priority.LOW)
- requirements: REQ-06
- evidence: >
  Readbacks von 200 und 299 laufen mit Priority.HIGH, 332 aber mit LOW.
  getPhases()/getPhaseSwitchAbility() lesen den Kanal aber jeden Zyklus;
  nach einem Phasenwechsel kann die Ability-Erneuerung verzoegert sein.
- recommendation: >
  Pruefen, ob die LOW-Abtastrate dafuer reicht; falls die Phasenwechsel-Latenz
  relevant ist, auf HIGH heben (Implikation fuer Task-Bloecke pruefen).
- resolution:

### REV-010
- id: REV-010
- severity: NIT
- status: OPEN
- found: 2026-11-20
- component: goe-test
- area: EvseGoeImplTest.getWriteRegisterValue() (Reflection auf Feld protocol)
- requirements: REQ-10
- evidence: >
  Der Test greift per Reflection auf das interne protocol-Feld zu, um die
  FC16-Write-Payload zu verifizieren. oe-junit-Skill: keine Implementierungs-
  details testen. Pragmatisch ok fuer Register-Payload-Verifikation, da kein
  oeffentlicher Hook existiert.
- recommendation: >
  Belassen mit Kommentar, oder Hook im Test-Framework vorschlagen (separates
  Thema, nicht im go-e-PR).
- resolution:

### REV-011
- id: REV-011
- severity: LOW
- status: OPEN
- found: 2026-11-20
- component: goe
- area: readme.adoc (fehlender Abschnitt Known limitations / Fehlerverhalten)
- requirements: REQ-05, REQ-09
- evidence: >
  Bei Kommunikationsverlust: Kanal ERROR behaelt den letzten Wert (kein
  falscher Fehlalarm, Tests decken das ab), das Geraet laeuft autonom mit
  letzter Konfiguration weiter, OpenEMS re-appliziert nach Reconnect
  (vgl. REV-004, 30-s-Refresh). Nichts davon steht im readme.
- recommendation: >
  Zwei bis drei Saetze im readme (Communication/Known limitations):
  Fallback-Verhalten des Geraets, Fehlerkanal-Verhalten, Re-Apply nach
  Wiederverbindung.
- resolution:

## 6. Als korrekt verifiziert (kein Handlungsbedarf)

- Register-Adressen, Typen, Skalierungen: vollstaendig konsistent mit modbus-de.md
  (VOLT x10^3 -> mV; AMP x10^2 -> mA; POWER_TOTAL x10^-2 -> W; POWER_L x10^2 -> W;
  ENERGY_TOTAL x10^2 -> Wh; AMPERE_MAX A -> mA; SNR/FWV ASCII).
- FN16-only-Write-Constraint umgesetzt (kein FC6); EEPROM 301 wird nie geschrieben.
- Enable-on-Charge ueber ALLOW mit Readback-Abgleich; Stopp sofort; Retest bei
  verlorenem Write (Tests testAllowCharging, testStopIsRetriedIfWriteIsLost).
- CAR_STATE-Mapping inkl. 1 = Ready ohne EV (Javadoc), UNDEFINED-Fall, EVCC-konsistent.
- ErrorCode.fromValue: unknown -> INTERNAL exakt wie Doku; ERROR=0 ehrlich kommentiert.
- Kommunikationsverlust erzeugt keinen Fehlalarm (Test testErrorIsUndefinedOnCommunicationLoss).
- Phasenwechsel Option A komplett: Ability-Guard (332 unlesbar / SINGLE_PHASE-Wiring),
  opposite-Phase-Ability (Mennekes-Muster 3), Auto-Modus wird auf OpenEMS-Modus gesetzt.
- Modbus-Register-Prioritaeten im Wesentlichen Alfen-konform; Debug-Mirror-Kanaele,
  translations de/en, @GenerateTargetsFromReferences (kein deprecated Modbus_target).
- Zeilen-/Wort-Order-Annahmen durch Standby-HW-Messungen gestuetzt (LSWMSW);
  finaler >64k-Beweis unter Last noch offen, im readme eingetragen.
- EdgeApp.bndrun: Bundle in -runrequires (Zeile 152) und -runbundles (Zeile 377)
  alphabetisch einsortiert.
- Tests: 13 Methoden, gute Abdeckung (Aktivierung, Skalen, parametrisierte
  Mappings, Ability-Grenzen inkl. 11-kW-Variante, Throttle-Timing mit Clock-Leap,
  Phasenwechsel, Read-Only).

## 7. Naechste Schritte vor Review-Runde 3

1. REV-003 (Clamp) und REV-002 (readme/DEVICE_SOFTWARE_OUTDATED) umsetzen.
2. REV-004, REV-005, REV-006, REV-011 als readme-Saetze ergaenzen; Nits streichen.
3. Rebuild: :io.openems.edge.evse.chargepoint.goe:test + checkstyleAll
   (letzter dokumentierter Full-Build war vor dem Runde-1-Umbau).
4. Live-Lauf der Komponente gegen die Gemini (schliesst REV-001 und offene
   HW-Punkte: CAR_STATE 2/3/4, Word-Order unter Last, psm-Write 332).
5. Danach Review-Runde 3 bei Sebastian; PR-Text: Throttle-Begruendung,
   evcs.goe-Abgrenzung, Alfen-Konformitaet; Trailer per Amend vor dem PR.

## 8. Changelog

- 2026-11-20: Initiale Review-Runde. Findings REV-001 bis REV-011 erstellt,
  Anforderungen REQ-01 bis REQ-11 festgehalten, Verifikations-Stand
  (Abschnitt 6) dokumentiert. Reviewer: Vibe (Mistral), Branch feature/go-e,
  Commit 5013e2ee3c3a1c292e8d9590a3a8e4c8650adec3.
- 2026-11-20: Tippfehler korrigiert (LSWMSW-Schreibweise, REV-008-Formulierung,
  REV-009-Formulierung, Abschnitt-7-Typo). Inhaltlich unveraendert.
