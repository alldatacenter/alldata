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
import {
  isArray,
  reduce,
  isEmpty,
  find,
  uniq,
  map,
  filter,
  merge,
  groupBy,
  max,
  maxBy,
  values,
  flatten,
  keys,
  get,
  min,
  minBy,
  difference,
  sortBy,
  cloneDeep,
  set,
} from 'lodash';
import { produce } from 'immer';
import { externalKey } from './utils';
// @ts-ignore
import Snap from 'snapsvg-cjs';

import './topology-utils.scss';

export interface INode {
  name: string;
  id: string;
  dashboardId: string;
  parent: string;
  metric: {
    rt: number;
    count: number;
    http_error?: number;
    error_rate?: number;
    running: number;
    stopped: number;
  };
  _external: INodeExternal;
  [pro: string]: any;
}

interface INodeExternal {
  x: number;
  y: number;
  deepth: number;
  uniqName: string;
  inTotal: number;
  outTotal: number;
  id: string;
}

interface INodeData {
  name: string;
  id: string;
  category: string;
  parents: Array<{
    id: string;
    metric: {
      rt: number;
      count: number;
      mqCount: number;
    };
    [pro: string]: any;
  }>;
}

// 画布常量定义
export const CANVAS = {
  direction: 'horizontal', // 画布方向: vertical | horizontal
  NODE: {
    width: 240, // 节点宽
    height: 150, // 节点高
    margin: {
      x: 200, // 节点横向间距
      y: 80, // 节点纵向间距
    },
  },
  LINK: {
    linkDis: 25, // 跨层级线间距
  },
  padding: {
    // 单个独立图的padding
    x: 100,
    y: 100,
  },
  boxMargin: {
    x: 80,
    y: 100,
  },
  svgAttr: {
    polyline: { stroke: 'rgba(0, 0, 0, 0.4)', strokeWidth: 1, fill: 'none', class: 'topology-link', opacity: 1 },
    polylineFade: { opacity: 0.1 },
  },
};

/**
 *
 * @param originData: 原始数据{name:xx,parents:[]}[]
 * @param snap: snap ref
 * @return
 *  containerWidth: svg图宽度
 *  containerHeight: svg图高度
 *
 */
const emptyFun = () => {};
export const renderTopology = (originData: INodeData[], snap: any, g: any, external: any) => {
  // 获取n个独立的节点组（n个独立的图）
  let containerWidth = 0;
  let containerHeight = 0;
  if (isEmpty(originData)) return { containerWidth, containerHeight };
  const microserviceChart = dataHandler.getMicroserviceChart(get(originData, 'nodes', []));

  const nodeGroup = dataHandler.getGroupChart(get(originData, 'nodes', []), get(originData, 'subGroupNodes', []));
  const marginToTop = get(microserviceChart, 'boxHeight', 0);
  const { direction } = CANVAS;
  let groupDataNodeMap = {};
  let groupChart = {};
  nodeGroup.forEach(
    ({ boxWidth, boxHeight, links, nodeMap, nodeList, linkTopDistance = 0, linkDownDistance = 0, categoryBox }) => {
      groupDataNodeMap = nodeMap;
      let x = 0;
      let y = marginToTop;
      let vx = 0;
      let vy = 0; // viewBox坐标
      if (direction === 'horizontal') {
        y = containerHeight + marginToTop;
        containerWidth = boxWidth > containerWidth ? boxWidth : containerWidth;
        containerHeight += boxHeight + marginToTop;
        vy = -linkTopDistance;
      } else if (direction === 'vertical') {
        x = containerWidth;
        containerWidth += boxWidth;
        containerHeight = boxHeight > containerHeight ? boxHeight : containerHeight;
        vx = -linkTopDistance;
      }
      // 在snap中创建独立的svg，并renderNodes和renderLinks
      const chart = snap.svg(x, y, boxWidth, boxHeight, vx, vy, boxWidth, boxHeight);
      g.append(chart);
      renderCategoryBox({ categoryBox, linkDownDistance, linkTopDistance }, chart, external);
      renderNodes({ nodeMap }, chart, external);
      renderLinks({ links, nodeMap }, chart, external);
      groupChart = chart;
    },
  );

  if (!isEmpty(microserviceChart)) {
    const {
      boxWidth: mBoxWidth,
      boxHeight: mBoxHeight,
      nodeMap: mNodeMap,
      categoryBox: mCategoryBox,
    } = microserviceChart as any;
    containerWidth = mBoxWidth > containerWidth ? mBoxWidth : containerWidth;
    containerHeight += mBoxHeight;
    const chartX = (containerWidth - mBoxWidth) / 2;
    const chart = snap.svg(chartX, 0, mBoxWidth, mBoxHeight, 0, 0, mBoxWidth, mBoxHeight);
    g.append(chart);
    renderCategoryBox(
      {
        categoryBox: mCategoryBox,
        linkDownDistance: -40,
        linkTopDistance: -40,
      },
      chart,
      external,
    );
    renderNodes({ nodeMap: mNodeMap, groupNodeMap: groupDataNodeMap, groupChart }, chart, external);
  }
  return { containerWidth, containerHeight };
};

const setDataLevelWithCategory = (categoryMap: any, groups: any, nodeList: any[], subGroupDeepth: any) => {
  const { gateway, addon } = categoryMap || {};
  const { endpoint, service } = groups;
  let resData = {
    startNodes: [],
    nodeList,
    deepMap: {},
  };

  const setGroupDataDeepth = (data: any, mapData: any) => {
    const curMapData: any = produce(mapData, (draft: any) => {
      if (!isEmpty(data)) {
        let curStartNode: string[] = [];
        const curDeepMap = {};
        const curMaxData: any = maxBy(map(draft.deepMap), '_external.deepth');
        const curMaxDeep = curMaxData ? get(curMaxData, '_external.deepth', 0) : 0;
        map(data, (item) => {
          const { deepMap, startNodes } = item;
          curStartNode = curStartNode.concat(startNodes);
          map(deepMap, (deepData: any, key) => {
            curDeepMap[key] = {
              _external: { ...deepData._external, deepth: curMaxDeep + get(deepData, '_external.deepth', 1) },
            };
          });
        });
        if (curMaxDeep === 0) set(draft, 'startNodes', curStartNode);
        set(draft, 'deepMap', { ...draft.deepMap, ...curDeepMap });
      }
    });
    return curMapData;
  };

  const setRegularDataDeepth = (data: any, mapData: any) => {
    const addonLevelSortMap = { addon: 1, ability: 2 };
    const curMapData: any = produce(mapData, (draft: any) => {
      if (!isEmpty(data)) {
        const curStartNode: string[] = [];
        const curDeepMap = {};
        const curMaxData: any = maxBy(map(draft.deepMap), '_external.deepth');
        const curMaxDeep = curMaxData ? get(curMaxData, '_external.deepth', 0) : 0;
        map(data, (item) => {
          const {
            id,
            type,
            _external: { subGroup },
          } = item;
          curStartNode.push(id);
          curDeepMap[id] = {
            _external: { deepth: curMaxDeep + 1, id, levelSort: addonLevelSortMap[subGroup] || type },
          };
        });
        if (curMaxDeep === 0) set(draft, 'startNodes', curStartNode);
        set(draft, 'deepMap', { ...draft.deepMap, ...curDeepMap });
      }
    });
    return curMapData;
  };

  const setServiceDataDeepth = (data: any, mapData: any, curMaxDeep: number, subGroup: string) => {
    const curMapData: any = produce(mapData, (draft: any) => {
      if (!isEmpty(data)) {
        let curStartNode: string[] = [];
        const curDeepMap = {};
        map(data, (item) => {
          const { deepMap, startNodes } = item;
          curStartNode = curStartNode.concat(startNodes);
          map(deepMap, (deepData: any, key) => {
            curDeepMap[key] = {
              _external: {
                ...deepData._external,
                deepth: curMaxDeep + get(deepData, '_external.deepth', 1),
                groupSort: subGroup,
              },
            };
          });
        });
        if (curMaxDeep === 0) set(draft, 'startNodes', curStartNode);
        set(draft, 'deepMap', { ...draft.deepMap, ...curDeepMap });
      }
    });
    return curMapData;
  };

  resData = setRegularDataDeepth(gateway, resData);
  resData = setGroupDataDeepth(endpoint, resData);
  const servieceDeepthMap = {};
  map(
    sortBy(service, (sItem) => {
      const subGroup = get(sItem, '[0].nodeList[0]._external.subGroup');
      const groupDeepth = get(subGroupDeepth, `${subGroup}._external.deepth`);
      return groupDeepth;
    }),
    (serviceItem) => {
      const subGroup = get(serviceItem, '[0].nodeList[0]._external.subGroup');
      const groupDeepth = get(subGroupDeepth, `${subGroup}._external.deepth`);
      const curMaxData: any = maxBy(map(resData.deepMap), '_external.deepth');
      const curMaxDeep = curMaxData ? get(curMaxData, '_external.deepth', 0) : 0;
      !servieceDeepthMap[`${groupDeepth}`] && (servieceDeepthMap[`${groupDeepth}`] = curMaxDeep);
      resData = setServiceDataDeepth(serviceItem, resData, servieceDeepthMap[`${groupDeepth}`], subGroup);
    },
  );
  resData = setRegularDataDeepth(addon, resData);
  return [resData];
};

