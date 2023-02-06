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

export const getCustomAlarms = (
  params: COMMON_CUSTOM_ALARM.IPageParam,
): IPagingResp<COMMON_CUSTOM_ALARM.CustomAlarms> => {
  return agent
    .get('/api/orgCenter/customize/alerts')
    .query(params)
    .then((response: any) => response.body);
};

export const getCustomAlarmDetail = ({ id }: { id: number }): COMMON_CUSTOM_ALARM.CustomAlarmDetail => {
  return agent.get(`/api/orgCenter/customize/alerts/${id}`).then((response: any) => response.body);
};

export const switchCustomAlarm = ({ id, enable }: { id: number; enable: boolean }) => {
  return agent
    .put(`/api/orgCenter/customize/alerts/${id}/switch?enable=${enable}`)
    .then((response: any) => response.body);
};

export const deleteCustomAlarm = ({ id }: { id: number }) => {
  return agent.delete(`/api/orgCenter/customize/alerts/${id}`).then((response: any) => response.body);
};

export const getCustomMetrics = (): COMMON_CUSTOM_ALARM.CustomMetrics => {
  return agent.get('/api/orgCenter/customize/alerts/metrics').then((response: any) => response.body);
};

export const getCustomAlarmTargets = (): { targets: COMMON_CUSTOM_ALARM.AlarmTarget[] } => {
  return agent.get('/api/orgCenter/customize/alerts/notifies/targets').then((response: any) => response.body);
};

export const createCustomAlarm = (payload: COMMON_CUSTOM_ALARM.CustomAlarmQuery) => {
  return agent
    .post('/api/orgCenter/customize/alerts')
    .send(payload)
    .then((response: any) => response.body);
};

export const editCustomAlarm = ({ id, ...rest }: COMMON_CUSTOM_ALARM.CustomAlarmQuery) => {
  return agent
    .put(`/api/orgCenter/customize/alerts/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const getPreviewMetaData = ({ ...payload }: COMMON_CUSTOM_ALARM.CustomAlarmQuery) => {
  return agent
    .post('/api/orgCenter/customize/alerts/dash-preview/query')
    .send(payload)
    .then((response: any) => response.body);
};
