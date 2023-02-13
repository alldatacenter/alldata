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

import { StartNode, EndNode } from '../nodes';
import zxGraySvg from 'app/images/zx-gray.svg';

export const externalKey = '_external_';

export enum NodeType {
  addNode = 'addNode',
  addRow = 'addRow',
  startNode = 'startNode',
  endNode = 'endNode',
}

export const NodeEleMap = {
  startNode: StartNode,
  endNode: EndNode,
};

// 节点大小
export const CHART_NODE_SIZE = {
  startNode: {
    WIDTH: 80,
    HEIGHT: 32,
  },
  endNode: {
    WIDTH: 80,
    HEIGHT: 32,
  },
};

export const CHART_CONFIG = {
  MARGIN: {
    NORMAL: {
      X: 60, // 节点左右间距
      Y: 60, // 节点上下间距
    },
    EDITING: {
      X: 60,
      Y: 20,
    },
  },
  PADDING: {
    X: 80, // 整个svg图的内边距，防止图顶着border
    Y: 60,
  },
  LINK: {
    RADIUS: 10, // 线的radius圆角
    ATTR: {
      stroke: '#B4B5BA',
      fill: 'transparent',
      strokeWidth: 1,
    },
    startMarker: {
      pos: [3, 3, 3],
      attr: { fill: '#bbb' },
      marker: [0, 0, 8, 8, 3, 3],
    },
    endMarker: {
      image: [zxGraySvg, 0, 0, 10, 10],
      attr: { transform: 'roate(-90deg)', fill: '#bbb' },
      marker: [0, 0, 10, 10, 5, 5],
    },
  },
};
