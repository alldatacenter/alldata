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

import { Radio } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';

const BasicRadio: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data: row, onChange }) => {
    const { value, comType, options, ...rest } = row;
    const items = options?.items || [];
    const needTranslate = !!options?.translateItemLabel;

    const handleValueChange = e => {
      const newValue = e.target.value;
      onChange?.(ancestors, newValue, options?.needRefresh);
    };

    return (
      <StyledBasicRadio label={t(row.label, true)}>
        <Radio.Group
          name={`${row.label}-${ancestors?.toString()}`}
          {...rest}
          {...options}
          onChange={handleValueChange}
          value={value}
        >
          {items?.map(o => {
            return (
              <Radio key={o.key} value={o.value}>
                {needTranslate ? t(o.label, true) : o?.label}
              </Radio>
            );
          })}
        </Radio.Group>
      </StyledBasicRadio>
    );
  },
  itemLayoutComparer,
);

export default BasicRadio;

const StyledBasicRadio = styled(BW)``;
