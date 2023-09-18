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
import { Button, Dropdown, Menu } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import styled from 'styled-components/macro';
import { G20 } from 'styles/StyleConstants';

// to see useHotkeys in function useBoardEditorHotkeys
export const Hotkeys = [
  { key: 'shift + click', text: 'multiSelect' },
  { key: 'ctrl + G', text: 'group' },
  { key: 'delete', text: 'delete' },
  { key: 'ctrl + Z', text: 'undo' },
  { key: 'ctrl + shift + Z', text: 'redo' },
  { key: 'ctrl + C', text: 'copy' },
  { key: 'ctrl + V', text: 'paste' },
  { key: 'ctrl + shift + ↑', text: 'toTop' },
  { key: 'ctrl + shift + ↓', text: 'toBottom' },
];
export const ShortcutKeys = () => {
  const t = useI18NPrefix(`viz.board.action`);
  const gt = useI18NPrefix(`global.button`);
  // shortcuts
  const menu = (
    <Menu>
      {Hotkeys.map(item => (
        <Menu.Item key={item.text}>
          <StyledWrap>
            <div>{t(item.text)}</div>
            <div>{item.key}</div>
          </StyledWrap>
        </Menu.Item>
      ))}
    </Menu>
  );

  return (
    <Dropdown overlay={menu} placement="bottomRight" trigger={['click']}>
      <Button type="text">{gt('shortcuts')}</Button>
    </Dropdown>
  );
};
const StyledWrap = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 240px;
  cursor: pointer;
  border-bottom: 1px solid ${G20};
`;