export const dataHandler = {
  // 获取n个独立的数据组
  getGroupChart: (originData: INodeData[], subGroupNodes: any) => {
    if (isEmpty(originData)) return [];
    const categoryMap = groupBy(originData, 'category') || {};
    const { microservice, ...rest } = categoryMap;
    const { data, countMap } = dataHandler.getNodesFormat(flatten(map(rest)));

    const serviceMap = groupBy(categoryMap.service, '_external.subGroup');
    const addonMap = groupBy(categoryMap.addon, '_external.subGroup');

    const serviceDataList = map(serviceMap, (item) => {
      return dataHandler.getNodesFormat(item || []);
    });

    const endpointData = dataHandler.getNodesFormat(categoryMap.endpoint || []);
    const addonDataList = map(addonMap, (item) => {
      return dataHandler.getNodesFormat(item || []);
    });

    const groupDeepth = dataHandler.getParentsGroupDeepth(subGroupNodes);
    const groupData = {
      service: map(serviceDataList, (item) => {
        return dataHandler.getGroupNodesDeepth(item.data);
      }),
      endpoint: dataHandler.getGroupNodesDeepth(endpointData.data),
      addon: map(addonDataList, (item) => {
        return dataHandler.getGroupNodesDeepth(item.data);
      }),
    };
    const reGroup = setDataLevelWithCategory(categoryMap, groupData, data, groupDeepth);
    const nodeGroup: any[] = [];
    reGroup.forEach((g: any, i: number) => {
      const { nodeList, deepMap } = g;
      // 获取节点的层级map
      // const { deepMap: oldDeepMap } = dataHandler.getDeepthMap(nodeList, startNodes);
      const curNodeIds = map(nodeList, (item) => item.id);
      let curNodeMap = {};
      map(
        filter(originData, (item) => curNodeIds.includes(item.id)),
        (item, i2) => {
          curNodeMap[item.id] = item;
          // 作为节点的唯一ID
          set(curNodeMap[item.id], '_external.uniqName', `node-${i}-${i2}`);
          set(curNodeMap[item.id], '_external.outCountTotal', countMap[item.id].outCountTotal || 0);
          set(curNodeMap[item.id], '_external.inCountTotal', countMap[item.id].inCountTotal || 0);
        },
      );
      curNodeMap = merge(curNodeMap, deepMap); // 合并节点层级属性
      // const {
      //   nodeMap, boxWidth, boxHeight, categoryBox,
      // } = dataHandler.getNodesPosition(curNodeMap); // 节点定位
      const { nodeMap, boxWidth, boxHeight, categoryBox } = dataHandler.getNewNodesPosition(curNodeMap, groupDeepth);
      const { links, linkTopDistance, linkDownDistance } = dataHandler.getLinks(
        nodeList,
        nodeMap,
        boxHeight,
        groupDeepth,
      ); // 获取链接（包含link定位）
      let totalWidth = boxWidth;
      let totalHeight = boxHeight;
      const { direction } = CANVAS;
      if (direction === 'horizontal') {
        totalHeight += linkTopDistance + linkDownDistance + 100;
      } else if (direction === 'vertical') {
        totalWidth += linkTopDistance + linkDownDistance;
      }
      nodeGroup.push({
        nodeMap, // 节点信息：包含节点层级、节点x/y坐标,
        nodeList,
        boxWidth: totalWidth, // 图宽
        boxHeight: totalHeight, // 图高
        links, // 连接线信息：包含连线坐标
        linkTopDistance, // 跨层级线高度（上方）
        linkDownDistance, // 跨层级线高度（下方）
        categoryBox,
      });
    });
    return nodeGroup;
  },
  // 获取subGroup层级
  getParentsGroupDeepth: (originData: any) => {
    const { data } = dataHandler.getNodesFormat(map(originData));
    const groupDeepth = dataHandler.getGroupNodesDeepth(data);
    let reGroupDeepth = {};
    map(groupDeepth, (g: any) => {
      reGroupDeepth = {
        ...reGroupDeepth,
        ...g.deepMap,
      };
    });
    return reGroupDeepth;
  },
  // 微服务chart
  getMicroserviceChart: (originData: INodeData[]) => {
    const categoryMap = groupBy(originData, 'category') || {};
    const { microservice } = categoryMap;
    if (isEmpty(microservice)) return {};
    const len = microservice.length;
    const {
      padding,
      NODE: { width, height, margin },
      boxMargin,
    } = CANVAS;
    const nodeMap = {};
    const boxWidth = (width + margin.x) * len - margin.x + padding.x * 2;
    const boxHeight = height + margin.y - margin.y + padding.y * 2;

    let startX = padding.x;
    let startY = padding.y;
    startX += width / 2;
    startY += height / 2;
    const categoryBox = {};
    const lineBoxMarginX = boxMargin.x;
    const lineBoxMarginY = boxMargin.y;
    map(microservice, (item, idx: number) => {
      const { id, category } = item;
      const x = startX + idx * (margin.x + width);
      const y = startY;
      if (idx === 0) {
        if (!categoryBox[category]) {
          categoryBox[category] = {
            startX: x - lineBoxMarginX - width / 2,
            startY: y - lineBoxMarginY - height / 2,
          };
        }
      }

      if (idx === len - 1) {
        categoryBox[category] = {
          ...(categoryBox[category] || {}),
          endX: x + lineBoxMarginX + width / 2,
          endY: y + lineBoxMarginY + height / 2,
        };
      }

      nodeMap[id] = {
        ...item,
        _external: {
          deepth: idx + 1,
          x,
          y,
          uniqName: id,
        },
      };
    });
    return {
      boxWidth,
      boxHeight,
      nodeMap,
      categoryBox,
    };
  },
  // 平铺节点: {name:x,parents:[...]} => [{name:x,parent:p1},...]
  getNodesFormat: (dataArr: any[]) => {
    if (!isArray(dataArr)) return { data: [], countMap: {} };
    const nodeCallCountMap = {};
    const data = reduce(
      dataArr,
      (res: any[], item) => {
        const { id, parents, ...rest } = item;
        if (parents && parents.length > 0) {
          const prevMap = nodeCallCountMap[id] || {};
          nodeCallCountMap[id] = { ...prevMap, inCountTotal: get(rest, 'metric.count') || 1 };
          // nodeCallCountMap[id] = { ...prevMap, inCountTotal: reduce(parents, (sum, { metric: { count } }) => (count + sum), 0) };
          let pCount = 0;
          parents.forEach((p: any) => {
            const { id: pId } = p;
            const count = get(p, 'metric.count') || 0;
            const curParentId = (find(dataArr, { id: pId }) || {}).id || '';
            // 过滤不存在的节点和自己调自己的节点
            if (curParentId && curParentId !== id) {
              if (nodeCallCountMap[`${pId}`]) {
                const prevCallCount = nodeCallCountMap[`${pId}`].outCountTotal || 0;
                nodeCallCountMap[`${pId}`].outCountTotal = prevCallCount + count;
              } else {
                nodeCallCountMap[`${pId}`] = { outCountTotal: count };
              }
              pCount += 1;
              res.push({ parent: curParentId, parents, id, nodeType: 'node', ...rest });
            }
          });
          if (pCount === 0) {
            // 有父但父count为0，则可能父节点不存在或只有自己是自己的父节点
            res.push({ parent: '', id, parents, nodeType: 'node', ...rest });
          }
        } else {
          if (!nodeCallCountMap[`${id}`]) {
            nodeCallCountMap[`${id}`] = {};
          }
          res.push({ parent: '', id, parents, nodeType: 'node', ...rest });
        }
        return res;
      },
      [],
    );
    return { data, countMap: nodeCallCountMap };
  },
  isCircleData: (n1: string, n2: string, nodeList: INode[]) => {
    const data1 = find(nodeList, { parent: n1, id: n2 });
    const data2 = find(nodeList, { parent: n2, id: n1 });
    if (data1 && data2) {
      // 直接环
      return true;
    } else {
      const getChildren = (nodeId: string, children: string[] = []) => {
        if (!children.includes(nodeId)) {
          children.push(nodeId);
          const childrenList = filter(nodeList, { parent: nodeId }) as INode[];
          for (let i = 0; i < childrenList.length; i++) {
            getChildren(childrenList[i].id, children);
          }
        }
        return children;
      };

      const n1_children = getChildren(n1);
      const n2_children = getChildren(n2);
      if (n1_children.includes(n2) && n2_children.includes(n1)) return true; // 间接环
      return false;
    }
  },
  getCountDeepMap: (nodeList: INode[], starts: string[]) => {
    const deepMap = {};
    // 找出每个节点开始往下遍历的最长路径，并记录节点deep
    const traversal = (nodeId: string, deep = 1, pNode = '') => {
      if (!deepMap[nodeId]) {
        const outTotal = filter(nodeList, { parent: nodeId }).length;
        const inTotal = filter(nodeList, { id: nodeId }).length;
        deepMap[nodeId] = { _external: { deepth: deep, outTotal, inTotal, id: nodeId } };
        const children = filter(nodeList, { parent: nodeId }) as INode[];
        for (let i = 0; i < children.length; i++) {
          traversal(children[i].id, deep + 1, nodeId);
        }
      } else if (deepMap[nodeId]) {
        // 已经设置过层级的节点
        // 若当前线为环，则deep不变，已经在列，则取大deep
        const prevDeep = deepMap[nodeId]._external.deepth;
        const pDeep = pNode ? deepMap[pNode]._external.deepth : 0;
        const isCircle = dataHandler.isCircleData(nodeId, pNode, nodeList);
        /** 层级变动需要顺延的两种情况
         *  1、非循环节点，且已设置深度小于当前深度，取更深后子节点顺延
         *  2、循环节点，且当前深度等于父节点深度，避免在同一层级，顺延
         */

        if ((!isCircle && prevDeep < deep) || (isCircle && prevDeep === pDeep)) {
          deepMap[nodeId]._external.deepth = deep;
          // 有层级变动的节点，其下所有节点都需要顺延改变
          const children = filter(nodeList, { parent: nodeId }) as INode[];
          for (let i = 0; i < children.length; i++) {
            traversal(children[i].id, deep + 1, nodeId);
          }
        }
      }
    };

    for (let i = 0, len = starts.length; i < len; i++) {
      traversal(starts[i]);
    }
    return deepMap;
  },
  // 获取节点组层级
  getGroupNodesDeepth: (nodeList: INode[]) => {
    const nodeIds = uniq(map(nodeList, (i) => i.id));
    const getTreeNodeList = (treeNodes: string[]) => {
      return filter(nodeList, (n: INode) => treeNodes.includes(n.id));
    };
    let deepMap = {};
    // 找出每个节点开始往下遍历的最长路径，并记录节点deep
    const traversal = (nodeId: string, IdList = [] as string[], deep = 1, pNode = '') => {
      if (nodeId && !IdList.includes(nodeId)) {
        IdList.push(nodeId);
        const outTotal = filter(nodeList, { parent: nodeId }).length;
        const inTotal = filter(nodeList, { id: nodeId }).length;
        deepMap[nodeId] = { _external: { deepth: deep, outTotal, inTotal, id: nodeId } };
        const children = filter(nodeList, { parent: nodeId }) as INode[];
        for (let i = 0; i < children.length; i++) {
          traversal(children[i].id, IdList, deep + 1, nodeId);
        }
      } else if (IdList.includes(nodeId)) {
        // 已经设置过层级的节点
        // 若当前线为环，则deep不变，已经在列，则取大deep
        const prevDeep = deepMap[nodeId]._external.deepth;
        const pDeep = deepMap[pNode]._external.deepth;
        const isCircle = dataHandler.isCircleData(nodeId, pNode, nodeList);
        /** 层级变动需要顺延的两种情况
         *  1、非循环节点，且已设置深度小于当前深度，取更深后子节点顺延
         *  2、循环节点，且当前深度等于父节点深度，避免在同一层级，顺延
         */

        if ((!isCircle && prevDeep < deep) || (isCircle && prevDeep === pDeep)) {
          deepMap[nodeId]._external.deepth = deep;
          // 有层级变动的节点，其下所有节点都需要顺延改变
          const children = filter(nodeList, { parent: nodeId }) as INode[];
          for (let i = 0; i < children.length; i++) {
            traversal(children[i].id, IdList, deep + 1, nodeId);
          }
        }
      }
      return IdList;
    };
    let startNodes: string[] = [];
    let sortTree = [];
    const startNodesDataMap = {};
    const traversalMap = {};
    const traversalDeepMap = {};
    for (let i = 0, len = nodeIds.length; i < len; i++) {
      deepMap = {};
      const nameList = traversal(nodeIds[i]);
      traversalDeepMap[nodeIds[i]] = deepMap;
      // 如果第一次的遍历中存在长度是总长的节点，则找到唯一树的开始节点
      if (nameList.length === nodeIds.length) {
        startNodes.push(`${nodeIds[i]}`);
        startNodesDataMap[nodeIds[i]] = nodeList;
        sortTree.push(nameList);
        break;
      }
      traversalMap[nodeIds[i]] = nameList;
    }
    // 第一次循环未找出开始节点，则为n个树，需要找出n个开始节点
    if (!startNodes.length) {
      const treeMap = reduce(
        traversalMap,
        (res: any, item: string[], key) => {
          let isInclude = false;
          map(res, (tree, treeKey) => {
            // 有"全包含"关系的节点，比较找出路径最长的节点
            const uniqLen = uniq([...tree, ...item]).length;
            if (uniqLen === tree.length || uniqLen === item.length) {
              isInclude = true;
              if (item.length > tree.length) {
                // 写入更长的路径
                delete res[treeKey];
                delete startNodesDataMap[treeKey];
                res[key] = item;
                startNodesDataMap[key] = getTreeNodeList(item);
              }
            }
          });
          if (!isInclude) {
            res[key] = item;
            startNodesDataMap[key] = getTreeNodeList(item);
          }
          return res;
        },
        {},
      );
      startNodes = uniq(Object.keys(treeMap));
      sortTree = sortBy(
        map(
          reduce(
            treeMap,
            (res: any, item, key) => {
              const resList = map(res, (treeList, treeKey) => ({ list: treeList, treeKey }));
              // filter所有map中的相同树，避免交叉树被遗漏，如当前item=[2,3]  res: [1,2] [4,3];
              const sameTree = filter(resList, (resItem: any[]) => {
                const { list } = resItem as any;
                const concatArr = [...list, ...item];
                const uniqLen = uniq([...concatArr]).length;
                return uniqLen !== concatArr.length;
              });
              if (sameTree.length) {
                let sameList: string[] = [];
                sameTree.forEach((sameItem: any) => {
                  const { list, treeKey } = sameItem;
                  delete res[treeKey];
                  sameList = sameList.concat(list);
                });
                res[key] = uniq([...item, ...sameList]);
              } else {
                res[key] = item;
              }
              return res;
            },
            {},
          ),
          (o) => o,
        ),
        (l) => -l.length,
      );
    }
    // 最终得到的startNodes及对应的节点list
    return map(sortTree, (tree: string[]) => {
      const starts: string[] = [];
      const list: INode[] = [];
      nodeList.forEach((node) => {
        if (tree.includes(node.id)) {
          list.push(node);
          if (startNodes.includes(`${node.id}`)) starts.push(node.id);
        }
      });
      let countDeepMap = dataHandler.getCountDeepMap(nodeList, starts);

      countDeepMap = dataHandler.forwardDeepth(countDeepMap, nodeList);
      countDeepMap = dataHandler.backwardDeepth(countDeepMap);
      countDeepMap = dataHandler.sortDeepthNode(countDeepMap, nodeList);
      return {
        startNodes: starts,
        nodeList: list,
        deepMap: countDeepMap,
      };
    });
  },
  // 获取节点父子节点数
  getNodesInOutTotal: (nodeList: INode[]) => {
    const dataMap = {};
    const nodeIds = uniq(map(nodeList, (i) => i.id));
    map(nodeIds, (nodeId) => {
      const outTotal = filter(nodeList, { parent: nodeId }).length;
      const inTotal = filter(nodeList, { id: nodeId }).length;
      dataMap[nodeId] = { _external: { outTotal, inTotal } };
    });
    return dataMap;
  },
  // 获取节点层级map
  // getDeepthMap: (data: INode[], parentNodes: string[]) => {
  //   let deepMap = {};
  //   const createTree = (array: any, rootNodes: string[], deepth = 0, lines = [] as string[]) => {
  //     const tree = [] as any;
  //     const deep = deepth + 1;

  //     each(rootNodes, (rootNode) => {
  //       const node = {
  //         id: rootNode,
  //         deepth: deep,
  //       } as any;
  //       // 给节点建立deepMap记录，inTotal/outTotal记录（调用和被调用）
  //       if (deepMap[rootNode] === undefined || deep > deepMap[rootNode]._external.deepth) {
  //         const outTotal = filter(data, { parent: rootNode }).length;
  //         const inTotal = filter(data, { id: rootNode }).length;
  //         // _external中的属性用于内部计算，故用__标识
  //         deepMap[rootNode] = { _external: { deepth: deep, outTotal, inTotal, id: rootNode } };
  //       }
  //       const childIds = [] as string[];
  //       // lines的作用：当有环形数据的时候，利用lines记录已经被遍历过的点，避免形成无限循环。
  //       each(array, (i) => {
  //         if (i.parent === rootNode) {
  //           lines.push(rootNode);
  //           !lines.includes(i.id) && childIds.push(i.id);
  //         }
  //       });
  //       if (childIds.length) {
  //         node.children = createTree(array, childIds, deep, lines);
  //       }
  //       tree.push(node);
  //     });
  //     return tree;
  //   };

  //   const tree = createTree(data, parentNodes, 0);
  //   deepMap = dataHandler.forwardDeepth(deepMap, data);
  //   deepMap = dataHandler.sortDeepthNode(deepMap, data);
  //   return { tree, deepMap };
  // },
  // 节点层级优化1：将跨层的节点往前移动
  forwardDeepth: (deepMap: any, nodeList: INode[]) => {
    const deepthGroup = groupBy(deepMap, '_external.deepth');
    const reMap = cloneDeep(deepMap);
    map(deepthGroup, (list: any) => {
      map(list, (item: any) => {
        const {
          _external: { id, deepth },
        } = item;
        const childrenDeep: number[] = [];
        map(nodeList, (dataItem: any) => {
          if (dataItem.parent === id) {
            childrenDeep.push(deepMap[dataItem.id]._external.deepth);
          }
        });
        const childMinDeep = min(childrenDeep) || 1; // 找到子的最上层级;
        if (childMinDeep - deepth > 1) {
          // 跨层级，将节点往后移动
          reMap[id]._external.deepth = childMinDeep - 1;
        }
      });
    });
    return reMap;
  },
  /**
   * 根据层级数生成最终deepth
   * 原因：
   *  深度遍历，节点的deepth会根据返回数据的顺序不同而变化
   *  有环数据可能会因为高层级节点在后导致层级靠后，则中间可能会有层级缺失。
   *  */
  backwardDeepth: (deepMap: any) => {
    const deepthGroup = map(groupBy(deepMap, '_external.deepth'));
    const reMap = cloneDeep(deepMap);
    map(deepthGroup, (list: any, index) => {
      map(list, (item: any) => {
        const {
          _external: { id },
        } = item;
        reMap[id]._external.deepth = index + 1;
      });
    });
    return reMap;
  },
  // 节点层级优化2：每一层节点打上sort标记
  sortDeepthNode: (deepMap: any, nodeList: INode[]) => {
    const deepthGroup = groupBy(deepMap, '_external.deepth');
    const reMap = cloneDeep(deepMap);
    map(deepthGroup, (list: any, lev: string) => {
      const len = list.length;
      // const max = maxBy(list, '_external.outTotal');
      if (lev === '1') {
        // const centerPos = Math.floor(len / 2);
        map(sortBy(list, '_external.outTotal'), ({ _external: { id, outTotal } }, i) => {
          set(reMap[id], '_external.levelSort', outTotal * 100 + i);
        });
      } else {
        map(list, ({ _external: { id, outTotal } }, idx: number) => {
          const curNode = find(nodeList, { id });
          const { parents = [], type, category } = curNode as INode;
          let levelSort = idx;
          if (category === 'addon') {
            set(reMap[id], '_external.levelSort', get(curNode, `${externalKey}.subGroup`) + type);
          } else {
            parents.forEach((p: any) => {
              const pMap = get(reMap, `[${p.id}]._external`);
              if (pMap && Number(lev) - Number(pMap.deepth) === 1) {
                // 上层父
                levelSort = pMap.levelSort > levelSort ? levelSort : pMap.levelSort;
              }
            });
            set(reMap[id], '_external.levelSort', levelSort);
          }
        });
      }
    });
    return reMap;
  },
  // 获取图links
  getLinks: (nodeList: INode[], nodeMap: object, boxHeight: number, groupDeepth: any) => {
    const links = [] as any;
    nodeList.forEach((node: INode) => {
      const { parent, id, callCount = '' } = node;
      if (parent) {
        const lk = { source: parent, target: id, value: callCount, nodeType: 'link' } as any;
        if (find(nodeList, { id: parent, parent: id })) {
          // 存在反向线
          lk.hasReverse = true;
        }
        links.push(lk);
      }
    });
    return dataHandler.getLinkPosition({ nodeMap, links, boxHeight, groupDeepth });
  },
  // 获取节点links的位置
  getLinkPosition: ({ nodeMap, links, boxHeight = 0, groupDeepth }: ILinkRender) => {
    const {
      NODE: { width, margin, height },
      direction,
      LINK: { linkDis },
    } = CANVAS;

    const halfWidth = width / 2;
    const halfMaginX = margin.x / 2;
    const halfHeight = height / 2;
    // const halfMarginY = margin.y / 2;

    const deepthGroup = groupBy(nodeMap, '_external.deepth');
    const edgePlusMap = {};
    const getDeepthHeightDistance = (deepthKey: string) => {
      return (
        get(maxBy(deepthGroup[deepthKey], '_external.y'), '_external.y', 0) -
        get(minBy(deepthGroup[deepthKey], '_external.y'), '_external.y', 0)
      );
    };
    const serviceMap = groupBy(get(groupBy(nodeMap, '_external.group'), 'service', []), '_external.subGroup');
    const serviceDeepth = {};
    map(serviceMap, (service) => {
      const subGroup = get(service, '[0]._external.subGroup');
      const lev = get(groupDeepth, `${subGroup}._external.deepth`);
      const prevList = serviceDeepth[`${lev}`] || [];
      serviceDeepth[`${lev}`] = [...prevList, ...service];
    });
    map(deepthGroup, (gList, lev) => {
      // 每个层级上跨层级线的边缘叠加数
      const group = get(gList, '[0]._external.group');
      let curStartNode = null as any;
      let curEndEdgeNode = null as any;
      if (group === 'service') {
        const curSubGroup = get(gList, '[0]._external.subGroup');
        const curLev = get(groupDeepth, `${curSubGroup}._external.deepth`);
        curStartNode = minBy(serviceDeepth[`${curLev}`], '_external.y');
        curEndEdgeNode = maxBy(serviceDeepth[`${curLev}`], '_external.y');
      } else {
        curStartNode = minBy(gList, '_external.y') as any;
        curEndEdgeNode = maxBy(gList, '_external.y') as any;
      }
      edgePlusMap[lev] = {
        startX: curStartNode._external.x - halfWidth,
        originStartX: curStartNode._external.x - halfWidth,
        startY: curStartNode._external.y - halfHeight,
        originStartY: curStartNode._external.y - halfHeight,
        endX: curEndEdgeNode._external.x + halfWidth,
        originEndX: curEndEdgeNode._external.x + halfWidth,
        endY: curEndEdgeNode._external.y + halfHeight,
        originEndY: curEndEdgeNode._external.y + halfHeight,
      };
    });
    const centerY = boxHeight / 2;

    const reLinks = map(links, (link) => {
      const { source, target, hasReverse } = link;
      const sourceNode = nodeMap[source]._external;
      const targetNode = nodeMap[target]._external;
      let posArr: any[] = [];
      // >0:起点在终点的后面；==0:终点起点同一垂直线；<0:起点在终点前面
      const xPos = sourceNode.x - targetNode.x;
      // >0:起点在终点下方；==0:终点起点同一水平线；<0:起点在终点上方
      const yPos = sourceNode.y - targetNode.y;
      const id = `link__${sourceNode.uniqName}__${targetNode.uniqName}`;
      if (direction === 'horizontal') {
        // 相邻层级节点：在两节点之间水平中心点位置开始折线
        if (Math.abs(sourceNode.deepth - targetNode.deepth) === 1) {
          const [p0_x, p1_x, p2_x] =
            xPos > 0
              ? [sourceNode.x - halfWidth, sourceNode.x - halfMaginX - halfWidth, targetNode.x + halfWidth]
              : [sourceNode.x + halfWidth, sourceNode.x + halfMaginX + halfWidth, targetNode.x - halfWidth];

          const p0_y = sourceNode.y;
          let p1_y = targetNode.y;
          const p2_y = targetNode.y;
          if (hasReverse && sourceNode.y <= centerY && targetNode.y <= centerY) {
            // 有反向线且为直线
            p1_y += xPos > 0 ? 25 : -25;
          }
          posArr = [p0_x, p0_y, p1_x, p1_y, p2_x, p2_y];
        } else if (Math.abs(sourceNode.deepth - targetNode.deepth) > 1) {
          // 跨层级节点：先移动到最上/下方，折线然后平移到目标节点的层级后，再次折线到目标
          const [p0_x, p1_x, p2_x, p3_x] =
            xPos > 0
              ? [
                  sourceNode.x - halfWidth,
                  sourceNode.x - halfMaginX - halfWidth,
                  targetNode.x + halfWidth + halfMaginX,
                  targetNode.x + halfWidth,
                ]
              : [
                  sourceNode.x + halfWidth,
                  sourceNode.x + halfMaginX + halfWidth,
                  targetNode.x - halfWidth - halfMaginX,
                  targetNode.x - halfWidth,
                ];

          const sourceDeepth = nodeMap[source]._external.deepth;
          const targetDeepth = nodeMap[target]._external.deepth;

          let betweenMaxDeepth = 0;
          let betweenMaxHeight = 0;
          // 计算跨层级中间最高的层级，最高层级的数据长度
          map(edgePlusMap, (pos, deepKey) => {
            if (
              (deepKey > sourceDeepth && deepKey < targetDeepth) ||
              (deepKey < sourceDeepth && deepKey > targetDeepth)
            ) {
              const curBetweenMaxHeight = getDeepthHeightDistance(deepKey);
              if (curBetweenMaxHeight > betweenMaxHeight) {
                betweenMaxDeepth = Number(deepKey);
                betweenMaxHeight = curBetweenMaxHeight;
              }
            }
          });
          const sourceHeightDis = getDeepthHeightDistance(sourceDeepth);
          const targetHeightDis = getDeepthHeightDistance(targetDeepth);
          const curMaxDeep: number = get(
            maxBy(
              [
                { deep: sourceDeepth, len: sourceHeightDis },
                { deep: targetDeepth, len: targetHeightDis },
                { deep: betweenMaxDeepth, len: betweenMaxHeight },
              ],
              (o) => o.len,
            ),
            'deep',
          );
          const curMaxEdge = edgePlusMap[`${curMaxDeep}`];

          const p0_y = sourceNode.y;
          let p1_y = 0;

          // 上折
          const upBreak = [curMaxEdge.startY - linkDis, curMaxEdge.startY - linkDis];
          // 下折
          const downBreak = [curMaxEdge.endY + linkDis, curMaxEdge.endY + linkDis];

          if (sourceNode.y === centerY && targetNode.y === centerY && hasReverse) {
            xPos > 0
              ? ([p1_y, edgePlusMap[`${curMaxDeep}`].endY] = downBreak)
              : ([p1_y, edgePlusMap[`${curMaxDeep}`].startY] = upBreak);
          } else if (sourceNode.y <= centerY && targetNode.y <= centerY) {
            // 中线上方，上折
            [p1_y, edgePlusMap[`${curMaxDeep}`].startY] = upBreak;
          } else if (sourceNode.y >= centerY && targetNode.y >= centerY) {
            // 中线下方，下折
            [p1_y, edgePlusMap[`${curMaxDeep}`].endY] = downBreak;
          } else {
            // 起点和终点分布在中线两边，则从起点就近折
            yPos > 0
              ? ([p1_y, edgePlusMap[`${curMaxDeep}`].endY] = downBreak)
              : ([p1_y, edgePlusMap[`${curMaxDeep}`].startY] = upBreak);
          }
          const p2_y = p1_y;
          const p3_y = targetNode.y;
          posArr = [p0_x, p0_y, p1_x, p1_y, p2_x, p2_y, p3_x, p3_y];
        }
      } else if (direction === 'vertical') {
        // TODO
      }
      return {
        ...link,
        _external: {
          posArr,
          id,
          sourceNode: nodeMap[source],
          targetNode: nodeMap[target],
          linkData: {
            ...nodeMap[target],
            parents: find(nodeMap[target].parents, { id: source }),
          },
        },
      };
    });
    const edgePlusList = map(edgePlusMap);
    const topObj: any = minBy(edgePlusList, (t: any) => t.startY);
    const downObj: any = maxBy(edgePlusList, (d: any) => d.endY);
    return {
      links: reLinks,
      linkTopDistance: topObj.originStartY - topObj.startY,
      linkDownDistance: topObj.endY - downObj.originEndY,
    };
  },
  setNodesWithGroup: (nodeMap: object) => {
    const groupData = groupBy(nodeMap, '_external.group');
    const reData = {} as any;
    map(groupData, (group, key) => {
      if (key === 'service') {
        reData.service = groupBy(group, '_external.subGroup');
      } else {
        reData[key] = group;
      }
    });
    return reData;
  },
  getNewNodesPosition: (nodeMap: object, groupDeepth: any) => {
    const { boxMargin } = CANVAS;
    const lineBoxMarginX = boxMargin.x;
    const lineBoxMarginY = boxMargin.y;
    const {
      padding,
      NODE: { width, height, margin },
      direction,
    } = CANVAS;
    const curNodeMap = cloneDeep(nodeMap);
    const categoryBox = {};
    let totalWidth = 0;
    let totalHeight = 0;

    const getMaxHeight = () => {
      const _categoryGroup = dataHandler.setNodesWithGroup(curNodeMap);
      let maxHeight = 0;
      const curMap = {};
      map(_categoryGroup, (list, key) => {
        if (key === 'service') {
          const serviceList = map(list);
          map(
            sortBy(serviceList, (item) => {
              const subGroup = get(item, '[0]._external.subGroup');
              const curGroupDeepth = get(groupDeepth, `${subGroup}._external.deepth`);
              return curGroupDeepth;
            }),
            (serviceItem: any, serviceKey) => {
              const subGroup = get(serviceItem, '[0]._external.subGroup');
              const curGroupDeepth = get(groupDeepth, `${subGroup}._external.deepth`);
              const prevHeight = curMap[curGroupDeepth] || 0;
              const deepthGroup = groupBy(serviceItem, '_external.deepth');
              const maxColumn = (maxBy(values(deepthGroup)) || []).length;
              const curServiceHeight = (height + margin.y) * maxColumn - margin.y + padding.y * 2;
              curMap[curGroupDeepth] = curServiceHeight + prevHeight;
            },
          );
        } else {
          const deepthGroup = groupBy(list, '_external.deepth');
          const maxColumn = (maxBy(values(deepthGroup)) || []).length;
          const curHeight = (height + margin.y) * maxColumn - margin.y + padding.y * 2;
          maxHeight = maxHeight < curHeight ? curHeight : maxHeight;
        }
      });
      const maxServiceHeight = (((max(map(curMap)) as any) || 0) + padding.y) as number;
      maxHeight = maxHeight < maxServiceHeight ? maxServiceHeight : maxHeight;
      return { maxHeight, serviceDeepHeight: curMap };
    };

    const setDeepthGroup = (
      list: any,
      { totalHeight: curTotal, heightDistance } = { totalHeight: 0, heightDistance: 0 },
      isService = false,
    ) => {
      const deepthGroup = groupBy(list, '_external.deepth');
      const maxColumn = (maxBy(values(deepthGroup)) || []).length;
      const curBoxHeight = (height + margin.y) * maxColumn - margin.y + padding.y * 2;
      const externalDistance = (curTotal - curBoxHeight) / 2;
      map(deepthGroup, (curList: INode[], deepth: number) => {
        const len = curList.length;
        let startX = padding.x;
        let startY = padding.y + heightDistance;
        let startDistance = 0;
        if (direction === 'horizontal') {
          startDistance =
            height / 2 +
            (curBoxHeight - padding.y * 2 - (len * (height + margin.y) - margin.y)) / 2 +
            (isService ? 0 : externalDistance);
          startY += startDistance;
          startX += width / 2;
        } else if (direction === 'vertical') {
          // TODO
        }
        map(
          sortBy(curList, (listItem: any) => {
            const groupSort = get(listItem, '_external.groupSort', '');
            return groupSort
              ? `${groupSort}${get(listItem, '_external.levelSort')}`
              : get(listItem, '_external.levelSort');
          }),
          (node: INode, i: number) => {
            // 每一个层级的最上和最下，在此做标记，用于画跨层级线
            if (direction === 'horizontal') {
              const x = startX + (deepth - 1) * (margin.x + width);
              const y = startY + i * (margin.y + height);
              if (node.category === 'service' || node.category === 'addon') {
                let subBoxKey = get(node, `${externalKey}.subGroup`);
                if (node.category === 'addon') subBoxKey = `addon_${subBoxKey}`;
                if (!categoryBox[subBoxKey]) {
                  categoryBox[subBoxKey] = {
                    type: 'sub',
                    startX: x - width / 2,
                    startY: y - height / 2,
                    endX: x + width / 2,
                    endY: y + height / 2,
                  };
                } else {
                  const preSubPos = categoryBox[subBoxKey];
                  const curSubBoxX = x - width / 2;
                  const curSubBoxY = y - height / 2;
                  const curBoxEndX = x + width / 2;
                  const curBoxEndY = y + height / 2;
                  categoryBox[subBoxKey] = {
                    type: 'sub',
                    startX: preSubPos.startX < curSubBoxX ? preSubPos.startX : curSubBoxX,
                    startY: preSubPos.startY < curSubBoxY ? preSubPos.startY : curSubBoxY,
                    endX: (preSubPos.endX || 0) > curBoxEndX ? preSubPos.endX : curBoxEndX,
                    endY: (preSubPos.endY || 0) > curBoxEndY ? preSubPos.endY : curBoxEndY,
                  };
                }
              }

              if (!categoryBox[node.category]) {
                categoryBox[node.category] = {
                  startX: x - lineBoxMarginX - width / 2,
                  startY: y - lineBoxMarginY - height / 2,
                  endX: x + lineBoxMarginX + width / 2,
                  endY: y + lineBoxMarginY + height / 2,
                };
              } else {
                const prePos = categoryBox[node.category];
                const curBoxX = x - lineBoxMarginX - width / 2;
                const curBoxY = y - lineBoxMarginY - height / 2;
                const curBoxEndX = x + lineBoxMarginX + width / 2;
                const curBoxEndY = y + lineBoxMarginY + height / 2;
                categoryBox[node.category] = {
                  ...prePos,
                  startX: prePos.startX < curBoxX ? prePos.startX : curBoxX,
                  startY: prePos.startY < curBoxY ? prePos.startY : curBoxY,
                  endX: (prePos.endX || 0) > curBoxEndX ? prePos.endX : curBoxEndX,
                  endY: (prePos.endY || 0) > curBoxEndY ? prePos.endY : curBoxEndY,
                };
              }

              curNodeMap[node.id]._external.x = x;
              curNodeMap[node.id]._external.y = y;
            } else if (direction === 'vertical') {
              curNodeMap[node.id]._external.x = startX + i * (margin.x + width);
              curNodeMap[node.id]._external.y = startY + (deepth - 1) * (margin.y + height);
            }
          },
        );
      });
      return (height + margin.y) * maxColumn - margin.y;
    };

    const serviceTpData = {};
    if (!isEmpty(curNodeMap)) {
      const deepthGroup = groupBy(curNodeMap, '_external.deepth');
      const rowNum = keys(deepthGroup).length;
      totalWidth = (width + margin.x) * rowNum - margin.x + padding.x * 2;
      const maxObj = getMaxHeight();
      totalHeight = maxObj.maxHeight || 0;
      const servicesHeight = maxObj.serviceDeepHeight;
      const categoryGroup = dataHandler.setNodesWithGroup(curNodeMap);
      map(categoryGroup, (list, key) => {
        if (key === 'service') {
          const serviceList = map(list);
          map(
            sortBy(serviceList, (item) => {
              const subGroup = get(item, '[0]._external.subGroup');
              const curGroupDeepth = get(groupDeepth, `${subGroup}._external.deepth`);
              return curGroupDeepth;
            }),
            (serviceItem, serviceKey) => {
              const subGroup = get(serviceItem, '[0]._external.subGroup');
              const curGroupDeepth = get(groupDeepth, `${subGroup}._external.deepth`);
              const prevHeight = serviceTpData[curGroupDeepth] || 0;
              const boxDis = (totalHeight - servicesHeight[curGroupDeepth]) / 2;
              const curServiceHeight = setDeepthGroup(
                serviceItem,
                { totalHeight, heightDistance: prevHeight + boxDis },
                true,
              );
              serviceTpData[curGroupDeepth] = curServiceHeight + prevHeight + padding.y * 2;
            },
          );
        } else {
          setDeepthGroup(list, { totalHeight, heightDistance: 0 });
        }
      });
    }
    return {
      nodeMap: curNodeMap,
      boxWidth: totalWidth,
      boxHeight: totalHeight,
      categoryBox: dataHandler.handelAddonCategory(categoryBox),
    };
  },
  // 获取节点位置信息
  getNodesPosition: (nodeMap: object) => {
    let boxWidth = 0;
    let boxHeight = 0;
    const { boxMargin } = CANVAS;
    const lineBoxMarginX = boxMargin.x;
    const lineBoxMarginY = boxMargin.y;
    const {
      padding,
      NODE: { width, height, margin },
      direction,
    } = CANVAS;
    const curNodeMap = cloneDeep(nodeMap);
    const categoryBox = {};
    if (!isEmpty(curNodeMap)) {
      const deepthGroup = groupBy(curNodeMap, '_external.deepth');
      const maxColumn = (maxBy(values(deepthGroup)) || []).length;
      const rowNum = keys(deepthGroup).length;
      if (direction === 'horizontal') {
        boxWidth = (width + margin.x) * rowNum - margin.x + padding.x * 2;
        boxHeight = (height + margin.y) * maxColumn - margin.y + padding.y * 2;
      } else if (direction === 'vertical') {
        boxWidth = (width + margin.x) * maxColumn - margin.x + padding.x * 2;
        boxHeight = (height + margin.y) * rowNum - margin.y + padding.y * 2;
      }
      map(deepthGroup, (list: INode[], deepth: number) => {
        const len = list.length;
        let startX = padding.x;
        let startY = padding.y;
        let startDistance = 0;
        if (direction === 'horizontal') {
          startDistance = height / 2 + (boxHeight - padding.y * 2 - (len * (height + margin.y) - margin.y)) / 2;
          startY += startDistance;
          startX += width / 2;
        } else if (direction === 'vertical') {
          // TODO
        }
        map(
          sortBy(list, (listItem: any) => {
            const groupSort = get(listItem, '_external.groupSort', '');
            return groupSort
              ? `${groupSort}${get(listItem, '_external.levelSort')}`
              : get(listItem, '_external.levelSort');
          }),
          (node: INode, i: number) => {
            // 每一个层级的最上和最下，在此做标记，用于画跨层级线
            if (direction === 'horizontal') {
              const x = startX + (deepth - 1) * (margin.x + width);
              const y = startY + i * (margin.y + height);
              if (i === 0) {
                curNodeMap[node.id]._external.edgeStart = true;
              }
              if (i === len - 1) {
                curNodeMap[node.id]._external.edgeEnd = true;
              }

              if (node.category === 'service' || node.category === 'addon') {
                let subBoxKey = get(node, `${externalKey}.subGroup`);
                if (node.category === 'addon') subBoxKey = `addon_${subBoxKey}`;
                if (!categoryBox[subBoxKey]) {
                  categoryBox[subBoxKey] = {
                    type: 'sub',
                    startX: x - width / 2,
                    startY: y - height / 2,
                    endX: x + width / 2,
                    endY: y + height / 2,
                  };
                } else {
                  const preSubPos = categoryBox[subBoxKey];
                  const curSubBoxX = x - width / 2;
                  const curSubBoxY = y - height / 2;
                  const curBoxEndX = x + width / 2;
                  const curBoxEndY = y + height / 2;
                  categoryBox[subBoxKey] = {
                    type: 'sub',
                    startX: preSubPos.startX < curSubBoxX ? preSubPos.startX : curSubBoxX,
                    startY: preSubPos.startY < curSubBoxY ? preSubPos.startY : curSubBoxY,
                    endX: (preSubPos.endX || 0) > curBoxEndX ? preSubPos.endX : curBoxEndX,
                    endY: (preSubPos.endY || 0) > curBoxEndY ? preSubPos.endY : curBoxEndY,
                  };
                }
              }

              if (!categoryBox[node.category]) {
                categoryBox[node.category] = {
                  startX: x - lineBoxMarginX - width / 2,
                  startY: y - lineBoxMarginY - height / 2,
                  endX: x + lineBoxMarginX + width / 2,
                  endY: y + lineBoxMarginY + height / 2,
                };
              } else {
                const prePos = categoryBox[node.category];
                const curBoxX = x - lineBoxMarginX - width / 2;
                const curBoxY = y - lineBoxMarginY - height / 2;
                const curBoxEndX = x + lineBoxMarginX + width / 2;
                const curBoxEndY = y + lineBoxMarginY + height / 2;
                categoryBox[node.category] = {
                  ...prePos,
                  startX: prePos.startX < curBoxX ? prePos.startX : curBoxX,
                  startY: prePos.startY < curBoxY ? prePos.startY : curBoxY,
                  endX: (prePos.endX || 0) > curBoxEndX ? prePos.endX : curBoxEndX,
                  endY: (prePos.endY || 0) > curBoxEndY ? prePos.endY : curBoxEndY,
                };
              }

              curNodeMap[node.id]._external.x = x;
              curNodeMap[node.id]._external.y = y;
            } else if (direction === 'vertical') {
              curNodeMap[node.id]._external.x = startX + i * (margin.x + width);
              curNodeMap[node.id]._external.y = startY + (deepth - 1) * (margin.y + height);
            }
          },
        );
      });
    }
    return {
      nodeMap: curNodeMap,
      boxWidth: boxWidth < 0 ? 0 : boxWidth,
      boxHeight: boxHeight < 0 ? 0 : boxHeight,
      categoryBox: dataHandler.handelAddonCategory(categoryBox),
    };
  },
  handelAddonCategory: (categoryBox: any) => {
    const { addon, addon_ability, addon_addon, ...rest } = categoryBox;
    const reBox = { ...rest };
    const addonBoxs = [];
    if (addon_addon) addonBoxs.push({ ...addon_addon, key: 'addon' });
    if (addon_ability) addonBoxs.push({ ...addon_ability, key: 'ability' });
    addonBoxs.forEach((item, index) => {
      if (index === 0) {
        const { key, ...aRest } = item;
        reBox[key] = {
          ...aRest,
          startX: addon.startX,
          startY: addon.startY,
          endX: addon.endX,
        };
      } else if (index === addonBoxs.length - 1) {
        const { key, ...aRest } = item;
        reBox[key] = {
          ...aRest,
          startX: addon.startX,
          endX: addon.endX,
          endY: addon.endY,
        };
      }
    });
    return reBox;
  },
};

