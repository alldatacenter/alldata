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

import { get, set } from 'lodash';

export const resetChartConfig = (chartConfig: any) => {
  const { showBox, boxMargin } = chartConfig;
  const reBoxMargin = showBox ? boxMargin : { x: 0, y: 0 };
  return { ...chartConfig, boxMargin: reBoxMargin };
};

export const externalKey = '_external';
export const getTopologyExternal = (node: any, key?: string) => {
  return get(node, key ? `${externalKey}.${key}` : externalKey, {});
};

interface IParent {
  id: string;
}
interface IData {
  [pro: string]: any;
  id: string;
  group?: string;
  subGroup?: string;
  groupLevel?: number;
  parents: IParent[];
}
export const setTopologyExternal = (node: any, data: IData) => {
  const curExternal = getTopologyExternal(node);
  const defaultData = { group: 'topology', subGroup: 'topology', groupLevel: 1 };
  return set(node, externalKey, { ...defaultData, ...curExternal, ...data });
};
