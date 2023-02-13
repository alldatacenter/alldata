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

import React, { CSSProperties, FC, useMemo } from 'react';
import { EdgeProps, getBezierPath, useStoreState } from 'react-flow-renderer';
import { edgeColor } from 'msp/env-overview/topology/pages/topology/utils';

import { getEdgeParams } from './utils';

const FloatingEdge: FC<EdgeProps<TOPOLOGY.TopoEdge>> = ({ id, source, target, style, data }) => {
  const { selectStatus, hoverStatus } = data || {};
  const nodes = useStoreState((state) => state.nodes);

  const sourceNode = useMemo(() => nodes.find((n) => n.id === source), [source, nodes]);
  const targetNode = useMemo(() => nodes.find((n) => n.id === target), [target, nodes]);

  if (!sourceNode || !targetNode) {
    return null;
  }

  const { sx, sy, tx, ty, sourcePos, targetPos } = getEdgeParams(sourceNode, targetNode);

  const d = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  });

  const color = [selectStatus, hoverStatus].includes(1)
    ? edgeColor.heightLight
    : [selectStatus, hoverStatus].includes(-1)
    ? edgeColor.blurColor
    : edgeColor.common;

  const mId = `topology-edge-end-marked-${id}`;

  return (
    <g className="react-flow__connection">
      <defs>
        <marker id={mId} markerWidth="12.5" markerHeight="12.5" viewBox="-10 -10 20 20" orient="auto" refX="0" refY="0">
          <polyline
            stroke={color}
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1"
            fill={color}
            points="-5,-4 0,0 -5,4 -5,-4"
          />
        </marker>
      </defs>
      <path
        id={id}
        className="react-flow__edge-path"
        d={d}
        markerEnd={`url(#${mId})`}
        style={{ ...style, stroke: color } as CSSProperties}
      />
    </g>
  );
};
export default FloatingEdge;
