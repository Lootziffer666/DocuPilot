# DocuPilot – Stolperfallen, Umsetzbarkeit und Architekturfahrplan

## 1) Größte Stolperfallen (vor dem Ausbau)

1. **Widersprüche zwischen Quellen**: Alte Snippets/PRDs können neuere Regeln übersteuern. Lösung: verbindliche Prioritätsordnung (neuester Timestamp + "source-of-truth" Tags).
2. **Kontext-Vermischung**: Wissenschaftliche Dokumente werden durch Produkt-/UI-Notizen "verschmutzt". Lösung: strikte Kontext-Isolation pro Output-Typ.
3. **Projekt-Zuordnung ohne Rückfrage**: Auto-Sorting kann falsch clustern. Lösung: "review queue" mit Rückfragepflicht vor finaler Zuordnung.
4. **API-Lock-in** (Perplexity/Gemini): Unterschiedliche Limits, Kosten, Datenhaltung. Lösung: Provider-Adapter mit identischem Interface.
5. **Datenschutz/Compliance**: Uploads können sensible Inhalte enthalten. Lösung: lokales Preprocessing, PII-Redaction, Audit-Log.
6. **Unkontrolliertes Changelog-Wachstum**: Jeder kleine Delta erzeugt Rauschen. Lösung: semantische Schwellen (major/minor/info).

## 2) Schnelle Umsetzbarkeit (ohne Dependency-Overload)

**Ja, zügig umsetzbar** als 2-stufiges Vorgehen:

- **Stufe A (1–3 Tage)**: Dependency-leichter Kern in Python-Stdlib
  - Dateiscan
  - Cluster-Heuristiken
  - Inventar-Matrix (CSV/JSON)
  - Hash-basiertes Delta-Changelog
- **Stufe B (3–7 Tage)**: optionale LLM-Adapter
  - API-Key-Verwaltung
  - Gemini/Perplexity-Connector
  - Rückfrage-Workflow (Projekt zuordnen vs. neu anlegen)

## 3) Option „Dokumente analysieren und Projekten zuordnen“

### UX-Flow

1. Upload
2. Auto-Scan + Themenextraktion
3. **Rückfragen**:
   - "Zu bestehendem Projekt A/B/C zuordnen?"
   - "Oder neues Projekt anlegen?"
4. Konfliktanzeige
5. Übernahme + Changelog-Eintrag

### Entscheidungsregeln

- Wenn Similarity >= 0.75 zu bestehendem Projekt: Vorschlag "zuordnen".
- Wenn 0.45–0.74: Rückfrage mit Top-3-Kandidaten.
- Wenn < 0.45: Vorschlag "neuen Projektordner erstellen".

## 4) API-Optionen

- **Perplexity**: gut für web-nahe Rechercheanreicherung.
- **Gemini**: attraktiv, wenn Drive-Integration priorisiert wird.

Empfehlung für MVP: Adapter-Interface vorbereiten, aber mit lokalem Heuristikmodus starten; API-Nutzung optional zuschalten.

## 5) Output-Mapping (für spätere Generierung)

- **Project Master Overview**: Status, offene Punkte, letzte Änderungen.
- **Scientific Prep**: Methodik, Datensätze, Hypothesen, Evidenzreferenzen.
- **Technical PRD**: Feature-Liste, Regeln, Akzeptanzkriterien.
- **Change Log**: Nur Deltas (neu/geändert/entfernt + Auswirkungen).


## 6) Drive-Auto-Sync (neu)

- **Technischer Ansatz (dependency-light):** Python orchestriert, `rclone` übernimmt Upload/Sync nach Google Drive.
- **Automatische Aktualisierung:** Hash-basierte Manifest-Prüfung erkennt neu/geändert/entfernt und stößt nur bei Delta einen Sync an.
- **Projektordner-Logik:** Zielpfad `gdrive:DocuPilot/<projektordner>` wird beim ersten Lauf automatisch angelegt.
- **Betriebsmodus:** einmaliger Sync oder Watch-Modus mit Intervall (z. B. 300s).
- **Auditierbarkeit:** letzter Delta-Stand + Timestamp in `.docupilot/drive_sync_state.json`.
