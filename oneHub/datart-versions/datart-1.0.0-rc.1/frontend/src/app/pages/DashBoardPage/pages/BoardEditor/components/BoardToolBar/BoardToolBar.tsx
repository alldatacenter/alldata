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

import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import { useContext } from 'react';
import styled from 'styled-components/macro';
import { LEVEL_5, SPACE, SPACE_XL } from 'styles/StyleConstants';
import { BoardToolBarContext } from './context/BoardToolBarContext';
import { ToolBar } from './ToolBar';

export const BoardToolBar = () => {
  const { boardId, boardType } = useContext(BoardContext);
  return (
    <Wrapper>
      <BoardToolBarContext.Provider
        value={{ className: '', boardId: boardId, boardType: boardType }}
      >
        <ToolBar />
      </BoardToolBarContext.Provider>
    </Wrapper>
  );
};

const Wrapper = styled.div`
  z-index: ${LEVEL_5};
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: ${SPACE} ${SPACE_XL};
  background-color: ${p => p.theme.componentBackground};
  border-top: 1px solid ${p => p.theme.borderColorSplit};
  box-shadow: ${p => p.theme.shadowSider};
`;
