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

export const getRDSList = (query: CLOUD_SERVICE.ICloudServiceQuery): IPagingResp<CLOUD_SERVICE.IRDS> => {
  return agent
    .get('/api/cloud-mysql')
    .query(query)
    .then((response: any) => response.body);
};

export const addRDS = (data: CLOUD_SERVICE.IRDSCreateBody) => {
  return agent
    .post('/api/cloud-mysql')
    .send(data)
    .then((response: any) => response.body);
};

export const getRDSDetails = ({ id, query }: CLOUD_SERVICE.IDetailsQuery): CLOUD_SERVICE.IDetailsResp[] => {
  return agent
    .get(`/api/cloud-mysql/${id}`)
    .query(query)
    .then((response: any) => response.body);
};

export const getRDSAccountList = ({
  id,
  query,
}: CLOUD_SERVICE.IUserQuery): IPagingResp<CLOUD_SERVICE.IRDSAccountResp> => {
  return agent
    .get(`/api/cloud-mysql/${id}/accounts`)
    .query(query)
    .then((response: any) => response.body);
};

export const addRDSAccount = (data: CLOUD_SERVICE.IRDSCreateAccountBody) => {
  return agent
    .post('/api/cloud-mysql/actions/create-account')
    .send(data)
    .then((response: any) => response.body);
};

export const resetRDSAccountPWD = (data: CLOUD_SERVICE.IRDSAccountBody) => {
  return agent
    .post('/api/cloud-mysql/actions/reset-password')
    .send(data)
    .then((response: any) => response.body);
};

export const revokeRDSAccountPriv = (data: CLOUD_SERVICE.IRDSRevokeAccountPrivBody) => {
  return agent
    .post('/api/cloud-mysql/actions/grant-privilege')
    .send(data)
    .then((response: any) => response.body);
};

export const getRDSDatabaseList = ({
  id,
  query,
}: CLOUD_SERVICE.IDatabaseQuery): IPagingResp<CLOUD_SERVICE.IRDSDatabaseResp> => {
  return agent
    .get(`/api/cloud-mysql/${id}/databases`)
    .query(query)
    .then((response: any) => response.body);
};

export const addRDSDatabase = (data: CLOUD_SERVICE.IRDSDatabaseBody) => {
  return agent
    .post('/api/cloud-mysql/actions/create-db')
    .send(data)
    .then((response: any) => response.body);
};

export const getMQList = (query: CLOUD_SERVICE.ICloudServiceQuery): IPagingResp<CLOUD_SERVICE.IMQ> => {
  return agent
    .get('/api/cloud-ons')
    .query(query)
    .then((response: any) => response.body);
};

export const addMQ = (data: CLOUD_SERVICE.IMQCreateBody) => {
  return agent
    .post('/api/cloud-ons')
    .send(data)
    .then((response: any) => response.body);
};

export const getMQDetails = ({ id, query }: CLOUD_SERVICE.IDetailsQuery): CLOUD_SERVICE.IDetailsResp[] => {
  return agent
    .get(`/api/cloud-ons/${id}`)
    .query(query)
    .then((response: any) => response.body);
};

export const getMQTopicList = (query: CLOUD_SERVICE.IMQManageQuery): IPagingResp<CLOUD_SERVICE.IMQTopic> => {
  return agent
    .get('/api/cloud-ons/actions/list-topic')
    .query(query)
    .then((response: any) => response.body);
};

export const addMQTopic = (data: CLOUD_SERVICE.IMQTopicBody) => {
  return agent
    .post('/api/cloud-ons/actions/create-topic')
    .send(data)
    .then((response: any) => response.body);
};

export const getMQGroupList = (query: CLOUD_SERVICE.IMQManageQuery): IPagingResp<CLOUD_SERVICE.IMQGroup> => {
  return agent
    .get('/api/cloud-ons/actions/list-group')
    .query(query)
    .then((response: any) => response.body);
};

export const addMQGroup = (data: CLOUD_SERVICE.IMQGroupBody) => {
  return agent
    .post('/api/cloud-ons/actions/create-group')
    .send(data)
    .then((response: any) => response.body);
};

export const getRedisList = (query: CLOUD_SERVICE.ICloudServiceQuery): IPagingResp<CLOUD_SERVICE.IRedis> => {
  return agent
    .get('/api/cloud-redis')
    .query(query)
    .then((response: any) => response.body);
};

export const addRedis = (data: CLOUD_SERVICE.IRedisCreateBody) => {
  return agent
    .post('/api/cloud-redis')
    .send(data)
    .then((response: any) => response.body);
};

export const getRedisDetails = ({ id, query }: CLOUD_SERVICE.IDetailsQuery): CLOUD_SERVICE.IDetailsResp[] => {
  return agent
    .get(`/api/cloud-redis/${id}`)
    .query(query)
    .then((response: any) => response.body);
};
