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
import { generateShareLinkAsync } from 'app/utils/fetch';
import { createContext, FC, memo, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { BOARD_UNDO } from '../../constants';
import { boardDownLoadAction } from '../../pages/Board/slice/asyncActions';
import { fetchBoardDetail } from '../../pages/Board/slice/thunk';
import { clearEditBoardState } from '../../pages/BoardEditor/slice/actions/actions';
import { toUpdateDashboard } from '../../pages/BoardEditor/slice/thunk';

export interface BoardActionContextProps {
  // read
  onGenerateShareLink?: ({
    expiryDate,
    authenticationMode,
    roles,
    users,
    rowPermissionBy,
  }: {
    expiryDate: string;
    authenticationMode: string;
    roles: string[];
    users: string[];
    rowPermissionBy: string;
  }) => any;
  onBoardToDownLoad: (downloadType: DownloadFileType) => any;
  // edit
  updateBoard?: (callback?: () => void) => void;

  onCloseBoardEditor: (boardId: string) => void;
  undo: () => void;
  redo: () => void;
}
export const BoardActionContext = createContext<BoardActionContextProps>(
  {} as BoardActionContextProps,
);
export const BoardActionProvider: FC<{
  boardId: string;
}> = memo(({ boardId, children }) => {
  const dispatch = useDispatch();
  const history = useHistory();

  const actions = useMemo(() => {
    const actionObj: BoardActionContextProps = {
      updateBoard: (callback?: () => void) => {
        dispatch(toUpdateDashboard({ boardId, callback }));
      },

      onGenerateShareLink: async ({
        expiryDate,
        authenticationMode,
        roles,
        users,
        rowPermissionBy,
      }) => {
        const result = await generateShareLinkAsync({
          expiryDate,
          authenticationMode,
          roles,
          users,
          rowPermissionBy,
          vizId: boardId,
          vizType: 'DASHBOARD',
        });
        return result;
      },
      onBoardToDownLoad: downloadType => {
        dispatch(
          boardDownLoadAction({
            boardId,
            downloadType,
          }),
        );
      },
      onCloseBoardEditor: (boardId: string) => {
        const pathName = history.location.pathname;
        const prePath = pathName.split('/boardEditor')[0];
        history.push(`${prePath}`);
        dispatch(clearEditBoardState());
        // 更新view界面数据
        dispatch(fetchBoardDetail({ dashboardRelId: boardId }));
      },
      undo: () => {
        dispatch({ type: BOARD_UNDO.undo });
      },
      redo: () => {
        dispatch({ type: BOARD_UNDO.redo });
      },
    };
    return actionObj;
  }, [boardId, dispatch, history]);
  return (
    <BoardActionContext.Provider value={actions}>
      {children}
    </BoardActionContext.Provider>
  );
});
