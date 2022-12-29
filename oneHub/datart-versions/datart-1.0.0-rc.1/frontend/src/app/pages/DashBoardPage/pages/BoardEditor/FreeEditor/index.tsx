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

import { Layout } from 'antd';
import { memo } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';
import { BoardToolBar } from '../components/BoardToolBar/BoardToolBar';
import { LayerTreePanel } from '../components/LayerPanel/LayerTreePanel';
import SlideSetting from '../components/SlideSetting/SlideSetting';
import { editDashBoardInfoActions, editWidgetInfoActions } from '../slice';
import { FreeBoardEditor } from './FreeBoardEditor';

export const FreeEditor: React.FC = memo(() => {
  const dispatch = useDispatch();
  const clearSelectedWidgets = e => {
    e.stopPropagation();
    dispatch(editWidgetInfoActions.clearSelectedWidgets());
    dispatch(editDashBoardInfoActions.changeShowBlockMask(true));
  };

  return (
    <Layout onClick={clearSelectedWidgets}>
      <Wrapper>
        <BoardToolBar />
        <Editor>
          {/* <LayerList /> */}
          <LayerTreePanel />
          <FreeBoardEditor />
          <SlideSetting />
        </Editor>
      </Wrapper>
    </Layout>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
`;

const Editor = styled.div`
  display: flex;
  flex: 1;
  min-height: 0;
`;
