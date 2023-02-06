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

// @ts-ignore
import Snap from 'snapsvg-cjs';
import React from 'react';
import ReactDOM from 'react-dom';
import { get, maxBy, map, sumBy } from 'lodash';
import { externalKey, CHART_CONFIG, NodeType } from './config';
import { StartNode } from '../nodes/start-node';
import { EndNode } from '../nodes/end-node';
import { IData } from './yml-chart';

interface IChartConfig extends Omit<typeof CHART_CONFIG, 'MARGIN'> {
  NODE: {
    [pro: string]: { WIDTH: number; HEIGHT: number };
  };
  MARGIN: { X: number; Y: number };
}

interface IExternal {
  [pro: string]: any;
  nodeEleMap?: {
    [pro: string]: React.ReactNode;
  };
  editing: boolean;
}

export interface IExternalData {
  [externalKey]: {
    x: number;
    y: number;
    width: number;
    height: number;
    nodeId: string;
    nodeType: string;
    xIndex: number;
    yIndex: number;
  };
}

const NodeEleMap = {
  startNode: StartNode,
  endNode: EndNode,
};

export const renderSvgChart = (
  originData: IData[][],
  snap: any,
  g: any,
  chartConfig: IChartConfig,
  external: IExternal,
) => {
  const { nodeData, chartHeight, chartWidth } = getNodePostion(originData, chartConfig, external);
  const { linkData } = getLinkPosition(nodeData, chartConfig, external);
  const chart = snap.svg(0, 0, chartWidth, chartHeight, 0, 0, chartWidth, chartHeight);
  g.append(chart);

  renderNodes(nodeData, chart, chartConfig, external, chartWidth);
  renderLinks(linkData, chart, chartConfig, external);

  return { chartHeight, chartWidth };
};

const getNodePostion = (data: IData[][], chartConfig: IChartConfig, external: IExternal) => {
  const { PADDING, NODE, MARGIN } = chartConfig;
  const nodeData = [] as IData[][];
  const isEdit = external && external.editing;
  const boxSizeArr = map(data, (item) => {
    return {
      width:
        PADDING.X * 2 +
        (item.length - 1) * MARGIN.X +
        sumBy(
          item,
          (subItem: IData) => (get(NODE, `${subItem[externalKey].nodeType}.WIDTH`) as unknown as number) || 0,
        ),
      height: get(NODE, `${get(item, `[0].${externalKey}.nodeType`)}.HEIGHT`) as unknown as number,
      itemWidth: get(NODE, `${get(item, `[0].${externalKey}.nodeType`)}.WIDTH`) as unknown as number,
    };
  });
  const maxWidthBox = (maxBy(boxSizeArr, (item) => item.width) as Obj) || { width: 0, height: 0 };
  const dataLen = data.length;
  let chartHeight = PADDING.Y;
  map(data, (item, index) => {
    const curBoxSize = boxSizeArr[index];
    const curData = [] as any[];
    let xDis = (curBoxSize.width < maxWidthBox.width ? (maxWidthBox.width - curBoxSize.width) / 2 : 0) + PADDING.X;
    map(item, (subItem, subIndex) => {
      const curNode = NODE[subItem[externalKey].nodeType] as any;
      curData.push({
        ...subItem,
        [externalKey]: {
          ...(subItem[externalKey] || {}),
          x: xDis + curNode.WIDTH / 2,
          y: chartHeight + curNode.HEIGHT / 2,
          nodeId: `node-${index}-${subIndex}`,
          width: curNode.WIDTH,
          height: curNode.HEIGHT,
        },
      });
      xDis += curNode.WIDTH + MARGIN.X;
    });
    chartHeight += curBoxSize.height + (dataLen === index + 1 ? 0 : MARGIN.Y);
    nodeData.push(curData);
  });
  chartHeight += PADDING.Y;
  return {
    chartHeight,
    chartWidth: isEdit ? maxWidthBox.width + maxWidthBox.itemWidth + MARGIN.X : maxWidthBox.width,
    nodeData,
  } as {
    chartHeight: number;
    chartWidth: number;
    nodeData: IExternalData[][];
  };
};

