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

export const getVpcList = (payload?: NETWORKS.ICloudVpcQuery): IPagingResp<NETWORKS.ICloudVpc> => {
  return agent
    .get('/api/cloud-vpc')
    .query(payload)
    .then((response: any) => response.body);
};

export const addVpc = (payload: NETWORKS.IVpcCreateBody): { vpcID: string } => {
  return agent
    .post('/api/cloud-vpc')
    .send(payload)
    .then((response: any) => response.body);
};

export const getVswList = (payload: NETWORKS.ICloudVswQuery): IPagingResp<NETWORKS.ICloudVsw> => {
  return agent
    .get('/api/cloud-vsw')
    .query(payload)
    .then((response: any) => response.body);
};

export const addVsw = (payload: NETWORKS.IVswCreateBody): { vswID: string } => {
  return agent
    .post('/api/cloud-vsw')
    .send(payload)
    .then((response: any) => response.body);
};

export const getCloudZone = (payload: NETWORKS.ICloudZoneQuery): NETWORKS.ICloudZone[] => {
  return agent
    .get('/api/cloud-zone')
    .query(payload)
    .then((response: any) => response.body);
};
