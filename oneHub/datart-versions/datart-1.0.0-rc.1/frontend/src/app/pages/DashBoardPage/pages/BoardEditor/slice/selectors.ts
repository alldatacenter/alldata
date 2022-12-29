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
import { createSelector } from '@reduxjs/toolkit';
import {
  DefaultWidgetData,
  ORIGINAL_TYPE_MAP,
} from 'app/pages/DashBoardPage/constants';
import {
  DeviceType,
  WidgetInfo,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  EditBoardState,
  HistoryEditBoard,
} from 'app/pages/DashBoardPage/pages/BoardEditor/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { StateWithHistory } from 'redux-undo';
import { widgetMapToTree } from '../components/LayerPanel/utils';
import { getLayoutWidgets } from './../../../utils/widget';
import { EditBoardStack } from './types';

// First select the relevant part from the state
// record
const DefaultObject = {};
export const editBoardStackState = (state: { editBoard: HistoryEditBoard }) =>
  state.editBoard.stack.present;

export const selectEditBoard = createSelector([editBoardStackState], state => {
  return state.dashBoard;
});
export const selectEditWidgetRecord = createSelector(
  [editBoardStackState],
  state => {
    return state.widgetRecord;
  },
);
export const selectAllWidgetMap = createSelector(
  [editBoardStackState],
  state => state.widgetRecord || DefaultObject,
);
export const selectEditWidgetById = createSelector(
  selectAllWidgetMap,
  (_, widgetId: string) => widgetId,
  (widgetRecord, widgetId) => {
    if (!widgetId) {
      return undefined;
    }
    try {
      return widgetRecord[widgetId];
    } catch (error) {
      return undefined;
    }
  },
);

export const selectSortAllWidgets = createSelector(
  [selectAllWidgetMap],
  WidgetConfig =>
    Object.values(WidgetConfig).sort((w1, w2) => {
      return w1.config.index! - w2.config.index!;
    }),
);
export const selectHasQueryBtn = createSelector(
  [selectSortAllWidgets],
  allWidgets => {
    let target = allWidgets.find(
      it => it.config.originalType === ORIGINAL_TYPE_MAP.queryBtn,
    );
    return !!target;
  },
);
export const selectHasResetBtn = createSelector(
  [selectSortAllWidgets],
  allWidgets => {
    let target = allWidgets.find(
      it => it.config.originalType === ORIGINAL_TYPE_MAP.resetBtn,
    );
    return !!target;
  },
);
export const selectLayoutWidgetMap = createSelector(
  [selectAllWidgetMap],
  allWidgetMap => {
    const layoutWidgets = getLayoutWidgets(allWidgetMap);
    const LayoutWidgetMap: Record<string, Widget> = {};
    layoutWidgets.forEach(w => {
      LayoutWidgetMap[w.id] = allWidgetMap[w.id];
    });
    return LayoutWidgetMap;
  },
);

// widgetsInfo
export const selectAllWidgetInfoMap = (state: { editBoard: EditBoardState }) =>
  state.editBoard.widgetInfoRecord || undefined;

export const selectEditingWidgetIds = (state: { editBoard: EditBoardState }) =>
  Object.values(state.editBoard.widgetInfoRecord)
    ?.filter(r => r.editing)
    ?.map(r => r.id) || [];

export const selectWidgetInfoById = createSelector(
  [selectAllWidgetInfoMap, (_, widgetId: string) => widgetId],
  (allWidgetInfoMap, wId) => allWidgetInfoMap[wId] || undefined,
);
export const selectLayoutWidgetInfoMap = createSelector(
  [selectAllWidgetMap, selectAllWidgetInfoMap],
  (allWidgetMap, allWidgetInfo) => {
    const layoutWidgets = getLayoutWidgets(allWidgetMap);
    const layoutWidgetInfoMap: Record<string, WidgetInfo> = {};
    layoutWidgets.forEach(w => {
      layoutWidgetInfoMap[w.id] = allWidgetInfo[w.id];
    });
    return layoutWidgetInfoMap;
  },
);

export const selectSelectedIds = createSelector(
  [selectAllWidgetInfoMap],
  WidgetsInfo =>
    Object.values(WidgetsInfo)
      .filter(widgetInfo => widgetInfo.selected)
      .map(widgetInfo => widgetInfo.id) || [],
);

export const selectWidgetInfoDatachartId = createSelector(
  [selectAllWidgetMap],
  WidgetsInfo =>
    Object.values(WidgetsInfo).map(widgetInfo => {
      return widgetInfo.datachartId || undefined;
    }) || [],
);

export const selectLayerTree = createSelector(
  [selectAllWidgetMap, selectAllWidgetInfoMap],
  (widgetMap, widgetInfoMap) => {
    return widgetMapToTree({
      widgetMap,
      widgetInfoMap,
      parentId: '',
      tree: [],
    });
  },
);
// boardInfo
export const boardInfoState = (state: { editBoard: EditBoardState }) =>
  state.editBoard.boardInfo;
export const selectEditBoardLoading = (state: { editBoard: EditBoardState }) =>
  state.editBoard.boardInfo.loading;
export const selectDeviceType = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.deviceType || DeviceType.Desktop,
);
export const selectControllerPanel = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.controllerPanel,
);

export const selectBoardChartEditorProps = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.chartEditorProps,
);
export const selectDashLayouts = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.layouts,
);
export const selectDashFullScreenItemId = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.fullScreenItemId,
);
export const selectShowBlockMask = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.showBlockMask,
);
// record Clipboard
export const selectClipboardWidgets = createSelector(
  [boardInfoState],
  boardInfo => boardInfo.clipboardWidgetMap || [],
);
// editWidgetData
export const editWidgetDataState = (state: { editBoard: EditBoardState }) =>
  state.editBoard.widgetDataMap;
export const selectEditWidgetData = createSelector(
  [editWidgetDataState, (_, widgetId: string) => widgetId],
  (widgetDataMap, wid) => {
    if (!widgetDataMap) return DefaultWidgetData;
    if (!widgetDataMap[wid]) return DefaultWidgetData;
    return widgetDataMap[wid] || DefaultWidgetData;
  },
);
// past
const recordPastState = (state: {
  editBoard: { stack: StateWithHistory<EditBoardStack> };
}) => state.editBoard.stack.past;

export const selectPastState = createSelector(
  [recordPastState],
  state => state,
);
// Future
const recordFutureState = (state: {
  editBoard: { stack: StateWithHistory<EditBoardStack> };
}) => state.editBoard.stack.future;

export const selectFutureState = createSelector(
  [recordFutureState],
  state => state,
);

// SelectedItems
export const selectEditSelectedItems = (state: { editBoard: EditBoardState }) =>
  state.editBoard.selectedItemsMap;
export const makeSelectSelectedItemsInEditor = () => {
  return createSelector(
    selectEditSelectedItems,
    (_, widgetId: string) => widgetId,
    (selectedItemsMap, wId) => selectedItemsMap.selectedItems?.[wId],
  );
};
