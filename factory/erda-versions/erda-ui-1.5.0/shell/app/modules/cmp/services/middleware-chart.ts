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

export interface IChartMeta {
  name: string;
  title: string;
  parameters: any;
}

export interface IChartDataQuery {
  [propName: string]: any;
  name: string;
  start: number;
  end: number;
  filter_cluster_name: string;
  filter_addon_id: number;
  points: number;
}

export const getChartMeta = (params: { type: string }): IChartMeta[] => {
  return agent
    .get('/api/chart/meta')
    .query(params)
    .then((response: any) => response.body);
};

export const getContainerChart = ({ name, ...rest }: IChartDataQuery) => {
  return agent
    .get(`/api/system/addon/metrics/${name}/histogram`)
    .query(rest)
    .then((response: any) => response.body);
};
