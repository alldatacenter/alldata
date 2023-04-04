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
import WidgetOfFree from 'app/pages/DashBoardPage/pages/Board/FreeDashboard/WidgetOfFree';
import WidgetOfFreeEdit from 'app/pages/DashBoardPage/pages/BoardEditor/FreeEditor/WidgetOfFreeEdit';
import { memo, useContext } from 'react';
import styled from 'styled-components/macro';
import { BoardContext } from '../../BoardProvider/BoardProvider';
import { WidgetWrapProvider } from '../../WidgetProvider/WidgetWrapProvider';

export const GroupWidgetCore1: React.FC<{ widgetIds: string[] }> = memo(
  ({ widgetIds }) => {
    const { editing, boardId } = useContext(BoardContext);
    return (
      <>
        {widgetIds.map(wid => {
          return (
            <Wrapper key={wid}>
              <WidgetWrapProvider
                id={wid}
                boardEditing={editing}
                boardId={boardId}
              >
                {editing ? <WidgetOfFreeEdit /> : <WidgetOfFree />}
              </WidgetWrapProvider>
            </Wrapper>
          );
        })}
      </>
    );
  },
);

const Wrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
`;
