import { JobTypes } from '../constants';
export interface FoldersTreeItem {}
export interface DemosTreeItem {}
export interface ScheduleState {
  schedules: Schedule[];
  archived: Schedule[];
  editingSchedule: Schedule | null;
  scheduleListLoading: boolean;
  archivedListLoading: boolean;
  scheduleDetailsLoading: boolean;
  saveLoading: boolean;
  unarchiveScheduleLoading: boolean;
  deleteLoading: boolean;
  logs: ErrorLog[];
  logsLoading: boolean;
}

export interface Schedule {
  id: string;
  index: number;
  isFolder: boolean | null;
  name: string;
  parentId: string;
  orgId: string;
  status: number;
  type: JobTypes;
  startDate?: string;
  endDate?: string;
  setCronExpressionManually?: boolean;
  config: string;
  createBy: string;
  createTime: string;
  updateBy: string;
  updateTime: string;
  active: boolean; // true->started
  cronExpression: string;
}

export interface VizContentsItem {
  vizId: string;
  vizType: string;
}
export interface JobConfig {
  to?: string;
  cc?: string;
  bcc?: string;
  attachments?: string[];
  subject?: string;
  imageWidth?: number;
  vizContents?: VizContentsItem[];
  setCronExpressionManually?: boolean;
  type?: string;
  webHookUrl?: string;
  textContent?: string;
}
export interface AddScheduleParams {
  cronExpression: string;
  endDate?: string;
  startDate?: string;
  name: string;
  type: JobTypes;
  orgId: string;
  config: string;
  description?: string;
  id?: string;
}

export interface ScheduleParamsResolve {
  params: AddScheduleParams;
  resolve: (id: string) => void;
}
export interface EditScheduleParamsResolve {
  params: AddScheduleParams;
  scheduleId: string;
  resolve: () => void;
}

export interface DeleteScheduleParams {
  id: string;
  archive: boolean;
  resolve: () => void;
}

export interface FolderType {
  id: string;
  name: string;
  orgId: string;
  parentId: string | null;
  relId: string;
}

export interface ParamsWithResolve {
  scheduleId: string;
  resolve: () => void;
}

export interface ErrorLog {
  createBy: null;
  createTime: null | string;
  end: string;
  id: string;
  message: string;
  permission: null;
  scheduleId: string;
  start: string;
  status: number;
  updateBy: null;
  updateTime: null;
}
