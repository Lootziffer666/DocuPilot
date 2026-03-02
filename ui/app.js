const tabButtons = document.querySelectorAll('.tab-btn');
const panels = document.querySelectorAll('.tab-panel');
const panelTitle = document.getElementById('panel-title');

const TITLES = {
  overview: 'Projektübersicht',
  ingest: 'Ingestion & Zuordnung',
  rules: 'Regel- und Feature-Matrix',
  changes: 'Änderungsprotokoll',
  settings: 'Einstellungen & Integrationen'
};

tabButtons.forEach((btn) => {
  btn.addEventListener('click', () => {
    const target = btn.dataset.tab;
    tabButtons.forEach((b) => b.classList.remove('active'));
    panels.forEach((p) => p.classList.remove('active'));

    btn.classList.add('active');
    document.getElementById(target)?.classList.add('active');
    panelTitle.textContent = TITLES[target] ?? 'DocuPilot';
  });
});

const driveToggle = document.getElementById('drive-sync-toggle');
const projectFolder = document.getElementById('drive-project-folder');
const intervalInput = document.getElementById('drive-interval');
const syncCommand = document.getElementById('sync-command');

function updateSyncCommand() {
  if (!syncCommand) return;
  const folder = projectFolder?.value?.trim() || 'default-project';
  const interval = Number(intervalInput?.value || 300);
  const watchFlag = driveToggle?.checked ? '--watch' : '';
  const sanitizedInterval = Number.isFinite(interval) && interval >= 30 ? interval : 300;
  syncCommand.textContent = `python3 src/drive_sync.py --project-folder ${folder} --interval-s ${sanitizedInterval} ${watchFlag}`.trim();
}

[driveToggle, projectFolder, intervalInput].forEach((el) => {
  el?.addEventListener('input', updateSyncCommand);
  el?.addEventListener('change', updateSyncCommand);
});

updateSyncCommand();