const renderNodes = (nodeData: any[][], chart: any, chartConfig: IChartConfig, external: any, chartWidth: number) => {
  const { nodeEleMap = {}, chartId } = external || {};
  const AllNodeEleMap = {
    ...NodeEleMap,
    ...nodeEleMap,
  };
  const isEdit = external && external.editing;
  const { MARGIN } = chartConfig;
  map(nodeData, (nodeList, index) => {
    const curStartNode = get(nodeList, `[0].${externalKey}`);
    const listG = chart.g();
    if (isEdit && curStartNode.nodeType !== NodeType.startNode && curStartNode.nodeType !== NodeType.addRow) {
      const listGMask = chart
        .rect(0, curStartNode.y - curStartNode.height / 2, chartWidth, curStartNode.height)
        .attr({ fill: 'transparent' });
      listG
        .mouseover(() => {
          if (isEdit) {
            const addEle = document.getElementById(`${chartId}-_yml-node-add-${index}_`);
            if (addEle) {
              addEle.classList.replace('invisible', 'visible');
            }
          }
        })
        .mouseout(() => {
          if (isEdit) {
            const addEle = document.getElementById(`${chartId}-_yml-node-add-${index}_`);
            if (addEle) {
              addEle.classList.replace('visible', 'invisible');
            }
          }
        });
      listG.append(listGMask);
    }
    const nodeLen = nodeList.length;
    map(nodeList, (node, subIndex) => {
      const {
        [externalKey]: { x, y, nodeId, height, width, nodeType },
      } = node; // x,y为中心坐标
      const xPos = x - width / 2;
      const yPos = y - height / 2;
      const fobjectSVG = `<foreignObject id="${chartId}-${nodeId}" class="svg-model-node-carrier" x="${xPos}" y="${yPos}" width="${width}" height="${height}"></foreignObject>`;
      // g标签上加id，用于设置opcity属性（兼容safari）
      const g = chart.g().attr({ id: `${chartId}-${nodeId}-g` });
      const f = Snap.parse(fobjectSVG);
      g.append(f);
      listG.append(g);
      const NodeComp = AllNodeEleMap[nodeType];
      // write append node as a React Component
      NodeComp &&
        ReactDOM.render(<NodeComp {...external} data={node} />, document.getElementById(`${chartId}-${nodeId}`));
      if (
        isEdit &&
        nodeLen === subIndex + 1 &&
        ![NodeType.startNode, NodeType.endNode, NodeType.addRow].includes(curStartNode.nodeType)
      ) {
        // 编辑态，末尾追加添加节点
        const add_xPos = x + width / 2 + MARGIN.X;
        const add_yPos = y - height / 2;
        const addNodeId = `${chartId}-_yml-node-add-${index}_`;
        const addFobjectSVG = `<foreignObject id="${addNodeId}" class="svg-model-node-carrier invisible" x="${add_xPos}" y="${add_yPos}" width="${width}" height="${height}"></foreignObject>`;
        // g标签上加id，用于设置opcity属性（兼容safari）
        const addG = chart.g().attr({ id: `${chartId}-${nodeId}-g` });
        const addF = Snap.parse(addFobjectSVG);
        addG.append(addF);
        listG.append(addG);
        listG.append();
        // write append node as a React Component
        const AddNodeComp = AllNodeEleMap.addNode;
        ReactDOM.render(
          <AddNodeComp
            {...external}
            data={{
              [externalKey]: {
                nodeType: NodeType.addNode,
                xIndex: curStartNode.xIndex,
                yIndex: subIndex + 1,
              },
            }}
          />,
          document.getElementById(`${addNodeId}`),
        );
      }
    });
  });
};

