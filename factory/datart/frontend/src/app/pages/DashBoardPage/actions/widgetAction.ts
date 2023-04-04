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

import { ChartDataSectionType } from 'app/constants';
import { PageInfo } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { ChartMouseEventParams } from 'app/types/Chart';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { tablePagingAndSortEventListener } from 'app/utils/ChartEventListenerHelper';
import {
  filterFiltersByInteractionRule,
  filterVariablesByInteractionRule,
} from 'app/utils/internalChartHelper';
import { FilterSqlOperator } from 'globalConstants';
import i18next from 'i18next';
import { RootState } from 'types';
import { isEmptyArray } from 'utils/object';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';
import { jumpTypes, ORIGINAL_TYPE_MAP } from '../constants';
import { boardActions } from '../pages/Board/slice';
import {
  getChartWidgetDataAsync,
  getControllerOptions,
  getWidgetData,
  syncBoardWidgetChartDataAsync,
} from '../pages/Board/slice/thunk';
import {
  BoardLinkFilter,
  BoardState,
  RectConfig,
  VizRenderMode,
} from '../pages/Board/slice/types';
import {
  editBoardStackActions,
  editDashBoardInfoActions,
  editWidgetDataActions,
  editWidgetInfoActions,
} from '../pages/BoardEditor/slice';
import {
  editorWidgetClearLinkageAction,
  widgetClearLinkageAction,
} from '../pages/BoardEditor/slice/actions/actions';
import {
  getEditChartWidgetDataAsync,
  getEditControllerOptions,
  getEditWidgetData,
  syncEditBoardWidgetChartDataAsync,
} from '../pages/BoardEditor/slice/thunk';
import {
  EditBoardState,
  HistoryEditBoard,
} from '../pages/BoardEditor/slice/types';
import { Widget } from '../types/widgetTypes';
import { getTheWidgetFiltersAndParams } from '../utils';
import {
  getCascadeControllers,
  getNeedRefreshWidgetsByController,
  getValueByRowData,
} from '../utils/widget';

export const toggleLinkageAction =
  (boardEditing: boolean, boardId: string, widgetId: string, toggle: boolean) =>
  dispatch => {
    if (boardEditing) {
      dispatch(
        editWidgetInfoActions.changeWidgetInLinking({
          boardId,
          widgetId,
          toggle,
        }),
      );
    } else {
      dispatch(
        boardActions.changeWidgetInLinking({
          boardId,
          widgetId,
          toggle,
        }),
      );
    }
  };
// tableChartClickAction
export const tableChartClickAction =
  (
    boardId: string,
    editing: boolean,
    renderMode: VizRenderMode,
    widget: Widget,
    params: ChartMouseEventParams,
  ) =>
  dispatch => {
    const opt = {
      pageInfo: { pageNo: params?.value?.pageNo },
      sorters: [
        {
          column: params?.seriesName!,
          operator: (params?.value as any)?.direction,
          aggOperator: (params?.value as any)?.aggOperator,
        },
      ],
    };
    if (editing) {
      dispatch(
        getEditChartWidgetDataAsync({
          widgetId: widget.id,
          option: opt,
        }),
      );
    } else {
      dispatch(
        getChartWidgetDataAsync({
          boardId,
          widgetId: widget.id,
          renderMode,
          option: opt,
        }),
      );
    }
  };

