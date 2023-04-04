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
import { createAsyncThunk } from '@reduxjs/toolkit';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import { boardDrillManager } from 'app/pages/DashBoardPage/components/BoardDrillManager/BoardDrillManager';
import { getControlOptionQueryParams } from 'app/pages/DashBoardPage/components/Widgets/ControllerWidget/config';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { FilterSearchParams } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { mainActions } from 'app/pages/MainPage/slice';
import { shareActions } from 'app/pages/SharePage/slice';
import { ExecuteToken, ShareVizInfo } from 'app/pages/SharePage/slice/types';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import {
  fetchAvailableSourceFunctionsAsync,
  fetchAvailableSourceFunctionsAsyncForShare,
  fetchChartDataSet,
} from 'app/utils/fetch';
import { filterSqlOperatorName } from 'app/utils/internalChartHelper';
import { RootState } from 'types';
import { request2 } from 'utils/request';
import { getErrorMessage } from 'utils/utils';
import { boardActions } from '.';
import { getChartWidgetRequestParams } from '../../../utils';
import { handleServerBoardAction } from './asyncActions';
import { selectBoardById, selectBoardWidgetMap } from './selector';
import {
  BoardState,
  ControllerWidgetContent,
  Dashboard,
  getDataOption,
  ServerDashboard,
  VizRenderMode,
  WidgetData,
} from './types';

/**
 * @param ''
 * @description '先拿本地缓存，没有缓存再去服务端拉数据'
 */
export const getBoardDetail = createAsyncThunk<
  null,
  {
    dashboardRelId: string;
    filterSearchParams?: FilterSearchParams;
    vizToken?: ExecuteToken;
    shareToken?: string;
  }
>(
  'board/getBoardDetail',
  async (params, { getState, dispatch, rejectWithValue }) => {
    // 1 在 内存里找到就返回
    const dashboard = selectBoardById(
      getState() as { board: BoardState },
      params?.dashboardRelId,
    );
    if (dashboard) {
      return null;
    }
    if (params.vizToken) {
      await dispatch(
        fetchBoardDetailInShare({ ...params, vizToken: params.vizToken }),
      );
    } else {
      await dispatch(fetchBoardDetail(params));
    }
    return null;
  },
);

export const fetchBoardDetail = createAsyncThunk<
  null,
  {
    dashboardRelId: string;
    filterSearchParams?: FilterSearchParams;
  }
>('board/fetchBoardDetail', async (params, { dispatch, rejectWithValue }) => {
  const { data } = await request2<ServerDashboard>(
    `/viz/dashboards/${params?.dashboardRelId}`,
  );

  dispatch(
    handleServerBoardAction({
      data,
      renderMode: 'read',
      filterSearchMap: {
        params: params?.filterSearchParams,
        isMatchByName: !!params?.filterSearchParams?.isMatchByName?.[0],
      },
    }),
  );

  return null;
});
export const exportBoardTpl = createAsyncThunk<
  null,
  {
    dashboard: Partial<Dashboard>;
    widgets: Partial<Widget>[];
    callBack: () => void;
  }
>('board/exportBoardTpl', async (params, { dispatch, rejectWithValue }) => {
  const { dashboard, widgets, callBack } = params;
  await request2<any>({
    url: `viz/export/dashboard/template`,
    method: 'POST',
    data: { dashboard, widgets },
  });
  callBack();
  dispatch(mainActions.setDownloadPolling(true));
  return null;
});
export const fetchBoardDetailInShare = createAsyncThunk<
  null,
  {
    dashboardRelId: string;
    vizToken: ExecuteToken;
    filterSearchParams?: FilterSearchParams;
    shareToken?: string;
  }
