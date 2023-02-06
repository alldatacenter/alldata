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

export const addResource = ({ type, ...data }: PURCHASE.AddResource) => {
  return agent
    .post(`/api/resource/aliyun/${type}`)
    .send(data)
    .then((response: any) => response.body);
};

export const getPurchaseList = (params: { name: string }): PURCHASE.PurchaseItem[] => {
  return agent
    .get('/api/resource/deployments')
    .query(params)
    .then((response: any) => response.body);
};

export const getAvailableRegions = (data: Omit<PURCHASE.QueryZone, 'regionId'>): PURCHASE.Region[] => {
  return agent
    .post('/api/resource/aliyun/regions')
    .send(data)
    .then((response: any) => response.body);
};

export const getAvailableZones = (data: PURCHASE.QueryZone): PURCHASE.Zone[] => {
  return agent
    .post('/api/resource/aliyun/zones')
    .send(data)
    .then((response: any) => response.body);
};

export const checkEcsAvailable = (data: PURCHASE.CheckEcs): PURCHASE.CheckEscRes => {
  return agent
    .post('/api/resource/aliyun/ecs/check')
    .send(data)
    .then((response: any) => response.body);
};

export const addPhysicalCluster = (data: PURCHASE.AddPhysicalCluster) => {
  return agent
    .post('/api/resource/aliyun/cluster')
    .send(data)
    .then((response: any) => response.body);
};
