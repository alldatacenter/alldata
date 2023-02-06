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

// 获取项目迭代列表
export function getProjectIterations(payload: ITERATION.ListQuery): IPagingResp<ITERATION.Detail> {
  const { pageSize, ...rest } = payload;
  return agent
    .get('/api/iterations')
    .query({ ...rest, pageSize: pageSize || 10 })
    .then((response: any) => response.body);
}

export function getIterationDetail(payload: ITERATION.DetailQuery): Promise<ITERATION.Detail> {
  const { id, ...rest } = payload;
  return agent
    .get(`/api/iterations/${id}`)
    .query(rest)
    .then((response: any) => response.body);
}

// 创建项目迭代
export function createProjectIteration(payload: ITERATION.CreateBody) {
  return agent
    .post('/api/iterations')
    .send(payload)
    .then((response: any) => response.body);
}

export function editProjectIteration({ id, ...rest }: ITERATION.UpdateBody) {
  return agent
    .put(`/api/iterations/${id}`)
    .send(rest)
    .then((response: any) => response.body);
}

export function deleteIteration(id: number) {
  return agent.delete(`/api/iterations/${id}`).then((response: any) => response.body);
}
