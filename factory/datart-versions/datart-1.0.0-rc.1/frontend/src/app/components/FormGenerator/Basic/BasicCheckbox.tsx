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

import { Checkbox } from 'antd';
import { CheckboxChangeEvent } from 'antd/lib/checkbox';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';

const BasicCheckbox: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data: row, onChange }) => {
    const { comType, options, ...rest } = row;

    const handleCheckedChange = (e: CheckboxChangeEvent) => {
      onChange?.(ancestors, e.target?.checked, options?.needRefresh);
    };

    return (
      <StyledVizBasicCheckbox
        label={!options?.hideLabel ? t(row.label, true) : ''}
        labelCol={{ span: 20 }}
        wrapperCol={{ span: 4 }}
      >
        <Checkbox
          {...rest}
          {...options}
          checked={row.value}
          onChange={handleCheckedChange}
        />
      </StyledVizBasicCheckbox>
    );
  },
  itemLayoutComparer,
);

export default BasicCheckbox;

const StyledVizBasicCheckbox = styled(BW)`
  flex-direction: row;
`;
