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
import { DefaultWidgetData } from 'app/pages/DashBoardPage/constants';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { RootState } from 'types';
import { boardInit } from '.';
import { getLayoutWidgets } from '../../../utils/widget';
import { WidgetInfo } from './types';
const DefaultObject = {};
export const selectPropsId = (_: unknown, id: string) => id;

export const boardState = (state: RootState) => state.board || boardInit;
export const selectBoardRecord = createSelector(
  [boardState],
  state => state.boardRecord,
);

export const selectBoardInfoRecord = createSelector(
  [boardState],
  state => state?.boardInfoRecord,
);
export const selectBoardInfoById = createSelector(
  [selectBoardInfoRecord, selectPropsId],
  (BoardInfoMap, id) => BoardInfoMap[id],
);
export const selectBoardWidgetMap = createSelector(
  [boardState],
  state => state.widgetRecord,
);
export const selectWidgetInfoGroupMap = createSelector(
  [boardState],
  state => state.widgetInfoRecord,
);
export const selectWidgetSelectedItemsMap = createSelector(
  [boardState],
  state => state.selectedItems,
);

export const selectBoardById = createSelector(
  selectBoardRecord,
  selectPropsId,
  (selectBoards, id) => {
    return selectBoards[id] || undefined;
  },
);

export const selectBoardWidgetMapById = createSelector(
  selectBoardWidgetMap,
  selectPropsId,
  (widgetRecord, id) => widgetRecord[id] || DefaultObject,
);

export const selectWidgetBy2Id = createSelector(
  selectBoardWidgetMap,
  selectPropsId,
  (_1, _2, wid: string) => wid,
  (boardWidgetRecord, recordId, widgetId) => {
    try {
      return boardWidgetRecord?.[recordId]?.[widgetId];
    } catch (error) {
      return undefined;
    }
  },
);

export const selectLayoutWidgetMapById = () =>
  createSelector(selectBoardWidgetMap, selectPropsId, (widgetRecord, id) => {
    if (!widgetRecord[id]) return DefaultObject as Record<string, Widget>;
    const allWidgetMap = widgetRecord[id];
    const layoutWidgets = getLayoutWidgets(allWidgetMap);
    const LayoutWidgetMap: Record<string, Widget> = {};
    layoutWidgets.forEach(w => {
      LayoutWidgetMap[w.id] = allWidgetMap[w.id];
    });
    return LayoutWidgetMap;
  });

export const selectWidgetInfoMap = createSelector(
  selectWidgetInfoGroupMap,
  selectPropsId,
  (widgetInfoGroupMap, id) => widgetInfoGroupMap[id] || DefaultObject,
);

export const makeSelectSelectedItems = () => {
  return createSelector(
    selectWidgetSelectedItemsMap,
    selectPropsId,
    (widgetSelectedItemsMap, widgetId) => {
      return widgetSelectedItemsMap?.[widgetId];
    },
  );
};

export const selectWidgetInfoBy2Id = createSelector(
  selectWidgetInfoGroupMap,
  selectPropsId,
  (_, bId: string, wId: string) => wId,
  (widgetInfoGroupMap, boardId, widgetId) => {
    try {
      return widgetInfoGroupMap?.[boardId]?.[widgetId];
    } catch (error) {
      return undefined;
    }
  },
);
export const selectLayoutWidgetInfoMapById = createSelector(
  selectBoardWidgetMap,
  selectWidgetInfoGroupMap,
  selectPropsId,
  (allWidgetMap, allWidgetInfoMap, id) => {
    if (!allWidgetMap[id]) return DefaultObject;
    if (!allWidgetInfoMap[id]) return DefaultObject;

    const layoutWidgets = getLayoutWidgets(allWidgetMap[id]);
    const widgetInfoMap = allWidgetInfoMap[id];
    const layoutWidgetsInfo: Record<string, WidgetInfo> = DefaultObject;

    layoutWidgets.forEach(w => {
      layoutWidgetsInfo[w.id] = widgetInfoMap[w.id];
    });
    return layoutWidgetsInfo;
  },
);

export const makeSelectBoardConfigById = () =>
  createSelector(selectBoardRecord, selectPropsId, (boardRecord, id) => {
    if (boardRecord[id]) {
      return boardRecord?.[id];
    } else {
      return undefined;
    }
  });

export const makeSelectBoardFullScreenPanelById = () =>
  createSelector(
    selectBoardInfoRecord,
    selectPropsId,
    (BoardInfoRecord, id) => BoardInfoRecord?.[id]?.fullScreenItemId,
  );

export const selectDataChartMap = createSelector(
  [boardState],
  state => state.dataChartMap,
);

export const selectDataChartById = createSelector(
  [selectDataChartMap, (_, chartId: string) => chartId],
  (dataChartMap, id) => dataChartMap[id] || undefined,
);

export const selectViewMap = createSelector(
  [boardState],
  state => state.viewMap,
);

export const selectAvailableSourceFunctionsMap = createSelector(
  [boardState],
  state => state.availableSourceFunctionsMap,
);

// dataChartMap
export const selectWidgetDataById = createSelector(
  [boardState, selectPropsId],
  (state, wid) => state.widgetDataMap[wid] || DefaultWidgetData,
);

//  share
export const selectShareBoard = createSelector([boardState], state => {
  return Object.values(state?.boardRecord)[0] || undefined;
});
export const selectShareBoardInfo = createSelector([boardState], state => {
  return Object.values(state?.boardInfoRecord)[0] || undefined;
});
