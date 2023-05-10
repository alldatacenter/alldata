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

export interface ISummary {
  podName: string;
  nodeName: string;
  clusterName: string;
  restartTotal: number;
  stateCode: number;
  terminatedReason: string;
  hostIP: string;
  namespace: string;
}

export interface IInstances {
  containerId: string;
  hostIP: string;
  podIP: string;
  startedAt: string;
  finishedAt: string;
  exitCode: number;
}

export interface IPodDetail {
  summary?: ISummary;
  instances?: IInstances[];
}

export const getPodDetail = (params: { name: string; timestamp: number; clusterName: string }): IPodDetail => {
  return agent
    .get('/api/system/pod/metrics')
    .query(params)
    .then((response: any) => response.body);
};
