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
import ApiGatewayNode from './api-gateway-node';
import ServicesNode from './services-node';
import ExternalServiceNode from './external-service-node';
import AddonNode from './addon-node';
import { NodeProps } from 'react-flow-renderer';
import { IProps } from './common-node';

const customerNode = (onMouseMoving: IProps['onMouseMoving'], onClick: IProps['onClick']) => {
  return {
    apigateway: (props: NodeProps<TOPOLOGY.TopoNode>) => {
      return <ApiGatewayNode {...props} onMouseMoving={onMouseMoving} onClick={onClick} />;
    },
    service: (props: NodeProps<TOPOLOGY.TopoNode>) => {
      return <ServicesNode {...props} onMouseMoving={onMouseMoving} onClick={onClick} />;
    },
    externalservice: (props: NodeProps<TOPOLOGY.TopoNode>) => {
      return <ExternalServiceNode {...props} onMouseMoving={onMouseMoving} onClick={onClick} />;
    },
    addon: (props: NodeProps<TOPOLOGY.TopoNode>) => {
      return <AddonNode {...props} onMouseMoving={onMouseMoving} onClick={onClick} />;
    },
  };
};

export default customerNode;
