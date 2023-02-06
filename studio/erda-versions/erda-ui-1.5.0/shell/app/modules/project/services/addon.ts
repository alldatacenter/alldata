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

export const getCloudInstances = ({
  type,
  ...rest
}: CUSTOM_ADDON.InsQuery): IPagingResp<CUSTOM_ADDON.CloudInstance> => {
  return agent
    .get(`/api/cloud-${type}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const addCustomAddonIns = (params: CUSTOM_ADDON.AddBody) => {
  return agent
    .post('/api/addons/actions/create-custom')
    .send(params)
    .then((response: any) => response.body);
};

export const addDiceAddonIns = (params: CUSTOM_ADDON.AddDiceAddOns): string => {
  return agent
    .post('/api/addons/actions/create-addon')
    .send(params)
    .then((response: any) => response.body);
};

export const addTenantAddonIns = (params: CUSTOM_ADDON.AddTenantAddon): string => {
  return agent
    .post('/api/addons/actions/create-tenant')
    .send(params)
    .then((response: any) => response.body);
};

export const updateCustomAddonConfig = ({
  projectId,
  instanceId,
  orgId,
  operatorId,
  config,
}: CUSTOM_ADDON.UpdateBody) => {
  return agent
    .put(`/api/addons/${instanceId}/actions/update-custom`)
    .query({
      projectId,
      orgId,
      operatorId,
    })
    .send(config)
    .then((response: any) => response.body);
};

export const getAddonsByCategory = (params: CUSTOM_ADDON.QueryCustoms): CUSTOM_ADDON.Item[] => {
  return agent
    .get('/api/addons/actions/list-customs')
    .query(params)
    .then((response: any) => response.body);
};

export const getCloudGateway = (payload: CUSTOM_ADDON.QueryCloudGateway): CUSTOM_ADDON.CloudGateway => {
  return agent
    .get('/api/cloud-gateway')
    .query(payload)
    .then((response: any) => response.body);
};