>(
  'board/fetchBoardDetailInShare',
  async (params, { dispatch, rejectWithValue }) => {
    const { vizToken, shareToken } = params;
    const { data } = await request2<ShareVizInfo>({
      url: `shares/${shareToken}/viz`,
      method: 'POST',
      data: {
        authorizedToken: vizToken.authorizedToken,
      },
    });
    dispatch(
      shareActions.setExecuteTokenMap({
        executeToken: data.executeToken,
      }),
    );
    const serverBoard = data.vizDetail as ServerDashboard;
    dispatch(
      handleServerBoardAction({
        data: serverBoard,
        renderMode: 'share',
        filterSearchMap: {
          params: params.filterSearchParams,
          isMatchByName: true,
        },
        executeToken: data.executeToken,
      }),
    );

    return null;
  },
);
export const renderedWidgetAsync = createAsyncThunk<
  null,
  { boardId: string; widgetId: string; renderMode?: VizRenderMode },
  { state: { board: BoardState } }
>(
  'board/renderedWidgetAsync',
  async ({ boardId, widgetId, renderMode }, { getState, dispatch }) => {
    const widgetMapMap = selectBoardWidgetMap(getState());
    const widgetMap = widgetMapMap?.[boardId];
    const curWidget = widgetMap?.[widgetId];
    if (!curWidget) return null;
    // 1 widget render
    dispatch(boardActions.renderedWidgets({ boardId, widgetIds: [widgetId] }));
    // 2 widget getData
    dispatch(
      getWidgetData({ boardId: boardId, widget: curWidget, renderMode }),
    );
    return null;
  },
);

export const getWidgetData = createAsyncThunk<
  null,
  {
    boardId: string;
    widget: Widget;
    renderMode: VizRenderMode | undefined;
    option?: getDataOption;
  },
  { state: RootState }
>(
  'board/getWidgetData',
  ({ widget, renderMode, option }, { getState, dispatch }) => {
    const boardId = widget.dashboardId;
    dispatch(boardActions.renderedWidgets({ boardId, widgetIds: [widget.id] }));
    const widgetId = widget.id;
    switch (widget.config.type) {
      case 'chart':
        dispatch(
          getChartWidgetDataAsync({ boardId, widgetId, renderMode, option }),
        );
        return null;
      case 'controller':
        dispatch(getControllerOptions({ boardId, widgetId, renderMode }));
        return null;
      default:
        return null;
    }
  },
);

export const syncBoardWidgetChartDataAsync = createAsyncThunk<
  null,
  {
    boardId: string;
    sourceWidgetId: string;
    widgetId: string;
    option?: getDataOption;
    extraFilters?: PendingChartDataRequestFilter[];
    tempFilters?: PendingChartDataRequestFilter[];
    variableParams?: Record<string, any[]>;
  } & {
    executeToken?: any;
  },
  { state: RootState }
>(
  'board/syncBoardWidgetChartDataAsync',
  async (
    {
      boardId,
      sourceWidgetId,
      widgetId,
      option,
      extraFilters,
      tempFilters,
      variableParams,
      executeToken,
    },
    { getState, dispatch },
  ) => {
    const boardState = getState() as { board: BoardState };
    const widgetMapMap = boardState.board.widgetRecord;
    const widgetMap = widgetMapMap[boardId];
    const curWidget = widgetMap[widgetId];
    if (!curWidget) {
      return null;
    }
    const viewMap = boardState.board.viewMap;
    const dataChartMap = boardState.board.dataChartMap;
    const drillOption = boardDrillManager.getWidgetDrill({
      bid: curWidget.dashboardId,
      wid: widgetId,
    });
    const dataChart = dataChartMap?.[curWidget.datachartId];
    const chartDataView = viewMap?.[dataChart?.viewId];
    const requestParams = new ChartDataRequestBuilder(
      {
        id: chartDataView?.id || '',
        config: chartDataView?.config || {},
        meta: chartDataView?.meta,
        computedFields: dataChart?.config?.computedFields || [],
      },
      dataChart?.config?.chartConfig?.datas,
      dataChart?.config?.chartConfig?.settings,
      {},
      false,
      dataChart?.config?.aggregation,
    )
      .addVariableParams(variableParams)
      .addExtraSorters(option?.sorters as any[])
      .addRuntimeFilters((extraFilters || []).concat(tempFilters || []))
      .addDrillOption(drillOption)
      .build();

    try {
      const data = await fetchChartDataSet(requestParams, executeToken);
      await dispatch(
        boardActions.setWidgetData({
          wid: widgetId,
          data: { ...data, id: widgetId },
        }),
      );
      await dispatch(
        boardActions.renderedWidgets({ boardId, widgetIds: [widgetId] }),
      );
      await dispatch(
        boardActions.changeWidgetLinkInfo({
          boardId,
          widgetId,
          linkInfo: {
            sourceWidgetId,
            filters: extraFilters,
            tempFilters: tempFilters,
            variables: variableParams,
          },
        }),
      );
      await dispatch(
        boardActions.changePageInfo({
          boardId,
          widgetId,
          pageInfo: data?.pageInfo,
        }),
      );
      await dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId,
          errInfo: undefined,
          errorType: 'request',
        }),
      );
    } catch (error) {
      await dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId,
          errInfo: getErrorMessage(error),
          errorType: 'request',
        }),
      );
      await dispatch(
        boardActions.setWidgetData({
          wid: widgetId,
          data: undefined,
        }),
      );
    } finally {
      await dispatch(
        boardActions.changeSelectedItems({
          wid: widgetId,
          data: [],
        }),
      );
    }
    return null;
  },
);

