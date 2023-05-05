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

export const getCloudECSList = (params: IPagingReq): IPagingResp<COMPUTING.ECS> => {
  return agent
    .get('/api/cloud-ecs')
    .query(params)
    .then((response: any) => response.body);
};

export const stopCloudECS = (params: COMPUTING.ECSActionReq): COMPUTING.ECSActionRes => {
  return agent
    .post('/api/cloud-ecs/actions/stop')
    .send(params)
    .then((response: any) => response.body);
};

export const startCloudECS = (params: COMPUTING.ECSActionReq): COMPUTING.ECSActionRes => {
  return agent
    .post('/api/cloud-ecs/actions/start')
    .send(params)
    .then((response: any) => response.body);
};

export const restartCloudECS = (params: COMPUTING.ECSActionReq): COMPUTING.ECSActionRes => {
  return agent
    .post('/api/cloud-ecs/actions/restart')
    .send(params)
    .then((response: any) => response.body);
};

export const renewalCloudECS = (params: COMPUTING.ECSActionReq): COMPUTING.ECSActionRes => {
  return agent
    .post('/api/cloud-ecs/actions/config-renew-attribute')
    .send(params)
    .then((response: any) => response.body);
};
