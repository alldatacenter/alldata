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

export const createTestEnv = (data: TEST_ENV.CreateBody): TEST_ENV.Item => {
  return agent
    .post('/api/testenv')
    .send(data)
    .then((response: any) => response.body);
};

export const createAutoTestEnv = (data: TEST_ENV.ICreateAutoTestEnv) => {
  return agent
    .post('/api/autotests/global-configs')
    .send(data)
    .then((response: any) => response.body);
};

export const updateTestEnv = (data: TEST_ENV.Item): number => {
  return agent
    .put(`/api/testenv/${data.id}`)
    .send(data)
    .then((response: any) => response.body);
};

export const updateAutoTestEnv = (data: TEST_ENV.ICreateAutoTestEnv) => {
  return agent
    .put(`/api/autotests/global-configs/${data.ns}`)
    .send(data)
    .then((response: any) => response.body);
};

export const deleteAutoTestEnv = (ns: string): string => {
  return agent.delete(`/api/autotests/global-configs/${ns}`).then((response: any) => response.body);
};

export const deleteTestEnv = (id: number): string => {
  return agent.delete(`/api/testenv/${id}`).then((response: any) => response.body);
};

export const getTestEnvList = (params: TEST_ENV.EnvListQuery): TEST_ENV.Item[] => {
  return agent
    .get('/api/testenv/actions/list-envs')
    .query(params)
    .then((response: any) => response.body);
};

export const getAutoTestEnvList = (params: TEST_ENV.IAutoEnvQuery): TEST_ENV.IAutoEnvData[] => {
  return agent
    .get('/api/autotests/global-configs')
    .query(params)
    .then((response: any) => response.body);
};
