import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

import src.drive_sync as drive_sync


class DriveSyncTests(unittest.TestCase):
    def test_dry_run_does_not_persist_state(self):
        with TemporaryDirectory() as td:
            root = Path(td)
            (root / 'a.txt').write_text('hello', encoding='utf-8')
            state_file = root / '.docupilot' / 'drive_sync_state.json'

            cfg = drive_sync.SyncConfig(
                local_root=root,
                remote='gdrive',
                project_folder='proj',
                interval_s=60,
                dry_run=True,
            )

            with patch.object(drive_sync, 'STATE_DIR', root / '.docupilot'), \
                 patch.object(drive_sync, 'STATE_FILE', state_file), \
                 patch.object(drive_sync, 'run_rclone_sync') as run_sync:
                rc = drive_sync.sync_once(cfg)

            self.assertEqual(rc, 0)
            run_sync.assert_called_once()
            self.assertFalse(state_file.exists(), 'dry-run must not write sync state')

    def test_manifest_is_ignored_when_target_changes(self):
        with TemporaryDirectory() as td:
            root = Path(td)
            (root / 'a.txt').write_text('hello', encoding='utf-8')
            state_file = root / '.docupilot' / 'drive_sync_state.json'

            cfg = drive_sync.SyncConfig(
                local_root=root,
                remote='gdrive',
                project_folder='new-target',
                interval_s=60,
                dry_run=False,
            )

            with patch.object(drive_sync, 'STATE_DIR', root / '.docupilot'), \
                 patch.object(drive_sync, 'STATE_FILE', state_file), \
                 patch.object(drive_sync, 'run_rclone_sync') as run_sync:
                drive_sync.save_state(
                    {
                        'manifest': {'a.txt': drive_sync.file_hash(root / 'a.txt')},
                        'remote': 'gdrive',
                        'project_folder': 'old-target',
                    }
                )
                rc = drive_sync.sync_once(cfg)

            self.assertEqual(rc, 0)
            run_sync.assert_called_once()


if __name__ == '__main__':
    unittest.main()
