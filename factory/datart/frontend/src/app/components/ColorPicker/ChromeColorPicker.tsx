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

import { Button } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { useState } from 'react';
import { ChromePicker, ColorResult } from 'react-color';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';
import { colorSelectionPropTypes } from './slice/types';

const toChangeValue = (data: ColorResult) => {
  const { r, g, b, a } = data.rgb;
  return `rgba(${r}, ${g}, ${b}, ${a})`;
};

/**
 * 单色选择组件
 * @param onChange
 * @param color
 * @returns 返回一个新的颜色值
 */
function ChromeColorPicker({ color, onChange }: colorSelectionPropTypes) {
  const [selectColor, setSelectColor] = useState<any>(color);
  const t = useI18NPrefix('components.colorPicker');

  return (
    <ChromeColorWrap>
      <ChromePicker
        color={selectColor}
        onChangeComplete={color => {
          let colorRgb = toChangeValue(color);
          setSelectColor(colorRgb);
        }}
      />
      <BtnWrap>
        <Button
          size="middle"
          onClick={() => {
            onChange?.(false);
          }}
        >
          {t('cancel')}
        </Button>
        <Button
          type="primary"
          size="middle"
          onClick={() => {
            onChange?.(selectColor);
          }}
        >
          {t('ok')}
        </Button>
      </BtnWrap>
    </ChromeColorWrap>
  );
}

export default ChromeColorPicker;

const ChromeColorWrap = styled.div`
  .chrome-picker {
    box-shadow: none !important;
  }
`;

const BtnWrap = styled.div`
  margin-top: ${SPACE_TIMES(2.5)};
  text-align: right;

  > button:first-child {
    margin-right: ${SPACE_TIMES(2.5)};
  }
`;
