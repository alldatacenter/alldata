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

export const getCaseDetail = ({ id }: { id: string }): AUTO_TEST.ICaseDetail => {
  return agent.get(`/api/autotests/filetree/${id}`).then((response: any) => response.body);
};

export const updateCasePipeline = ({ nodeId, ...rest }: AUTO_TEST.IUpdateCaseBody) => {
  return agent
    .post(`/api/autotests/filetree/${nodeId}/actions/save-pipeline`)
    .send({ ...rest })
    .then((response: any) => response.body);
};

export const getSnippetNodeDetail = (query: AUTO_TEST.ISnippetDetailQuery): AUTO_TEST.ISnippetDetailRes => {
  return agent
    .post('/api/pipeline-snippets/actions/query-details')
    .send(query)
    .then((response: any) => response.body);
};

export const getAutoTestConfigEnv = (query: AUTO_TEST.IConfigEnvQuery): AUTO_TEST.IConfigEnv[] => {
  return agent
    .get('/api/autotests/global-configs')
    .query(query)
    .then((response: any) => response.body);
};

export const createPipelineAndRun = (query: AUTO_TEST.ICreateAndRunQuery) => {
  return agent
    .post('/api/cicds-project')
    .send(query)
    .then((response: any) => response.body);
};

export const getPipelineRecordList = (query: AUTO_TEST.IRunRecordQuery): IPagingResp<PIPELINE.IPipeline> => {
  return agent
    .get('/api/pipelines')
    .query(query)
    .then((response: any) => response.body);
};

export const getPipelineDetail = ({ pipelineID }: { pipelineID: string }): PIPELINE.IPipelineDetail => {
  return agent.get(`/api/pipelines/${pipelineID}`).then((response: any) => response.body);
};

export const getPipelineReport = ({ pipelineID }: { pipelineID: number }) => {
  return agent
    .get(`/api/reportsets/${pipelineID}`)
    .query('type=api-test')
    .then((response: any) => response.body);
};

export const reRunFailed = ({ pipelineID }: { pipelineID: string }) => {
  return agent.post(`/api/cicds/${pipelineID}/actions/rerun-failed`).then((response: any) => response.body);
};

export const reRunEntire = ({ pipelineID }: { pipelineID: string }) => {
  return agent.post(`/api/cicds/${pipelineID}/actions/rerun`).then((response: any) => response.body);
};

export const cancelBuild = ({ pipelineID }: { pipelineID: string }) => {
  return agent.post(`/api/pipelines/${pipelineID}/actions/cancel`).then((response: any) => response.body);
};
export const runBuild = ({ pipelineID, runPipelineParams }: { pipelineID: string; runPipelineParams?: any }) => {
  return agent
    .post(`/api/cicds/${pipelineID}/actions/run`)
    .send({ runPipelineParams })
    .then((response: any) => response.body);
};

export const updateTaskEnv = ({
  pipelineID,
  taskID,
  disabled,
}: {
  pipelineID: string;
  taskID: number;
  disabled: boolean;
}) => {
  // const paramDisable = disabled || undefined; // recover it when add pause
  return agent
    .put(`/api/cicds/${pipelineID}`)
    .send({ taskOperates: [{ taskID, disable: disabled }] })
    .then((response: any) => response.body);
};

export const getConfigDetailRecordList = (inode: string): AUTO_TEST.ICaseDetail[] => {
  return agent.get(`/api/autotests/filetree/${inode}/actions/get-histories`).then((response: any) => response.body);
};