export const widgetClickJumpAction =
  (obj: {
    renderMode: VizRenderMode;
    widget: Widget;
    params: ChartMouseEventParams;
    history: any;
  }) =>
  (dispatch, getState) => {
    const { renderMode, widget, params, history } = obj;
    const state = getState() as RootState;
    const orgId = state?.main?.orgId || '';
    const folderIds = state.viz?.vizs?.map(v => v.relId) || [];
    const jumpConfig = widget.config?.jumpConfig;
    const targetType = jumpConfig?.targetType || jumpTypes[0].value;

    const URL = jumpConfig?.URL || '';
    const queryName = jumpConfig?.queryName || '';
    const targetId = jumpConfig?.target?.relId;
    const jumpFieldName: string = jumpConfig?.field?.jumpFieldName || '';
    // table chart
    if (
      params.componentType === 'table' &&
      jumpFieldName !== params.seriesName
    ) {
      return;
    }

    const rowDataValue = getValueByRowData(params.data, jumpFieldName);
    console.warn(' jumpValue:', rowDataValue);
    console.warn('rowData', params.data?.rowData);
    console.warn(`rowData[${jumpFieldName}]:${rowDataValue} `);
    if (targetType === 'URL') {
      // jump url
      let jumpUrl;
      if (URL.indexOf('?') > -1) {
        jumpUrl = `${URL}&${queryName}=${rowDataValue}`;
      } else {
        jumpUrl = `${URL}?${queryName}=${rowDataValue}`;
      }
      window.location.href = jumpUrl;
      return;
    }
    // jump in datart
    if (jumpConfig?.targetType === 'INTERNAL') {
      if (!folderIds.includes(jumpConfig.target.relId)) {
        dispatch(
          showJumpErrorAction(renderMode, widget.dashboardId, widget.id),
        );
        return;
      }
      if (typeof jumpConfig?.filter === 'object') {
        const searchParamsStr = urlSearchTransfer.toUrlString({
          [jumpConfig?.filter?.filterId]: rowDataValue,
        });
        history.push(
          `/organizations/${orgId}/vizs/${targetId}?${searchParamsStr}`,
        );
      }
    }
  };

export const widgetLinkEventAction =
  (
    renderMode,
    widget: Widget,
    params: Array<{ isUnSelectedAll: boolean; filters; rule }>,
  ) =>
  async (dispatch, getState) => {
    const targetLinkDataChartIds = (params || []).map(p => p.rule?.relId);
    const rootState = getState() as RootState;
    const executeTokenMap = rootState.share?.executeTokenMap || {};
    const viewBoardState = rootState.board as BoardState;
    const editBoardState = rootState.editBoard as unknown as HistoryEditBoard;
    const widgetMapMap =
      renderMode === 'edit'
        ? editBoardState.stack?.present?.widgetRecord
        : viewBoardState?.widgetRecord?.[widget?.dashboardId];
    const boardWidgetInfoRecord =
      renderMode === 'edit'
        ? editBoardState.widgetInfoRecord
        : viewBoardState?.widgetInfoRecord?.[widget?.dashboardId];
    const dataChartMap = viewBoardState.dataChartMap;
    const widgetMap = widgetMapMap || {};
    const sourceWidgetInfo = boardWidgetInfoRecord?.[widget.id];
    const sourceWidgetRuntimeLinkInfo = sourceWidgetInfo?.linkInfo || {};
    const {
      filterParams: sourceControllerFilters,
      variableParams: sourceVariableParams,
    } = getTheWidgetFiltersAndParams<PendingChartDataRequestFilter>({
      chartWidget: widget,
      widgetMap: widgetMap,
      params: undefined,
    });
    const boardLinkWidgets = Object.entries(widgetMapMap || {})
      .filter(([k, v]) => {
        return targetLinkDataChartIds.includes(v.datachartId);
      })
      .map(([k, v]) => v);
    boardLinkWidgets.forEach(targetWidget => {
      const dataChart = dataChartMap?.[targetWidget.datachartId];
      const dimensionNames = dataChart?.config?.chartConfig?.datas?.flatMap(
        d => {
          if (
            d.type === ChartDataSectionType.Group ||
            d.type === ChartDataSectionType.Color
          ) {
            return d.rows?.map(r => r.colName) || [];
          }
          return [];
        },
      );
      const targetWidgetSelectedItems =
        renderMode === 'read'
          ? viewBoardState?.selectedItems?.[targetWidget.id]
          : editBoardState.selectedItemsMap.selectedItems?.[targetWidget.id];
      const clickEventParam = params?.find(
        p => p?.rule?.relId === targetWidget.datachartId,
      );
      const filterObj = clickEventParam?.filters;
      const isUnSelectedAll = clickEventParam?.isUnSelectedAll;
      const clickFilters: PendingChartDataRequestFilter[] = Object.entries(
        filterObj || {},
      )
        .map(([k, v]) => {
          return {
            sqlOperator: FilterSqlOperator.In,
            column: k,
            values: (v as any)?.map(vv => ({ value: vv, valueType: 'STRING' })),
          };
        })
        .filter(f =>
          moveFilterIfHasSelectedItems(
            targetWidgetSelectedItems,
            dimensionNames,
            f.column,
          ),
        );
      const sourceLinkFilters =
        sourceWidgetRuntimeLinkInfo?.filters?.filter(f =>
          moveFilterIfHasSelectedItems(
            targetWidgetSelectedItems,
            dimensionNames,
            f.column,
          ),
        ) || [];
      const widgetInfo = boardWidgetInfoRecord?.[targetWidget.id];
      const { filterParams: controllerFilters, variableParams } =
        getTheWidgetFiltersAndParams<PendingChartDataRequestFilter>({
          chartWidget: targetWidget,
          widgetMap: widgetMap,
          params: undefined,
        });
      const sourceLinkAndControllerFilterByRule =
        filterFiltersByInteractionRule(
          clickEventParam?.rule,
          ...sourceLinkFilters,
          ...sourceControllerFilters,
        );
      const sourceLinkAndControllerVariablesByRule =
        filterVariablesByInteractionRule(
          clickEventParam?.rule,
          Object.assign(
            sourceWidgetRuntimeLinkInfo?.variables || {},
            sourceVariableParams,
          ),
        );

      const fetchChartDataParam = {
        boardId: targetWidget.dashboardId,
        sourceWidgetId: widget.id,
        widgetId: targetWidget.id,
        option: widgetInfo,
        extraFilters: controllerFilters,
        tempFilters: isUnSelectedAll
          ? []
          : (clickFilters || []).concat(sourceLinkAndControllerFilterByRule),
        variableParams: isUnSelectedAll
          ? variableParams || {}
          : Object.assign(
              variableParams,
              sourceLinkAndControllerVariablesByRule,
            ),
      };

      if (renderMode === 'edit') {
        dispatch(syncEditBoardWidgetChartDataAsync(fetchChartDataParam));
      } else {
        // set auth token if exist
        let executeToken;
        if (renderMode === 'share') {
          executeToken =
            executeTokenMap?.[targetWidget?.viewIds?.[0]]?.authorizedToken;
        }
        dispatch(
          syncBoardWidgetChartDataAsync(
            Object.assign(fetchChartDataParam, { executeToken }),
          ),
        );
      }
    });

    // set current widget to is linking status
    dispatch(
      toggleLinkageAction(
        renderMode === 'edit',
        widget?.dashboardId,
        widget.id,
        !params?.[0]?.isUnSelectedAll,
      ),
    );
  };

