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
import ReactDOM from 'react-dom';
import { map, get, minBy, maxBy, compact, difference, find } from 'lodash';
// @ts-ignore
import Snap from 'snapsvg-cjs';
import { externalKey } from './utils';

export const renderCategoryBox = (
  { categoryBox, linkDownDistance = 0, linkTopDistance = 0 }: any,
  snap: any,
  external: any,
) => {
  const list = map(categoryBox);
  const minStartY = get(minBy(list, 'startY'), 'startY', 0) as number;
  const maxEndY = get(maxBy(list, 'endY'), 'endY', 0) as number;
  const BoxComp = external.boxEle;
  map(categoryBox, (posData: any, key: string) => {
    const { startX, startY, endX, type, endY, groupType, addonStart, addonEnd } = posData;
    let pos = { startX, startY: minStartY - linkTopDistance, endX, endY: maxEndY + linkDownDistance };
    if (type === 'sub') {
      if (groupType === 'addon') {
        pos = {
          startX,
          startY: addonStart ? minStartY : startY,
          endX,
          endY: addonEnd ? maxEndY : endY,
        };
      } else {
        pos = { startX, startY, endX, endY };
      }
    }
    const fobjectSVG = `<foreignObject id="${key}" class="node-carrier" x="${pos.startX}" y="${pos.startY}" width="${
      pos.endX - pos.startX
    }" height="${pos.endY - pos.startY}">
    </foreignObject>`;
    const box = Snap.parse(fobjectSVG);
    snap.append(box);

    ReactDOM.render(<BoxComp name={key} pos={pos} />, document.getElementById(`${key}`));
  });
};

