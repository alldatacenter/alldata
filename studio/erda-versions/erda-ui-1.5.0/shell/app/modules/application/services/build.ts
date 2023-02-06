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

export const getRuntimeDetail = ({ runtimeId }: { runtimeId: number }): RUNTIME.Detail => {
  return agent.get(`/api/runtimes/${runtimeId}`).then((response: any) => response.body);
};

export const getPipelineDetail = ({ pipelineID }: { pipelineID: number }): BUILD.IPipelineDetail => {
  return agent.get(`/api/pipelines/${pipelineID}`).then((response: any) => response.body);
};

export const cancelBuild = ({ pipelineID }: { pipelineID: number }) => {
  return agent.post(`/api/cicds/${pipelineID}/actions/cancel`).then((response: any) => response.body);
};

export const cancelBuildCron = ({ cronID }: { cronID: number }) => {
  return agent.put(`/api/cicd-crons/${cronID}/actions/stop`).then((response: any) => response.body);
};

export const startBuildCron = ({ cronID }: { cronID: number }) => {
  return agent.put(`/api/cicd-crons/${cronID}/actions/start`).then((response: any) => response.body);
};

export const runBuild = ({ pipelineID }: { pipelineID: number }) => {
  return agent.post(`/api/cicds/${pipelineID}/actions/run`).then((response: any) => response.body);
};

export const reRunFailed = ({ pipelineID }: { pipelineID: number }) => {
  return agent.post(`/api/cicds/${pipelineID}/actions/rerun-failed`).then((response: any) => response.body);
};

export const reRunEntire = ({ pipelineID }: { pipelineID: number }) => {
  return agent.post(`/api/cicds/${pipelineID}/actions/rerun`).then((response: any) => response.body);
};

export const getExecuteRecords = (params: BUILD.IGetExecRecordsReq): IPagingResp<BUILD.ExecuteRecord> => {
  return agent
    .get('/api/cicds')
    .query(params)
    .then((response: any) => response.body);
};

export const updateTaskEnv = ({ pipelineID, taskAlias, taskID, disabled }: BUILD.ITaskUpdatePayload) => {
  // const paramDisable = disabled || undefined; // recover it when add pause
  return agent
    .put(`/api/cicds/${pipelineID}`)
    .send({ taskOperates: [{ taskID, taskAlias, disable: disabled }] })
    .then((response: any) => response.body);
};

export const addPipeline = ({ appId, ...rest }: Merge<BUILD.CreatePipelineBody, { appId: number }>) => {
  return agent
    .post('/api/cicds')
    .send({ ...rest, appID: Number(appId), source: 'dice', pipelineYmlSource: 'gittar' })
    .then((response: any) => response.body);
};

export const getComboPipelines = ({
  appId,
  branches,
  sources,
}: {
  appId: number;
  branches?: string;
  sources?: string;
}) => {
  return agent
    .get('/api/cicds/actions/app-invoked-combos')
    .query({ appID: appId, branches, sources })
    .then((response: any) => response.body);
};

export const batchCreateTask = ({ appId, batchPipelineYmlPaths }: any) => {
  return agent
    .post('/api/pipelines/actions/batch-create')
    .send({ appID: +appId, autoRun: true, batchPipelineYmlPaths, branch: 'master', source: 'bigdata' })
    .then((response: any) => response.body);
};

export const getPipelineLog = (query: BUILD.IPipelineLogQuery): IPagingResp<BUILD.IPipelineLog> => {
  return agent
    .get('/api/task-error/actions/list')
    .query(query)
    .then((response: any) => response.body);
};

export const getPipelineYmlList = (query: BUILD.PipelineYmlListQuery): string[] => {
  return agent
    .get('/api/cicds/actions/pipelineYmls')
    .query(query)
    .then((response: any) => response.body);
};

// 根据pipelineId获取节点id
export const getINodeByPipelineId = (query: { pipelineId: string }) => {
  return agent
    .get('/api/cicd-pipeline/filetree/actions/get-inode-by-pipeline')
    .query(query)
    .then((response: any) => response.body);
};
