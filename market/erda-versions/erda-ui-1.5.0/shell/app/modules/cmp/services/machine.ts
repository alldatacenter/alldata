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

export const addMachine = (payload: ORG_MACHINE.IAddMachineBody): ORG_MACHINE.IClusterOperateRecord => {
  return agent
    .post('/api/nodes')
    .send(payload)
    .then((response: any) => response.body);
};

export const addCloudMachine = (payload: ORG_MACHINE.IAddCloudMachineBody): ORG_MACHINE.IAddCloudMachineResp => {
  return agent
    .post('/api/ops/cloud-nodes')
    .send(payload)
    .then((response: any) => response.body);
};

export const updaterMachineLabels = (payload: ORG_MACHINE.IMachineLabelBody): { recordID: number } => {
  return agent
    .post('/api/node-labels')
    .send(payload)
    .then((response: any) => response.body);
};

export interface IOpHistoryQuery {
  orgID: number;
  clusterName: string;
  pageNo: number;
  pageSize?: number;
  recordType?: string;
  scope?: string;
}
export const getClusterOperationHistory = (
  payload: IOpHistoryQuery,
): Promise<IPagingResp<ORG_MACHINE.IClusterOperateRecord>> => {
  return agent
    .get('/api/records')
    .query(payload)
    .then((response: any) => response.body);
};

export const getClusterOperationDetail = (id: string): ORG_MACHINE.IClusterOperateRecord => {
  return agent.get(`/api/records/${id}`).then((response: any) => response.body);
};

export const getClusterOperationTypes = (): ORG_MACHINE.IClusterOperateType[] => {
  return agent.get('/api/recordtypes').then((response: any) => response.body);
};
