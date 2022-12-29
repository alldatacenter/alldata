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

import { createContext, FC, memo, useMemo } from 'react';
import { Dashboard, VizRenderMode } from '../../pages/Board/slice/types';
import { BoardActionProvider } from '../ActionProvider/BoardActionProvider';
import { WidgetActionProvider } from '../ActionProvider/WidgetActionProvider';
import { BoardConfigProvider } from './BoardConfigProvider';
import { BoardInfoProvider } from './BoardInfoProvider';
import { BoardContextProps, BoardProvider } from './BoardProvider';

export interface BoardInitContextProps {
  board: Dashboard;
  renderMode: VizRenderMode;
  editing: boolean;
  autoFit?: boolean;
  allowDownload?: boolean;
  allowShare?: boolean;
  allowManage?: boolean;
}

export const BoardInitContext = createContext<BoardInitContextProps>(
  {} as BoardInitContextProps,
);
export const BoardInitProvider: FC<BoardInitContextProps> = memo(
  ({
    board,
    editing,
    children,
    renderMode,
    autoFit,
    allowDownload,
    allowShare,
    allowManage,
  }) => {
    const boardContextProps: BoardContextProps = useMemo(() => {
      const boardContextProps: BoardContextProps = {
        name: board.name,
        boardId: board.id,
        status: board.status,
        queryVariables: board.queryVariables,
        renderMode,
        orgId: board.orgId,
        boardType: board.config.type,
        editing: editing,
        autoFit: autoFit,
        allowDownload,
        allowShare,
        allowManage,
      };
      return boardContextProps;
    }, [
      allowDownload,
      allowManage,
      allowShare,
      autoFit,
      board.config.type,
      board.id,
      board.name,
      board.orgId,
      board.queryVariables,
      board.status,
      editing,
      renderMode,
    ]);
    return (
      <BoardActionProvider boardId={board.id}>
        <WidgetActionProvider
          boardId={board.id}
          boardEditing={editing}
          renderMode={renderMode}
          orgId={board.orgId}
          boardType={board.config.type}
        >
          <BoardProvider {...boardContextProps}>
            <BoardConfigProvider config={board.config} boardId={board.id}>
              <BoardInfoProvider id={board.id} editing={editing}>
                {children}
              </BoardInfoProvider>
            </BoardConfigProvider>
          </BoardProvider>
        </WidgetActionProvider>
      </BoardActionProvider>
    );
  },
);
