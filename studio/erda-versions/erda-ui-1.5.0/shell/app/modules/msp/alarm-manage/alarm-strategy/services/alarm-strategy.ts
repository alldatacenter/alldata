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

export const getAlerts = (params: {
  pageNo?: number;
  pageSize?: number;
  tenantGroup: string;
}): { list: COMMON_STRATEGY_NOTIFY.IAlert[]; total: number } => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${params.tenantGroup}/alerts`)
    .query(params)
    .then((response: any) => response.body);
};

export const getAlertDetail = ({
  id,
  tenantGroup,
}: {
  id: number;
  tenantGroup: string;
}): COMMON_STRATEGY_NOTIFY.IAlertDetail => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/alerts/${id}`)
    .then((response: any) => response.body);
};

export const createAlert = ({
  body,
  tenantGroup,
}: {
  body: COMMON_STRATEGY_NOTIFY.IAlertBody;
  tenantGroup: string;
}) => {
  return agent
    .post(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/alerts`)
    .send(body)
    .then((response: any) => response.body);
};

export const editAlert = ({
  body,
  id,
  tenantGroup,
}: {
  body: COMMON_STRATEGY_NOTIFY.IAlertBody;
  id: string;
  tenantGroup: string;
}) => {
  return agent
    .put(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/alerts/${id}`)
    .send(body)
    .then((response: any) => response.body);
};

export const deleteAlert = ({ id, tenantGroup }: { id: number; tenantGroup: string }) => {
  return agent
    .delete(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/alerts/${id}`)
    .then((response: any) => response.body);
};

export const getAlertTypes = (tenantGroup: string): COMMON_STRATEGY_NOTIFY.IAlertType => {
  return agent
    .get(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/alerts-rules`)
    .then((response: any) => response.body);
};

export const toggleAlert = ({ id, enable, tenantGroup }: { id: string; enable: boolean; tenantGroup: string }) => {
  return agent
    .put(`/api/tmc/micro-service/tenantGroup/${tenantGroup}/alerts/${id}/switch`)
    .query({ enable })
    .then((response: any) => response.body);
};

export const getClusterList = ({ orgId }: { orgId: number }) => {
  return agent
    .get('/api/clusters')
    .query({ orgID: orgId })
    .then((response: any) => response.body);
};

export const getAlertTriggerConditions = (scopeType: string) => {
  return agent
    .get('/api/msp/apm/conditions')
    .query({ scopeType })
    .then((response: any) => response.body);
};

export const getAlertTriggerConditionsContent = (
  payload: COMMON_STRATEGY_NOTIFY.IAlertTriggerConditionQueryItem[],
): COMMON_STRATEGY_NOTIFY.IAlertTriggerConditionContent[] => {
  return agent
    .post('/api/msp/apm/conditions/value')
    .send({ conditions: payload })
    .then((response: any) => response.body);
};
