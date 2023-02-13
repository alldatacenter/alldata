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

import { AddNode, StartNode, AddRow, PipelineNode, EndNode } from './nodes';
import { externalKey } from './chart/config';

export { externalKey };

export enum NodeType {
  addNode = 'addNode',
  addRow = 'addRow',
  startNode = 'startNode',
  endNode = 'endNode',
}

export const NodeEleMap = {
  addNode: AddNode,
  startNode: StartNode,
  endNode: EndNode,
  addRow: AddRow,
  pipeline: PipelineNode,
};

// 节点大小
export const CHART_NODE_SIZE = {
  pipeline: {
    WIDTH: 280,
    HEIGHT: 170,
  },
  addRow: {
    WIDTH: 20,
    HEIGHT: 20,
  },
  startNode: {
    WIDTH: 80,
    HEIGHT: 32,
  },
  endNode: {
    WIDTH: 80,
    HEIGHT: 32,
  },
};
