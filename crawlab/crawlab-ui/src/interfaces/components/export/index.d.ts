import {
  ExportTypeCsv,
  ExportTypeJson,
} from '@/constants/export';

export declare global {
  type ExportType = ExportTypeCsv | ExportTypeJson;
}

export * from './ExportDialog';