export const renderNodes = (
  { nodeMap, groupNodeMap, groupChart }: any,
  snap: any,
  external: any,
  chartConfig: any,
  distance = { disX: 0, disY: 0 },
) => {
  const NodeComp = external.nodeEle;
  const { disX, disY } = distance;
  map(nodeMap, (node: TOPOLOGY.INode) => {
    const {
      NODE: { width, height },
    } = chartConfig;
    const {
      [externalKey]: { x, y, uniqName },
    } = node as any; // x,y为中心点
    const startX = x - width / 2 + disX;
    const startY = y - height / 2 + disY;
    const nodeId = uniqName;
    const fobjectSVG = `<foreignObject id="${nodeId}" class="node-carrier" x="${startX}" y="${startY}" width="${width}" height="${height}">
    </foreignObject>`;
    // g标签上加id，用于设置opcity属性（兼容safari）
    const g = snap.g().attr({ id: `${nodeId}-g` });
    const f = Snap.parse(fobjectSVG);
    g.append(f);
    // write append node as a React Component
    ReactDOM.render(
      <NodeComp
        {...external}
        nodeStyle={chartConfig.NODE}
        node={node}
        onHover={() => onHover(node)}
        outHover={() => outHover(node)}
        onClick={() => clickNode(node)}
      />,
      document.getElementById(`${nodeId}`),
    );
  });
  // 获取关联节点
  const getRelativeNodes = (_node: TOPOLOGY.INode) => {
    const { category, parents: curParents = [] } = _node;
    // 微服务特殊需求
    if (category === 'microservice') {
      const fullParents = map(curParents, 'id');
      const mRelativeNode: string[] = [];
      const mUnRelativeNode: string[] = [];
      map(groupNodeMap, (item) => {
        const {
          parents,
          [externalKey]: { uniqName },
          id,
        } = item;
        const beParentId = map(parents, 'id');
        if (fullParents.includes(id) || beParentId.includes(_node.id)) {
          mRelativeNode.push(uniqName);
        } else {
          mUnRelativeNode.push(uniqName);
        }
      });
      return {
        relativeNode: mRelativeNode,
        unRelativeNode: mUnRelativeNode,
      };
    }

    let relativeNode: string[] = [];
    const unRelativeNode: string[] = [];
    const curNodeName = _node[externalKey].uniqName as string;
    map(nodeMap, (item: TOPOLOGY.INode) => {
      const {
        parents = [],
        [externalKey]: { uniqName },
      } = item;
      const parentNames = compact(
        map(parents, (p: TOPOLOGY.INode) => get(nodeMap, `${p.id}.${externalKey}.uniqName`, '')),
      );
      if (uniqName === curNodeName) {
        relativeNode = relativeNode.concat(parentNames).concat(uniqName);
      } else if (parentNames.includes(curNodeName)) {
        relativeNode.push(uniqName);
      } else {
        unRelativeNode.push(uniqName);
      }
    });
    const allNodeUniqName = map(nodeMap, (item: TOPOLOGY.INode) => item[externalKey].uniqName);
    return {
      relativeNode,
      unRelativeNode: difference(allNodeUniqName, relativeNode),
    };
  };
  // 获取关联links
  const getRelativeLinks = (_node: TOPOLOGY.INode) => {
    // 微服务特殊需求
    if (_node.category === 'microservice') {
      const allGroupLinks = groupChart.selectAll('.topology-link'); // 选出所有link;
      return {
        relativeLink: [],
        unRelativeLink: map(allGroupLinks),
      };
    }
    const allLinks = snap.selectAll('.topology-link'); // 选出所有link;
    const curNodeName = _node[externalKey].uniqName;
    const relativeLink: any[] = [];
    const unRelativeLink: any[] = [];
    map(allLinks, (link) => {
      const linkId = link.node.id;
      const [_x, source, target] = linkId.split('__');
      if ([source, target].includes(curNodeName)) {
        relativeLink.push(link);
      } else {
        unRelativeLink.push(link);
      }
    });
    return {
      relativeLink,
      unRelativeLink,
    };
  };

  const onHover = (_node: TOPOLOGY.INode) => {
    const { relativeLink, unRelativeLink } = getRelativeLinks(_node);
    const { relativeNode, unRelativeNode } = getRelativeNodes(_node);
    // 微服务特殊需求
    hoverAction(
      true,
      {
        relativeNode,
        unRelativeNode,
        relativeLink,
        unRelativeLink,
        hoverNode: _node,
      },
      _node.category === 'microservice' ? groupChart : snap,
      external,
      chartConfig,
    );
  };

  const outHover = (_node: TOPOLOGY.INode) => {
    const { relativeLink, unRelativeLink } = getRelativeLinks(_node);
    const { relativeNode, unRelativeNode } = getRelativeNodes(_node);

    // 微服务特殊需求
    hoverAction(
      false,
      {
        relativeNode,
        unRelativeNode,
        relativeLink,
        unRelativeLink,
      },
      _node.category === 'microservice' ? groupChart : snap,
      external,
      chartConfig,
    );
  };
  const clickNode = (_node: TOPOLOGY.INode) => {
    external.onClickNode(_node);
  };
};

