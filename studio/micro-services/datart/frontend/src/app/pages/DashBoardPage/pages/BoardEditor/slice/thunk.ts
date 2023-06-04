import { createAsyncThunk } from '@reduxjs/toolkit';
import migrateWidgetChartConfig from 'app/migration/BoardConfig/migrateWidgetChartConfig';
import migrateWidgetConfig from 'app/migration/BoardConfig/migrateWidgetConfig';
import { migrateWidgets } from 'app/migration/BoardConfig/migrateWidgets';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import {
  boardDrillManager,
  EDIT_PREFIX,
} from 'app/pages/DashBoardPage/components/BoardDrillManager/BoardDrillManager';
import widgetManager from 'app/pages/DashBoardPage/components/WidgetManager';
import { getControlOptionQueryParams } from 'app/pages/DashBoardPage/components/Widgets/ControllerWidget/config';
import { ORIGINAL_TYPE_MAP } from 'app/pages/DashBoardPage/constants';
import { boardActions } from 'app/pages/DashBoardPage/pages/Board/slice';
import {
  BoardState,
  ControllerWidgetContent,
  DataChart,
  getDataOption,
  SaveDashboard,
  ServerDatachart,
  WidgetData,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { getChartWidgetRequestParams } from 'app/pages/DashBoardPage/utils';
import {
  getChartDataView,
  getDashBoardByResBoard,
  getDataChartsByServer,
  getInitBoardInfo,
} from 'app/pages/DashBoardPage/utils/board';
import {
  convertWrapChartWidget,
  createToSaveWidgetGroup,
  createWidgetInfo,
  getWidgetMap,
} from 'app/pages/DashBoardPage/utils/widget';
import { Variable } from 'app/pages/MainPage/pages/VariablePage/slice/types';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import ChartDataView from 'app/types/ChartDataView';
import { View } from 'app/types/View';
import { filterSqlOperatorName } from 'app/utils/internalChartHelper';
import { RootState } from 'types';
import { request2 } from 'utils/request';
import { getErrorMessage, uuidv4 } from 'utils/utils';
import {
  editBoardStackActions,
  editDashBoardInfoActions,
  editWidgetDataActions,
  editWidgetInfoActions,
  editWidgetSelectedItemsActions,
} from '.';
import { BoardInfo, BoardType, ServerDashboard } from '../../Board/slice/types';
import { getDataChartMap } from './../../../utils/board';
import { adjustWidgetsToBoard } from './../../../utils/widget';
import { addVariablesToBoard } from './actions/actions';
import {
  boardInfoState,
  editBoardStackState,
  selectEditBoard,
} from './selectors';
import { EditBoardState, HistoryEditBoard } from './types';

/**
 * @param ''
 * @description '先拿本地缓存，没有缓存再去服务端拉数据'
 */
export const getEditBoardDetail = createAsyncThunk<
  null,
  string,
  { state: RootState }
>(
  'editBoard/getEditBoardDetail',
  async (dashboardId, { getState, dispatch }) => {
    if (!dashboardId) {
      return null;
    }
    //2 从 editor 内存中取
    const editDashboard = selectEditBoard(
      getState() as unknown as {
        editBoard: HistoryEditBoard;
      },
    );

    if (editDashboard?.id === dashboardId) {
      return null;
    }
    dispatch(fetchEditBoardDetail(dashboardId));
    return null;
  },
);

export const fetchEditBoardDetail = createAsyncThunk<
  null,
  string,
  { state: RootState }
>(
  'editBoard/fetchEditBoardDetail',
  async (dashboardId, { getState, dispatch }) => {
    if (!dashboardId) {
      return null;
    }
    const { data } = await request2<ServerDashboard>(
      `/viz/dashboards/${dashboardId}`,
    );

    const dashboard = getDashBoardByResBoard(data);
    const boardType = dashboard.config.type;
    const {
      datacharts: serverDataCharts,
      views: serverViews,
      widgets: serverWidgets,
    } = data;
    // TODO
    const dataCharts: DataChart[] = getDataChartsByServer(
      serverDataCharts,
      serverViews,
    );
    let migratedWidgets = migrateWidgets(serverWidgets, boardType);
    migratedWidgets = migrateWidgetConfig(migratedWidgets);
    migratedWidgets = migrateWidgetChartConfig(migratedWidgets);
    const { widgetMap, wrappedDataCharts } = getWidgetMap(
      migratedWidgets, //todo
      dataCharts,
      boardType,
      serverViews,
    );
    const widgetInfos = Object.keys(widgetMap).map(id => createWidgetInfo(id));
    // TODO xld migration about filter

    const widgetIds = serverWidgets.map(w => w.id);
    // const boardInfo = getInitBoardInfo({ id: dashboard.id, widgetIds });

    const boardInfo = getInitBoardInfo({ id: dashboard.id, widgetIds });
    // datacharts

    const allDataCharts: DataChart[] = dataCharts.concat(wrappedDataCharts);
    dispatch(boardActions.setDataChartToMap(allDataCharts));
    const viewViews = getChartDataView(serverViews, allDataCharts);

    dispatch(boardActions.updateViewMap(viewViews));
    // BoardInfo
    dispatch(editDashBoardInfoActions.initEditBoardInfo(boardInfo));
    // widgetInfoRecord
    dispatch(editWidgetInfoActions.addWidgetInfos(widgetInfos));
    //dashBoard,widgetRecord
    dispatch(
      editBoardStackActions.setBoardToEditStack({
        dashBoard: dashboard,
        widgetRecord: widgetMap,
      }),
    );
    return null;
  },
);

/**
 * @param boardId string
 * @description '更新保存 board'
 */
export const toUpdateDashboard = createAsyncThunk<
  any,
  { boardId: string; callback?: () => void },
  { state: RootState }
>(
  'editBoard/toUpdateDashboard',
  async ({ boardId, callback }, { getState, dispatch }) => {
    const { dashBoard, widgetRecord } = editBoardStackState(
      getState() as unknown as {
        editBoard: HistoryEditBoard;
      },
    );
    const boardInfo = boardInfoState(
      getState() as { editBoard: EditBoardState },
    );
    const boardState = getState() as unknown as { board: BoardState };

    const { dataChartMap, viewMap } = boardState.board;
    const widgets = convertWrapChartWidget({
      widgetMap: widgetRecord,
      dataChartMap,
      viewMap,
    });

    const group = createToSaveWidgetGroup(widgets, boardInfo.widgetIds);
    const updateData: SaveDashboard = {
      ...dashBoard,
      subType: dashBoard?.config?.type,
      config: JSON.stringify(dashBoard.config),
      widgetToCreate: group.widgetToCreate,
      widgetToUpdate: group.widgetToUpdate,
      widgetToDelete: group.widgetToDelete,
    };

    await request2<any>({
      url: `viz/dashboards/${dashBoard.id}`,
      method: 'put',
      data: updateData,
    });
    callback?.();
  },
);
/**
 * @param 'Widget[]'
 * @description '添加 widget 到board'
 */
export const addWidgetsToEditBoard = createAsyncThunk<
  null,
  Widget[],
  { state: RootState }
>('editBoard/addWidgetsToEditBoard', (widgets, { getState, dispatch }) => {
  const { dashBoard } = editBoardStackState(
    getState() as unknown as {
      editBoard: HistoryEditBoard;
    },
  );
  const { layouts } = boardInfoState(
    getState() as { editBoard: EditBoardState },
  );
  const widgetInfos = widgets.map(item => createWidgetInfo(item.id));
  const updatedWidgets = adjustWidgetsToBoard({
    widgets,
    boardType: dashBoard.config.type,
    boardId: dashBoard.id,
    layouts,
  });
  // widgetInfoRecord
  dispatch(editWidgetInfoActions.addWidgetInfos(widgetInfos));
  // WidgetRecord
  dispatch(editBoardStackActions.addWidgets(updatedWidgets));
  return null;
});

export const addGroupWidgetToEditBoard = createAsyncThunk<
  null,
  Widget[],
  { state: RootState }
>('editBoard/addGroupWidgetToEditBoard', (widgets, { getState, dispatch }) => {
  const { dashBoard } = editBoardStackState(
    getState() as unknown as {
      editBoard: HistoryEditBoard;
    },
  );
  const { layouts } = boardInfoState(
    getState() as { editBoard: EditBoardState },
  );
  const widgetInfos = widgets.map(t => createWidgetInfo(t.id));
  const updatedWidgets = adjustWidgetsToBoard({
    widgets,
    boardType: dashBoard.config.type,
    boardId: dashBoard.id,
    layouts,
  });
  // widgetInfoRecord
  dispatch(editWidgetInfoActions.addWidgetInfos(widgetInfos));
  // WidgetRecord
  dispatch(editBoardStackActions.addWidgets(updatedWidgets));
  return null;
});
// addDataChartWidgets
export const addDataChartWidgets = createAsyncThunk<
  null,
  { boardId: string; chartIds: string[]; boardType: BoardType },
  { state: RootState }
>(
  'editBoard/addDataChartWidgets',
  async ({ boardId, chartIds, boardType }, { getState, dispatch }) => {
    const {
      data: { datacharts, views, viewVariables },
    } = await request2<{
      datacharts: ServerDatachart[];
      views: View[];
      viewVariables: Record<string, Variable[]>;
    }>({
      url: `viz/datacharts?datachartIds=${chartIds.join()}`,
      method: 'get',
    });
    const dataCharts: DataChart[] = getDataChartsByServer(datacharts, views);
    const dataChartMap = getDataChartMap(dataCharts);
    const viewViews = getChartDataView(views, dataCharts);
    dispatch(boardActions.setDataChartToMap(dataCharts));
    dispatch(boardActions.setViewMap(viewViews));

    const widgets = chartIds.map(dcId => {
      const dataChart = dataChartMap[dcId];
      const viewIds = dataChart.viewId ? [dataChart.viewId] : [];
      let widget = widgetManager.toolkit(ORIGINAL_TYPE_MAP.linkedChart).create({
        boardType: boardType,
        datachartId: dcId,
        relations: [],
        name: dataChart.name,
        content: dataChartMap[dcId],
        viewIds: viewIds,
      });
      return widget;
    });
    dispatch(addWidgetsToEditBoard(widgets));

    Object.values(viewVariables).forEach(variables => {
      dispatch(addVariablesToBoard(variables));
    });
    return null;
  },
);

// addWrapChartWidget
export const addWrapChartWidget = createAsyncThunk<
  null,
  {
    boardId: string;
    chartId: string;
    boardType: BoardType;
    dataChart: DataChart;
    view: ChartDataView;
  },
  { state: RootState }
>(
  'editBoard/addWrapChartWidget',
  async (
    { boardId, chartId, boardType, dataChart, view },
    { getState, dispatch },
  ) => {
    const dataCharts = [dataChart];
    const viewViews = view ? [view] : [];
    dispatch(boardActions.setDataChartToMap(dataCharts));
    dispatch(boardActions.setViewMap(viewViews));
    let widget = widgetManager.toolkit(ORIGINAL_TYPE_MAP.ownedChart).create({
      boardType: boardType,
      datachartId: chartId,
      relations: [],
      name: dataChart.name,
      content: dataChart,
      viewIds: view?.id ? [view.id] : [],
    });
    dispatch(addWidgetsToEditBoard([widget]));
    dispatch(addVariablesToBoard(view?.variables));

    return null;
  },
);

export const addChartWidget = createAsyncThunk<
  null,
  {
    boardId: string;
    chartId: string;
    boardType: BoardType;
    dataChart: DataChart;
    view: ChartDataView;
    subType: 'widgetChart' | 'dataChart';
  },
  { state: RootState }
>(
  'editBoard/addChartWidget',
  async (
    { boardId, chartId, boardType, dataChart, view, subType },
    { dispatch },
  ) => {
    const dataCharts = [dataChart];
    const viewViews = [view];
    dispatch(boardActions.setDataChartToMap(dataCharts));
    dispatch(boardActions.setViewMap(viewViews));

    const originalType =
      subType === 'dataChart'
        ? ORIGINAL_TYPE_MAP.linkedChart
        : ORIGINAL_TYPE_MAP.ownedChart;

    let widget = widgetManager.toolkit(originalType).create({
      boardType: boardType,
      datachartId: chartId,
      relations: [],
      content: dataChart,
      viewIds: view.id ? [view.id] : [],
    });

    dispatch(addWidgetsToEditBoard([widget]));
    dispatch(addVariablesToBoard(view.variables));
    return null;
  },
);

export const renderedEditWidgetAsync = createAsyncThunk<
  null,
  { boardId: string; widgetId: string },
  { state: RootState }
>(
  'editBoard/renderedEditWidgetAsync',
  async ({ boardId, widgetId }, { getState, dispatch, rejectWithValue }) => {
    const { widgetRecord: WidgetMap } = editBoardStackState(
      getState() as unknown as {
        editBoard: HistoryEditBoard;
      },
    );
    const curWidget = WidgetMap[widgetId];
    if (!curWidget) return null;

    dispatch(editWidgetInfoActions.renderedWidgets([widgetId]));

    // 2 widget getData
    dispatch(getEditWidgetData({ widget: curWidget }));
    return null;
  },
);

//
export const uploadBoardImage = createAsyncThunk<
  string,
  {
    boardId: string;
    fileName: string;
    formData: FormData;
    resolve: (url: string) => void;
  }
>(
  'editBoard/uploadBoardImage',
  async ({ boardId, formData, fileName, resolve }, { getState, dispatch }) => {
    const { data } = await request2<string>({
      url: `files/viz/image?ownerType=${'DASHBOARD'}&ownerId=${boardId}&fileName=${
        uuidv4() + '@' + fileName
      }`,
      method: 'POST',
      data: formData,
    });
    resolve(data);
    return data;
  },
);

export const getEditWidgetData = createAsyncThunk<
  null,
  { widget: Widget; option?: getDataOption },
  { state: RootState }
>(
  'editBoard/getEditWidgetData',
  ({ widget, option }, { getState, dispatch }) => {
    dispatch(editWidgetInfoActions.renderedWidgets([widget.id]));
    if (widget.config.type === 'chart') {
      dispatch(getEditChartWidgetDataAsync({ widgetId: widget.id, option }));
    }
    if (widget.config.type === 'controller') {
      dispatch(getEditControllerOptions(widget.id));
    }
    return null;
  },
);

export const syncEditBoardWidgetChartDataAsync = createAsyncThunk<
  null,
  {
    boardId: string;
    sourceWidgetId: string;
    widgetId: string;
    option?: getDataOption;
    extraFilters?: PendingChartDataRequestFilter[];
    tempFilters?: PendingChartDataRequestFilter[];
    variableParams?: Record<string, any[]>;
  },
  { state: RootState }
>(
  'board/syncEditBoardWidgetChartDataAsync',
  async (
    {
      boardId,
      sourceWidgetId,
      widgetId,
      option,
      extraFilters,
      tempFilters,
      variableParams,
    },
    { getState, dispatch },
  ) => {
    const boardState = getState() as { board: BoardState };
    const rootState = getState() as RootState;
    const stackEditBoard = rootState.editBoard as unknown as HistoryEditBoard;
    const { widgetRecord: widgetMap } = stackEditBoard.stack.present;
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

    const { data } = await request2<WidgetData>(
      {
        method: 'POST',
        url: `data-provider/execute`,
        data: requestParams,
      },
      undefined,
      {
        onRejected: async error => {
          await dispatch(
            editWidgetInfoActions.setWidgetErrInfo({
              boardId,
              widgetId,
              errInfo: getErrorMessage(error),
              errorType: 'request',
            }),
          );
          await dispatch(
            editWidgetDataActions.setWidgetData({
              wid: widgetId,
              data: undefined,
            }),
          );
        },
      },
    );
    await dispatch(
      editWidgetDataActions.setWidgetData({
        wid: widgetId,
        data: { ...data, id: widgetId },
      }),
    );
    await dispatch(editWidgetInfoActions.renderedWidgets([widgetId]));
    await dispatch(
      editWidgetInfoActions.changeWidgetLinkInfo({
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
      editWidgetInfoActions.changePageInfo({
        boardId,
        widgetId,
        pageInfo: data?.pageInfo,
      }),
    );
    await dispatch(
      editWidgetInfoActions.setWidgetErrInfo({
        boardId,
        widgetId,
        errInfo: undefined,
        errorType: 'request',
      }),
    );
    return null;
  },
);

export const getEditChartWidgetDataAsync = createAsyncThunk<
  null,
  {
    widgetId: string;
    option?: getDataOption;
  },
  { state: RootState }
>(
  'editBoard/getEditChartWidgetDataAsync',
  async ({ widgetId, option }, { getState, dispatch, rejectWithValue }) => {
    const rootState = getState() as RootState;
    dispatch(editWidgetInfoActions.renderedWidgets([widgetId]));
    const stackEditBoard = rootState.editBoard as unknown as HistoryEditBoard;
    const { widgetRecord: widgetMap } = stackEditBoard.stack.present;
    const editBoard = rootState.editBoard;
    const boardInfo = editBoard?.boardInfo as BoardInfo;
    const boardState = rootState.board as BoardState;
    const widgetInfo = editBoard?.widgetInfoRecord[widgetId];
    const viewMap = boardState.viewMap;
    const curWidget = widgetMap[widgetId];

    if (!curWidget) return null;
    const dataChartMap = boardState.dataChartMap;
    const boardLinkFilters = boardInfo.linkFilter;
    const drillOption = boardDrillManager.getWidgetDrill({
      bid: EDIT_PREFIX + curWidget.dashboardId,
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
    const { data } = await request2<WidgetData>(
      {
        method: 'POST',
        url: `data-provider/execute`,
        data: requestParams,
      },
      undefined,
      {
        onRejected: async error => {
          await dispatch(
            editWidgetInfoActions.setWidgetErrInfo({
              widgetId,
              errInfo: (error as any)?.message as any,
              errorType: 'request',
            }),
          );
          await dispatch(
            editWidgetDataActions.setWidgetData({
              wid: widgetId,
              data: undefined,
            }),
          );
        },
      },
    );
    widgetData = data;
    await dispatch(
      editWidgetDataActions.setWidgetData({
        wid: widgetId,
        data: filterSqlOperatorName(requestParams, widgetData) as WidgetData,
      }),
    );
    await dispatch(
      editWidgetInfoActions.changePageInfo({
        widgetId,
        pageInfo: data.pageInfo,
      }),
    );
    await dispatch(
      editWidgetInfoActions.setWidgetErrInfo({
        widgetId,
        errInfo: undefined,
        errorType: 'request',
      }),
    );
    await dispatch(
      editWidgetSelectedItemsActions.changeSelectedItemsInEditor({
        wid: widgetId,
        data: [],
      }),
    );
    return null;
  },
);

export const getEditControllerOptions = createAsyncThunk<
  null,
  string,
  { state: RootState }
>(
  'editBoard/getEditControllerOptions',
  async (widgetId, { getState, dispatch }) => {
    dispatch(editWidgetInfoActions.renderedWidgets([widgetId]));
    const rootState = getState() as RootState;
    const stackEditBoard = rootState.editBoard as unknown as HistoryEditBoard;
    const { widgetRecord: widgetMap } = stackEditBoard.stack.present;
    const widget = widgetMap[widgetId];
    if (!widget) return null;
    const content = widget.config.content as ControllerWidgetContent;
    const config = content.config;
    if (!Array.isArray(config.assistViewFields)) return null;
    if (config.assistViewFields.length < 2) return null;

    const parentFields = config?.parentFields;
    const boardState = rootState.board as BoardState;
    const viewMap = boardState.viewMap;
    const [viewId, ...columns] = config.assistViewFields;
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
    const { data } = await request2<WidgetData>({
      method: 'POST',
      url: `data-provider/execute`,
      data: requestParams,
    });
    widgetData = data;
    dispatch(
      editWidgetDataActions.setWidgetData({
        wid: widgetId,
        data: filterSqlOperatorName(requestParams, widgetData) as WidgetData,
      }),
    );

    return null;
  },
);
