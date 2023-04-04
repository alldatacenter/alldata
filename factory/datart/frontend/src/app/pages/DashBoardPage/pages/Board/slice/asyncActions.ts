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
import { DownloadFileType } from 'app/constants';
import migrateWidgetChartConfig from 'app/migration/BoardConfig/migrateWidgetChartConfig';
import { migrateWidgets } from 'app/migration/BoardConfig/migrateWidgets';
import { FilterSearchParamsWithMatch } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { mainActions } from 'app/pages/MainPage/slice';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { ChartDataRequest } from 'app/types/ChartDataRequest';
import { makeDownloadDataTask } from 'app/utils/fetch';
import { RootState } from 'types';
import { UniqWith } from 'utils/object';
import { boardActions } from '.';
import { getBoardChartRequests } from '../../../utils';
import {
  getChartDataView,
  getDashBoardByResBoard,
  getDataChartsByServer,
  getInitBoardInfo,
  getScheduleBoardInfo,
} from '../../../utils/board';
import { getWidgetInfoMapByServer, getWidgetMap } from '../../../utils/widget';
import { PageInfo } from './../../../../MainPage/pages/ViewPage/slice/types';
import {
  fetchAvailableSourceFunctions,
  getChartWidgetDataAsync,
  getWidgetData,
} from './thunk';
import {
  BoardState,
  DataChart,
  ServerDashboard,
  VizRenderMode,
  WidgetData,
} from './types';

export const handleServerBoardAction =
  (params: {
    data: ServerDashboard;
    renderMode: VizRenderMode;
    filterSearchMap?: FilterSearchParamsWithMatch;
    executeToken?: Record<string, ExecuteToken>;
  }) =>
  async (dispatch, getState) => {
    const { data, renderMode, filterSearchMap, executeToken } = params;
    const dashboard = getDashBoardByResBoard(data);
    const { datacharts, views: serverViews, widgets: serverWidgets } = data;

    const dataCharts: DataChart[] = getDataChartsByServer(
      datacharts,
      serverViews,
    );
    let migratedWidgets = migrateWidgets(serverWidgets, dashboard.config.type);
    migratedWidgets = migrateWidgetChartConfig(migratedWidgets);

    const { widgetMap, wrappedDataCharts, controllerWidgets } = getWidgetMap(
      migratedWidgets,
      dataCharts,
      dashboard.config.type,
      serverViews,
      filterSearchMap,
    );
    const widgetIds = Object.values(widgetMap).map(w => w.id);
    let boardInfo = getInitBoardInfo({
      id: dashboard.id,
      widgetIds,
      controllerWidgets,
    });
    if (renderMode === 'schedule') {
      boardInfo = getScheduleBoardInfo(boardInfo, widgetMap);
    }

    const widgetInfoMap = getWidgetInfoMapByServer(widgetMap);
    const allDataCharts: DataChart[] = dataCharts.concat(wrappedDataCharts);
    const viewViews = getChartDataView(serverViews, allDataCharts);

    if (viewViews) {
      const idList = UniqWith(
        Object.values(viewViews).map(v => {
          return { sourceId: v.sourceId, viewId: v.id };
        }),
        (a, b) => a.sourceId === b.sourceId,
      );

      idList.forEach(({ sourceId, viewId }) => {
        dispatch(
          fetchAvailableSourceFunctions({
            sourceId: sourceId,
            authorizedToken: executeToken?.[viewId]?.authorizedToken || '',
            renderMode,
          }),
        );
      });
    }

    await dispatch(
      boardActions.setBoardState({
        board: dashboard,
        boardInfo: boardInfo,
      }),
    );
    dispatch(boardActions.setViewMap(viewViews));
    dispatch(boardActions.setDataChartToMap(allDataCharts));
    dispatch(
      boardActions.setWidgetMapState({
        boardId: dashboard.id,
        widgetMap: widgetMap,
        widgetInfoMap: widgetInfoMap,
      }),
    );
  };
