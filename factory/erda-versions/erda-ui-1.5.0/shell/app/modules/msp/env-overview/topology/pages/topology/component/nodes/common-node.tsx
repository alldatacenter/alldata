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
import { Handle, NodeProps, Position } from 'react-flow-renderer';
import './common-node.scss';

export interface IProps extends NodeProps<TOPOLOGY.TopoNode> {
  showRuntime?: boolean;
  className?: string;
  children: (data: IProps['data']['metaData']) => JSX.Element;
  onMouseMoving?: (data: TOPOLOGY.TopoNode, flag: 'in' | 'out') => void;
  onClick?: (data: IProps['data']['metaData']) => void;
}

const CommonNode = ({ isConnectable, data, children, className, onMouseMoving, onClick }: IProps) => {
  const { isRoot, isLeaf, metaData, hoverStatus, selectStatus } = data;
  const timer = React.useRef(Date.now());
  const handleMouseEnter = () => {
    onMouseMoving?.(data, 'in');
  };

  const handleMouseLeave = () => {
    onMouseMoving?.(data, 'out');
  };

  const handleClick = () => {
    if (Date.now() - timer.current > 300) {
      return;
    }
    onClick?.(metaData);
  };
  return (
    <>
      {isRoot ? null : (
        <Handle className="node-handle-start" type="target" position={Position.Left} isConnectable={isConnectable} />
      )}
      <div
        className={`topology-common-node ${[hoverStatus, selectStatus].includes(-1) ? 'opacity-30' : ''} ${
          className ?? ''
        }`}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        onMouseDown={() => {
          timer.current = Date.now();
        }}
        onMouseUp={handleClick}
      >
        {children(metaData)}
      </div>
      {isLeaf ? null : (
        <Handle className="node-handle-end" type="source" position={Position.Right} isConnectable={isConnectable} />
      )}
    </>
  );
};

export default CommonNode;
