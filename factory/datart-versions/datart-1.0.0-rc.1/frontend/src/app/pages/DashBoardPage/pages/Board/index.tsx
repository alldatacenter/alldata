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

import useResizeObserver from 'app/hooks/useResizeObserver';
import { FC, memo, useEffect, useMemo } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';
import { boardDrillManager } from '../../components/BoardDrillManager/BoardDrillManager';
import { TitleHeader } from '../../components/BoardHeader/TitleHeader';
import { BoardLoading } from '../../components/BoardLoading';
import { BoardInitProvider } from '../../components/BoardProvider/BoardInitProvider';
import { FullScreenPanel } from '../../components/FullScreenPanel/FullScreenPanel';
import { selectEditBoard } from '../BoardEditor/slice/selectors';
import { AutoBoardCore } from './AutoDashboard/AutoBoardCore';
import { FreeBoardCore } from './FreeDashboard/FreeBoardCore';
import { boardActions } from './slice';
import { makeSelectBoardConfigById } from './slice/selector';
import { fetchBoardDetail } from './slice/thunk';
import { BoardState, VizRenderMode } from './slice/types';

export interface BoardProps {
  id: string;
  renderMode: VizRenderMode;
  hideTitle?: boolean;
  fetchData?: boolean;
  filterSearchUrl?: string;
  allowDownload?: boolean;
  allowShare?: boolean;
  allowManage?: boolean;
  autoFit?: boolean;
  showZoomCtrl?: boolean;
}

export const Board: FC<BoardProps> = memo(
  ({
    id,
    hideTitle,
    fetchData = true,
    renderMode,
    filterSearchUrl,
    allowDownload,
    allowShare,
    allowManage,
    autoFit,
    showZoomCtrl,
  }) => {
    const boardId = id;
    const dispatch = useDispatch();
    const editingBoard = useSelector(selectEditBoard);

    const readBoardHide = useMemo(
      () => editingBoard?.id === boardId,
      [boardId, editingBoard.id],
    );
    const { ref, width, height } = useResizeObserver<HTMLDivElement>({
      refreshMode: 'debounce',
      refreshRate: 2000,
    });

    const dashboard = useSelector((state: { board: BoardState }) =>
      makeSelectBoardConfigById()(state, boardId),
    );

    const searchParams = useMemo(() => {
      return filterSearchUrl
        ? urlSearchTransfer.toParams(filterSearchUrl)
        : undefined;
    }, [filterSearchUrl]);

    const viewBoard = useMemo(() => {
      let boardType = dashboard?.config?.type;
      if (dashboard && boardType) {
        return (
          <div className="board-provider">
            <BoardInitProvider
              board={dashboard}
              editing={false}
              autoFit={autoFit}
              renderMode={renderMode}
              allowDownload={allowDownload}
              allowShare={allowShare}
              allowManage={allowManage}
            >
              {!hideTitle && <TitleHeader />}
              {!readBoardHide && (
                <>
                  {boardType === 'auto' && (
                    <AutoBoardCore boardId={dashboard.id} />
                  )}
                  {boardType === 'free' && (
                    <FreeBoardCore
                      boardId={dashboard.id}
                      showZoomCtrl={showZoomCtrl}
                    />
                  )}
                </>
              )}

              <FullScreenPanel />
            </BoardInitProvider>
          </div>
        );
      } else {
        return <BoardLoading />;
      }
    }, [
      dashboard,
      autoFit,
      renderMode,
      allowDownload,
      allowShare,
      allowManage,
      hideTitle,
      readBoardHide,
      showZoomCtrl,
    ]);

    useEffect(() => {
      if (width! > 0 && height! > 0 && dashboard?.id && !readBoardHide) {
        dispatch(
          boardActions.changeBoardVisible({ id: dashboard?.id, visible: true }),
        );
      } else {
        dispatch(
          boardActions.changeBoardVisible({
            id: dashboard?.id as string,
            visible: false,
          }),
        );
      }
    }, [readBoardHide, dashboard?.id, dispatch, height, width]);

    useEffect(() => {
      if (boardId && fetchData) {
        dispatch(
          fetchBoardDetail({
            dashboardRelId: boardId,
            filterSearchParams: searchParams,
          }),
        );
      }

      // 销毁组件 清除该对象缓存
      return () => {
        dispatch(boardActions.clearBoardStateById(boardId));
        boardDrillManager.clearMapByBoardId(boardId);
      };
    }, [boardId, dispatch, fetchData, searchParams]);

    return (
      <Wrapper ref={ref} className="dashboard-box">
        <DndProvider backend={HTML5Backend}>{viewBoard}</DndProvider>
      </Wrapper>
    );
  },
);

export default Board;

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;

  .board-provider {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
  }
`;
