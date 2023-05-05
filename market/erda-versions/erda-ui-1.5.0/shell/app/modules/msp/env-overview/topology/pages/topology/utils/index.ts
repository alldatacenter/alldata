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
import { ArrowHeadType, Edge, Node } from 'react-flow-renderer';
import { cloneDeep, omit, uniqBy } from 'lodash';
import { getFormatter } from 'charts/utils';

export const servicesTypes = ['service'];
export const externalserviceTypes = ['externalservice'];
export const notAddonTypes = [
  ...servicesTypes,
  ...externalserviceTypes,
  'registercenter',
  'configcenter',
  'noticecenter',
  'apigateway',
  'internalservice',
];

export const isService = (type: string) => servicesTypes.includes(type.toLocaleLowerCase());
export const isAddon = (type: string) => !notAddonTypes.includes(type.toLocaleLowerCase());

const getNodeType = (type: string) => {
  const nodeType = type.toLocaleLowerCase();
  if (isAddon(nodeType)) {
    return 'addon';
  }
  return nodeType;
};

export const genNodes = (list: TOPOLOGY.INode[], edges: Edge[]): Node<TOPOLOGY.TopoNode>[] => {
  const nodes: Node<TOPOLOGY.TopoNode>[] = [];
  cloneDeep(list).forEach((item) => {
    const { parents = [], ...rest } = item;
    const children = edges.filter((t) => t.source === rest.id).map((t) => t.target);
    const childrenCount = children.length;
    const parent = edges.filter((t) => t.target === rest.id).map((t) => t.source);
    const parentCount = parent.length;
    const isCircular = parent.some((t) => children.includes(t));
    const isLeaf = !childrenCount;
    nodes.push({
      id: rest.id,
      type: getNodeType(rest.type),
      data: {
        isAddon: isAddon(rest.type),
        isService: isService(rest.type),
        isCircular,
        isUnhealthy: rest.metric.error_rate > 0,
        hoverStatus: 0,
        selectStatus: 0,
        isRoot: !parentCount,
        isParent: !isLeaf,
        isLeaf,
        childrenCount,
        parentCount,
        label: rest.name,
        metaData: rest,
      },
      position: {
        x: 0,
        y: 0,
      },
    });
  });
  return uniqBy(nodes, 'id');
};

export const edgeColor = {
  common: '#FFFFFF52',
  blurColor: '#FFFFFF1A',
  heightLight: '#FFFFFFCC',
};

export const genEdges = (data: TOPOLOGY.INode[]): Edge<TOPOLOGY.TopoEdge>[] => {
  const convert = (list: TOPOLOGY.INode[], edges: Edge<TOPOLOGY.TopoEdge>[]) => {
    cloneDeep(list).forEach((item) => {
      const { parents = [], ...rest } = item;
      if (parents.length) {
        parents.forEach((parent: TOPOLOGY.INode) => {
          const isCircular = !!list.find((t) => t.id === parent.id)?.parents.find((t) => t.id === rest.id);
          const parentInRoot = list.find((t) => t.id === parent.id) ?? ({} as TOPOLOGY.INode);
          // If neither node is healthy, it is unhealthy edge
          const isUnhealthy = rest.metric.error_rate > 0 && parentInRoot.metric?.error_rate > 0;
          edges.push({
            id: `${parent.id}-${rest.id}`,
            source: parent.id,
            target: rest.id,
            type: 'float',
            style: { stroke: edgeColor.common },
            arrowHeadType: ArrowHeadType.ArrowClosed,
            data: {
              isCircular,
              isUnhealthy,
              isAddon: isAddon(rest.type) && isAddon(parent.type),
              isService: isService(rest.type) && isService(parent.type),
              hoverStatus: 0,
              selectStatus: 0,
              source: omit(parent, 'parents'),
              target: rest,
            },
          });
        });
      }
    });
    return edges;
  };
  return convert(data, []);
};

export const formatNumber = (num: number) => {
  if (num >= 1000) {
    return getFormatter('NUMBER').format(num, 1);
  } else {
    return num || 0;
  }
};
