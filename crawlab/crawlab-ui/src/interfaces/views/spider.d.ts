type SpiderTabName = 'overview' | 'files' | 'tasks' | 'settings';

interface SpiderDialogVisible extends DialogVisible {
  run: boolean;
  uploadFiles: boolean;
}

type SpiderDialogKey = DialogKey | 'run' | 'uploadFiles';