export const widgetClickLinkageAction =
  (
    boardId: string,
    editing: boolean,
    renderMode: VizRenderMode,
    widget: Widget,
    params: ChartMouseEventParams,
  ) =>
  (dispatch, getState) => {
    const { componentType, seriesType, seriesName } = params;
    const isTableHandle = componentType === 'table' && seriesType === 'body';

    const linkRelations = widget.relations.filter(re => {
      const {
        config: { type, widgetToWidget },
      } = re;
      if (type !== 'widgetToWidget') return false;
      if (isTableHandle) {
        if (widgetToWidget?.triggerColumn === seriesName) return true;
        return false;
      }
      return true;
    });

    const boardFilters = linkRelations
      .map(re => {
        let linkageFieldName: string =
          re?.config?.widgetToWidget?.triggerColumn || '';
        const linkValue = getValueByRowData(params.data, linkageFieldName);

        if (!linkValue) {
          console.warn('linkageFieldName:', linkageFieldName);
          console.warn('rowData', params.data?.rowData);
          console.warn(`rowData[${linkageFieldName}]:${linkValue} `);
          return undefined;
        }

        const filter: BoardLinkFilter = {
          triggerWidgetId: widget.id,
          triggerValue: linkValue,
          triggerDataChartId: widget.datachartId,
          linkerWidgetId: re.targetId,
        };
        return filter;
      })
      .filter(item => !!item) as BoardLinkFilter[];
    if (editing) {
      dispatch(
        editDashBoardInfoActions.changeBoardLinkFilter({
          boardId: boardId,
          triggerId: widget.id,
          linkFilters: boardFilters,
        }),
      );
    } else {
      dispatch(
        boardActions.changeBoardLinkFilter({
          boardId: boardId,
          triggerId: widget.id,
          linkFilters: boardFilters,
        }),
      );
    }
    dispatch(toggleLinkageAction(editing, boardId, widget.id, true));
    setTimeout(() => {
      boardFilters.forEach(f => {
        if (editing) {
          dispatch(
            getEditChartWidgetDataAsync({
              widgetId: f.linkerWidgetId,
              option: {
                pageInfo: { pageNo: 1 },
              },
            }),
          );
        } else {
          dispatch(
            getChartWidgetDataAsync({
              boardId,
              widgetId: f.linkerWidgetId,
              renderMode,
              option: {
                pageInfo: { pageNo: 1 },
              },
            }),
          );
        }
      });
    }, 60);
  };
