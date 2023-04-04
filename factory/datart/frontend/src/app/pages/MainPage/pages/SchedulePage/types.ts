import { Moment } from 'moment';
import { FileTypes, JobTypes, TimeModes } from './constants';
import { VizContentsItem } from './slice/types';

export interface FormValues {
  name: string;
  jobType?: JobTypes;
  description?: string;
  type?: FileTypes[];
  imageWidth?: number;
  periodUnit?: TimeModes;
  month?: number;
  day?: number;
  hour?: number;
  minute?: number;
  weekDay?: number;
  cronExpression?: string;
  webHookUrl?: string;
  textContent: string;
  dateRange?: Moment[];
  subject?: string;
  to?: string;
  cc?: string;
  bcc?: string;
  folderContent?: VizContentsItem[];
  demoContent?: VizContentsItem[];
  setCronExpressionManually?: boolean;
  index: number | null;
  parentId: string | null;
}

export interface IUserInfo {
  id: number;
  username: string;
  email: string;
  avatar: string;
}
