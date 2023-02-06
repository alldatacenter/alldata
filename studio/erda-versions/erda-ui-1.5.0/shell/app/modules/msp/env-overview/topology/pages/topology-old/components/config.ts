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

// 画布常量定义
export const CHART_CONFIG = {
  direction: 'horizontal', // 画布方向: vertical | horizontal
  NODE: {
    width: 240, // 节点宽
    height: 150, // 节点高
    margin: {
      x: 140, // 节点横向间距
      y: 60, // 节点纵向间距
    },
  },
  LINK: {
    linkDis: 20, // 跨层级线间距
    linkRadius: 5, // 划线折角处的圆角弧度半径
  },
  padding: {
    // 单个独立图的padding
    x: 80,
    y: 80,
  },
  groupPadding: {
    x: 80,
    y: 80,
  },
  boxMargin: {
    x: 40,
    y: 40,
  },
  svgAttr: {
    polyline: { stroke: 'rgba(0, 0, 0, 0.4)', strokeWidth: 1, fill: 'none', class: 'topology-link', opacity: 1 },
    polylineFade: { opacity: 0.1 },
  },
  showBox: true, // 是否需要显示box
};
