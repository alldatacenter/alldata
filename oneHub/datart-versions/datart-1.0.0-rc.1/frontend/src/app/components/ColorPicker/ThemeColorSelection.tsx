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

import { List, Popover } from 'antd';
import { colorThemes } from 'app/assets/theme/colorsConfig';
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled from 'styled-components/macro';
import { FONT_SIZE_BODY, SPACE_TIMES } from 'styles/StyleConstants';
import { themeColorPropTypes } from './slice/types';

/**
 * @param callbackFn 回调函数返回一个颜色数组
 * @param children 点击弹出按钮的文字 支持文字和html类型
 */
function ThemeColorSelection({ children, callbackFn }: themeColorPropTypes) {
  const [switchStatus, setSwitchStatus] = useState(false);
  const [colors] = useState(colorThemes);
  const { i18n } = useTranslation();

  return (
    <Popover
      destroyTooltipOnHide
      onVisibleChange={setSwitchStatus}
      visible={switchStatus}
      trigger="click"
      placement="bottomRight"
      content={
        <ColorWrapAlert>
          <List
            itemLayout="horizontal"
            dataSource={colors}
            renderItem={item => (
              <List.Item
                onClick={() => {
                  callbackFn(item.colors);
                  setSwitchStatus(false);
                }}
              >
                <ColorTitle>{item[i18n.language].title}</ColorTitle>
                <ColorBlockWrap>
                  {item.colors.map((v, i) => {
                    return <ColorBlock color={v} key={i}></ColorBlock>;
                  })}
                </ColorBlockWrap>
              </List.Item>
            )}
          />
        </ColorWrapAlert>
      }
    >
      <ChooseTheme
        onClick={() => {
          setSwitchStatus(!switchStatus);
        }}
      >
        <ChooseThemeSpan>{children}</ChooseThemeSpan>
      </ChooseTheme>
    </Popover>
  );
}

export default ThemeColorSelection;

const ChooseTheme = styled.div`
  display: inline-block;
  width: 100%;
  margin-bottom: ${SPACE_TIMES(1)};
  text-align: right;
`;
const ChooseThemeSpan = styled.div`
  display: inline-block;
  width: max-content;
  font-size: ${FONT_SIZE_BODY};
  cursor: pointer;
  &:hover {
    color: ${p => p.theme.primary};
  }
`;
const ColorWrapAlert = styled.div`
  width: 350px;
  max-height: 300px;
  overflow-y: auto;
  .ant-list-item {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding: ${SPACE_TIMES(2.5)};
    cursor: pointer;
    &:hover {
      background-color: ${p => p.theme.bodyBackground};
    }
  }
`;
const ColorTitle = styled.span``;
const ColorBlockWrap = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-top: ${SPACE_TIMES(1)};
`;
const ColorBlock = styled.span<{ color: string }>`
  display: inline-block;
  min-width: ${SPACE_TIMES(6)};
  min-height: ${SPACE_TIMES(6)};
  background-color: ${p => p.color};
`;
