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
import { CHART_LINE_STYLES, CHART_LINE_WIDTH } from 'globalConstants';
import { FC, memo } from 'react';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';
import { Group, WithColorPicker } from './components/Group';

const BasicLine: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange }) => {
    const { label, comType, options, ...rest } = data;

    const handlePickerSelect = value => {
      handleSettingChange('color')(value);
    };

    const handleSettingChange = key => value => {
      const newLineStyle = Object.assign({}, data.value, { [key]: value });
      const newData = updateByKey(data, 'value', newLineStyle);
      onChange?.(ancestors, newData);
    };

    return (
      <BW label={!options?.hideLabel ? t(label, true) : ''}>
        <WithColorPicker>
          <Group>
            <Select
              className="datart-ant-select"
              dropdownMatchSelectWidth
              placeholder={t('select')}
              value={data.value?.type}
              onChange={handleSettingChange('type')}
            >
              {CHART_LINE_STYLES.map(o => (
                <Select.Option key={o.value} value={o.value}>
                  {t(o.name, true)}
                </Select.Option>
              ))}
            </Select>
            <Select
              className="datart-ant-select"
              placeholder={t('select')}
              value={data.value?.width}
              onChange={handleSettingChange('width')}
            >
              {CHART_LINE_WIDTH.map(o => (
                <Select.Option key={o} value={o}>
                  {o}
                </Select.Option>
              ))}
            </Select>
          </Group>
          <ColorPickerPopover
            {...rest}
            {...options}
            defaultValue={data.value?.color}
            onSubmit={handlePickerSelect}
          />
        </WithColorPicker>
      </BW>
    );
  },
  itemLayoutComparer,
);

export default BasicLine;
