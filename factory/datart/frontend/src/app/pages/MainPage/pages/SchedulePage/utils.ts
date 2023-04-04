import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { PermissionLevels, ResourceTypes } from '../PermissionPage/constants';
import { JobTypes, TimeModes, VizTypes } from './constants';
import { AddScheduleParams, Schedule, VizContentsItem } from './slice/types';
import { FormValues } from './types';

const computePeriodUnit = (cronExpression: string) => {
  const partitions = cronExpression.split(' ');
  const stars = partitions.filter(item => item === '*').length;
  switch (stars) {
    case 3:
      return partitions[1].includes('/') ? TimeModes.Minute : TimeModes.Hour;
    case 2:
      return TimeModes.Day;
    case 1:
      return partitions[partitions.length - 1] === '?'
        ? TimeModes.Month
        : TimeModes.Week;
    case 0:
      return TimeModes.Year;
    default:
      return TimeModes.Minute;
  }
};
export const getTimeValues = (cronExpression: string) => {
  const partitions = cronExpression.split(' ');
  const currentPeriodUnit = computePeriodUnit(cronExpression);
  let minute = +((partitions[1] || ([] as string[])).includes('/')
    ? partitions[1].slice(2) // slice(2) to remove */
    : partitions[1]);
  // min minute duration is 10
  if (currentPeriodUnit === 'Minute' && minute < 10) {
    minute = 10;
  }
  const hour = +partitions[2] || 0;
  const day = +partitions[3] || 1;
  const month = +partitions[4] || 1;
  const weekDay = +partitions[5] || 1;
  return { minute, hour, day, month, weekDay, periodUnit: currentPeriodUnit };
};

export const getCronExpressionByPartition = ({
  minute,
  month,
  hour,
  weekDay,
  periodUnit,
  day,
}: FormValues) => {
  switch (periodUnit as TimeModes) {
    case TimeModes.Minute:
      return `0 */${minute || 0} * * * ?`;
    case TimeModes.Hour:
      return `0 ${minute || 0} * * * ?`;
    case TimeModes.Day:
      return `0 ${minute || 0} ${hour || 0} * * ?`;
    case TimeModes.Week:
      return `0 ${minute || 0} ${hour || 0} ? * ${weekDay || 0}`;
    case TimeModes.Month:
      return `0 ${minute || 0} ${hour || 0} ${day || 0} * ?`;
    case TimeModes.Year:
      return `0 ${minute || 0} ${hour || 0} ${day || 0} ${month || 0} ?`;
    default:
      return '0 */10 * * * ?';
  }
};

export const toScheduleSubmitParams = (
  values: FormValues,
  orgId: string,
): AddScheduleParams => {
  const {
    jobType,
    name,
    dateRange = [],
    folderContent = [],
    imageWidth,
    setCronExpressionManually,
    cronExpression = '',
    type,
    to,
    cc,
    bcc,
    subject,
    webHookUrl,
    textContent,
    parentId,
    index,
  } = values;
  const jobConfig = {
      subject,
      vizContents: folderContent,
      imageWidth,
      setCronExpressionManually: setCronExpressionManually,
      attachments: type || [],
      to,
      cc,
      bcc,
      webHookUrl,
      textContent,
    },
    jobConfigStr = JSON.stringify(jobConfig);
  return {
    name: name || '',
    cronExpression: setCronExpressionManually
      ? cronExpression
      : getCronExpressionByPartition(values),
    type: jobType as JobTypes,
    startDate: dateRange[0] ? dateRange[0].format(TIME_FORMATTER) : undefined,
    endDate: dateRange[1] ? dateRange[1].format(TIME_FORMATTER) : undefined,
    orgId,
    parentId,
    index,
    config: jobConfigStr,
  };
};
export const toEchoFormValues = ({
  name,
  type,
  startDate,
  config,
  endDate,
  cronExpression,
  parentId,
  index,
}: Schedule): FormValues => {
  const configObj: any = config ? JSON.parse(config || '{}') : {},
    vizContents: VizContentsItem[] = configObj?.vizContents || [],
    folderContent = vizContents.filter(v => v?.vizType !== VizTypes.StoryBoard),
    demoContent = vizContents.filter(v => v?.vizType === VizTypes.StoryBoard),
    setCronExpressionManually = !!configObj?.setCronExpressionManually;
  const timeValues = setCronExpressionManually
    ? { cronExpression }
    : getTimeValues(cronExpression);
  return {
    name,
    parentId,
    index,
    jobType: type as JobTypes,
    dateRange:
      startDate && endDate ? [moment(startDate), moment(endDate)] : undefined,
    subject: configObj?.subject,
    textContent: configObj?.textContent || '',
    imageWidth: configObj?.imageWidth,
    to: configObj?.to || '',
    cc: configObj?.cc || '',
    bcc: configObj?.bcc || '',
    type: configObj?.attachments || [],
    webHookUrl: configObj?.webHookUrl || '',
    demoContent,
    folderContent,
    setCronExpressionManually,
    ...timeValues,
  };
};

export function allowCreateSchedule() {
  return {
    module: ResourceTypes.Schedule,
    id: ResourceTypes.Schedule,
    level: PermissionLevels.Create,
  };
}
