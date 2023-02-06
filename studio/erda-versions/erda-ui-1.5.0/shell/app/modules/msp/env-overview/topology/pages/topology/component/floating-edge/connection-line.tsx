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

import React from 'react';

import { ConnectionLineComponentProps, getBezierPath, Node } from 'react-flow-renderer';

import { getEdgeParams } from './utils';

const FloatingConnectionLine: React.FC<ConnectionLineComponentProps> = ({
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  sourceNode,
}) => {
  if (!sourceNode) {
    return null;
  }

  const targetNode = {
    id: 'connection-target',
    __rf: { width: 1, height: 1, position: { x: targetX, y: targetY } },
  } as Node;

  const { sx, sy } = getEdgeParams(sourceNode, targetNode);
  const d = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition,
    targetPosition,
    targetX,
    targetY,
  });
  if (isNaN(sx) || isNaN(sy) || isNaN(targetX) || isNaN(targetY)) {
    return null;
  }

  return (
    <g>
      <path fill="none" stroke="#f00" strokeWidth={1.5} className="animated" d={d} />
      <circle cx={targetX} cy={targetY} fill="#fff" r={3} stroke="#f00" strokeWidth={1.5} />
    </g>
  );
};

export default FloatingConnectionLine;
