#!/usr/bin/env python3
"""Dependency-light Google Drive auto-sync via rclone.

This module intentionally avoids third-party Python dependencies.
It uses the `rclone` CLI as adapter to sync local project folders to Drive.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

STATE_DIR = Path('.docupilot')
STATE_FILE = STATE_DIR / 'drive_sync_state.json'


@dataclass
class SyncConfig:
    local_root: Path
    remote: str
    project_folder: str
    interval_s: int
    dry_run: bool


EXCLUDE_DIRS = {'.git', '__pycache__', '.docupilot', '.docupilot_test'}


def iter_files(root: Path) -> Iterable[Path]:
    for path in root.rglob('*'):
        if not path.is_file():
            continue
        if any(part in EXCLUDE_DIRS for part in path.parts):
            continue
        yield path


def file_hash(path: Path) -> str:
    h = hashlib.sha1()
    with path.open('rb') as f:
        while True:
            chunk = f.read(1024 * 64)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()


def build_local_manifest(root: Path) -> dict[str, str]:
    manifest: dict[str, str] = {}
    for file in iter_files(root):
        manifest[str(file.relative_to(root))] = file_hash(file)
    return manifest


def load_state() -> dict:
    if not STATE_FILE.exists():
        return {}
    try:
        return json.loads(STATE_FILE.read_text(encoding='utf-8'))
    except Exception:
        return {}


def save_state(state: dict) -> None:
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    STATE_FILE.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding='utf-8')


def compute_delta(old: dict[str, str], new: dict[str, str]) -> dict[str, list[str]]:
    old_keys = set(old)
    new_keys = set(new)
    created = sorted(new_keys - old_keys)
    deleted = sorted(old_keys - new_keys)
    changed = sorted(k for k in old_keys & new_keys if old[k] != new[k])
    return {'created': created, 'changed': changed, 'deleted': deleted}


def ensure_rclone() -> str:
    exe = shutil.which('rclone')
    if not exe:
        raise RuntimeError('rclone not found. Install rclone and run `rclone config` for your Google Drive remote.')
    return exe


def run_rclone_sync(cfg: SyncConfig) -> None:
    exe = ensure_rclone()
    remote_target = f"{cfg.remote}:DocuPilot/{cfg.project_folder}"
    cmd = [
        exe,
        'sync',
        str(cfg.local_root),
        remote_target,
        '--progress',
        '--exclude', '.git/**',
        '--exclude', '.docupilot/**',
        '--exclude', '__pycache__/**',
    ]
    if cfg.dry_run:
        cmd.append('--dry-run')
    subprocess.run(cmd, check=True)


def sync_once(cfg: SyncConfig) -> int:
    state = load_state()
    local_manifest = build_local_manifest(cfg.local_root)
    previous = state.get('manifest', {})
    delta = compute_delta(previous, local_manifest)

    total_delta = len(delta['created']) + len(delta['changed']) + len(delta['deleted'])
    if total_delta == 0:
        print('No local changes detected; Drive sync skipped.')
        return 0

    print(f"Detected {len(delta['created'])} new, {len(delta['changed'])} changed, {len(delta['deleted'])} deleted files.")
    run_rclone_sync(cfg)

    state.update(
        {
            'manifest': local_manifest,
            'last_sync_epoch': int(time.time()),
            'remote': cfg.remote,
            'project_folder': cfg.project_folder,
            'last_delta': delta,
        }
    )
    save_state(state)
    print('Drive sync completed.')
    return 0


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description='DocuPilot Drive auto-sync (rclone-based)')
    p.add_argument('--local-root', default='.', help='Local folder to mirror')
    p.add_argument('--remote', default='gdrive', help='rclone remote name (configured via `rclone config`)')
    p.add_argument('--project-folder', default='default-project', help='Project folder under Drive/DocuPilot')
    p.add_argument('--interval-s', type=int, default=300, help='Polling interval for --watch mode')
    p.add_argument('--watch', action='store_true', help='Keep syncing whenever changes are detected')
    p.add_argument('--dry-run', action='store_true', help='Print/plans sync without uploading')
    return p.parse_args()


def main() -> int:
    args = parse_args()
    cfg = SyncConfig(
        local_root=Path(args.local_root).resolve(),
        remote=args.remote,
        project_folder=args.project_folder,
        interval_s=args.interval_s,
        dry_run=args.dry_run,
    )

    if not args.watch:
        return sync_once(cfg)

    print(f'Watch mode started (every {cfg.interval_s}s). Press Ctrl+C to stop.')
    while True:
        try:
            sync_once(cfg)
        except subprocess.CalledProcessError as exc:
            print(f'Sync failed (rclone exit {exc.returncode}). Retrying...')
        except RuntimeError as exc:
            print(str(exc))
            return 2
        time.sleep(cfg.interval_s)


if __name__ == '__main__':
    raise SystemExit(main())