export const getWidgetChartDatasAction =
  (boardId: string) => (dispatch, getState) => {
    const boardState = getState() as { board: BoardState };
    const boardMapWidgetMap = boardState.board.widgetRecord;
    const WidgetDataMap = boardState.board.widgetDataMap;
    const widgetMap = boardMapWidgetMap[boardId];
    const dataMap = Object.values(widgetMap || {})
      .filter(w => w.config.type === 'chart')
      .map(w => {
        const item = {
          id: w.id,
          name: w.config.name,
          data: WidgetDataMap[w.id],
        };
        return item;
      })
      .reduce((acc, cur) => {
        acc[cur.id] = cur;
        return acc;
      }, {} as Record<string, WidgetData | undefined>);

    return dataMap;
  };
export const getBoardStateAction =
  (boardId: string) => (dispatch, getState) => {
    const boardState = getState() as { board: BoardState };
    const boardMapWidgetMap = boardState.board.widgetRecord;
    const widgetMap = boardMapWidgetMap[boardId];
    const board = boardState.board.boardRecord[boardId];
    const dataChartMap = boardState.board.dataChartMap;
    return {
      board,
      widgetMap,
      dataChartMap,
    };
  };
export const boardDownLoadAction =
  (params: { boardId: string; downloadType: DownloadFileType }) =>
  async (dispatch, getState) => {
    const state = getState() as RootState;
    const { boardId, downloadType } = params;
    const vizs = state.viz?.vizs;
    const folderId = vizs?.filter(v => v.relId === boardId)[0].id;
    const boardInfoRecord = state.board?.boardInfoRecord;
    let imageWidth = 0;

    if (boardInfoRecord) {
      const { boardWidthHeight } = Object.values(boardInfoRecord)[0];
      imageWidth = boardWidthHeight[0];
    }

    const { requestParams, fileName } = await dispatch(
      getBoardDownloadParams({ boardId }),
    );

    dispatch(
      makeDownloadDataTask({
        downloadParams:
          downloadType === DownloadFileType.Excel
            ? requestParams
            : [{ analytics: false, vizType: 'dashboard', vizId: folderId }],
        fileName,
        downloadType,
        imageWidth,
        resolve: () => {
          dispatch(mainActions.setDownloadPolling(true));
        },
      }),
    );
  };
export const getBoardDownloadParams =
  (params: { boardId: string }) => (dispatch, getState) => {
    const { boardId } = params;
    const boardState = getState() as { board: BoardState };
    const widgetMapMap = boardState.board.widgetRecord;
    const widgetMap = widgetMapMap[boardId];
    const viewMap = boardState.board.viewMap;
    const dataChartMap = boardState.board.dataChartMap;

    const fileName = boardState.board?.boardRecord?.[boardId].name || 'board';
    let requestParams = getBoardChartRequests({
      widgetMap,
      viewMap,
      dataChartMap,
    }) as ChartDataRequest[];

    return { requestParams, fileName };
  };

export const widgetsQueryAction =
  ({ boardId, renderMode }) =>
  (dispatch, getState) => {
    const pageInfo: Partial<PageInfo> = {
      pageNo: 1,
    };
    const boardState = getState() as { board: BoardState };
    const boardMapWidgetMap = boardState.board.widgetRecord;
    const widgetMap = boardMapWidgetMap[boardId];
    Object.values(widgetMap)
      .filter(it => it.config.type === 'chart')
      .forEach(it => {
        dispatch(
          getChartWidgetDataAsync({
            boardId,
            widgetId: it.id,
            renderMode,
            option: { pageInfo },
          }),
        );
      });
  };

export const resetControllerAction =
  ({ boardId, renderMode }) =>
  async (dispatch, getState) => {
    const boardState = getState() as { board: BoardState };
    const boardInfo = boardState.board.boardInfoRecord[boardId];
    if (!boardInfo) return;
    dispatch(boardActions.resetControlWidgets({ boardId }));
    const boardMapWidgetMap = boardState.board.widgetRecord;
    const widgetMap = boardMapWidgetMap[boardId];

    const pageInfo: Partial<PageInfo> = {
      pageNo: 1,
    };

    Object.values(widgetMap).forEach(widget => {
      dispatch(
        getWidgetData({
          boardId,
          widget: widget,
          renderMode,
          option: { pageInfo },
        }),
      );
    });
  };