//
export const widgetChartClickAction =
  (obj: {
    boardId: string;
    editing: boolean;
    renderMode: VizRenderMode;
    widget: Widget;
    params: ChartMouseEventParams;
    history: any;
  }) =>
  dispatch => {
    const { boardId, editing, renderMode, widget, params } = obj;
    //is tableChart
    tablePagingAndSortEventListener(params, p => {
      dispatch(
        tableChartClickAction(boardId, editing, renderMode, widget, params),
      );
    });
  };

export const widgetLinkEventActionCreator =
  (obj: { renderMode: string; widget: Widget; params: any }) => dispatch => {
    const { renderMode, widget, params } = obj;
    dispatch(widgetLinkEventAction(renderMode, widget, params));
  };

export const widgetGetDataAction =
  (editing: boolean, widget: Widget, renderMode: VizRenderMode) => dispatch => {
    const boardId = widget.dashboardId;
    if (editing) {
      dispatch(getEditWidgetData({ widget }));
    } else {
      dispatch(getWidgetData({ boardId, widget, renderMode }));
    }
  };

export const widgetToClearLinkageAction =
  (editing: boolean, widget: Widget, renderMode: VizRenderMode) => dispatch => {
    if (editing) {
      dispatch(editorWidgetClearLinkageAction(widget));
    } else {
      dispatch(widgetClearLinkageAction(widget, renderMode));
    }
  };

export const showJumpErrorAction =
  (renderMode: VizRenderMode, boardId: string, wid: string) => dispatch => {
    const errorInfo = i18next.t('viz.jump.jumpError');
    if (renderMode === 'edit') {
      dispatch(
        editWidgetInfoActions.setWidgetErrInfo({
          boardId,
          widgetId: wid,
          errInfo: errorInfo, // viz.linkage.linkageError
          errorType: 'interaction',
        }),
      );
    } else {
      dispatch(
        boardActions.setWidgetErrInfo({
          boardId,
          widgetId: wid,
          errInfo: errorInfo,
          errorType: 'interaction',
        }),
      );
    }
  };
export const setWidgetSampleDataAction =
  (args: { boardEditing: boolean; datachartId: string; wid: string }) =>
  (dispatch, getState) => {
    const { boardEditing, datachartId, wid } = args;
    const rootState = getState() as RootState;
    const viewBoardState = rootState.board as BoardState;
    const editBoardState = rootState.editBoard as EditBoardState;
    const dataChartMap = viewBoardState.dataChartMap;
    const curChart = dataChartMap[datachartId];
    if (!curChart) return;
    if (curChart.viewId) return;
    if (!curChart.config.sampleData) return;
    if (boardEditing) {
      const dataset = editBoardState.widgetDataMap[wid];
      if (dataset?.id) return;
      dispatch(
        editWidgetDataActions.setWidgetData({
          wid,
          data: curChart.config.sampleData,
        }),
      );
    } else {
      const dataset = viewBoardState.widgetDataMap[wid];
      if (dataset?.id) return;
      dispatch(
        boardActions.setWidgetData({ wid, data: curChart.config.sampleData }),
      );
    }
  };
