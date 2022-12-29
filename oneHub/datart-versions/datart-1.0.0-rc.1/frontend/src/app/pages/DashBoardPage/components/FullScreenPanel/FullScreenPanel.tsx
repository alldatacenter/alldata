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
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons';
import { Button, Menu } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { WidgetWrapProvider } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetWrapProvider';
import { boardActions } from 'app/pages/DashBoardPage/pages/Board/slice';
import {
  makeSelectBoardFullScreenPanelById,
  selectBoardWidgetMapById,
} from 'app/pages/DashBoardPage/pages/Board/slice/selector';
import { BoardState } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { memo, useCallback, useContext, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { LEVEL_10, LEVEL_100, SPACE_LG, SPACE_SM } from 'styles/StyleConstants';
import { BoardContext } from '../BoardProvider/BoardProvider';
import widgetManager from '../WidgetManager/index';
import { WidgetMapper } from '../WidgetMapper/WidgetMapper';

export const FullScreenPanel: React.FC<{}> = memo(() => {
  const { boardId } = useContext(BoardContext);
  const dispatch = useDispatch();

  const itemId = useSelector((state: { board: BoardState }) =>
    makeSelectBoardFullScreenPanelById()(state, boardId),
  );

  const widgetMap = useSelector((state: { board: BoardState }) =>
    selectBoardWidgetMapById(state, boardId),
  );

  const widgets = useMemo(() => {
    return Object.values(widgetMap).filter(
      item => widgetManager.meta(item.config.originalType).canFullScreen,
    );
  }, [widgetMap]);

  const [collapsed, setCollapsed] = useState(false);

  const toggle = useCallback(() => {
    setCollapsed(c => !c);
  }, []);
  const closeFullScreen = useCallback(() => {
    setCollapsed(c => false);
    dispatch(boardActions.updateFullScreenPanel({ boardId, itemId: '' }));
  }, [boardId, dispatch]);
  const changeItem = useCallback(
    e => {
      dispatch(
        boardActions.updateFullScreenPanel({
          boardId,
          itemId: e.key,
        }),
      );
    },
    [boardId, dispatch],
  );

  const chart = useMemo(() => {
    if (!itemId) return null;
    const widget = widgetMap[itemId];
    if (widget) {
      return (
        <WidgetWrapProvider
          id={widget.id}
          boardEditing={false}
          boardId={boardId}
        >
          <WidgetMapper boardEditing={false} hideTitle={true} />
        </WidgetWrapProvider>
      );
    }
  }, [boardId, itemId, widgetMap]);

  const t = useI18NPrefix(`viz.widget.action`);

  return (
    <>
      {itemId && (
        <FullScreenWrap show={collapsed}>
          <FullHeader>
            <div onClick={toggle}>
              {collapsed ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
              &nbsp;
              <span>{widgetMap[itemId].config.name}</span>
            </div>
            <Button
              size="small"
              className="close-fullscreen"
              onClick={closeFullScreen}
            >
              {t('exitFullScreen')}
            </Button>
          </FullHeader>
          <div className="full-container">
            {chart}
            {itemId && (
              <div className="full-menu">
                <Menu
                  mode="inline"
                  onClick={changeItem}
                  defaultSelectedKeys={[itemId]}
                >
                  {widgets.map(ele => (
                    <Menu.Item key={ele.id}>{ele.config.name}</Menu.Item>
                  ))}
                </Menu>
              </div>
            )}
          </div>
        </FullScreenWrap>
      )}
    </>
  );
});

const FullScreenWrap = styled.div<{ show: boolean }>`
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_100};
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background-color: ${p => p.theme.componentBackground};
  transition: all 3s ease-out;

  .full-container {
    position: relative;
    display: flex;
    flex: 1;
  }

  .full-menu {
    position: absolute;
    left: ${p => (p.show ? '0' : '-300px')};
    z-index: ${LEVEL_10};
    width: 300px;
    height: 100%;
    background-color: ${p => p.theme.bodyBackground};
    transition: all 0.3s;
  }
`;
const FullHeader = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: ${SPACE_SM} ${SPACE_LG};
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};
`;
