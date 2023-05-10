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

export const getWorkFlowFiles = ({ gitRepoAbbrev }) => {
  return agent
    .get(`/api/repo/${gitRepoAbbrev}/tree-search?ref=master&pattern=*.workflow`)
    .then((response) => response.body);
};

export const batchCreateTask = ({ appId, batchPipelineYmlPaths }) => {
  return agent
    .post('/api/pipelines/actions/batch-create')
    .send({ appID: +appId, autoRun: true, batchPipelineYmlPaths, branch: 'master', source: 'bigdata' })
    .then((response) => response.body);
};

export const getBusinessScope = ({ remoteUri }) => {
  return agent
    .get('/api/analysis')
    .query({ remoteUri })
    .then((response) => response.body);
};

export const getBusinessProcesses = ({ businessDomain, dataDomain, keyWord, remoteUri, pageSize, pageNo }) => {
  return agent
    .get('/api/analysis/businessProcesses')
    .query({ businessDomain, dataDomain, keyWord, remoteUri, pageSize, pageNo })
    .then((response) => response.body);
};

export const getOutputTables = ({ businessDomain, marketDomain, keyWord, remoteUri, pageSize, pageNo }) => {
  return agent
    .get('/api/analysis/outputTables')
    .query({ businessDomain, marketDomain, keyWord, remoteUri, pageSize, pageNo })
    .then((response) => response.body);
};

export const getTableAttrs = ({ filePath, searchKey, pageSize, pageNo }) => {
  return agent
    .get('/api/analysis/fuzzyAttrs')
    .query({ filePath, keyWord: searchKey || undefined, pageSize, pageNo })
    .then((response) => response.body);
};

export const getStarChartSource = ({ filePath }) => {
  return agent
    .get('/api/analysis/star')
    .query({ filePath })
    .then((response) => response.body);
};
