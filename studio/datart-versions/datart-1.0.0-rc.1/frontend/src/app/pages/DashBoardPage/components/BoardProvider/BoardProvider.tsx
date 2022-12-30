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
import {
  BoardType,
  Dashboard,
  VizRenderMode,
} from '../../pages/Board/slice/types';

export interface BoardContextProps {
  orgId: string;
  boardId: string;
  name: string;
  renderMode: VizRenderMode;
  boardType: BoardType;
  status: number;
  editing: boolean;
  thumbnail?: string;
  autoFit?: boolean | undefined;
  hideTitle?: boolean | undefined;
  allowDownload?: boolean;
  allowShare?: boolean;
  allowManage?: boolean;
  queryVariables: Dashboard['queryVariables'];
}

export const BoardContext = createContext<BoardContextProps>(
  {} as BoardContextProps,
);
export const BoardProvider: FC<BoardContextProps> = memo(
  ({
    orgId,
    boardId,
    name,
    renderMode,
    editing,
    boardType,
    status,
    autoFit,
    allowDownload,
    allowShare,
    allowManage,
    queryVariables,
    children,
  }) => {
    const boardContextValue: BoardContextProps = useMemo(() => {
      return {
        orgId,
        boardId,
        name,
        boardType,
        status,
        queryVariables,
        renderMode,
        editing,
        autoFit,
        allowDownload,
        allowShare,
        allowManage,
      };
    }, [
      allowDownload,
      allowManage,
      allowShare,
      autoFit,
      boardId,
      boardType,
      editing,
      name,
      orgId,
      queryVariables,
      renderMode,
      status,
    ]);

    return (
      <BoardContext.Provider value={boardContextValue}>
        {children}
      </BoardContext.Provider>
    );
  },
);
