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

export const getAddonList = ({ path, orgId }: { path: string; orgId: number }): ADDON.Instance[] => {
  return agent
    .get(`/api/${path}/service/addons`) // path: 'orgCenter' | 'workBench'
    .query({ orgId })
    .then((response: any) => response.body);
};

export const getAddonDetail = (insId: string): ADDON.Instance => {
  return agent.get(`/api/addons/${insId}`).then((response: any) => response.body);
};

export const getAddonReferences = (insId: string): ADDON.Reference[] => {
  return agent.get(`/api/addons/${insId}/actions/references`).then((response: any) => response.body);
};

export const deleteAddonIns = (insId: string) => {
  return agent.delete(`/api/addons/${insId}`).then((response: any) => response.body);
};

export const getExportAddonSpec = (projectId: string | number): string => {
  return agent.post(`/api/addon/action/yml-export?project=${projectId}`).then((response: any) => response.body);
};

export const importCustomAddon = ({ projectId, body }: { projectId: string | number; body: Obj }) => {
  return agent
    .post(`/api/addon/action/yml-import?project=${projectId}`)
    .send(body)
    .then((response: any) => response.body);
};
