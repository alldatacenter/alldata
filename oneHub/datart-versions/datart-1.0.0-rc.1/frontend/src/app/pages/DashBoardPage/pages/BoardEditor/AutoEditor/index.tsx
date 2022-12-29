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

import { memo, useContext } from 'react';
import styled from 'styled-components/macro';
import { LEVEL_1 } from 'styles/StyleConstants';
import { WidgetActionContext } from '../../../components/ActionProvider/WidgetActionProvider';
import { BoardToolBar } from '../components/BoardToolBar/BoardToolBar';
import { LayerTreePanel } from '../components/LayerPanel/LayerTreePanel';
import SlideSetting from '../components/SlideSetting/SlideSetting';
import { AutoBoardEditor } from './AutoBoardEditor';
export const AutoEditor: React.FC<{}> = memo(() => {
  const { onEditClearActiveWidgets } = useContext(WidgetActionContext);
  return (
    <Wrapper onClick={onEditClearActiveWidgets}>
      <BoardToolBar />
      <Editor>
        <LayerTreePanel />
        <AutoBoardEditor />
        <SlideSetting />
      </Editor>
    </Wrapper>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
`;

const Editor = styled.div`
  z-index: ${LEVEL_1};
  display: flex;
  flex: 1;
  min-height: 0;
  overflow-x: auto;
`;