// hover高亮效果
const emptyFun = () => {};
const hoverAction = (isHover: boolean, params: any, snap: any, external: any, chartConfig: any) => {
  const { relativeLink = [], relativeNode = [], unRelativeLink = [], unRelativeNode = [], hoverNode } = params;

  const { linkTextHoverAction = emptyFun, nodeHoverAction = emptyFun } = external;
  const { svgAttr } = chartConfig;
  if (isHover) {
    map(unRelativeNode, (name) => {
      snap.select(`#${name}-g`).node.classList.add('topology-node-fade');
      nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
        ...external,
        hoverNode,
        isRelative: false,
      });
    });
    map(relativeNode, (name) => {
      snap.select(`#${name}-g`).node.classList.add('topology-node-focus');
      nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
        ...external,
        hoverNode,
        isRelative: true,
      });
    });
    map(relativeLink, (link: any) => {
      const { [externalKey]: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      // 线上text
      const textId = `text__${sourceNode[externalKey].uniqName}__${targetNode[externalKey].uniqName}`;

      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        if (hoverNode) {
          curText.node.classList.add('topology-link-text-focus');
          linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
            ...external,
            isRelative: true,
            hoverNode,
            targetNode,
            sourceNode,
            hoverNodeExternal: get(hoverNode, externalKey),
          });
        }
      }
      // link.attr({ ...svgAttr.polylineFoc });
    });
    map(unRelativeLink, (link: any) => {
      const { [externalKey]: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      const textId = `text__${sourceNode[externalKey].uniqName}__${targetNode[externalKey].uniqName}`;
      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        curText.node.classList.add('topology-link-text-fade');
        linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
          ...external,
          isRelative: false,
          hoverNode,
          targetNode,
          sourceNode,
          hoverNodeExternal: get(hoverNode, externalKey),
        });
      }
      link.attr({ ...svgAttr.polylineFade });
    });
  } else {
    map(unRelativeNode, (name) => {
      snap.select(`#${name}-g`).node.classList.remove('topology-node-fade');
      nodeHoverAction &&
        nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
          ...external,
          hoverNode,
          isRelative: false,
        });
    });
    map(relativeNode, (name) => {
      snap.select(`#${name}-g`).node.classList.remove('topology-node-focus');
      nodeHoverAction &&
        nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
          ...external,
          hoverNode,
          isRelative: true,
        });
    });
    map(relativeLink, (link: any) => {
      const { [externalKey]: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      // 线上text
      const textId = `text__${sourceNode[externalKey].uniqName}__${targetNode[externalKey].uniqName}`;
      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        curText.node.classList.remove('topology-link-text-focus');
        linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
          ...external,
          isRelative: false,
          hoverNode,
          targetNode,
          sourceNode,
          hoverNodeExternal: get(hoverNode, externalKey),
        });
      }
      link.attr({ ...svgAttr.polyline });
    });
    map(unRelativeLink, (link: any) => {
      const { [externalKey]: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      const textId = `text__${sourceNode[externalKey].uniqName}__${targetNode[externalKey].uniqName}`;
      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        curText.node.classList.remove('topology-link-text-fade');
        linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
          ...external,
          isRelative: false,
          hoverNode,
          targetNode,
          sourceNode,
          hoverNodeExternal: get(hoverNode, externalKey),
        });
      }
      link.attr({ ...svgAttr.polyline });
    });
  }
};

