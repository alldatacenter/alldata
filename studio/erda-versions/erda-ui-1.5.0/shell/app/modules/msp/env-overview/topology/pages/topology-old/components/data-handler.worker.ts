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

import {
  cloneDeep,
  filter,
  find,
  flatten,
  get,
  groupBy,
  isArray,
  isEmpty,
  keys,
  map,
  maxBy,
  merge,
  min,
  minBy,
  reduce,
  set,
  sortBy,
  uniq,
  values,
} from 'lodash';
import { externalKey } from './utils';
import { CHART_CONFIG } from './config';

const chartConfig = { ...CHART_CONFIG };
const dataHandler = {
  getGroupChart: (list: TOPOLOGY.INode[]) => {
    const formatData = dataHandler.getNodesFormat(list);
    const nodeDeepthList = dataHandler.getGroupNodesDeepth(formatData);
    const chartArr = [] as any[];
    if (!isEmpty(nodeDeepthList)) {
      nodeDeepthList.forEach((g: any) => {
        const { nodeList, deepMap } = g || {};
        const curNodeIds = map(nodeList, (item) => item.id);
        let curNodeMap = {};
        map(
          filter(formatData, (item) => curNodeIds.includes(item.id)),
          (item) => {
            curNodeMap[item.id] = item;
            // 作为节点的唯一ID
            set(curNodeMap[item.id], `${externalKey}.uniqName`, `node-${item.id}`);
          },
        );
        curNodeMap = merge(curNodeMap, deepMap); // 合并节点层级属性
        const {
          nodeMap,
          boxWidth,
          boxHeight, // categoryBox,
        } = dataHandler.getNodesPosition(curNodeMap); // 节点定位

        const { links, linkTopDistance, linkDownDistance } = dataHandler.getLinks({ nodeList, nodeMap, boxHeight }); // 获取链接（包含link定位）

        let totalWidth = boxWidth;
        let totalHeight = boxHeight;
        const { direction } = chartConfig;
        // const curTopDistance = linkTopDistance > 0 ? linkTopDistance + padding.y / 2 : linkTopDistance;
        // const curDownDistance = linkDownDistance > 0 ? linkDownDistance + padding.y / 2 : linkDownDistance;
        if (direction === 'horizontal') {
          totalHeight += linkTopDistance + linkDownDistance;
        } else if (direction === 'vertical') {
          totalWidth += linkTopDistance + linkDownDistance;
        }
        const curG = {
          nodeMap, // 节点信息：包含节点层级、节点x/y坐标,
          nodeList,
          boxWidth: totalWidth, // 图宽
          boxHeight: totalHeight, // 图高
          links, // 连接线信息：包含连线坐标
          linkTopDistance, // 跨层级线高度（上方）
          linkDownDistance, // 跨层级线高度（下方）
        };
        chartArr.push(curG);
      });
    }
    return chartArr;
  },
  // 平铺节点: {name:x,parents:[...]} => [{name:x,parent:p1},...]
  getNodesFormat: (dataArr: TOPOLOGY.INode[]) => {
    if (!isArray(dataArr)) return [];
    const data = reduce(
      dataArr,
      (res: any[], item) => {
        const { id, parents, ...rest } = item;
        if (parents && parents.length > 0) {
          let pCount = 0;
          parents.forEach((p: any) => {
            const curParentId = (find(dataArr, { id: p.id }) || {}).id || '';
            // 过滤不存在的节点和自己调自己的节点
            if (curParentId && curParentId !== id) {
              pCount += 1;
              res.push({ parent: curParentId, parents, id, nodeType: 'node', ...rest });
            }
          });
          if (pCount === 0) {
            // 有父但父count为0，则可能父节点不存在或只有自己是自己的父节点
            res.push({ parent: '', id, parents, nodeType: 'node', ...rest });
          }
        } else {
          res.push({ parent: '', id, parents, nodeType: 'node', ...rest });
        }
        return res;
      },
      [],
    );
    return data;
  },
  // 获取节点组层级
  getGroupNodesDeepth: (nodeList: TOPOLOGY.INode[]): any => {
    const nodeIds = uniq(map(nodeList, (i) => i.id));
    const getTreeNodeList = (treeNodes: string[]) => {
      return filter(nodeList, (n: TOPOLOGY.INode) => treeNodes.includes(n.id));
    };
    let deepMap = {};
    // 找出每个节点开始往下遍历的最长路径，并记录节点deep
    const traversal = (nodeId: string, IdList = [] as string[], deep = 1, pNode = '') => {
      if (nodeId && !IdList.includes(nodeId)) {
        IdList.push(nodeId);
        const outTotal = filter(nodeList, { parent: nodeId }).length;
        const inTotal = filter(nodeList, { id: nodeId }).length;
        deepMap[nodeId] = { [externalKey]: { deepth: deep, outTotal, inTotal, id: nodeId } };
        const children = filter(nodeList, { parent: nodeId }) as TOPOLOGY.INode[];
        for (let i = 0; i < children.length; i++) {
          traversal(children[i].id, IdList, deep + 1, nodeId);
        }
      } else if (IdList.includes(nodeId)) {
        // 已经设置过层级的节点
        // 若当前线为环，则deep不变，已经在列，则取大deep
        const prevDeep = deepMap[nodeId][externalKey].deepth;
        const pDeep = pNode ? deepMap[pNode][externalKey].deepth : 0;
        const isCircle = dataHandler.isCircleData(nodeId, pNode, nodeList);
        /** 层级变动需要顺延的两种情况
         *  1、非循环节点，且已设置深度小于当前深度，取更深后子节点顺延
         *  2、循环节点，且当前深度等于父节点深度，避免在同一层级，顺延
         */
        if ((!isCircle && prevDeep < deep) || (isCircle && prevDeep === pDeep)) {
          deepMap[nodeId][externalKey].deepth = deep;
          // 有层级变动的节点，其下所有节点都需要顺延改变
          const children = filter(nodeList, { parent: nodeId }) as TOPOLOGY.INode[];
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
        startNodes.push(nodeIds[i]);
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
          const currentRes = { ...res };
          let isInclude = false;
          map(res, (tree, treeKey) => {
            // 有"全包含"关系的节点，比较找出路径最长的节点
            const uniqLen = uniq([...tree, ...item]).length;
            if (uniqLen === tree.length || uniqLen === item.length) {
              isInclude = true;
              if (item.length > tree.length) {
                // 写入更长的路径
                delete currentRes[treeKey];
                delete startNodesDataMap[treeKey];
                currentRes[key] = item;
                startNodesDataMap[key] = getTreeNodeList(item);
              }
            }
          });
          if (!isInclude) {
            currentRes[key] = item;
            startNodesDataMap[key] = getTreeNodeList(item);
          }
          return currentRes;
        },
        {},
      );
      startNodes = uniq(Object.keys(treeMap));
      sortTree = sortBy(
        map(
          reduce(
            treeMap,
            (res: any, item, key) => {
              const currentRes = { ...res };
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
                  delete currentRes[treeKey];
                  sameList = sameList.concat(list);
                });
                currentRes[key] = uniq([...item, ...sameList]);
              } else {
                currentRes[key] = item;
              }
              return currentRes;
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
      const list: TOPOLOGY.INode[] = [];
      nodeList.forEach((node) => {
        if (tree.includes(node.id)) {
          list.push(node);
          if (startNodes.includes(node.id)) starts.push(node.id);
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
  // 节点层级优化1：将跨层的节点往前移动
  forwardDeepth: (deepMap: any, nodeList: TOPOLOGY.INode[]) => {
    const deepthGroup = groupBy(deepMap, `${externalKey}.deepth`);
    const reMap = cloneDeep(deepMap);
    map(deepthGroup, (list: any) => {
      map(list, (item: any) => {
        const {
          [externalKey]: { id, deepth },
        } = item;
        const childrenDeep: number[] = [];
        map(nodeList, (dataItem: any) => {
          if (dataItem.parent === id) {
            childrenDeep.push(deepMap[dataItem.id][externalKey].deepth);
          }
        });
        const childMinDeep = min(childrenDeep) || 1; // 找到子的最上层级;
        if (childMinDeep - deepth > 1) {
          // 跨层级，将节点往后移动
          reMap[id][externalKey].deepth = childMinDeep - 1;
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
   * */
  backwardDeepth: (deepMap: any) => {
    const deepthGroup = map(groupBy(deepMap, `${externalKey}.deepth`));
    const reMap = cloneDeep(deepMap);
    map(deepthGroup, (list: any, index) => {
      map(list, (item: any) => {
        const {
          [externalKey]: { id },
        } = item;
        reMap[id][externalKey].deepth = index + 1;
      });
    });
    return reMap;
  },
  // 节点层级优化2：每一层节点打上sort标记
  sortDeepthNode: (deepMap: any, nodeList: TOPOLOGY.INode[]) => {
    const deepthGroup = groupBy(deepMap, `${externalKey}.deepth`);
    const reMap = cloneDeep(deepMap);
    map(deepthGroup, (list: any, lev: string) => {
      const len = list.length;
      if (lev === '1') {
        map(sortBy(list, `${externalKey}.outTotal`), ({ [externalKey]: { id, outTotal } }, i) => {
          set(reMap[id], `${externalKey}.levelSort`, outTotal * 100 + i);
        });
      } else {
        map(list, ({ [externalKey]: { id, outTotal } }, idx: number) => {
          const curNode = find(nodeList, { id });
          const { parents = [] } = curNode as TOPOLOGY.INode;
          let levelSort = idx;
          parents.forEach((p: any) => {
            const pMap = get(reMap, `[${p.id}].${externalKey}`);
            if (pMap && Number(lev) - Number(pMap.deepth) === 1) {
              // 上层父
              levelSort = pMap.levelSort > levelSort ? levelSort : pMap.levelSort;
            }
          });
          set(reMap[id], `${externalKey}.levelSort`, levelSort);
        });
      }
    });
    return reMap;
  },
  getCountDeepMap: (nodeList: TOPOLOGY.INode[], starts: string[]) => {
    const deepMap = {};
    // 找出每个节点开始往下遍历的最长路径，并记录节点deep
    const traversal = (nodeId: string, deep = 1, pNode = '') => {
      if (!deepMap[nodeId]) {
        const outTotal = filter(nodeList, { parent: nodeId }).length;
        const inTotal = filter(nodeList, { id: nodeId }).length;
        deepMap[nodeId] = { [externalKey]: { deepth: deep, outTotal, inTotal, id: nodeId } };
        const children = filter(nodeList, { parent: nodeId }) as TOPOLOGY.INode[];
        for (let i = 0; i < children.length; i++) {
          traversal(children[i].id, deep + 1, nodeId);
        }
      } else if (deepMap[nodeId]) {
        // 已经设置过层级的节点
        // 若当前线为环，则deep不变，已经在列，则取大deep
        const prevDeep = deepMap[nodeId][externalKey].deepth;
        const pDeep = pNode ? deepMap[pNode][externalKey].deepth : 0;
        const isCircle = dataHandler.isCircleData(nodeId, pNode, nodeList);
        /** 层级变动需要顺延的两种情况
         *  1、非循环节点，且已设置深度小于当前深度，取更深后子节点顺延
         *  2、循环节点，且当前深度等于父节点深度，避免在同一层级，顺延
         */

        if ((!isCircle && prevDeep < deep) || (isCircle && prevDeep === pDeep)) {
          deepMap[nodeId][externalKey].deepth = deep;
          // 有层级变动的节点，其下所有节点都需要顺延改变
          const children = filter(nodeList, { parent: nodeId }) as TOPOLOGY.INode[];
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
  isCircleData: (n1: string, n2: string, nodeList: TOPOLOGY.INode[]) => {
    const data1 = find(nodeList, { parent: n1, id: n2 });
    const data2 = find(nodeList, { parent: n2, id: n1 });
    if (data1 && data2) return true;
    const getChildren = (nodeId: string, children: string[] = []) => {
      if (!children.includes(nodeId)) {
        children.push(nodeId);
        const childrenList = filter(nodeList, { parent: nodeId }) as TOPOLOGY.INode[];
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
  },
  // 获取节点位置信息
  getNodesPosition: (nodeMap: object) => {
    let boxWidth = 0;
    let boxHeight = 0;
    const {
      NODE: { width, height, margin },
      direction,
      padding,
    } = chartConfig;
    const curNodeMap = cloneDeep(nodeMap);
    if (!isEmpty(curNodeMap)) {
      const deepthGroup = groupBy(curNodeMap, `${externalKey}.deepth`);
      const maxColumn = (maxBy(values(deepthGroup)) || []).length;
      const rowNum = keys(deepthGroup).length;
      if (direction === 'horizontal') {
        boxWidth = (width + margin.x) * rowNum - margin.x + padding.x * 2;
        boxHeight = (height + margin.y) * maxColumn - margin.y + padding.y * 2;
      } else if (direction === 'vertical') {
        // TODO
      }

      map(deepthGroup, (list: TOPOLOGY.INode[], deepth: number) => {
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
        map(sortBy(list, `${externalKey}.levelSort`), (node: TOPOLOGY.INode, i: number) => {
          // 每一个层级的最上和最下，在此做标记，用于画跨层级线
          if (direction === 'horizontal') {
            const x = startX + (deepth - 1) * (margin.x + width);
            const y = startY + i * (margin.y + height);
            curNodeMap[node.id][externalKey].x = x;
            curNodeMap[node.id][externalKey].y = y;
          } else if (direction === 'vertical') {
            // TODO
            // curNodeMap[node.id][externalKey].x = startX + i * (margin.x + width);
            // curNodeMap[node.id][externalKey].y = startY + (deepth - 1) * (margin.y + height);
          }
        });
      });
    }
    return {
      nodeMap: curNodeMap,
      boxWidth: boxWidth < 0 ? 0 : boxWidth,
      boxHeight: boxHeight < 0 ? 0 : boxHeight,
    };
  },
  // 获取图links
  getLinks: (linkProps: {
    nodeList: TOPOLOGY.INode[];
    nodeMap: object;
    boxHeight: number;
    exceptLink?: any[];
    isCross?: boolean;
  }) => {
    const { nodeList, nodeMap, boxHeight, exceptLink = [], isCross = false } = linkProps;
    const links = [] as any;
    nodeList.forEach((node: TOPOLOGY.INode) => {
      const { parent, id } = node;
      if (parent) {
        const curLink = find(exceptLink, (item) => item.source === parent && item.target === id);
        if (!curLink) {
          const lk = { source: parent, target: id, nodeType: 'link' } as any;
          if (find(nodeList, { id: parent, parent: id })) {
            // 存在反向线
            lk.hasReverse = true;
          }
          links.push(lk);
        }
      }
    });
    return dataHandler.getLinkPosition({ nodeMap, links, boxHeight, isCross });
  },
  // 获取节点links的位置
  getLinkPosition: ({ nodeMap, links, boxHeight, isCross }: any) => {
    const {
      NODE: { width, margin, height },
      direction,
      boxMargin,
      LINK: { linkDis },
    } = chartConfig;

    const halfWidth = width / 2;
    const halfMaginX = margin.x / 2;
    const halfHeight = height / 2;

    const deepthGroup = groupBy(nodeMap, `${externalKey}.deepth`);
    const edgePlusMap = {};
    const getDeepthHeightDistance = (deepthKey: string) => {
      const curGroup = get(deepthGroup[deepthKey], `[0].${externalKey}.group`);
      const curSubGroupLevel = get(deepthGroup[deepthKey], `[0].${externalKey}.subGroupLevel`);
      if (isCross && curGroup === 'service') {
        // 如果是跨层级节点
        const { maxNode, minNode } = getDeepthEdgeNode(curSubGroupLevel);
        return get(maxNode, `${externalKey}.y`, 0) - get(minNode, `${externalKey}.y`, 0);
      }
      return (
        get(maxBy(deepthGroup[deepthKey], `${externalKey}.y`), `${externalKey}.y`, 0) -
        get(minBy(deepthGroup[deepthKey], `${externalKey}.y`), `${externalKey}.y`, 0)
      );
    };

    const getDeepthEdgeNode = (subLevel: number) => {
      const allList = flatten(map(deepthGroup));
      const serviceList = filter(allList, (item) => get(item, `${externalKey}.group`) === 'service');
      const serviceGroup = groupBy(serviceList, `${externalKey}.subGroupLevel`);

      const maxNode = maxBy(serviceGroup[subLevel], (item) => {
        const curExternal = item[externalKey];
        return curExternal.y;
      });
      const minNode = minBy(serviceGroup[subLevel], (item) => {
        const curExternal = item[externalKey];
        return curExternal.y;
      });
      return { maxNode, minNode };
    };

    map(deepthGroup, (gList: any, lev) => {
      // 每个层级上跨层级线的边缘叠加数
      const curGroup = get(gList, `[0].${externalKey}.group`);
      let curStartNode: any = minBy(gList, `${externalKey}.y`);
      let curEndEdgeNode: any = maxBy(gList, `${externalKey}.y`);
      if (isCross && curGroup === 'service') {
        const subLevel = get(deepthGroup[lev], `[0].${externalKey}.subGroupLevel`);
        const { maxNode, minNode } = getDeepthEdgeNode(subLevel);
        curStartNode = minNode;
        curEndEdgeNode = maxNode;
      }
      edgePlusMap[lev] = {
        startX: curStartNode[externalKey].x - halfWidth,
        originStartX: curStartNode[externalKey].x - halfWidth,
        startY: curStartNode[externalKey].y - halfHeight - (isCross ? boxMargin.y * 2 : 0),
        originStartY: curStartNode[externalKey].y - halfHeight - (isCross ? boxMargin.y * 2 : 0), // curStartNode[externalKey].y - halfHeight,
        endX: curEndEdgeNode[externalKey].x + halfWidth,
        originEndX: curEndEdgeNode[externalKey].x + halfWidth,
        endY: curEndEdgeNode[externalKey].y + halfHeight,
        originEndY: curEndEdgeNode[externalKey].y + halfHeight,
      };
    });

    // const maxColumn: any = (maxBy(values(deepthGroup)) || []);
    // const edgeStartNode = get(minBy(maxColumn, `${externalKey}.y`), externalKey) as any;
    // const edgeEndNode = get(maxBy(maxColumn, `${externalKey}.y`), externalKey) as any;
    // const centerY = edgeStartNode.y + (edgeEndNode.y - edgeStartNode.y) / 2;
    // TODO：vertical
    // const centerX = edgeStartNode.x + (edgeEndNode.x - edgeStartNode.x) / 2;
    const centerY = boxHeight / 2;

    const reLinks = map(links, (link) => {
      const { source, target, hasReverse } = link;
      const sourceNode = nodeMap[source][externalKey];
      const targetNode = nodeMap[target][externalKey];
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

          const sourceDeepth = nodeMap[source][externalKey].deepth;
          const targetDeepth = nodeMap[target][externalKey].deepth;

          let betweenMaxDeepth = 0;
          // const betweenMaxLen = 0;
          let betweenMaxHeight = 0;
          // 计算跨层级中间最高的层级，最高层级的数据长度
          map(edgePlusMap, (pos, deepKey) => {
            if (
              (Number(deepKey) > sourceDeepth && Number(deepKey) < targetDeepth) ||
              (Number(deepKey) < sourceDeepth && Number(deepKey) > targetDeepth)
            ) {
              // if (deepthGroup[deepKey].length > betweenMaxLen) {
              //   betweenMaxLen = deepthGroup[deepKey].length;
              //   betweenMaxDeepth = Number(deepKey);
              // }
              const curBetweenMaxHeight = getDeepthHeightDistance(deepKey);
              if (curBetweenMaxHeight > betweenMaxHeight) {
                betweenMaxDeepth = Number(deepKey);
                betweenMaxHeight = curBetweenMaxHeight;
              }
            }
          });
          // const sourceLen = deepthGroup[sourceDeepth].length;
          // const targetLen = deepthGroup[targetDeepth].length;
          const sourceHeightDis = getDeepthHeightDistance(sourceDeepth);
          const targetHeightDis = getDeepthHeightDistance(targetDeepth);
          const curMaxDeep: number = get(
            maxBy(
              [
                // { deep: sourceDeepth, len: sourceLen },
                // { deep: targetDeepth, len: targetLen },
                // { deep: betweenMaxDeepth, len: betweenMaxLen },
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
        [externalKey]: {
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
      linkDownDistance: downObj.endY - downObj.originEndY,
    };
  },
};

self.addEventListener(
  'message',
  (e: MessageEvent<{ type: 'open' | 'close'; data: TOPOLOGY.ITopologyResp['nodes'] }>) => {
    const { data, type } = e.data;
    if (type === 'close') {
      self.close();
    } else {
      let nodeGroup = [];
      try {
        nodeGroup = dataHandler.getGroupChart(data);
      } catch (_) {}
      self.postMessage(nodeGroup);
    }
  },
  false,
);
