import {FILE_UPLOAD_MODE_DIR, FILE_UPLOAD_MODE_FILES} from '@/constants';

declare global {
  interface FileUploadProps {
    mode?: FileUploadMode;
    getInputProps?: () => any;
    open?: () => void;
  }

  interface FileUploadModeOption {
    label: string;
    value: string;
  }

  interface FileUploadInfo {
    dirName?: string;
    fileCount?: number;
    filePaths?: string[];
  }

  type FileUploadMode = FILE_UPLOAD_MODE_DIR | FILE_UPLOAD_MODE_FILES;
}
