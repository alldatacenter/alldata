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

// @ts-ignore
import { CHART_CONFIG } from './config';
import { renderNodes, renderLinks } from './render-utils';
import './topology-utils.scss';

interface InodeGroupItem {
  boxWidth: number;
  boxHeight: number;
  links: {
    source: string;
    target: string;
    hasReverse?: boolean;
  }[];
  nodeMap: Obj;
  linkTopDistance: number;
}

/**
 * @param originData: 原始数据{name:xx,parents:[]}[]
 * @param snap: snap ref
 * @param g: sanp.group ref
 * @param external: config object(配置项)
 * @return
 *  containerWidth: svg图宽度
 *  containerHeight: svg图高度
 */
const chartConfig = { ...CHART_CONFIG };
export const renderTopology = (nodeGroup: InodeGroupItem[], snap: any, g: any, external: Obj = {}) => {
  let containerWidth = 0;
  let containerHeight = 0;

  nodeGroup.forEach((chartItem) => {
    const { boxWidth, boxHeight, links, nodeMap, linkTopDistance } = chartItem;
    const x = 0;
    const y = containerHeight;
    const vx = 0;
    const vy = -linkTopDistance; // viewBox坐标

    containerWidth = boxWidth > containerWidth ? boxWidth : containerWidth;
    containerHeight += boxHeight;
    // 在snap中创建独立的svg，并renderNodes和renderLinks
    const chart = snap.svg(x, y, boxWidth, boxHeight, vx, vy, boxWidth, boxHeight);
    g.append(chart);
    renderNodes({ nodeMap }, chart, external, chartConfig);
    renderLinks({ links, nodeMap }, chart, external, chartConfig);
  });
  return { containerWidth, containerHeight };
};