export const getChartWidgetDataAsync = createAsyncThunk<
  null,
  {
    boardId: string;
    widgetId: string;
    renderMode: VizRenderMode | undefined;
    option?: getDataOption;
  },
  { state: RootState }
>(
  'board/getChartWidgetDataAsync',
  async ({ boardId, widgetId, renderMode, option }, { getState, dispatch }) => {
    dispatch(boardActions.renderedWidgets({ boardId, widgetIds: [widgetId] }));
    const boardState = getState() as { board: BoardState };

    const widgetMapMap = boardState.board.widgetRecord;
    const widgetInfo =
      boardState.board?.widgetInfoRecord?.[boardId]?.[widgetId];
    const widgetMap = widgetMapMap[boardId];
    const curWidget = widgetMap[widgetId];

    if (!curWidget) return null;
    const viewMap = boardState.board.viewMap;
    const dataChartMap = boardState.board.dataChartMap;
    const boardLinkFilters =
      boardState.board.boardInfoRecord?.[boardId]?.linkFilter;
    const drillOption = boardDrillManager.getWidgetDrill({
      bid: curWidget.dashboardId,
      wid: widgetId,
    });
    let requestParams = getChartWidgetRequestParams({
      widgetId,
      widgetMap,
      viewMap,
      option,
      widgetInfo,
      dataChartMap,
      boardLinkFilters,
      drillOption,
    });

    if (!requestParams) {
      return null;
    }
    let widgetData;
    try {
      if (renderMode === 'read') {
        const { data } = await request2<WidgetData>({
          method: 'POST',
          url: `data-provider/execute`,
          data: requestParams,
        });
        widgetData = data;
      } else {
        const executeTokenMap = (getState() as RootState)?.share
          ?.executeTokenMap;

        const dataChart = dataChartMap[curWidget.datachartId];
        const viewId = viewMap[dataChart.viewId].id;
        const executeToken = executeTokenMap?.[viewId];
        const { data } = await request2<WidgetData>({
          method: 'POST',
          url: `shares/execute`,
          params: {
            executeToken: executeToken?.authorizedToken,
          },
          data: requestParams,
        });
        widgetData = data;
      }
      dispatch(
        boardActions.setWidgetData({
          wid: widgetId,
          data: filterSqlOperatorName(requestParams, widgetData) as WidgetData,
        }),
      );
      dispatch(
        boardActions.changePageInfo({
          boardId,
          widgetId,
          pageInfo: widgetData.pageInfo,
        }),
      );
      dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId,
          errInfo: undefined,
          errorType: 'request',
        }),
      );
    } catch (error) {
      dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId,
          errInfo: getErrorMessage(error),
          errorType: 'request',
        }),
      );
      dispatch(
        boardActions.setWidgetData({
          wid: widgetId,
          data: undefined,
        }),
      );
    } finally {
      await dispatch(
        boardActions.changeSelectedItems({
          wid: widgetId,
          data: [],
        }),
      );
    }
    dispatch(
      boardActions.addFetchedItem({
        boardId,
        widgetId,
      }),
    );
    return null;
  },
);

