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

export const getConfigs = ({
  appID,
  payload,
  apiPrefix,
}: {
  appID: string;
  payload: PIPELINE_CONFIG.ConfigQuery[];
  apiPrefix?: string;
}): PIPELINE_CONFIG.ConfigItemMap => {
  return agent
    .post(`/api/${apiPrefix || 'cicds'}/multinamespace/configs`)
    .query({ appID })
    .send({ namespaceParams: payload })
    .then((response: any) => response.body);
};

export const addConfigs = ({ query, configs, apiPrefix }: PIPELINE_CONFIG.AddConfigsBody) => {
  return agent
    .post(`/api/${apiPrefix || 'cicds'}/configs`)
    .query(query)
    .send({ configs })
    .then((response: any) => response.body);
};

export const updateConfigs = ({ query, configs, apiPrefix }: PIPELINE_CONFIG.AddConfigsBody) => {
  return agent
    .put(`/api/${apiPrefix || 'cicds'}/configs`)
    .query(query)
    .send({ configs })
    .then((response: any) => response.body);
};

export const removeConfigs = ({ apiPrefix, ...rest }: PIPELINE_CONFIG.DeleteConfigQuery) => {
  return agent
    .delete(`/api/${apiPrefix || 'cicds'}/configs`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getConfigNameSpaces = (appID: string): PIPELINE_CONFIG.NameSpaces => {
  return agent
    .get('/api/cicds/actions/fetch-config-namespaces')
    .query({ appID })
    .then((response: any) => response.body);
};

export const importConfigs = ({ query, configs }: PIPELINE_CONFIG.AddConfigsBody) => {
  return agent
    .post('/api/config/actions/import')
    .query(query)
    .send(configs)
    .then((response: any) => response.body);
};

export const exportConfigs = (query: string) => {
  return agent
    .get('/api/config/actions/export')
    .query(query)
    .then((response: any) => response.body);
};
