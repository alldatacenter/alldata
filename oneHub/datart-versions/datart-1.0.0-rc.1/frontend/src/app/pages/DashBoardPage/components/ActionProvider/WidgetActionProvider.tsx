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

import { ControllerFacadeTypes } from 'app/constants';
import { ChartMouseEventParams } from 'app/types/Chart';
import { ChartConfig } from 'app/types/ChartConfig';
import debounce from 'lodash/debounce';
import { createContext, FC, memo, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router-dom';
import {
  changeGroupRectAction,
  refreshWidgetsByControllerAction,
  widgetChartClickAction,
  widgetGetDataAction,
  widgetLinkEventActionCreator,
  widgetToClearLinkageAction,
} from '../../actions/widgetAction';
import { ORIGINAL_TYPE_MAP } from '../../constants';
import { boardActions } from '../../pages/Board/slice';
import {
  resetControllerAction,
  widgetsQueryAction,
} from '../../pages/Board/slice/asyncActions';
import { renderedWidgetAsync } from '../../pages/Board/slice/thunk';
import {
  BoardType,
  RectConfig,
  VizRenderMode,
} from '../../pages/Board/slice/types';
import {
  editBoardStackActions,
  editDashBoardInfoActions,
  editWidgetInfoActions,
  editWidgetSelectedItemsActions,
} from '../../pages/BoardEditor/slice';
import {
  clearActiveWidgets,
  copyWidgetsAction,
  deleteWidgetsAction,
  editChartInWidgetAction,
  onComposeGroupAction,
  onUnGroupAction,
  pasteWidgetsAction,
  selectWidgetAction,
  widgetsToPositionAction,
} from '../../pages/BoardEditor/slice/actions/actions';
import { editWidgetsQueryAction } from '../../pages/BoardEditor/slice/actions/controlActions';
import { renderedEditWidgetAsync } from '../../pages/BoardEditor/slice/thunk';
import { Widget, WidgetConf } from '../../types/widgetTypes';

export const WidgetActionProvider: FC<{
  orgId: string;
  boardId: string;
  boardEditing: boolean;
  renderMode: VizRenderMode;
  boardType: BoardType;
}> = memo(
  ({ boardEditing, boardId, orgId, boardType, renderMode, children }) => {
    const dispatch = useDispatch();
    const history = useHistory<any>();
    const methods = useMemo(() => {
      const contextValue: WidgetActionContextProps = {
        onEditLayerToTop: () => {
          dispatch(widgetsToPositionAction('top'));
        },
        onEditLayerToBottom: () => {
          dispatch(widgetsToPositionAction('bottom'));
        },
        onEditSelectWidget: args => {
          dispatch(selectWidgetAction(args));
        },
        onEditCopyWidgets: (ids?: string[]) => {
          dispatch(copyWidgetsAction());
        },
        onEditPasteWidgets: () => {
          dispatch(pasteWidgetsAction());
        },
        onChangeGroupRect: debounce(
          (args: {
            wid: string;
            w: number;
            h: number;
            isAutoGroupWidget: boolean;
          }) => {
            const { wid, w, h, isAutoGroupWidget } = args;
            dispatch(
              changeGroupRectAction({
                renderMode,
                boardId,
                wid,
                w,
                h,
                isAutoGroupWidget,
              }),
            );
          },
          60,
        ),
        onEditDeleteActiveWidgets: debounce((ids?: string[]) => {
          dispatch(deleteWidgetsAction(ids));
        }, 200),
        onRenderedWidgetById: (wid: string) => {
          if (boardEditing) {
            dispatch(
              renderedEditWidgetAsync({ boardId: boardId, widgetId: wid }),
            );
          } else {
            dispatch(
              renderedWidgetAsync({
                boardId: boardId,
                widgetId: wid,
                renderMode: renderMode,
              }),
            );
          }
        },
        onEditClearActiveWidgets: () => {
          dispatch(clearActiveWidgets());
        },
        onWidgetsQuery: debounce(() => {
          if (boardEditing) {
            dispatch(editWidgetsQueryAction());
          } else {
            dispatch(widgetsQueryAction({ boardId, renderMode }));
          }
        }, 500),
        onWidgetsReset: debounce(() => {
          if (boardEditing) {
            return;
          } else {
            dispatch(resetControllerAction({ boardId, renderMode }));
          }
        }, 500),
        onRefreshWidgetsByController: debounce((widget: Widget) => {
          dispatch(refreshWidgetsByControllerAction(renderMode, widget));
        }, 500),
        onUpdateWidgetConfig: (config: WidgetConf, wid: string) => {
          dispatch(editBoardStackActions.updateWidgetConfig({ wid, config }));
        },
        onEditFreeWidgetRect: (rect, wid, isAutoGroupWidget) => {
          dispatch(
            editBoardStackActions.changeFreeWidgetRect({
              rect,
              wid,
              isAutoGroupWidget,
            }),
          );
        },
        onUpdateWidgetConfigByKey: ops => {
          if (boardEditing) {
            dispatch(editBoardStackActions.updateWidgetConfigByKey(ops));
          } else {
            dispatch(boardActions.updateWidgetConfigByKey({ ...ops, boardId }));
          }
          dispatch(editBoardStackActions.updateWidgetConfigByKey(ops));
        },
        onWidgetUpdate: (widget: Widget) => {
          if (boardEditing) {
            dispatch(editBoardStackActions.updateWidget(widget));
          } else {
            dispatch(boardActions.updateWidget(widget));
          }
        },
        onWidgetLinkEvent: (widget: Widget) => params => {
          dispatch(
            widgetLinkEventActionCreator({
              renderMode,
              widget,
              params,
            }),
          );
        },
        onUpdateWidgetSelectedItems: (widget: Widget, selectedItems) => {
          if (boardEditing) {
            dispatch(
              editWidgetSelectedItemsActions.changeSelectedItemsInEditor({
                wid: widget?.id,
                data: selectedItems,
              }),
            );
          } else {
            dispatch(
              boardActions.changeSelectedItems({
                wid: widget?.id,
                data: selectedItems,
              }),
            );
          }
        },
        onWidgetChartClick: (widget: Widget, params: ChartMouseEventParams) => {
          dispatch(
            widgetChartClickAction({
              boardId,
              editing: boardEditing,
              renderMode,
              widget,
              params,
              history,
            }),
          );
        },
        onWidgetClearLinkage: (widget: Widget) => {
          dispatch(
            widgetToClearLinkageAction(boardEditing, widget, renderMode),
          );
        },
        onWidgetFullScreen: (itemId: string) => {
          dispatch(
            boardActions.updateFullScreenPanel({
              boardId,
              itemId,
            }),
          );
        },
        onWidgetGetData: (widget: Widget) => {
          dispatch(widgetGetDataAction(boardEditing, widget, renderMode));
        },
        onEditChartWidget: (widget: Widget) => {
          const originalType = widget.config.originalType;
          const chartType =
            originalType === ORIGINAL_TYPE_MAP.ownedChart
              ? 'widgetChart'
              : 'dataChart';
          dispatch(
            editChartInWidgetAction({
              orgId,
              widgetId: widget.id,
              chartName: widget.config.name,
              dataChartId: widget.datachartId,
              chartType: chartType,
            }),
          );
        },
        onEditMediaWidget: (id: string) => {
          dispatch(editWidgetInfoActions.openWidgetEditing({ id }));
        },
        onEditContainerWidget: (id: string) => {
          dispatch(editWidgetInfoActions.openWidgetEditing({ id }));
          dispatch(editDashBoardInfoActions.changeShowBlockMask(false)); // TODO(Stephen): to be check, this is only used for tab container?
        },
        onEditControllerWidget: (widget: Widget) => {
          dispatch(
            editDashBoardInfoActions.changeControllerPanel({
              type: 'edit',
              widgetId: widget.id,
              controllerType: widget.config.content
                .type as ControllerFacadeTypes,
            }),
          );
        },
        onEditWidgetLock: (id: string) => {
          dispatch(editBoardStackActions.toggleLockWidget({ id, lock: true }));
        },
        onEditWidgetUnLock: (id: string) => {
          dispatch(editBoardStackActions.toggleLockWidget({ id, lock: false }));
        },
        onWidgetDataUpdate: ({ computedFields, payload, widgetId }) => {
          dispatch(
            boardActions.updateDataChartGroup({
              id: widgetId,
              payload,
            }),
          );

          dispatch(
            boardActions.updateDataChartComputedFields({
              id: widgetId,
              computedFields,
            }),
          );
        },
        onEditComposeGroup: wid => {
          dispatch(onComposeGroupAction(wid));
        },
        onEditUnGroupAction: wid => {
          dispatch(onUnGroupAction(wid));
        },
      };
      return contextValue;
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [boardEditing, boardId, boardType, dispatch, orgId, renderMode]);

    return (
      <WidgetActionContext.Provider value={methods}>
        {children}
      </WidgetActionContext.Provider>
    );
  },
);
export interface WidgetActionContextProps {
  // all
  onWidgetChartClick: (widget: Widget, params: ChartMouseEventParams) => void;
  onWidgetClearLinkage: (widget: Widget) => void;
  onWidgetGetData: (widget: Widget) => void;
  onWidgetUpdate: (widget: Widget) => void;
  onUpdateWidgetConfig: (config: WidgetConf, wid: string) => void;
  onUpdateWidgetConfigByKey: (opt: { wid: string; key: string; val }) => void;
  onRefreshWidgetsByController: (widget: Widget) => void;
  onWidgetsQuery: () => void;
  onRenderedWidgetById: (wid: string) => void;
  onChangeGroupRect: (args: {
    wid: string;
    w: number;
    h: number;
    isAutoGroupWidget: boolean;
  }) => void;
  onWidgetLinkEvent: (widget: Widget) => (params) => void;
  onUpdateWidgetSelectedItems: (widget: Widget, selectedItems) => void;

  onWidgetDataUpdate: ({
    computedFields,
    payload,
    widgetId,
  }: {
    computedFields: any;
    payload: ChartConfig;
    widgetId: string;
  }) => void;

  // read
  onWidgetFullScreen: (itemId: string) => void;
  onWidgetsReset: () => void;

  // editor
  // selectWidget
  onEditSelectWidget: (args: {
    multipleKey: boolean;
    id: string;
    selected: boolean;
  }) => void;
  onEditChartWidget: (widget: Widget) => void;
  onEditContainerWidget: (wid: string) => void;
  onEditMediaWidget: (wid: string) => void;
  onEditControllerWidget: (widget: Widget) => void;
  // onEditWidgetLinkage: (wid: string) => void;
  // onEditWidgetJump: (wid: string) => void;
  // onEditWidgetCloseLinkage: (widget: Widget) => void;
  // onEditWidgetCloseJump: (widget: Widget) => void;
  onEditWidgetLock: (id: string) => void;
  onEditWidgetUnLock: (id: string) => void;
  onEditClearActiveWidgets: () => void;
  onEditDeleteActiveWidgets: (ids?: string[]) => void;
  onEditLayerToTop: () => void;
  onEditLayerToBottom: () => void;
  onEditCopyWidgets: (ids?: string[]) => void;
  onEditPasteWidgets: () => void;
  onEditComposeGroup: (wid?: string) => void;
  onEditUnGroupAction: (wid?: string) => void;
  onEditFreeWidgetRect: (
    rect: RectConfig,
    wid: string,
    isAutoGroupWidget: boolean,
  ) => void;
  //
}
export const WidgetActionContext = createContext<WidgetActionContextProps>(
  {} as WidgetActionContextProps,
);
