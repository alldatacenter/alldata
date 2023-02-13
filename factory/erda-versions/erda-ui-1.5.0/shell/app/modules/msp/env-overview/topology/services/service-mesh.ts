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

export const getCircuitBreaker = (query: TOPOLOGY.IServiceMeshQuery): Promise<TOPOLOGY.ICircuitBreaker> => {
  const { projectId, env, tenantGroup, ...rest } = query;
  return agent
    .get(`/api/tmc/servicemesh/circuitbreaker/${projectId}/${env}/${tenantGroup}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const saveCircuitBreaker = (params: TOPOLOGY.ICircuitBreakerSave): Promise<string> => {
  const { projectId, env, tenantGroup, data, query } = params;
  return agent
    .post(`/api/tmc/servicemesh/circuitbreaker/${projectId}/${env}/${tenantGroup}`)
    .query(query)
    .send(data)
    .then((response: any) => response.body);
};

export const getFaultInject = (query: TOPOLOGY.IServiceMeshQuery): Promise<TOPOLOGY.IFaultInject> => {
  const { projectId, env, tenantGroup, ...rest } = query;
  return agent
    .get(`/api/tmc/servicemesh/faultinject/${projectId}/${env}/${tenantGroup}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const saveFaultInject = (params: TOPOLOGY.IFaultInjectSave): Promise<string> => {
  const { projectId, env, tenantGroup, data, query } = params;
  return agent
    .post(`/api/tmc/servicemesh/faultinject/${projectId}/${env}/${tenantGroup}`)
    .query(query)
    .send(data)
    .then((response: any) => response.body);
};

export const deleteFaultInject = (params: TOPOLOGY.IFaultInjectDelete): Promise<boolean> => {
  const { projectId, env, tenantGroup, id, ...rest } = params;
  return agent
    .delete(`/api/tmc/servicemesh/faultinject/${projectId}/${env}/${tenantGroup}/${id}`)
    .query(rest)
    .then((response: any) => response.body);
};
