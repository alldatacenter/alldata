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
import ReactFlow, {
  Elements,
  isNode,
  Node,
  Position,
  ReactFlowProvider,
  removeElements,
  useZoomPanHelper,
} from 'react-flow-renderer';
import dagre from 'dagrejs';
import { genEdges, genNodes } from 'msp/env-overview/topology/pages/topology/utils';
import customerNode from './nodes';
import FloatingEdge from 'msp/env-overview/topology/pages/topology/component/floating-edge';
import FloatingConnectionLine from 'msp/env-overview/topology/pages/topology/component/floating-edge/connection-line';
import ErdaIcon from 'common/components/erda-icon';
import { useUpdateEffect } from 'react-use';
import { INodeKey } from './topology-overview';

const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const nodeExtent = [
  [0, 0],
  [1000, 1000],
];

export interface ITopologyRef {
  selectNode: (metaData: Omit<TOPOLOGY.INode, 'parents'>) => void;
  cancelSelectNode: () => void;
}

interface IProps {
  defaultZoom?: number;
  allowScroll?: boolean;
  filterKey: INodeKey;
  data: { nodes: TOPOLOGY.INode[] };
  clockNode?: (data: TOPOLOGY.TopoNode['metaData']) => void;
  topologyRef: React.Ref<ITopologyRef>;
}

const genEle = (nodes: TOPOLOGY.INode[], filterKey: INodeKey) => {
  let edge = genEdges(nodes);
  let node = genNodes(nodes, edge);
  switch (filterKey) {
    case 'unhealthyService':
      edge = edge.filter((t) => t.data?.isUnhealthy && t.data.isService);
      node = node.filter((t) => t.data?.isUnhealthy && t.data.isService);
      break;
    case 'addon':
      edge = edge.filter((t) => t.data?.isAddon);
      node = node.filter((t) => t.data?.isAddon);
      break;
    case 'service':
      edge = edge.filter((t) => t.data?.isService);
      node = node.filter((t) => t.data?.isService);
      break;
    case 'circularDependencies':
      edge = edge.filter((t) => t.data?.isCircular);
      node = node.filter((t) => t.data?.isCircular);
      break;
    case 'freeService':
      edge = [];
      node = node.filter((t) => t.data?.parentCount === 0 && t.data.childrenCount === 0 && t.data.isService);
      break;
  }
  return { node, edge };
};

/**
 * @description calculate node position
 * @see https://github.com/dagrejs/dagre/wiki
 */
const calculateLayout = (
  list: Elements,
): [Elements, { width: number; height: number; maxY: number; maxX: number; minX: number; minY: number }] => {
  let width = 0;
  let height = 0;
  let minX = Number.MAX_SAFE_INTEGER;
  let minY = Number.MAX_SAFE_INTEGER;
  dagreGraph.setGraph({ rankdir: 'LR' });
  list.forEach((el) => {
    if (isNode(el)) {
      dagreGraph.setNode(el.id, { width: 150, height: 80 });
    } else {
      dagreGraph.setEdge(el.source, el.target);
    }
  });
  dagre.layout(dagreGraph, { weight: 2 });
  let layoutElements = list.map((el) => {
    const temp: Partial<Node> = {};
    if (isNode(el)) {
      // get node coordinates
      const nodeWithPosition = dagreGraph.node(el.id);
      temp.targetPosition = Position.Left;
      temp.sourcePosition = Position.Right;
      const x = (nodeWithPosition.x + Math.random() / 1000) * 0.75;
      const y = nodeWithPosition.y;
      width = width < x ? x : width;
      height = height < y ? y : height;
      minX = minX < x ? minX : x;
      minY = minY < y ? minY : y;
      temp.position = { x, y };
    }
    return { ...el, ...temp };
  });
  // horizontal offset：prevents the leftmost node from being outside the view. The reduction of 50 is to prevent the node from overlapping the border of the view
  const horizontalOffset = minX - 50;
  // vertical offset：prevents the bottommost node from being outside the view. The reduction of 50 is to prevent the node from overlapping the border of the view
  const verticalOffset = minY - 50;
  layoutElements = layoutElements.map((el) => {
    if (isNode(el)) {
      return {
        ...el,
        position: {
          ...el.position,
          x: el.position.x - horizontalOffset,
          y: el.position.y - verticalOffset,
        },
      };
    } else {
      return el;
    }
  });
  return [
    layoutElements,
    {
      width: width - horizontalOffset,
      height: height - verticalOffset,
      maxY: height - verticalOffset,
      maxX: width - horizontalOffset,
      minX,
      minY,
    },
  ];
};

