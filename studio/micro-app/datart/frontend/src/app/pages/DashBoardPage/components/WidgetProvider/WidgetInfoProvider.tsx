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

import { WidgetInfo } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { createContext, FC, memo, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { selectWidgetInfoBy2Id } from '../../pages/Board/slice/selector';
import { BoardState } from '../../pages/Board/slice/types';
import { selectWidgetInfoById } from '../../pages/BoardEditor/slice/selectors';
import { EditBoardState } from '../../pages/BoardEditor/slice/types';

export const WidgetInfoContext = createContext<WidgetInfo>({} as WidgetInfo);

export const WidgetInfoProvider: FC<{
  boardId: string;
  boardEditing: boolean;
  widgetId: string;
}> = memo(({ widgetId, boardId, boardEditing, children }) => {
  // 浏览模式
  const readWidgetInfo = useSelector((state: { board: BoardState }) =>
    selectWidgetInfoBy2Id(state, boardId, widgetId),
  );
  // 编辑模式
  const editWidgetInfo = useSelector((state: { editBoard: EditBoardState }) =>
    selectWidgetInfoById(state, widgetId),
  );
  const widgetInfo = useMemo(
    () => (boardEditing ? editWidgetInfo : readWidgetInfo),
    [boardEditing, editWidgetInfo, readWidgetInfo],
  );
  return widgetInfo ? (
    <WidgetInfoContext.Provider value={widgetInfo}>
      {children}
    </WidgetInfoContext.Provider>
  ) : null;
});
