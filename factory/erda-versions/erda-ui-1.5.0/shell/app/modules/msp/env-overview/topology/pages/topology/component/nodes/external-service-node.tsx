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
import CommonNode, { IProps } from 'msp/env-overview/topology/pages/topology/component/nodes/common-node';
import { NodeProps } from 'react-flow-renderer';
import ErdaIcon from 'common/components/erda-icon';
import './index.scss';
import { formatNumber } from '../../utils';

const ExternalServiceNode: React.FC<
  NodeProps<TOPOLOGY.TopoNode> & { onMouseMoving: IProps['onMouseMoving']; onClick: IProps['onClick'] }
> = (props) => {
  return (
    <CommonNode {...props}>
      {(data: TOPOLOGY.TopoNode['metaData']) => {
        const { rps } = data.metric;
        return (
          <div className="external-service-node">
            <div className="h-full">
              <div className="h-full count relative flex justify-center items-center">
                <ErdaIcon type="qita" className="absolute z-0" size={60} />
                <div className="text-white">
                  <div className="text-center">{formatNumber(rps)}</div>
                  <div className="text-center text-xs text-white-6 font-light unit">reqs/s</div>
                </div>
              </div>
            </div>
          </div>
        );
      }}
    </CommonNode>
  );
};

export default ExternalServiceNode;
