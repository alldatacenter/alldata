// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import agent from 'agent';

export const getAlarmRecordList = (
  query: Merge<ALARM_REPORT.RecordListQuery, IPagingReq>,
): IPagingResp<ALARM_REPORT.RecordListItem> => {
  return agent
    .get('/api/org-alert-records')
    .query(query)
    .then((response: any) => response.body);
};

// 集群总览需要带机器ip来筛选
export const getMachineAlarmRecordList = ({
  clusters,
  ...query
}: Merge<ALARM_REPORT.RecordListQuery, IPagingReq>): IPagingResp<ALARM_REPORT.RecordListItem> => {
  return agent
    .post('/api/org-hosts-alert-records')
    .query(query)
    .send({ clusters })
    .then((response: any) => response.body);
};

export const getAlarmAttrs = (): { [k: string]: Array<{ key: string; display: string }> } => {
  return agent.get('/api/org-alert-record-attrs').then((response: any) => response.body);
};

export const getAlarmRecordDetail = (groupId: string): ALARM_REPORT.RecordListItem => {
  return agent.get(`/api/org-alert-records/${groupId}`).then((response: any) => response.body);
};

export const getAlarmTimesChart = (query: ALARM_REPORT.AlarmTimesQuery): any => {
  return agent
    .get('/api/orgCenter/metrics/alert_trend/histogram')
    .query(query)
    .then((response: any) => response.body);
};

export const getAlarmRecordHistories = ({
  groupId,
  ...query
}: ALARM_REPORT.AlarmHistoriesQuery): ALARM_REPORT.AlarmHistory[] => {
  return agent
    .get(`/api/org-alert-records/${groupId}/histories`)
    .query(query)
    .then((response: any) => response.body);
};