// 根据 字段获取 Controller 的options
export const getControllerOptions = createAsyncThunk<
  null,
  { boardId: string; widgetId: string; renderMode: VizRenderMode | undefined },
  { state: RootState }
>(
  'board/getControllerOptions',
  async ({ boardId, widgetId, renderMode }, { getState, dispatch }) => {
    dispatch(
      boardActions.renderedWidgets({
        boardId: boardId,
        widgetIds: [widgetId],
      }),
    );
    const boardState = getState() as { board: BoardState };
    const viewMap = boardState.board.viewMap;
    const widgetMapMap = boardState.board.widgetRecord;
    const widgetMap = widgetMapMap[boardId];
    const widget = widgetMap[widgetId];
    if (!widget) return null;
    const content = widget.config.content as ControllerWidgetContent;
    const config = content.config;
    if (!Array.isArray(config.assistViewFields)) return null;
    if (config.assistViewFields.length < 2) return null;

    const executeTokenMap = (getState() as RootState)?.share?.executeTokenMap;

    const [viewId, ...columns] = config.assistViewFields;

    const parentFields = config?.parentFields;

    const executeToken = executeTokenMap?.[viewId];

    const view = viewMap[viewId];
    if (!view) return null;
    if (parentFields) {
      columns.push(...parentFields);
    }

    const requestParams = getControlOptionQueryParams({
      view,
      columns: columns,
      curWidget: widget,
      widgetMap,
    });

    if (!requestParams) {
      return null;
    }
    let widgetData;
    try {
      if (executeToken && renderMode !== 'read') {
        const { data } = await request2<ChartDataSetDTO>({
          method: 'POST',
          url: `shares/execute`,
          params: {
            executeToken: executeToken?.authorizedToken,
          },
          data: requestParams,
        });
        widgetData = data;
      } else {
        const { data } = await request2<WidgetData>({
          method: 'POST',
          url: `data-provider/execute`,
          data: requestParams,
        });
        widgetData = data;
      }
      dispatch(
        boardActions.setWidgetData({
          wid: widgetId,
          data: filterSqlOperatorName(requestParams, widgetData) as WidgetData,
        }),
      );
      dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId,
          errInfo: undefined,
          errorType: 'request',
        }),
      );
    } catch (error) {
      dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId,
          errInfo: getErrorMessage(error),
          errorType: 'request',
        }),
      );
    }
    dispatch(
      boardActions.addFetchedItem({
        boardId,
        widgetId,
      }),
    );
    return null;
  },
);

export const fetchAvailableSourceFunctions = createAsyncThunk<
  { value: string[]; sourceId: string } | false,
  { sourceId: string; authorizedToken: string; renderMode: VizRenderMode },
  { state: RootState }
>(
  'workbench/fetchAvailableSourceFunctions',
  async ({ sourceId, authorizedToken, renderMode }, { getState }) => {
    const boardState = getState() as { board: BoardState };
    const availableSourceFunctionsMap =
      boardState.board.availableSourceFunctionsMap;
    if (!availableSourceFunctionsMap[sourceId]) {
      try {
        let functions: string[] = [];
        if (renderMode === 'share') {
          functions = await fetchAvailableSourceFunctionsAsyncForShare(
            sourceId,
            authorizedToken,
          );
        } else {
          functions = await fetchAvailableSourceFunctionsAsync(sourceId);
        }
        return { value: functions, sourceId: sourceId };
      } catch (err) {
        throw err;
      }
    }
    return false;
  },
);
