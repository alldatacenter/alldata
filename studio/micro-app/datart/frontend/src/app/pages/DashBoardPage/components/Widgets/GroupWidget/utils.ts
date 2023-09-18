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
import { RectConfig } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from '../../../types/widgetTypes';

export const getParentRect = (args: {
  childIds: string[] | undefined;
  widgetMap: Record<string, Widget>;
  preRect: RectConfig;
}) => {
  const { childIds, widgetMap, preRect } = args;
  if (!Array.isArray(childIds)) return preRect;

  const rectList = Object.values(widgetMap)
    .filter(w => childIds.includes(w.id))
    .map(t => t.config.rect);

  if (!rectList.length) return preRect;
  let left;
  let top;
  let right;
  let bottom;
  rectList.forEach(rect => {
    if (left === undefined || left > rect.x) left = rect.x;

    if (top === undefined || top > rect.y) top = rect.y;

    const rectRight = rect.x + rect.width;
    if (right === undefined || right < rectRight) right = rectRight;

    const rectBottom = rect.y + rect.height;
    if (bottom === undefined || bottom < rectBottom) bottom = rectBottom;
  });
  const newRect: RectConfig = {
    x: left || 0,
    y: top || 0,
    width: right - left || 0,
    height: bottom - top || 0,
  };
  return newRect;
};

export const findChildIds = (args: {
  widget: Widget;
  widgetMap: Record<string, Widget>;
  childIds: string[];
}) => {
  const { widget, widgetMap, childIds } = args;
  if (!widget) return;
  if (widget.config.originalType !== ORIGINAL_TYPE_MAP.group) return;
  widget.config.children?.forEach(id => {
    childIds.push(id);
    findChildIds({ widget: widgetMap[id], widgetMap, childIds });
  });
};

export const findParentIds = (args: {
  widget: Widget;
  widgetMap: Record<string, Widget>;
  parentIds: string[];
}) => {
  const { widget, widgetMap, parentIds } = args;
  if (!widget) return;
  if (!widget.parentId) return;
  if (!widgetMap[widget.parentId]) return;
  const parentWidget = widgetMap[widget.parentId];
  if (parentWidget.config.originalType === ORIGINAL_TYPE_MAP.group) {
    parentIds.push(parentWidget.id);
    findParentIds({ widget: parentWidget, widgetMap, parentIds });
  }
};

export const adjustGroupWidgets = (args: {
  groupIds: string[];
  widgetMap: Record<string, Widget>;
  isAutoGroupWidget?: boolean;
}) => {
  const { groupIds, widgetMap } = args;
  groupIds.forEach(gid => {
    const curGroup = widgetMap[gid];
    if (!curGroup) return;
    if (curGroup.config.originalType !== ORIGINAL_TYPE_MAP.group) return;
    if (args.isAutoGroupWidget) {
      return;
    }
    if (!curGroup.config.children) {
      delete widgetMap[gid];
      return;
    }

    const newChildren = curGroup.config.children?.filter(id => widgetMap[id]);
    if (!newChildren.length) {
      delete widgetMap[curGroup.id];
      return;
    }
    curGroup.config.rect = getParentRect({
      childIds: curGroup.config.children,
      widgetMap,
      preRect: curGroup.config.rect,
    });
  });
};

export const moveGroupAllChildren = (args: {
  childIds: string[];
  widgetMap: Record<string, Widget>;
  diffRect: RectConfig;
}) => {
  const { childIds, widgetMap, diffRect } = args;
  childIds.forEach(id => {
    const curWidget = widgetMap[id];
    if (!curWidget) return;
    const oldRect = curWidget.config.rect;
    curWidget.config.rect.x = oldRect.x + diffRect.x;
    curWidget.config.rect.y = oldRect.y + diffRect.y;
  });
};
export const resetGroupAllChildrenRect = (args: {
  childIds: string[];
  widgetMap: Record<string, Widget>;
  oldRect: RectConfig;
  newRect: RectConfig;
}) => {
  const { childIds, widgetMap, newRect, oldRect } = args;
  const scaleW = newRect.width / oldRect.width;
  const scaleH = newRect.height / oldRect.height;
  childIds?.forEach(id => {
    const curWidget = widgetMap[id];
    if (!curWidget) return;
    const preRect = curWidget.config.rect;
    const nextRect: RectConfig = {
      x: (preRect.x - oldRect.x) * scaleW + oldRect.x,
      y: (preRect.y - oldRect.y) * scaleH + oldRect.y,
      width: preRect.width * scaleW,
      height: preRect.height * scaleH,
    };
    curWidget.config.rect = nextRect;
  });
};
