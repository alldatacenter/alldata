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

export const getCustomAlarms = ({
  tenantGroup,
  ...rest
}: COMMON_CUSTOM_ALARM.IPageParam): IPagingResp<COMMON_CUSTOM_ALARM.CustomAlarms> => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getCustomAlarmDetail = ({
  id,
  tenantGroup,
}: {
  id: number;
  tenantGroup: string;
}): COMMON_CUSTOM_ALARM.CustomAlarmDetail => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/${id}`)
    .then((response: any) => response.body);
};

export const switchCustomAlarm = ({
  id,
  enable,
  tenantGroup,
}: {
  id: number;
  enable: boolean;
  tenantGroup: string;
}) => {
  return agent
    .put(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/${id}/switch?enable=${enable}`)
    .then((response: any) => response.body);
};

export const deleteCustomAlarm = ({ id, tenantGroup }: { id: number; tenantGroup: string }) => {
  return agent
    .delete(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/${id}`)
    .then((response: any) => response.body);
};

export const getCustomMetrics = (tenantGroup: string): COMMON_CUSTOM_ALARM.CustomMetrics => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/metrics`)
    .then((response: any) => response.body);
};

export const getCustomAlarmTargets = (tenantGroup: string): { targets: COMMON_CUSTOM_ALARM.AlarmTarget[] } => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/notifies/targets`)
    .then((response: any) => response.body);
};

export const createCustomAlarm = ({ tenantGroup, ...rest }: COMMON_CUSTOM_ALARM.CustomAlarmQuery) => {
  return agent
    .post(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts`)
    .send(rest)
    .then((response: any) => response.body);
};

export const editCustomAlarm = ({ tenantGroup, id, ...rest }: COMMON_CUSTOM_ALARM.CustomAlarmQuery) => {
  return agent
    .put(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const getPreviewMetaData = ({
  tenantGroup,
  ...payload
}: Merge<COMMON_CUSTOM_ALARM.CustomAlarmQuery, 'tenantGroup'>) => {
  return agent
    .post(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/customize/alerts/dash-preview/query`)
    .send(payload)
    .then((response: any) => response.body);
};
