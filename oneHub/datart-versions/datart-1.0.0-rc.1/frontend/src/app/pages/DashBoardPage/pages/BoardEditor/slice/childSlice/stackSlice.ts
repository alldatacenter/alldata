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

import { PayloadAction } from '@reduxjs/toolkit';
import widgetManager from 'app/pages/DashBoardPage/components/WidgetManager';
import {
  adjustGroupWidgets,
  findChildIds,
  findParentIds,
  moveGroupAllChildren,
  resetGroupAllChildrenRect,
} from 'app/pages/DashBoardPage/components/Widgets/GroupWidget/utils';
import {
  ContainerItem,
  Dashboard,
  DeviceType,
  MediaWidgetContent,
  RectConfig,
  TabWidgetContent,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget, WidgetConf } from 'app/pages/DashBoardPage/types/widgetTypes';
import { Variable } from 'app/pages/MainPage/pages/VariablePage/slice/types';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateCollectionByAction } from 'app/utils/mutation';
import produce from 'immer';
import { Layout } from 'react-grid-layout';
import { createSlice } from 'utils/@reduxjs/toolkit';
import { ORIGINAL_TYPE_MAP } from '../../../../constants';
import { EditBoardStack } from '../types';

export type updateWidgetConf = {
  id: string;
  config: WidgetConf;
};
export const initEditBoardState: EditBoardStack = {
  dashBoard: {} as Dashboard,
  widgetRecord: {},
};
// editBoardStackActions
export const editBoardStackSlice = createSlice({
  name: 'editBoard',
  initialState: initEditBoardState,
  reducers: {
    // styles: updateCollectionByAction(state.styles || [], {
    //   ancestors: action.payload.ancestors!,
    //   value: action.payload.value,
    // }),
    updateBoardConfigByKey(
      state,
      action: PayloadAction<{
        ancestors: number[];
        configItem: ChartStyleConfig;
      }>,
    ) {
      const { ancestors, configItem } = action.payload;

      state.dashBoard.config.jsonConfig.props = updateCollectionByAction(
        state.dashBoard.config.jsonConfig.props || [],
        {
          ancestors: ancestors!,
          value: configItem,
        },
      );
    },
    setBoardToEditStack(state, action: PayloadAction<EditBoardStack>) {
      const record = action.payload;
      Object.keys(record).forEach(key => {
        state[key] = record[key];
      });
    },
    updateBoard(state, action: PayloadAction<Dashboard>) {
      state.dashBoard = action.payload;
    },

    updateBoardConfig(state, action: PayloadAction<{}>) {
      // state.dashBoard.config = action.payload;
    },
    updateQueryVariables(state, action: PayloadAction<Variable[]>) {
      const variables = action.payload;
      state.dashBoard.queryVariables = variables;
    },

    // Widget
    addWidgets(state, action: PayloadAction<Widget[]>) {
      const widgets = action.payload;
      const board = state.dashBoard;
      const { type } = board.config;
      let maxWidgetIndex = 0;
      const widgetList = Object.values(state.widgetRecord || {});
      if (widgetList.length) {
        maxWidgetIndex = widgetList
          .map(w => w.config.index)
          .sort((b, a) => a - b)[0];
      }
      widgets.forEach(ele => {
        maxWidgetIndex++;
        const newName = widgetManager
          .toolkit(ele.config.originalType)
          .getName();
        const newEle = produce(ele, draft => {
          draft.dashboardId = board.id;
          draft.config.index = maxWidgetIndex;
          draft.config.name =
            draft.config.name ||
            `${newName}_${Number(maxWidgetIndex.toFixed(4)) * 1000}`;
          draft.config.boardType = type;
        });

        state.widgetRecord[newEle.id] = newEle;
        if (newEle.parentId && state.widgetRecord[newEle.parentId]) {
          if (
            !state.widgetRecord[newEle.parentId].config.children?.includes(
              newEle.id,
            )
          ) {
            state.widgetRecord[newEle.parentId].config.children?.push(
              newEle.id,
            );
          }
        }
      });
    },
    //
    toggleLockWidget(
      state,
      action: PayloadAction<{ id: string; lock: boolean }>,
    ) {
      const { id, lock } = action.payload;
      if (state.widgetRecord?.[id]?.config) {
        state.widgetRecord[id].config.lock = lock;
      }
    },
    changeWidgetsParentId(
      state,
      action: PayloadAction<{
        items: {
          wid: string;
          nextIndex: number;
          parentId: string;
        }[];
      }>,
    ) {
      const { items } = action.payload;
      items.forEach(({ wid, nextIndex, parentId }) => {
        const prePId = state.widgetRecord[wid].parentId;
        if (prePId && state.widgetRecord[prePId]) {
          const preParent = state.widgetRecord[prePId];
          const oldChildren = preParent.config.children;
          preParent.config.children = oldChildren?.filter(id => id !== wid);
          adjustGroupWidgets({
            groupIds: [prePId],
            widgetMap: state.widgetRecord,
          });
        }
        if (state.widgetRecord?.[wid]) {
          state.widgetRecord[wid].parentId = parentId;
          state.widgetRecord[wid].config.index = nextIndex;
        }
        const nextParent = state.widgetRecord[parentId];
        if (nextParent) {
          let pChildren = nextParent.config.children;
          if (!pChildren?.includes(wid)) {
            pChildren?.push(wid);
          }
          adjustGroupWidgets({
            groupIds: [nextParent.id],
            widgetMap: state.widgetRecord,
          });
        }
      });
    },
    deleteWidgets(state, action: PayloadAction<string[]>) {
      const ids = action.payload;
      if (!ids?.length) return;

      ids.forEach(id => {
        const targetWidget = state.widgetRecord?.[id];
        const parent = state.widgetRecord?.[targetWidget.parentId];
        if (parent) {
          parent.config.children = parent.config.children?.filter(
            t => t !== targetWidget.id,
          );
        }
        delete state.widgetRecord[id];
      });
    },

    updateWidget(state, action: PayloadAction<Widget>) {
      const widget = action.payload;
      state.widgetRecord[widget.id] = widget;
    },
    updateWidgetStyleConfigByPath(
      state,
      action: PayloadAction<{
        wid: string;
        ancestors: number[];
        configItem: ChartStyleConfig;
      }>,
    ) {
      const { ancestors, configItem, wid } = action.payload;
      if (!state.widgetRecord[wid]) return;
      const newProps = updateCollectionByAction(
        state.widgetRecord[wid].config.customConfig.props || [],
        {
          ancestors: ancestors!,
          value: configItem,
        },
      );
      state.widgetRecord[wid].config.customConfig.props = newProps;
    },
    updateWidgetInteractionConfigByPath(
      state,
      action: PayloadAction<{
        wid: string;
        ancestors: number[];
        configItem: ChartStyleConfig;
      }>,
    ) {
      const { ancestors, configItem, wid } = action.payload;
      if (!state.widgetRecord[wid]) return;
      const newProps = updateCollectionByAction(
        state.widgetRecord[wid].config.customConfig.interactions || [],
        {
          ancestors: ancestors!,
          value: configItem,
        },
      );
      state.widgetRecord[wid].config.customConfig.interactions = newProps;
    },
    updateWidgetConfig(
      state,
      action: PayloadAction<{ wid: string; config: WidgetConf }>,
    ) {
      const { wid, config } = action.payload;
      state.widgetRecord[wid].config = config;
    },
    updateWidgetConfigByKey(
      state,
      action: PayloadAction<{ wid: string; key: string; val }>,
    ) {
      const { wid, key, val } = action.payload;
      if (!state.widgetRecord?.[wid]?.config) return;
      state.widgetRecord[wid].config[key] = val;
    },
    updateWidgetsConfig(state, action: PayloadAction<updateWidgetConf[]>) {
      const nextWidgetConfigs = action.payload;
      nextWidgetConfigs.forEach(item => {
        state.widgetRecord[item.id].config = item.config;
      });
    },
    clearWidgetConfig(state) {
      Object.keys(state.widgetRecord || []).forEach(key => {
        if (state.widgetRecord) {
          delete state.widgetRecord[key];
        }
      });
    },

    changeAutoBoardWidgetsRect(
      state,
      action: PayloadAction<{ layouts: Layout[]; deviceType: DeviceType }>,
    ) {
      const { layouts, deviceType } = action.payload;
      layouts.forEach(it => {
        const { i, x, y, w, h } = it;
        if (!state.widgetRecord?.[i]?.config) return;
        const rectItem = { x, y, width: w, height: h };
        if (deviceType === DeviceType.Desktop) {
          state.widgetRecord[i].config.pRect = rectItem;
        }
        if (deviceType === DeviceType.Mobile) {
          state.widgetRecord[i].config.mRect = rectItem;
        }
      });
    },

    // free

    changeWidgetsIndex(
      state,
      action: PayloadAction<{ id: string; index: number }[]>,
    ) {
      const opts = action.payload;
      opts.forEach(it => {
        const { id, index } = it;
        state.widgetRecord[id].config.index = index;
      });
    },
    // group
    changeFreeWidgetRect(
      state,
      action: PayloadAction<{
        boardId?: string;
        wid: string;
        rect: RectConfig;
        isAutoGroupWidget: boolean;
      }>,
    ) {
      const { wid, rect: newRect } = action.payload;
      const widgetMap = state.widgetRecord;
      const targetWidget = widgetMap[wid];
      if (!targetWidget) return;
      const oldRect = targetWidget.config.rect;
      const diffRect: RectConfig = {
        x: newRect.x - oldRect.x,
        y: newRect.y - oldRect.y,
        width: newRect.width - oldRect.width,
        height: newRect.height - oldRect.height,
      };
      targetWidget.config.rect = newRect;

      // NOTE: if group is auto, should not adjust all children and itself rect.
      const isAutoGroupWidget = action.payload.isAutoGroupWidget;
      if (isAutoGroupWidget) {
        return;
      }

      if (
        !targetWidget.parentId &&
        targetWidget.config.originalType !== ORIGINAL_TYPE_MAP.group
      ) {
        return;
      }

      const hasMoveEvent = diffRect.x !== 0 || diffRect.y !== 0;
      const hasResizeEvent = diffRect.width !== 0 || diffRect.height !== 0;

      if (hasMoveEvent) {
        // handle children : collect all children and move them
        const childIds: string[] = [];
        findChildIds({ widget: targetWidget, widgetMap, childIds });
        moveGroupAllChildren({ childIds, widgetMap, diffRect });
        // handle parents : collect all parents and resetParentsRect
        const parentIds: string[] = [];
        findParentIds({ widget: targetWidget, widgetMap, parentIds });
        adjustGroupWidgets({
          groupIds: parentIds,
          widgetMap,
        });
      }

      if (hasResizeEvent) {
        // handle children : collect all children and resize them
        const childIds: string[] = [];
        findChildIds({ widget: targetWidget, widgetMap, childIds });
        resetGroupAllChildrenRect({ childIds, widgetMap, oldRect, newRect });
        // handle parents : collect all parents and resetParentsRect
        const parentIds: string[] = [];
        findParentIds({ widget: targetWidget, widgetMap, parentIds });
        adjustGroupWidgets({
          groupIds: parentIds,
          widgetMap,
        });
      }
    },
    adjustGroupWidgets(
      state,
      action: PayloadAction<{
        groupIds: string[];
        isAutoGroupWidget?: boolean;
      }>,
    ) {
      const { groupIds, isAutoGroupWidget } = action.payload;
      const widgetMap = state.widgetRecord;
      adjustGroupWidgets({ groupIds: groupIds, widgetMap, isAutoGroupWidget });
    },
    /* tabs widget */
    addWidgetToTabWidget(
      state,
      action: PayloadAction<{
        tabWidgetId: string;
        tabItem: ContainerItem;
        sourceId: string;
      }>,
    ) {
      const { tabWidgetId, tabItem, sourceId } = action.payload;
      const tabContent = state.widgetRecord[tabWidgetId].config
        .content as TabWidgetContent;
      const sourceWidget = state.widgetRecord[sourceId];
      tabContent.itemMap[sourceWidget.config.clientId] = {
        ...tabItem,
        name: sourceWidget.config.name,
        tabId: sourceWidget.config.clientId,
        childWidgetId: sourceWidget.id,
      };
      delete state.widgetRecord[tabWidgetId].config.content.itemMap[
        tabItem.tabId
      ];
      state.widgetRecord[tabWidgetId].config.content = tabContent;
      state.widgetRecord[sourceId].parentId = tabWidgetId;
    },

    tabsWidgetAddTab(
      state,
      action: PayloadAction<{
        parentId: string;
        tabItem: ContainerItem;
      }>,
    ) {
      const { parentId, tabItem } = action.payload;

      const tabContent = state.widgetRecord[parentId].config
        .content as TabWidgetContent;

      tabContent.itemMap[tabItem.tabId] = tabItem;
    },
    tabsWidgetRemoveTab(
      state,
      action: PayloadAction<{
        parentId: string;
        sourceTabId: string;
        mode: string;
      }>,
    ) {
      const { parentId, sourceTabId } = action.payload;
      const tabWidget = state.widgetRecord[parentId];
      const tabContent = tabWidget.config.content as TabWidgetContent;

      const tabItem = tabContent.itemMap[sourceTabId];

      delete tabContent.itemMap[sourceTabId];
      const itemWidget = state.widgetRecord[tabItem.childWidgetId];
      if (itemWidget) {
        itemWidget.parentId = tabWidget.parentId;
      }
    },

    dropWidgetToGroup(
      state,
      action: PayloadAction<{
        sourceId: string;
        newIndex: number;
        targetId: string;
      }>,
    ) {
      const widgetMap = state.widgetRecord;
      const { sourceId, newIndex, targetId } = action.payload;

      const dragWidget = widgetMap[sourceId];
      //
      const dragParent = widgetMap[dragWidget.parentId || ''];
      if (dragParent) {
        dragParent.config.children = dragParent.config.children?.filter(
          t => t !== dragWidget.id,
        );

        if (dragParent.config.originalType === ORIGINAL_TYPE_MAP.tab) {
          const srcTabItemMap = (dragParent.config.content as TabWidgetContent)
            .itemMap;

          const srcItem = Object.values(srcTabItemMap).find(
            item => item.childWidgetId === dragWidget.id,
          );
          if (srcItem) {
            delete srcTabItemMap[srcItem.tabId];
          }
        }
      }
      //
      dragWidget.config.index = newIndex;
      dragWidget.parentId = targetId;
      if (widgetMap[targetId]) {
        widgetMap[targetId].config.children?.push(dragWidget.id);
      }
      //
    },

    dropWidgetToTab(
      state,
      action: PayloadAction<{
        newItem: ContainerItem;
        targetId: string;
      }>,
    ) {
      const { newItem, targetId } = action.payload;
      const widgetMap = state.widgetRecord;
      const dragWidget = widgetMap[newItem.childWidgetId];
      //
      const dragParent = widgetMap[dragWidget.parentId || ''];
      if (dragParent) {
        dragParent.config.children = dragParent.config.children?.filter(
          t => t !== dragWidget.id,
        );
        if (dragParent.config.originalType === ORIGINAL_TYPE_MAP.tab) {
          const srcTabItemMap = (dragParent.config.content as TabWidgetContent)
            .itemMap;

          const srcItem = Object.values(srcTabItemMap).find(
            item => item.childWidgetId === dragWidget.id,
          );
          if (srcItem) {
            delete srcTabItemMap[srcItem.tabId];
          }
        }
      }

      //
      const targetTabWidget = widgetMap[targetId];
      const targetTabItemMap = (
        targetTabWidget.config.content as TabWidgetContent
      ).itemMap;
      targetTabItemMap[dragWidget.config.clientId] = newItem;
      dragWidget.parentId = targetId;

      //
    },
    /* MediaWidgetConfig */
    changeMediaWidgetConfig(
      state,
      action: PayloadAction<{
        id: string;
        mediaWidgetContent: MediaWidgetContent;
      }>,
    ) {
      const { id, mediaWidgetContent } = action.payload;
      if (state.widgetRecord[id]) {
        state.widgetRecord[id].config.content = mediaWidgetContent;
      }
    },
  },
  extraReducers: builder => {},
});
