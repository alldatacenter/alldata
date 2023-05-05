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

interface ICommonQuery {
  projectId: number;
  env: string;
  az: string;
  interfacename: string;
  tenantId: string;
}

interface IDubboChartQuery extends ICommonQuery {
  start: number;
  end: number;
  point: number;
  chartType: string;
}

export const getDubboDetailTime = ({ env, interfacename, projectId, ...rest }: ICommonQuery) => {
  return agent
    .get(`/api/tmc/mesh/timestamp/${interfacename}/${projectId}/${env}`)
    .query(rest)
    .then((response: any) => response.body);
};

export const getDubboDetailChart = ({ chartType, env, interfacename, projectId, ...rest }: IDubboChartQuery) => {
  return agent
    .get(`/api/tmc/mesh/${chartType}/${interfacename}/${projectId}/${env}`)
    .query(rest)
    .then((response: any) => response.body);
};
