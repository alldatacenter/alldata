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

import { WidgetData } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { createContext, FC, memo } from 'react';
import { useSelector } from 'react-redux';
import { selectWidgetDataById } from '../../pages/Board/slice/selector';
import { BoardState } from '../../pages/Board/slice/types';
import { selectEditWidgetData } from '../../pages/BoardEditor/slice/selectors';
import { EditBoardState } from '../../pages/BoardEditor/slice/types';

export const WidgetDataContext = createContext<{
  data: WidgetData | undefined;
}>({
  data: { id: '', columns: [], rows: [] } as WidgetData,
});
export const WidgetDataProvider: FC<{
  boardId: string;
  boardEditing: boolean;
  widgetId: string;
}> = memo(({ boardId, boardEditing, widgetId, children }) => {
  // 浏览模式
  const viewWidgetData = useSelector((state: { board: BoardState }) =>
    selectWidgetDataById(state, widgetId),
  );
  // 编辑模式
  const editWidgetData = useSelector((state: { editBoard: EditBoardState }) =>
    selectEditWidgetData(state, widgetId),
  );
  const widgetData = boardEditing ? editWidgetData : viewWidgetData;
  return (
    <WidgetDataContext.Provider value={{ data: widgetData }}>
      {boardId ? children : null}
    </WidgetDataContext.Provider>
  );
});
