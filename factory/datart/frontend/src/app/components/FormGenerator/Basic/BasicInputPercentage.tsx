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

import { InputNumber } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';

const BasicInputPercentage: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data: row, onChange }) => {
    const { comType, options, ...rest } = row;

    // Note: upgrade to antd v4.17.x with input number `addonAfter`
    return (
      <BW label={t(row.label, true)}>
        <InputNumber
          className="datart-ant-input-number"
          {...rest}
          {...options}
          step={0.1}
          onChange={value => onChange?.(ancestors, value)}
          defaultValue={rest?.default}
        />
      </BW>
    );
  },
  itemLayoutComparer,
);

export default BasicInputPercentage;
