/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ORIGINAL_TYPE_MAP } from 'app/pages/DashBoardPage/constants';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { TabWidgetContent, WidgetInfo } from '../../../Board/slice/types';
import { EventLayerNode, LayerNode } from './LayerTreeItem';

export const widgetMapToTree = (args: {
  widgetMap: Record<string, Widget>;
  widgetInfoMap: Record<string, WidgetInfo>;
  parentId: string;
  tree: LayerNode[] | undefined;
}) => {
  const { widgetMap, widgetInfoMap, parentId, tree } = args;

  const widgets = Object.values(widgetMap).filter(widget => {
    if (!parentId && !widget.parentId) {
      return true;
    }
    return widget.parentId === parentId;
  });
  if (!widgets.length) return [];
  let sortedWidgets = widgets;

  if (
    widgetMap[parentId] &&
    widgetMap[parentId].config.originalType === ORIGINAL_TYPE_MAP.tab
  ) {
    // tab
    const itemMap = (widgetMap[parentId].config.content as TabWidgetContent)
      .itemMap;
    const items = Object.values(itemMap).sort((b, a) => a.index - b.index);

    sortedWidgets = items
      .map(item => {
        return widgetMap[item.childWidgetId];
      })
      .filter(item => !!item);
  } else {
    // 普通group
    sortedWidgets = widgets.sort((a, b) => {
      return b.config.index - a.config.index;
    });
  }
  if (!sortedWidgets.length) return [];
  sortedWidgets.forEach(widget => {
    const treeNode: LayerNode = {
      key: widget.id,
      widgetIndex: widget.config.index,
      parentId,
      title: widget.config.name,
      isLeaf: true,
      children: [],
      boardId: widget.dashboardId,
      content: widget.config.content,
      originalType: widget.config.originalType,
      selected: widgetInfoMap[widget.id].selected,
    };

    if (widget.config.originalType === ORIGINAL_TYPE_MAP.group) {
      treeNode.isLeaf = false;
      treeNode.children = widgetMapToTree({
        widgetMap,
        widgetInfoMap,
        parentId: widget.id,
        tree: treeNode.children,
      });
    } else if (widget.config.originalType === ORIGINAL_TYPE_MAP.tab) {
      treeNode.isLeaf = false;
      treeNode.children = widgetMapToTree({
        widgetMap,
        widgetInfoMap,
        parentId: widget.id,
        tree: treeNode.children,
      });
    }
    tree?.push(treeNode);
  });
  return tree as LayerNode[];
};

export function parentIsGroup(
  widgetMap: Record<string, Widget>,
  parentId?: string,
) {
  if (!parentId) return true;
  if (
    widgetMap[parentId] &&
    widgetMap[parentId].config.originalType !== ORIGINAL_TYPE_MAP.group
  ) {
    return false;
  }
  return true;
}

export function getChildrenValList(
  widgetMap: Record<string, Widget>,
  pid: string,
) {
  const widgetList = Object.values(widgetMap).filter(
    widget => widget.parentId === pid,
  );

  if (parentIsGroup(widgetMap, pid)) {
    // group
    return widgetList
      .map(widget => ({
        id: widget.id,
        index: widget.config.index,
      }))
      .sort((b, a) => a.index - b.index);
  } else {
    // container
    const container = widgetMap[pid];
    const childItemMap = (container.config.content as TabWidgetContent).itemMap;
    return Object.values(childItemMap || {})
      .map(item => ({
        id: item.childWidgetId,
        index: item.index,
      }))
      .sort((b, a) => a.index - b.index);
  }
}
export function getNewDragNodeValue(args: {
  widgetMap: Record<string, Widget>;
  dragNode: EventLayerNode;
  targetNode: EventLayerNode;
}) {
  const { widgetMap, dragNode, targetNode } = args;
  const newVal = {
    index: 0,
    parentId: targetNode.parentId,
    parentIsGroup: true,
  };
  if (targetNode.dragOverGapTop) {
    const indexList = getChildrenValList(widgetMap, targetNode.parentId);
    newVal.index = indexList[0].index + 1;

    return newVal;
  }
  if (targetNode.dragOver) {
    if (targetNode.isLeaf) {
      // dragOver Leaf
      const indexList = getChildrenValList(widgetMap, targetNode.parentId);
      const targetIndex = indexList.findIndex(t => t.id === targetNode.key);
      if (targetIndex < indexList.length - 1) {
        let indexA = indexList[targetIndex].index;
        let indexC = indexList[targetIndex + 1].index;
        newVal.index = (indexA + indexC) / 2;
      } else {
        newVal.index = indexList[targetIndex].index - 1;
      }
      newVal.parentId = targetNode.parentId;
      newVal.parentIsGroup = parentIsGroup(widgetMap, targetNode.parentId);
      return newVal;
    } else {
      // dragOver folder
      const indexList = getChildrenValList(widgetMap, targetNode.key);
      if (indexList.length < 1) {
        newVal.index = dragNode.widgetIndex;
      } else {
        newVal.index = indexList[0].index + 1;
      }
      newVal.parentId = targetNode.key;
      newVal.parentIsGroup = parentIsGroup(widgetMap, targetNode.key);
      return newVal;
    }
  } else if (!targetNode.dragOver) {
    const indexList = getChildrenValList(widgetMap, targetNode.parentId);
    const targetIndex = indexList.findIndex(t => t.id === targetNode.key);
    if (targetIndex < indexList.length - 1) {
      let indexA = indexList[targetIndex].index;
      let indexC = indexList[targetIndex + 1].index;
      newVal.index = (indexA + indexC) / 2;
    } else {
      newVal.index = indexList[targetIndex].index - 1;
    }
    newVal.parentId = targetNode.parentId;
    newVal.parentIsGroup = parentIsGroup(widgetMap, targetNode.parentId);
    return newVal;
  }
  return newVal;
}
