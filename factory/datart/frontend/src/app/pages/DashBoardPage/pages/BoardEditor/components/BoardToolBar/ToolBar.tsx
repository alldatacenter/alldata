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
import { Divider, Space } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BoardActionContext } from 'app/pages/DashBoardPage/components/ActionProvider/BoardActionProvider';
import useBoardEditorHotkeys from 'app/pages/DashBoardPage/hooks/useBoardEditorHotkeys';
import React, { useContext } from 'react';
import styled from 'styled-components/macro';
import { WidgetActionContext } from '../../../../components/ActionProvider/WidgetActionProvider';
import { AddChart } from './AddChart/AddChart';
import { AddContainer } from './AddContainer/AddContainer';
import { AddController } from './AddControler/AddControler';
import { AddMedia } from './AddMedia/AddMedia';
import { BoardToolRights } from './BoardToolRights';
import { BoardToolBarContext } from './context/BoardToolBarContext';
import { CopyBtn, PasteBtn } from './CopyPaste/CopyPaste';
import { DelWidgetsBtn } from './DelWidgetsBtn';
import { DeviceSwitcher } from './DeviceSwitch/DeviceSwitcher';
import { ToBottomBtn, ToTopBtn } from './ToTopToBottom/ToTopToBottom';
import { RedoBtn, UndoBtn } from './UndoRedo/UndoRedo';

export const ToolBar = () => {
  const ssp = e => {
    e.stopPropagation();
  };
  const { boardType } = useContext(BoardToolBarContext);
  const {
    onEditLayerToTop,
    onEditLayerToBottom,
    onEditCopyWidgets,
    onEditPasteWidgets,
    onEditDeleteActiveWidgets,
  } = useContext(WidgetActionContext);
  const { undo, redo } = useContext(BoardActionContext);
  //
  useBoardEditorHotkeys();

  const t = useI18NPrefix(`viz.board.action`);
  return (
    <Wrapper onClick={ssp}>
      <Space>
        <AddChart />

        <AddController />

        <AddMedia />

        <AddContainer />

        <Divider type="vertical" />

        <UndoBtn fn={undo} title={t('undo')} />
        <RedoBtn fn={redo} title={t('redo')} />

        <Divider type="vertical" />

        <DelWidgetsBtn fn={onEditDeleteActiveWidgets} title={t('delete')} />
        <Divider type="vertical" />

        <ToTopBtn fn={onEditLayerToTop} title={t('toTop')} />
        <ToBottomBtn fn={onEditLayerToBottom} title={t('toBottom')} />

        <CopyBtn fn={onEditCopyWidgets} title={t('copy')} />
        <PasteBtn fn={onEditPasteWidgets} title={t('paste')} />

        {boardType === 'auto' && (
          <>
            <Divider type="vertical" />

            <DeviceSwitcher />
          </>
        )}
      </Space>

      <BoardToolRights />
    </Wrapper>
  );
};
const Wrapper = styled.div`
  z-index: 0;
  display: flex;
  flex: 1;
  justify-content: space-between;
`;