const TopologyComp = ({
  data,
  filterKey = 'node',
  clockNode,
  allowScroll = true,
  defaultZoom = 0.8,
  topologyRef,
}: IProps) => {
  const topologyData = React.useRef(genEle(data.nodes, filterKey));
  const layoutData = React.useRef<Elements>([]);
  const wrapperRaf = React.useRef<HTMLDivElement>();
  const { node, edge } = topologyData.current;
  const initElement: Elements = [...node, ...edge];
  const [elements, setElements] = React.useState<Elements>(initElement);
  const { zoomIn, zoomOut } = useZoomPanHelper();
  const onElementsRemove = (elementsToRemove: Elements) => setElements((els) => removeElements(elementsToRemove, els));

  const setFlowConfig = (list: Elements) => {
    const [ele, wrapperSize] = calculateLayout(list);
    if (wrapperRaf.current && allowScroll) {
      // 200: prevents nodes from being covered by borders
      wrapperRaf.current.style.height = `${wrapperSize.height + 200}px`;
      wrapperRaf.current.style.width = `${wrapperSize.width + 200}px`;
    }
    layoutData.current = ele;
    setElements(layoutData.current);
  };

  useUpdateEffect(() => {
    const temp = genEle(data.nodes, filterKey);
    topologyData.current = temp;
    setFlowConfig([...temp.node, ...temp.edge]);
  }, [data.nodes, filterKey]);

  const layout = () => {
    setFlowConfig(elements);
  };

  const changeNodeStatus = (id: string, key: string) => {
    let newLayoutData = layoutData.current;
    const originData = topologyData.current;
    const prevNodeIds = originData.edge.filter((t) => t.target === id).map((t) => t.source);
    const nextNodeIds = originData.edge.filter((t) => t.source === id).map((t) => t.target);
    newLayoutData = newLayoutData.map((item) => {
      if (isNode(item)) {
        return {
          ...item,
          data: {
            ...item.data,
            [key]: [...prevNodeIds, ...nextNodeIds, id].includes(item.id) ? 1 : -1,
          },
        };
      } else {
        return {
          ...item,
          data: {
            ...item.data,
            [key]: item.id.includes(id) ? 1 : -1,
          },
        };
      }
    });
    return newLayoutData;
  };

  const handleSelect = ({ id }: Omit<TOPOLOGY.INode, 'parents'>) => {
    const newLayoutData = changeNodeStatus(id, 'selectStatus');
    layoutData.current = newLayoutData;
    setElements(newLayoutData);
  };

  React.useImperativeHandle(
    topologyRef,
    () => {
      return {
        selectNode: handleSelect,
        cancelSelectNode: () => {
          const newLayoutData = layoutData.current.map((item) => {
            return {
              ...item,
              data: {
                ...item.data,
                selectStatus: 0,
              },
            };
          });
          layoutData.current = newLayoutData;
          setElements(newLayoutData);
        },
      };
    },
    [],
  );

  const nodeTypes = customerNode(
    (currentNode, flag) => {
      const { id } = currentNode.metaData;
      let newLayoutData = layoutData.current;
      if (flag === 'in') {
        newLayoutData = changeNodeStatus(id, 'hoverStatus');
      }
      setElements(newLayoutData);
    },
    (metaData) => {
      if (clockNode) {
        handleSelect(metaData);
        clockNode(metaData);
      }
    },
  );

  return (
    <>
      <div className={`h-full w-full relative ${allowScroll ? 'overflow-auto scroll-bar-dark' : ''}`}>
        <div className={`min-h-full min-w-full ${allowScroll ? '' : 'h-full w-full'}`} ref={wrapperRaf}>
          <ReactFlow
            elements={elements}
            nodeTypes={nodeTypes}
            onElementsRemove={onElementsRemove}
            edgeTypes={{
              float: FloatingEdge,
            }}
            preventScrolling={false}
            zoomOnScroll={false}
            connectionLineComponent={FloatingConnectionLine}
            nodeExtent={nodeExtent}
            defaultZoom={defaultZoom}
            minZoom={0.2}
            maxZoom={2}
            onLoad={layout}
          />
        </div>
      </div>
      <div className="zoom-buttons absolute bottom-4 right-4 h-8 w-20 flex z-10">
        <div
          className="cursor-pointer w-9 flex justify-center items-center mr-0.5 bg-white-1 text-white-4 hover:text-white"
          onClick={() => {
            zoomOut();
          }}
        >
          <ErdaIcon type="minus" size={12} />
        </div>
        <div
          className="cursor-pointer w-9 flex justify-center items-center bg-white-1 text-white-4 hover:text-white"
          onClick={() => {
            zoomIn();
          }}
        >
          <ErdaIcon type="plus" size={12} />
        </div>
      </div>
    </>
  );
};
export default React.forwardRef((props: IProps, ref?: React.Ref<ITopologyRef>) => {
  const refs = React.useRef<ITopologyRef>(null);
  return (
    <ReactFlowProvider>
      <TopologyComp {...props} topologyRef={ref ?? refs} />
    </ReactFlowProvider>
  );
});
