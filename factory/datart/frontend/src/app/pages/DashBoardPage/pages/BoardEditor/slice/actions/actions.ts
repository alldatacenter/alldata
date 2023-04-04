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
import { ChartEditorBaseProps } from 'app/components/ChartEditor';
import widgetManager from 'app/pages/DashBoardPage/components/WidgetManager';
import { initClientId } from 'app/pages/DashBoardPage/components/WidgetManager/utils/init';
import {
  findParentIds,
  getParentRect,
} from 'app/pages/DashBoardPage/components/Widgets/GroupWidget/utils';
import { boardActions } from 'app/pages/DashBoardPage/pages/Board/slice';
import {
  BoardState,
  ContainerItem,
  Dashboard,
  DataChart,
  TabWidgetContent,
  VizRenderMode,
  WidgetOfCopy,
  WidgetType,
  WidgetTypes,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  editWidgetInfoActions,
  editWidgetSelectedItemsActions,
} from 'app/pages/DashBoardPage/pages/BoardEditor/slice';
import {
  Widget,
  WidgetMapping,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import {
  cloneWidgets,
  createWidgetInfo,
} from 'app/pages/DashBoardPage/utils/widget';
import { Variable } from 'app/pages/MainPage/pages/VariablePage/slice/types';
import ChartDataView from 'app/types/ChartDataView';
import { BOARD_SELF_CHART_PREFIX } from 'globalConstants';
import produce from 'immer';
import { ActionCreators } from 'redux-undo';
import { RootState } from 'types';
import { uuidv4 } from 'utils/utils';
import { editBoardStackActions, editDashBoardInfoActions } from '..';
import { ORIGINAL_TYPE_MAP } from '../../../../constants';
import { selectWidgetInfoMap } from '../../../Board/slice/selector';
import { syncBoardWidgetChartDataAsync } from '../../../Board/slice/thunk';
import { EventLayerNode } from '../../components/LayerPanel/LayerTreeItem';
import { getNewDragNodeValue } from '../../components/LayerPanel/utils';
import { selectAllWidgetInfoMap } from '../selectors';
import {
  getEditChartWidgetDataAsync,
  syncEditBoardWidgetChartDataAsync,
} from '../thunk';
import { EditBoardState, HistoryEditBoard } from '../types';
import { editWidgetsQueryAction } from './controlActions';

export const clearEditBoardState = () => async dispatch => {
  await dispatch(
    editBoardStackActions.setBoardToEditStack({
      dashBoard: {} as Dashboard,
      widgetRecord: {},
    }),
  );
  await dispatch(editDashBoardInfoActions.clearEditBoardInfo());
  await dispatch(editWidgetInfoActions.clearWidgetInfo());
  dispatch(ActionCreators.clearHistory());
};
export const deleteWidgetsAction = (ids?: string[]) => (dispatch, getState) => {
  const editBoard = getState().editBoard as HistoryEditBoard;
  let selectedIds: string[] = [];
  let shouldDeleteIds: string[] = [];
  let effectTypes: WidgetType[] = [];
  if (ids?.length) {
    selectedIds = ids;
  } else {
    selectedIds = Object.values(editBoard.widgetInfoRecord)
      .filter(WidgetInfo => WidgetInfo.selected)
      .map(WidgetInfo => WidgetInfo.id);
  }
  if (selectedIds.length === 0) return;

  const widgetMap = editBoard.stack.present.widgetRecord;

  while (selectedIds.length > 0) {
    const id = selectedIds.pop();

    if (!id) continue;
    const curWidget = widgetMap[id];
    if (!curWidget) continue;

    const widgetType = curWidget.config.type;
    shouldDeleteIds.push(id);
    effectTypes.push(widgetType);
    // 删除子节点
    (curWidget.config.children || []).forEach(id => {
      selectedIds.push(id);
    });
  }

  dispatch(editBoardStackActions.deleteWidgets(shouldDeleteIds));

  WidgetTypes.forEach(widgetType => {
    if (effectTypes.includes(widgetType)) {
      switch (widgetType) {
        case 'controller':
          dispatch(editWidgetsQueryAction());
          break;
        default:
          break;
      }
    }
  });
};

/* widgetToPositionAsync */
export const widgetsToPositionAction =
  (position: 'top' | 'bottom') => async (dispatch, getState) => {
    const editBoard = getState().editBoard as HistoryEditBoard;
    const widgetMap = editBoard.stack.present.widgetRecord;

    let curIds = Object.values(editBoard.widgetInfoRecord)
      .filter(WidgetInfo => WidgetInfo.selected)
      .map(item => item.id);

    if (curIds.length === 0) return;
    const sortedWidgetsIndex = Object.values(widgetMap)
      .sort((w1, w2) => {
        return w1.config.index - w2.config.index;
      })
      .map(item => item.config.index);
    const baseIndex =
      position === 'top'
        ? sortedWidgetsIndex[sortedWidgetsIndex.length - 1]
        : sortedWidgetsIndex[0];
    const opts = curIds.map((id, index) => {
      const diff = index + 1;
      const newIndex = position === 'top' ? baseIndex + diff : baseIndex - diff;
      return {
        id: id,
        index: newIndex,
      };
    });
    dispatch(editBoardStackActions.changeWidgetsIndex(opts));
  };

// 复制 copy widgets
export const copyWidgetsAction = (wIds?: string[]) => (dispatch, getState) => {
  const { editBoard } = getState();
  const { widgetInfoRecord } = editBoard as EditBoardState;
  const widgetMap = (editBoard as HistoryEditBoard).stack.present.widgetRecord;
  let selectedIds: string[] = [];

  if (wIds) {
    selectedIds = wIds;
  } else {
    selectedIds =
      Object.values(widgetInfoRecord)
        .filter(widgetInfo => widgetInfo.selected)
        .map(widgetInfo => widgetInfo.id) || [];
  }
  if (!selectedIds.length) return;
  const selectedItemParentIds: string[] = [];
  selectedIds.forEach(id => {
    if (widgetMap[id]) {
      const pid = widgetMap[id].parentId || '';
      if (!selectedItemParentIds.includes(pid)) {
        selectedItemParentIds.push(pid);
      }
    }
  });
  // 只可以同层级的widget可以复制 来自不同层级的 不可以同时复制
  /*
  a:{
    id:a,
    children:[b,c],
    parentId:''
  }
  **/
  //  复制 a 的时候以及复制了b和c ,不会再单独复制 b,c了
  if (selectedItemParentIds.length > 1) return;
  // 新复制前先清空
  dispatch(editDashBoardInfoActions.clearClipboardWidgets());
  const newWidgets: Record<string, WidgetOfCopy> = {};
  let needCopyWidgetIds = selectedIds;
  while (needCopyWidgetIds.length > 0) {
    const id = needCopyWidgetIds.pop();
    if (!id) continue;
    const widget = widgetMap[id];
    newWidgets[id] = { ...widget, selectedCopy: true };
    // group
    if (widget.config.children?.length) {
      needCopyWidgetIds = needCopyWidgetIds.concat(widget.config.children);
    }
    if (widget.config.type === 'container') {
      const content = widget.config.content as TabWidgetContent;
      const needCopyIds = Object.values(content.itemMap)
        .map(item => {
          if (item.childWidgetId) {
            const subWidget = widgetMap[item.childWidgetId];
            if (subWidget) return subWidget.id;
          }
          return undefined;
        })
        .filter(id => !!id);
      needCopyWidgetIds = needCopyWidgetIds.concat(needCopyIds as string[]);
    }
  }
  dispatch(editDashBoardInfoActions.addClipboardWidgets(newWidgets));
};

// 粘贴 widgets
export const pasteWidgetsAction = () => (dispatch, getState) => {
  const state = getState();
  const {
    boardInfo: { clipboardWidgetMap },
  } = state.editBoard as EditBoardState;
  const boardState = state.board as BoardState;

  const clipboardWidgetList = Object.values(clipboardWidgetMap);
  if (!clipboardWidgetList?.length) return;

  const newWidgetMapping = clipboardWidgetList.reduce((acc, cur) => {
    acc[cur.id] = {
      oldId: cur.id,
      newId: cur.config.originalType + '_' + uuidv4(),
      newClientId: initClientId(),
    };
    return acc;
  }, {} as WidgetMapping);
  const dataChartMap = boardState.dataChartMap;

  const { newDataCharts, newWidgets } = cloneWidgets({
    widgets: clipboardWidgetList,
    dataChartMap,
    newWidgetMapping,
  });
  const widgetInfos = newWidgets.map(widget => {
    const widgetInfo = createWidgetInfo(widget.id);
    return widgetInfo;
  });
  dispatch(boardActions.setDataChartToMap(newDataCharts));
  dispatch(editWidgetInfoActions.addWidgetInfos(widgetInfos));
  dispatch(editBoardStackActions.addWidgets(newWidgets));
};

export const editChartInWidgetAction =
  (props: {
    orgId: string;
    widgetId: string;
    chartName?: string;
    dataChartId: string;
    chartType: 'dataChart' | 'widgetChart';
  }) =>
  async (dispatch, getState) => {
    const {
      orgId,
      widgetId,
      dataChartId,
      chartType,
      chartName = `${BOARD_SELF_CHART_PREFIX}chart`,
    } = props;
    const board = (getState() as RootState).board!;

    const dataChartMap = board.dataChartMap;
    const dataChart = dataChartMap[dataChartId];
    const viewMap = board?.viewMap;
    const withViewDataChart = produce(dataChart, draft => {
      draft.view = viewMap[draft.viewId];
      draft.name = chartType === 'widgetChart' ? chartName : draft.name;
    });
    const editorProps: ChartEditorBaseProps = {
      widgetId: widgetId,
      dataChartId: dataChartId,
      orgId,
      chartType: chartType,
      container: 'widget',
      originChart: withViewDataChart,
    };
    dispatch(editDashBoardInfoActions.changeChartEditorProps(editorProps));
  };
export const editHasChartWidget =
  (props: { widgetId: string; dataChart: DataChart; view: ChartDataView }) =>
  async (dispatch, getState) => {
    const { dataChart, view, widgetId } = props;
    const editBoard = getState().editBoard as HistoryEditBoard;
    const widgetMap = editBoard.stack.present.widgetRecord;
    const curWidget = widgetMap[widgetId];
    const nextWidget = produce(curWidget, draft => {
      draft.viewIds = [dataChart.viewId];
    });
    dispatch(editBoardStackActions.updateWidget(nextWidget));
    const dataCharts = [dataChart];
    const viewViews = [view];
    dispatch(boardActions.setDataChartToMap(dataCharts));
    dispatch(boardActions.setViewMap(viewViews));
    dispatch(getEditChartWidgetDataAsync({ widgetId: curWidget.id }));
  };

export const onComposeGroupAction = (wid?: string) => (dispatch, getState) => {
  const rootState = getState() as RootState;
  const editBoardState = rootState.editBoard as unknown as HistoryEditBoard;
  const stackState = editBoardState.stack.present;
  const curBoard = stackState.dashBoard;
  const widgetMap = stackState.widgetRecord;
  const widgetInfos = Object.values(editBoardState.widgetInfoRecord || {});
  let selectedIds = widgetInfos.filter(it => it.selected).map(it => it.id);
  wid && selectedIds.push(wid);
  selectedIds = [...new Set(selectedIds)];
  if (!selectedIds.length) return;
  const selectedItemParentIds: string[] = [];
  selectedIds.forEach(id => {
    if (widgetMap[id]) {
      const pid = widgetMap[id].parentId || '';
      if (!selectedItemParentIds.includes(pid)) {
        selectedItemParentIds.push(pid);
      }
    }
  });
  // 只可以同层级的widget 成组 来自不同层级的 不可以成组
  if (selectedItemParentIds.length > 1) return;
  let groupWidget = widgetManager.toolkit(ORIGINAL_TYPE_MAP.group).create({
    boardType: curBoard.config.type,
    children: selectedIds,
    parentId: selectedItemParentIds[0],
  });
  groupWidget.config.rect = getParentRect({
    childIds: selectedIds,
    widgetMap,
    preRect: groupWidget.config.rect,
  });
  const items = selectedIds.map(id => {
    return {
      wid: id,
      nextIndex: widgetMap[id].config.index,
      parentId: groupWidget.id,
    };
  });
  const widgetInfo = createWidgetInfo(groupWidget.id);
  widgetInfo.selected = true;
  dispatch(editWidgetInfoActions.addWidgetInfos([widgetInfo]));
  dispatch(editBoardStackActions.addWidgets([groupWidget]));
  dispatch(editBoardStackActions.changeWidgetsParentId({ items }));
};
export const onUnGroupAction = (wid?: string) => (dispatch, getState) => {
  const rootState = getState() as RootState;
  const editBoardState = rootState.editBoard as unknown as HistoryEditBoard;
  const stackState = editBoardState.stack.present;
  const widgetMap = stackState.widgetRecord;
  const widgetInfos = Object.values(editBoardState.widgetInfoRecord || {});
  let selectedIds = widgetInfos.filter(it => it.selected).map(it => it.id);
  wid && selectedIds.push(wid);
  selectedIds = [...new Set(selectedIds)];
  if (!selectedIds.length) return;

  selectedIds = selectedIds.filter(id => {
    if (widgetMap[id]) {
      const pw = widgetMap[id];
      return pw.config.originalType === ORIGINAL_TYPE_MAP.group;
    }
    return false;
  });

  let childIdList: {
    wid: string;
    nextIndex: number;
    parentId: string;
  }[] = [];
  selectedIds.forEach(pid => {
    const pw = widgetMap[pid];
    (pw.config.children || [])
      .filter(id => widgetMap[id])
      .forEach(id => {
        childIdList.push({
          wid: id,
          nextIndex: widgetMap[id].config.index,
          parentId: pw.parentId,
        });
      });
  });

  dispatch(editBoardStackActions.changeWidgetsParentId({ items: childIdList }));
};
export const addVariablesToBoard =
  (variables: Variable[]) => (dispatch, getState) => {
    if (!variables?.length) return;
    const addedViewId = variables[0].viewId;
    if (!addedViewId) return;

    const editBoard = getState().editBoard as HistoryEditBoard;
    const queryVariables = editBoard.stack.present.dashBoard.queryVariables;
    const hasAddedViewId = queryVariables.find(v => v.viewId === addedViewId);
    if (hasAddedViewId) return;
    let newVariables = queryVariables.concat(variables);
    dispatch(editBoardStackActions.updateQueryVariables(newVariables));
  };

export const clearActiveWidgets = () => dispatch => {
  dispatch(editWidgetInfoActions.clearSelectedWidgets());
  dispatch(editDashBoardInfoActions.changeShowBlockMask(true));
};

export const widgetClearLinkageAction =
  (widget: Widget, renderMode: VizRenderMode) => (dispatch, getState) => {
    const { id, dashboardId } = widget;
    const rootState = getState();
    const boardWidgetInfoRecord = selectWidgetInfoMap(getState(), dashboardId);
    const executeTokenMap = rootState?.share?.executeTokenMap || {};
    const currentWidgetInfo = boardWidgetInfoRecord?.[id];

    let executeToken;
    if (renderMode === 'share') {
      executeToken = executeTokenMap?.[widget?.viewIds?.[0]]?.authorizedToken;
    }
    if (!currentWidgetInfo?.inLinking) {
      return;
    }
    dispatch(
      boardActions.changeWidgetInLinking({
        boardId: dashboardId,
        widgetId: id,
        toggle: false,
      }),
    );
    dispatch(boardActions.changeSelectedItems({ wid: id, data: [] }));
    const linkTargetWidgets = Object.values(boardWidgetInfoRecord || {}).filter(
      widgetInfo => widgetInfo?.linkInfo?.sourceWidgetId === id,
    );
    linkTargetWidgets.forEach(widgetInfo => {
      const filters = widgetInfo?.linkInfo?.filters || [];
      const variables = widgetInfo?.linkInfo?.variables;
      dispatch(
        syncBoardWidgetChartDataAsync({
          boardId: dashboardId,
          sourceWidgetId: '',
          widgetId: widgetInfo.id,
          executeToken,
          extraFilters: filters,
          variableParams: variables,
        }),
      );
    });
  };

export const editorWidgetClearLinkageAction =
  (widget: Widget) => (dispatch, getState) => {
    const { id, dashboardId } = widget;
    const boardWidgetInfoRecord = selectAllWidgetInfoMap(getState());
    const currentWidgetInfo = boardWidgetInfoRecord?.[id];
    if (!currentWidgetInfo?.inLinking) {
      return;
    }

    dispatch(
      editWidgetInfoActions.changeWidgetInLinking({
        boardId: dashboardId,
        widgetId: id,
        toggle: false,
      }),
    );
    dispatch(
      editWidgetSelectedItemsActions.changeSelectedItemsInEditor({
        wid: id,
        data: [],
      }),
    );
    const linkTargetWidgets = Object.values(boardWidgetInfoRecord || {}).filter(
      widgetInfo => widgetInfo?.linkInfo?.sourceWidgetId === id,
    );
    linkTargetWidgets.forEach(widgetInfo => {
      const filters = widgetInfo?.linkInfo?.filters || [];
      const variables = widgetInfo?.linkInfo?.variables;
      dispatch(
        syncEditBoardWidgetChartDataAsync({
          boardId: dashboardId,
          sourceWidgetId: '',
          widgetId: widgetInfo.id,
          extraFilters: filters,
          variableParams: variables,
        }),
      );
    });
  };

export const dropLayerNodeAction = info => (dispatch, getState) => {
  const dragNode = info.dragNode as EventLayerNode;
  const targetNode = info.node as EventLayerNode;
  const editBoard = getState().editBoard as HistoryEditBoard;
  const widgetMap = editBoard.stack.present.widgetRecord;

  /*
  1 -> group
  2 -> container
  */
  //拖拽节点来自 group
  const newDragVal = getNewDragNodeValue({ widgetMap, dragNode, targetNode });
  if (newDragVal.parentIsGroup) {
    // 拖进 group
    dispatch(
      editBoardStackActions.dropWidgetToGroup({
        sourceId: dragNode.key,
        newIndex: newDragVal.index,
        targetId: newDragVal.parentId,
      }),
    );
  } else {
    // 拖进 tab
    const dragWidget = widgetMap[dragNode.key];
    const newItem: ContainerItem = {
      index: newDragVal.index,
      name: dragWidget.config.name,
      tabId: dragWidget.config.clientId,
      childWidgetId: dragWidget.id,
    };
    dispatch(
      editBoardStackActions.dropWidgetToTab({
        newItem,
        targetId: newDragVal.parentId,
      }),
    );
    return;
  }
};

export const selectWidgetAction =
  (args: { multipleKey: boolean; id: string; selected: boolean }) =>
  (dispatch, getState) => {
    const { multipleKey, id, selected } = args;
    const editBoard = getState().editBoard as HistoryEditBoard;
    const widgetMap = editBoard.stack.present.widgetRecord;
    const parentIds: string[] = [];
    findParentIds({ widget: widgetMap[id], widgetMap, parentIds });
    dispatch(
      editWidgetInfoActions.selectWidget({
        multipleKey,
        id,
        selected,
        parentIds,
      }),
    );
  };

export const changeSelectedItems = (
  dispatch,
  type: VizRenderMode,
  data,
  wid,
) => {
  if (type === 'edit') {
    dispatch(
      editWidgetSelectedItemsActions.changeSelectedItemsInEditor({ wid, data }),
    );
  } else {
    dispatch(boardActions.changeSelectedItems({ wid, data }));
  }
};

export const showRectAction = (widget: Widget) => (dispatch, getState) => {
  if (widget.config.boardType === 'free') return true;
  if (!widget.parentId) return false;
  const editBoard = getState().editBoard as HistoryEditBoard;
  const widgetMap = editBoard.stack.present.widgetRecord;
  if (widget.parentId && widgetMap[widget.parentId]) {
    return widgetMap[widget.parentId].config.type === 'group';
  }
  return false;
};
