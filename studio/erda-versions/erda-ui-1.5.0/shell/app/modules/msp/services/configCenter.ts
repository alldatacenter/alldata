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

export const getAppList = ({ tenantId, ...rest }: ConfigCenter.GetAppList): ConfigCenter.RespAppList => {
  return agent
    .get(`/api/config/tenants/${tenantId}/groups`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getConfigList = ({
  tenantId,
  groupId,
  ...rest
}: ConfigCenter.GetConfigList): ConfigCenter.RespConfigList => {
  return agent
    .get(`/api/config/tenants/${tenantId}/groups/${groupId}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const saveConfig = ({ tenantId, groupId, configs }: ConfigCenter.SaveConfig): { success: boolean } => {
  return agent
    .post(`/api/config/tenants/${tenantId}/groups/${groupId}`)
    .send(configs)
    .then((response: any) => response.body);
};