export const refreshWidgetsByControllerAction =
  (renderMode: VizRenderMode, widget: Widget) => (dispatch, getState) => {
    const boardId = widget.dashboardId;
    const controllerIds = getCascadeControllers(widget);
    const rootState = getState() as RootState;
    const editBoardState = (rootState.editBoard as unknown as HistoryEditBoard)
      .stack.present;

    const viewBoardState = rootState.board as BoardState;
    const widgetMap =
      renderMode === 'edit'
        ? editBoardState.widgetRecord
        : viewBoardState.widgetRecord[boardId];
    const hasQueryBtn = Object.values(widgetMap || {}).find(
      item => item.config.originalType === ORIGINAL_TYPE_MAP.queryBtn,
    );
    // 获取级联选项
    controllerIds.forEach(controlWidgetId => {
      if (renderMode === 'edit') {
        dispatch(getEditControllerOptions(controlWidgetId));
      } else {
        dispatch(
          getControllerOptions({
            boardId,
            widgetId: controlWidgetId,
            renderMode,
          }),
        );
      }
    });
    // 如果有 hasQueryBtn 那么control不会立即触发查询
    if (hasQueryBtn) return;
    const pageInfo: Partial<PageInfo> = {
      pageNo: 1,
    };
    const chartWidgetIds = getNeedRefreshWidgetsByController(widget);

    chartWidgetIds.forEach(widgetId => {
      if (renderMode === 'edit') {
        dispatch(
          getEditChartWidgetDataAsync({ widgetId, option: { pageInfo } }),
        );
      } else {
        dispatch(
          getChartWidgetDataAsync({
            boardId,
            widgetId,
            renderMode,
            option: { pageInfo },
          }),
        );
      }
    });
  };

export const changeGroupRectAction =
  (args: {
    renderMode: VizRenderMode;
    boardId: string;
    wid: string;
    w: number;
    h: number;
    isAutoGroupWidget: boolean;
  }) =>
  dispatch => {
    const { renderMode } = args;
    if (renderMode === 'edit') {
      dispatch(changeEditGroupRectAction(args));
    } else {
      // NOTE: it should not be calculate group rect for non edit mode.
      // dispatch(changeViewGroupRectAction(args));
    }
  };

export const changeViewGroupRectAction =
  (args: {
    renderMode: VizRenderMode;
    boardId: string;
    wid: string;
    w: number;
    h: number;
    isAutoGroupWidget: boolean;
  }) =>
  (dispatch, getState) => {
    const { wid, w, h, boardId } = args;
    const rootState = getState() as RootState;
    const viewBoardState = rootState.board as BoardState;
    const widgetMap = viewBoardState.widgetRecord[boardId];
    if (!wid) return;
    const widget = widgetMap?.[wid];
    if (!widget) return;
    const parentWidget = widgetMap[widget.parentId || ''];
    const rect: RectConfig = {
      x: 0,
      y: 0,
      width: w,
      height: h,
    };

    const parentIsContainer =
      parentWidget && parentWidget.config.type === 'container';

    const parentIsAutoBoard =
      widget.config.boardType === 'auto' && !widget.parentId;

    // TODO(Stephen): to be fix board is auto board
    if (parentIsContainer || parentIsAutoBoard) {
      dispatch(
        boardActions.changeFreeWidgetRect({
          boardId: widget.dashboardId,
          wid,
          rect,
        }),
      );
      return;
    }
  };
export const changeEditGroupRectAction =
  (args: {
    renderMode: VizRenderMode;
    boardId: string;
    wid: string;
    w: number;
    h: number;
    isAutoGroupWidget: boolean;
  }) =>
  (dispatch, getState) => {
    const { wid, w, h } = args;
    const rootState = getState() as RootState;
    const editBoardState = (rootState.editBoard as unknown as HistoryEditBoard)
      .stack.present;
    const widgetMap = editBoardState.widgetRecord;
    if (!wid) return;
    const widget = widgetMap?.[wid];
    if (!widget) return;
    const parentWidget = widgetMap[widget.parentId || ''];
    const rect: RectConfig = {
      x: 0,
      y: 0,
      width: w,
      height: h,
    };
    const parentIsContainer =
      parentWidget && parentWidget.config.type === 'container';

    const parentIsAutoBoard =
      widget.config.boardType === 'auto' && !widget.parentId;

    if (parentIsContainer || parentIsAutoBoard) {
      dispatch(
        editBoardStackActions.changeFreeWidgetRect({
          wid,
          rect,
          isAutoGroupWidget: args.isAutoGroupWidget,
        }),
      );
    }
  };

const moveFilterIfHasSelectedItems = (
  selectedItems,
  dimensionNames,
  filterName,
) => {
  return isEmptyArray(selectedItems) || !dimensionNames?.includes(filterName);
};