// 渲染link
export const renderLinks = ({ links, nodeMap }: TOPOLOGY.ILinkRender, snap: any, external: any, chartConfig: any) => {
  const { svgAttr } = chartConfig;
  const LinkComp = external.linkTextEle;
  const startMarker = snap.circle(3, 3, 3).attr({ fill: '#333' }).marker(0, 0, 8, 8, 3, 3);
  const endMarker = snap
    .image('/images/zx.svg', 0, 0, 10, 10)
    .attr({ transform: 'roate(-90deg)' })
    .marker(0, 0, 10, 10, 5, 5);

  map(links, (link: any) => {
    const {
      [externalKey]: { id, posArr, linkData, sourceNode, targetNode },
    } = link;
    const [_x, source, target] = id.split('__');
    const textData: any = find(targetNode.parents, { id: sourceNode.id });
    const textId = `text__${source}__${target}`;

    // g标签上加id，用于设置opcity属性（兼容safari）
    const g = snap.g().attr({ id: `${textId}-g` });

    const { x: textX, y: textY, z: textZ, textUnderLine } = getLinkTextPos(posArr, chartConfig);
    let attrObj = {};
    if (textZ !== 0) {
      attrObj = {
        transform: `rotate(${textZ}deg)`,
      };
    }

    const fobjectSVG = `<foreignObject id="${`${textId}`}" class="line-text-carrier" x="${textX - 25}" y="${
      textY - 40
    }" width="${50}" height="${80}"></foreignObject>`;
    const text = Snap.parse(fobjectSVG);
    const in_g = snap.g();
    in_g.append(text).attr({ ...attrObj });
    g.append(in_g);

    const onHover = (_this: any) => {
      const allNodeUniqName = map(nodeMap, (item: TOPOLOGY.INode) => item[externalKey].uniqName);
      const unRelativeNode = difference(allNodeUniqName, [source, target]);
      const relativeNode = [source, target];

      const allLinks = snap.selectAll('.topology-link'); // 选出所有link;
      hoverAction(
        true,
        {
          unRelativeNode,
          relativeNode,
          unRelativeLink: difference(allLinks, [_this]),
          relativeLink: [_this],
        },
        snap,
        external,
        chartConfig,
      );
    };
    const outHover = (_this: any) => {
      _this.attr({ ...svgAttr.polyline });
      const allNodeUniqName = map(nodeMap, (item: TOPOLOGY.INode) => item[externalKey].uniqName);
      const unRelativeNode = difference(allNodeUniqName, [source, target]);
      const relativeNode = [source, target];
      const allLinks = snap.selectAll('.topology-link'); // 选出所有link;
      hoverAction(
        false,
        {
          unRelativeNode,
          relativeNode,
          unRelativeLink: difference(allLinks, [_this]),
          relativeLink: [_this],
        },
        snap,
        external,
        chartConfig,
      );
    };

    const l = snap
      .polyline(...posArr)
      .attr({
        id,
        ...svgAttr.polyline,
        markerEnd: endMarker,
        markerStart: startMarker,
      })
      .hover(
        function () {
          onHover(this);
        },
        function () {
          outHover(this);
        },
      );

    l[externalKey] = { linkData, posArr, sourceNode, targetNode };
    g.append(l);
    // 渲染线上文字，并添加hover事件
    ReactDOM.render(
      <LinkComp
        id={`${textId}__text`}
        data={textData}
        textUnderLine={textUnderLine}
        onHover={() => onHover(l)}
        outHover={() => outHover(l)}
      />,
      document.getElementById(`${textId}`),
    );
  });
};

const getLinkTextPos = (pos: number[], chartConfig: any) => {
  const { direction } = chartConfig;
  const len = pos.length;
  let z = 0;
  let [x, y] = [pos[len - 4], pos[len - 3]];
  let textUnderLine = false;
  if (direction === 'horizontal') {
    if (len === 8) {
      // 4点线，2折: __/————\__
      if (pos[1] === pos[3] && pos[3] === pos[5]) {
        const centerDisX = pos[6] - pos[4];
        const centerDisY = pos[7] - pos[5];
        x = pos[centerDisX > 0 ? 6 : 4] - Math.abs(centerDisX / 2);
        y = pos[centerDisY > 0 ? 7 : 5] - Math.abs(centerDisY / 2);
        z = (Math.atan2(pos[5] - pos[7], pos[4] - pos[6]) / Math.PI) * 180 + 180;
      } else {
        const centerDis = pos[4] - pos[2];
        x = pos[centerDis > 0 ? 4 : 2] - Math.abs(centerDis / 2);
        y = pos[3];
        textUnderLine = pos[1] < pos[3];
      }
    } else if (len === 6) {
      // 3点线
      if (pos[5] === pos[1] && pos[1] !== pos[3]) {
        // 1折对称：\/
        y = pos[3];
        textUnderLine = pos[3] > pos[1];
      } else if (pos[5] === pos[1] && pos[1] === pos[5]) {
        // 0折：————
        [x, y] = [pos[2], pos[3]];
      } else if (pos[1] !== pos[3]) {
        // 1折: ——\，文字在折线段中点
        const centerDisX = pos[2] - pos[0];
        const centerDisY = pos[3] - pos[1];
        x = pos[centerDisX > 0 ? 2 : 0] - Math.abs(centerDisX / 2);
        y = pos[centerDisY > 0 ? 3 : 1] - Math.abs(centerDisY / 2);
        z = (Math.atan2(pos[1] - pos[3], pos[0] - pos[2]) / Math.PI) * 180 + 180;
      }
    }
  } // TODO:vertical
  return { x, y, z, textUnderLine };
};
