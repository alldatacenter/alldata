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

import { Select } from 'antd';
import { ColorPickerPopover } from 'app/components/ColorPicker';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateByKey } from 'app/utils/mutation';
import {
  FONT_FAMILIES,
  FONT_LINE_HEIGHT,
  FONT_SIZES,
  FONT_STYLE,
  FONT_WEIGHT,
} from 'globalConstants';
import isUndefined from 'lodash/isUndefined';
import { FC, memo } from 'react';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';
import { Group, WithColorPicker } from './components/Group';

const BasicFont: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange }) => {
    const { comType, options, ...rest } = data;

    const handlePickerSelect = value => {
      handleSettingChange('color')(value);
    };

    const handleSettingChange = key => value => {
      const newFont = Object.assign({}, data.value, { [key]: value });
      const newData = updateByKey(data, 'value', newFont);
      onChange?.(ancestors, newData);
    };

    return (
      <BW label={!options?.hideLabel ? t(data.label, true) : ''}>
        <Group>
          <Select
            className="datart-ant-select"
            placeholder={t('select')}
            value={data.value?.fontFamily}
            dropdownMatchSelectWidth={false}
            onChange={handleSettingChange('fontFamily')}
          >
            {(options?.fontFamilies || FONT_FAMILIES).map(o => (
              <Select.Option
                key={typeof o === 'string' ? o : o.value}
                value={typeof o === 'string' ? o : o.value}
              >
                {typeof o === 'string' ? o : t(o.name, true)}
              </Select.Option>
            ))}
          </Select>
          <Select
            className="datart-ant-select"
            placeholder={t('select')}
            value={data.value?.fontWeight}
            onChange={handleSettingChange('fontWeight')}
          >
            {FONT_WEIGHT.map(o => (
              <Select.Option key={o.value} value={o.value}>
                {t(o.name, true)}
              </Select.Option>
            ))}
          </Select>
        </Group>
        <WithColorPicker>
          <Group>
            {(isUndefined(options?.showFontSize) || options?.showFontSize) && (
              <Select
                className="datart-ant-select"
                placeholder={t('select')}
                value={data.value?.fontSize}
                onChange={handleSettingChange('fontSize')}
              >
                {FONT_SIZES.map(o => (
                  <Select.Option key={o} value={o}>
                    {o}
                  </Select.Option>
                ))}
              </Select>
            )}
            {options?.showLineHeight && (
              <Select
                className="datart-ant-select"
                placeholder={t('select')}
                value={data.value?.lineHeight}
                onChange={handleSettingChange('lineHeight')}
              >
                {FONT_LINE_HEIGHT.map(o => (
                  <Select.Option
                    key={typeof o === 'number' ? o : o.value}
                    value={typeof o === 'number' ? o : o.value}
                  >
                    {typeof o === 'number' ? o : t(o.name, true)}
                  </Select.Option>
                ))}
              </Select>
            )}
            {(isUndefined(options?.showFontStyle) ||
              options?.showFontStyle) && (
              <Select
                className="datart-ant-select"
                placeholder={t('select')}
                value={data.value?.fontStyle}
                onChange={handleSettingChange('fontStyle')}
              >
                {FONT_STYLE.map(o => (
                  <Select.Option key={o.value} value={o.value}>
                    {t(o.name, true)}
                  </Select.Option>
                ))}
              </Select>
            )}
          </Group>
          {(isUndefined(options?.showFontColor) || options?.showFontColor) && (
            <ColorPickerPopover
              {...rest}
              {...options}
              defaultValue={data.value?.color}
              onSubmit={handlePickerSelect}
            />
          )}
        </WithColorPicker>
      </BW>
    );
  },
  itemLayoutComparer,
);

export default BasicFont;
