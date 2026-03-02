# DocuPilot

Dependency-light prototype for living documentation workflows.

## Included modules

- CLI scanner for inventory matrix + modular schedule (`src/docupilot_cli.py`)
- Drive Auto-Sync helper (`src/drive_sync.py`) using `rclone` (no Python dependencies)
- Minimalistische GUI mit Tabs, Icons, Hover-Labels und klarer Struktur (`ui/`)

## Run CLI scanner

```bash
python3 src/docupilot_cli.py . --out .docupilot
```

Outputs:

- `.docupilot/inventory_matrix.json`
- `.docupilot/inventory_matrix.csv`
- `.docupilot/modular_schedule.json`

## Drive Auto-Sync

1. Install and configure rclone once (`rclone config`) with a Google Drive remote (default name in this project: `gdrive`).
2. Start one-shot sync or watch mode:

```bash
python3 src/drive_sync.py --project-folder korpus-normalisierung
python3 src/drive_sync.py --project-folder korpus-normalisierung --watch --interval-s 300
```

State and last manifest are stored in `.docupilot/drive_sync_state.json`.

## Run UI

```bash
python3 -m http.server 4173
```

Then open `http://localhost:4173/ui/`.
