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
import ChartEditor from 'app/components/ChartEditor';
import { BOARD_SELF_CHART_PREFIX } from 'globalConstants';
import React, { memo, useCallback, useEffect, useMemo } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import { LEVEL_10 } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import {
  boardDrillManager,
  EDIT_PREFIX,
} from '../../components/BoardDrillManager/BoardDrillManager';
import EditorHeader from '../../components/BoardHeader/EditorHeader';
import { BoardLoading } from '../../components/BoardLoading';
import { BoardInitProvider } from '../../components/BoardProvider/BoardInitProvider';
import { fetchBoardDetail } from '../Board/slice/thunk';
import { DataChart, WidgetContentChartType } from '../Board/slice/types';
import { AutoEditor } from './AutoEditor';
import ControllerWidgetPanel from './components/ControllerWidgetPanel';
import { FreeEditor } from './FreeEditor';
import { editDashBoardInfoActions, useEditBoardSlice } from './slice';
import {
  addVariablesToBoard,
  clearEditBoardState,
  editHasChartWidget,
} from './slice/actions/actions';
import {
  selectBoardChartEditorProps,
  selectControllerPanel,
  selectEditBoard,
  selectEditBoardLoading,
} from './slice/selectors';
import { addChartWidget, fetchEditBoardDetail } from './slice/thunk';

export const BoardEditor: React.FC<{
  boardId: string;
}> = memo(({ boardId }) => {
  useEditBoardSlice();
  const dispatch = useDispatch();
  const history = useHistory();
  const board = useSelector(selectEditBoard);
  const boardLoading = useSelector(selectEditBoardLoading);
  const boardChartEditorProps = useSelector(selectBoardChartEditorProps);
  const widgetControllerPanelParams = useSelector(selectControllerPanel);
  const onCloseChartEditor = useCallback(() => {
    dispatch(editDashBoardInfoActions.changeChartEditorProps(undefined));
  }, [dispatch]);

  const onSaveToWidget = useCallback(
    (chartType: WidgetContentChartType, dataChart: DataChart, view) => {
      const widgetId = boardChartEditorProps?.widgetId!;
      dispatch(editHasChartWidget({ widgetId, dataChart, view }));
      onCloseChartEditor();
      dispatch(addVariablesToBoard(view.variables));
    },
    [boardChartEditorProps?.widgetId, dispatch, onCloseChartEditor],
  );

  const boardEditor = useMemo(() => {
    if (!board.id) return null;
    if (board?.id !== boardId) {
      return null;
    }
    const boardType = board.config?.type;

    return (
      <BoardInitProvider
        board={board}
        editing={true}
        autoFit={false}
        allowDownload={false}
        allowShare={false}
        allowManage={false}
        renderMode="edit"
      >
        <EditorHeader />
        {boardType === 'auto' && <AutoEditor />}
        {boardType === 'free' && <FreeEditor />}
        {widgetControllerPanelParams.type !== 'hide' && (
          <ControllerWidgetPanel {...widgetControllerPanelParams} />
        )}
        {boardChartEditorProps && (
          <ChartEditor
            {...boardChartEditorProps}
            onClose={onCloseChartEditor}
            onSaveInWidget={onSaveToWidget}
          />
        )}
      </BoardInitProvider>
    );
  }, [
    board,
    boardId,
    widgetControllerPanelParams,
    boardChartEditorProps,
    onCloseChartEditor,
    onSaveToWidget,
  ]);
  const initialization = useCallback(async () => {
    await dispatch(fetchEditBoardDetail(boardId));
    const histState = history.location.state as any;
    try {
      if (histState?.widgetInfo) {
        // TODO(Stephen): to be confirm if use history state widget info and for what
        console.error(
          'if you see the error on board editor, please contact to administrator',
        );
        const widgetInfo = JSON.parse(histState.widgetInfo);
        if (widgetInfo) {
          let subType: 'widgetChart' | 'dataChart' = 'dataChart';
          if (!widgetInfo.dataChart.id) {
            widgetInfo.dataChart.id = `${BOARD_SELF_CHART_PREFIX}${boardId}_${uuidv4()}`;
            subType = 'widgetChart';
          }
          dispatch(
            addChartWidget({
              boardId,
              chartId: widgetInfo.dataChart.id,
              boardType: widgetInfo.dashboardType,
              dataChart: widgetInfo.dataChart,
              view: widgetInfo.dataview,
              subType: subType,
            }),
          );
        }
      }
    } catch (error) {
      console.log(error);
    }
  }, [dispatch, history.location.state, boardId]);

  useEffect(() => {
    initialization();
    return () => {
      // fix issue: #800
      onCloseChartEditor();
      dispatch(clearEditBoardState());
      //销毁时  更新view界面数据
      dispatch(fetchBoardDetail({ dashboardRelId: boardId }));
      //
      boardDrillManager.clearMapByBoardId(EDIT_PREFIX + boardId);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onCloseChartEditor]);

  return (
    <StyledBoardEditor>
      <DndProvider backend={HTML5Backend}>
        {boardEditor}
        {boardLoading && <BoardLoading />}
      </DndProvider>
    </StyledBoardEditor>
  );
});
export default BoardEditor;

const StyledBoardEditor = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_10};
  display: flex;
  flex-direction: column;
  padding-bottom: 0;
  background-color: ${p => p.theme.bodyBackground};
`;