const getLinkPosition = (data: IExternalData[][], chartConfig: IChartConfig, external: any = {}) => {
  const { chartId } = external;
  const linkData = [] as any[];
  const { LINK } = chartConfig;
  if (data.length > 1) {
    map(data, (item, index) => {
      const targetNodes = data[index + 1];
      map(item, (subItem) => {
        if (targetNodes) {
          const firstTarget = get(targetNodes, `[0].${externalKey}`);
          const lastTarget = get(targetNodes, `[${targetNodes.length - 1}].${externalKey}`);
          const centerX = firstTarget.x + (lastTarget.x - firstTarget.x) / 2;
          map(targetNodes, (targetNode) => {
            const startPos = subItem[externalKey];
            const endPos = targetNode[externalKey];
            const startAddRow = startPos.nodeType === NodeType.addRow;
            const endAddRow = endPos.nodeType === NodeType.addRow;
            let path = '';
            const heightDis = endPos.y - endPos.height / 2 - (startPos.y + startPos.height / 2);
            if (!startAddRow && !endAddRow) {
              // 正常节点之间连线
              path = `M${startPos.x} ${startPos.y + startPos.height / 2}`;
              if (startPos.x === centerX) {
                path += `L${startPos.x} ${startPos.y + startPos.height / 2 + heightDis / 2}`;
              } else if (startPos.x < centerX) {
                path += `
                  L${startPos.x} ${startPos.y + startPos.height / 2 + heightDis / 2 - LINK.RADIUS}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 0 ${startPos.x + LINK.RADIUS} ${
                  startPos.y + startPos.height / 2 + heightDis / 2
                }
                  L${centerX} ${startPos.y + startPos.height / 2 + heightDis / 2}
                `;
              } else if (startPos.x > centerX) {
                path += `
                  L${startPos.x} ${startPos.y + startPos.height / 2 + heightDis / 2 - LINK.RADIUS}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 1 ${startPos.x - LINK.RADIUS} ${
                  startPos.y + startPos.height / 2 + heightDis / 2
                }
                  L${centerX} ${startPos.y + startPos.height / 2 + heightDis / 2}
                `;
              }

              if (endPos.x === centerX) {
                path += `L${endPos.x} ${endPos.y - endPos.height / 2}`;
              } else if (endPos.x < centerX) {
                path += `
                  L${endPos.x + LINK.RADIUS} ${startPos.y + startPos.height / 2 + heightDis / 2}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 0 ${endPos.x} ${
                  startPos.y + startPos.height / 2 + heightDis / 2 + LINK.RADIUS
                }
                  L${endPos.x} ${endPos.y - endPos.height / 2}
                `;
              } else if (endPos.x > centerX) {
                path += `
                  L${endPos.x - LINK.RADIUS} ${startPos.y + startPos.height / 2 + heightDis / 2}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 1 ${endPos.x} ${
                  startPos.y + startPos.height / 2 + heightDis / 2 + LINK.RADIUS
                }
                  L${endPos.x} ${endPos.y - endPos.height / 2}
                `;
              }
            } else if (startPos.x === endPos.x) {
              // 节点x相等，则为垂直竖线
              path = `
                M${startPos.x} ${startPos.y + startPos.height / 2}
                L${endPos.x} ${endPos.y - endPos.height / 2}
              `;
            } else if (startPos.x < endPos.x) {
              // 目标在起点的右侧
              if (startAddRow) {
                // 起点为添加行节点： ▔▔|
                path = `
                  M${startPos.x + startPos.width / 2} ${startPos.y}
                  L${endPos.x - LINK.RADIUS} ${startPos.y}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 1 ${endPos.x} ${startPos.y + LINK.RADIUS}
                  L${endPos.x} ${endPos.y - endPos.height / 2}
                `;
              } else if (endAddRow) {
                // 终点为添加行节点: |__
                path = `
                  M${startPos.x} ${startPos.y + startPos.height / 2}
                  L${startPos.x} ${endPos.y - LINK.RADIUS}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 0 ${startPos.x + LINK.RADIUS} ${endPos.y}
                  L${endPos.x - endPos.width / 2} ${endPos.y}
                `;
              }
            } else if (startPos.x > endPos.x) {
              // 目标在起点左侧
              if (startAddRow) {
                // 起点为添加行节点： |▔▔
                path = `
                  M${startPos.x - startPos.width / 2} ${startPos.y}
                  L${endPos.x + LINK.RADIUS} ${startPos.y}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 0 ${endPos.x} ${startPos.y + LINK.RADIUS}
                  L${endPos.x} ${endPos.y - endPos.height / 2}
                `;
              } else if (endAddRow) {
                // 终点为添加行节点: __|
                path = `
                  M${startPos.x} ${startPos.y + startPos.height / 2}
                  L${startPos.x} ${endPos.y - LINK.RADIUS}
                  A${LINK.RADIUS} ${LINK.RADIUS} 0 0 1 ${startPos.x - LINK.RADIUS} ${endPos.y}
                  L${endPos.x + endPos.width / 2} ${endPos.y}
                `;
              }
            }
            linkData.push({
              sourceNode: subItem,
              targetNode,
              id: `${chartId}-${startPos.nodeId}_${endPos.nodeId}`,
              needStartMark: !startAddRow,
              needEndMark: !endAddRow,
              path,
            });
          });
        }
      });
    });
  }
  return {
    linkData,
  };
};
// 渲染link
const renderLinks = (linkData: any[], chart: any, chartConfig: IChartConfig, external: any) => {
  const { LINK } = chartConfig;
  const { startMarker, endMarker } = LINK;
  const markerStart = chart
    .circle(...startMarker.pos)
    .attr(startMarker.attr)
    .marker(...startMarker.marker);
  const markerEnd = chart
    .image(...endMarker.image)
    .attr(endMarker.attr)
    .marker(...endMarker.marker);
  map(linkData, (link) => {
    const { path, id, needStartMark, needEndMark } = link;
    chart.path(path).attr({
      id,
      ...LINK.ATTR,
      markerStart: needStartMark ? markerStart : undefined,
      markerEnd: needEndMark ? markerEnd : undefined,
    });
  });
};