const renderCategoryBox = (
  { categoryBox, linkDownDistance = 0, linkTopDistance = 0 }: any,
  snap: any,
  external: any,
) => {
  const list = map(categoryBox);
  const minStartY = get(minBy(list, 'startY'), 'startY', 0) as number;
  const maxEndY = get(maxBy(list, 'endY'), 'endY', 0) as number;
  const BoxComp = external.boxEle;
  const hasAddon = !!categoryBox.addon;
  const hasAbility = !!categoryBox.ability;
  map(categoryBox, (posData: any, key: string) => {
    const { startX, endX, type, startY, endY } = posData;
    let pos = { startX, startY: minStartY - linkTopDistance, endX, endY: maxEndY + linkDownDistance };
    if (key === 'addon' && hasAbility) {
      pos.endY = endY + 10;
    } else if (key === 'ability' && hasAddon) {
      pos.startY = startY - 10;
    }
    if (!['addon', 'ability'].includes(key) && type === 'sub') {
      pos = { startX: startX - 10, startY: startY - 10, endX: endX + 10, endY: endY + 10 };
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

interface IRender {
  nodeMap: object;
  groupNodeMap?: any;
  groupChart?: any;
}
export const renderNodes = ({ nodeMap, groupNodeMap, groupChart }: IRender, snap: any, external: any) => {
  const NodeComp = external.nodeEle;
  map(nodeMap, (node: INode) => {
    const {
      NODE: { width, height },
    } = CANVAS;
    const {
      _external: { x, y, uniqName },
    } = node as any; // x,y为中心点
    const startX = x - width / 2;
    const startY = y - height / 2;
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
        nodeStyle={CANVAS.NODE}
        node={node}
        onHover={() => onHover(node)}
        outHover={() => outHover(node)}
        onClick={() => clickNode(node)}
      />,
      document.getElementById(`${nodeId}`),
    );
  });
  // 获取关联节点
  const getRelativeNodes = (_node: INode) => {
    const { category, parents: curParents = [] } = _node;
    // 微服务特殊需求
    if (category === 'microservice') {
      const fullParents = map(curParents, 'id');
      const mRelativeNode: string[] = [];
      const mUnRelativeNode: string[] = [];
      map(groupNodeMap, (item) => {
        const {
          parents,
          _external: { uniqName },
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
    const curNodeName = _node._external.uniqName as string;
    map(nodeMap, (item: INode) => {
      const {
        parents = [],
        _external: { uniqName },
      } = item;
      const parentNames = map(parents, (p: INode) => {
        return nodeMap[p.id] ? nodeMap[p.id]._external.uniqName : '';
      });
      if (uniqName === curNodeName) {
        relativeNode = relativeNode.concat(parentNames).concat(uniqName);
      } else if (parentNames.includes(curNodeName)) {
        relativeNode.push(uniqName);
      } else {
        unRelativeNode.push(uniqName);
      }
    });
    const allNodeUniqName = map(nodeMap, (item: INode) => item._external.uniqName);
    return {
      relativeNode,
      unRelativeNode: difference(allNodeUniqName, relativeNode),
    };
  };
  // 获取关联links
  const getRelativeLinks = (_node: INode) => {
    // 微服务特殊需求
    if (_node.category === 'microservice') {
      const allGroupLinks = groupChart.selectAll('.topology-link'); // 选出所有link;
      return {
        relativeLink: [],
        unRelativeLink: map(allGroupLinks),
      };
    }
    const allLinks = snap.selectAll('.topology-link'); // 选出所有link;
    const curNodeName = _node._external.uniqName;
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

  const onHover = (_node: INode) => {
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
    );
  };

  const outHover = (_node: INode) => {
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
    );
  };
  const clickNode = (_node: INode) => {
    external.onClickNode(_node);
  };
};

interface ILinkRender {
  links: ILink[];
  nodeMap: object;
  boxHeight?: number;
  groupDeepth?: any;
}
interface ILink {
  source: string;
  target: string;
  hasReverse?: boolean;
}

const getLinkTextPos = (pos: number[]) => {
  const { direction } = CANVAS;
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

// 渲染link
export const renderLinks = ({ links, nodeMap }: ILinkRender, snap: any, external: any) => {
  const { svgAttr } = CANVAS;
  const LinkComp = external.linkTextEle;
  const startMarker = snap.circle(3, 3, 3).attr({ fill: '#333' }).marker(0, 0, 8, 8, 3, 3);
  const endMarker = snap
    .image('/images/zx.svg', 0, 0, 10, 10)
    .attr({ transform: 'roate(-90deg)' })
    .marker(0, 0, 10, 10, 5, 5);

  map(links, (link: any) => {
    const {
      _external: { id, posArr, linkData, sourceNode, targetNode },
    } = link;
    const [_x, source, target] = id.split('__');
    const textData: any = find(targetNode.parents, { id: sourceNode.id });
    const textId = `text__${source}__${target}`;

    // g标签上加id，用于设置opcity属性（兼容safari）
    const g = snap.g().attr({ id: `${textId}-g` });

    const { x: textX, y: textY, z: textZ, textUnderLine } = getLinkTextPos(posArr);
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
      const allNodeUniqName = map(nodeMap, (item: INode) => item._external.uniqName);
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
      );
    };
    const outHover = (_this: any) => {
      _this.attr({ ...svgAttr.polyline });
      const allNodeUniqName = map(nodeMap, (item: INode) => item._external.uniqName);
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

    l._external = { linkData, posArr, sourceNode, targetNode };
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

// hover高亮效果
const hoverAction = (isHover: boolean, params: any, snap: any, external: any) => {
  const { relativeLink = [], relativeNode = [], unRelativeLink = [], unRelativeNode = [], hoverNode } = params;

  const { linkTextHoverAction = emptyFun, nodeHoverAction = emptyFun } = external;
  const { svgAttr } = CANVAS;
  if (isHover) {
    map(unRelativeNode, (name) => {
      if (name) {
        snap.select(`#${name}-g`).node.classList.add('topology-node-fade');
        nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
          ...external,
          hoverNode,
          isRelative: false,
        });
      }
    });
    map(relativeNode, (name) => {
      if (name) {
        snap.select(`#${name}-g`).node.classList.add('topology-node-focus');
        nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
          ...external,
          hoverNode,
          isRelative: true,
        });
      }
    });
    map(relativeLink, (link: any) => {
      const { _external: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      // 线上text
      const textId = `text__${sourceNode._external.uniqName}__${targetNode._external.uniqName}`;

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
            hoverNodeExternal: get(hoverNode, '_external'),
          });
        }
      }
      // link.attr({ ...svgAttr.polylineFoc });
    });
    map(unRelativeLink, (link: any) => {
      const { _external: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      const textId = `text__${sourceNode._external.uniqName}__${targetNode._external.uniqName}`;
      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        curText.node.classList.add('topology-link-text-fade');
        linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
          ...external,
          isRelative: false,
          hoverNode,
          targetNode,
          sourceNode,
          hoverNodeExternal: get(hoverNode, '_external'),
        });
      }
      link.attr({ ...svgAttr.polylineFade });
    });
  } else {
    map(unRelativeNode, (name) => {
      if (name) {
        snap.select(`#${name}-g`).node.classList.remove('topology-node-fade');
        nodeHoverAction &&
          nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
            ...external,
            hoverNode,
            isRelative: false,
          });
      }
    });
    map(relativeNode, (name) => {
      if (name) {
        snap.select(`#${name}-g`).node.classList.remove('topology-node-focus');
        nodeHoverAction &&
          nodeHoverAction(isHover, snap.select(`#${name}-g`).node, {
            ...external,
            hoverNode,
            isRelative: true,
          });
      }
    });
    map(relativeLink, (link: any) => {
      const { _external: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      // 线上text
      const textId = `text__${sourceNode._external.uniqName}__${targetNode._external.uniqName}`;
      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        curText.node.classList.remove('topology-link-text-focus');
        linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
          ...external,
          isRelative: false,
          hoverNode,
          targetNode,
          sourceNode,
          hoverNodeExternal: get(hoverNode, '_external'),
        });
      }
      link.attr({ ...svgAttr.polyline });
    });
    map(unRelativeLink, (link: any) => {
      const { _external: linkExternal } = link;
      const { sourceNode, targetNode } = linkExternal;
      const textId = `text__${sourceNode._external.uniqName}__${targetNode._external.uniqName}`;
      const curText = snap.select(`#${textId}-g`);
      if (curText) {
        curText.node.classList.remove('topology-link-text-fade');
        linkTextHoverAction(isHover, document.getElementById(`${textId}__text`), {
          ...external,
          isRelative: false,
          hoverNode,
          targetNode,
          sourceNode,
          hoverNodeExternal: get(hoverNode, '_external'),
        });
      }
      link.attr({ ...svgAttr.polyline });
    });
  }
};
