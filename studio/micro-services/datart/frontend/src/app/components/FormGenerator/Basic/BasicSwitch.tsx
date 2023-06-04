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

import { Switch } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateByKey } from 'app/utils/mutation';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';

const BasicSwitch: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data: row, onChange }) => {
    const { comType, options, ...rest } = row;

    const handleSwitchChange = value => {
      const newRow = updateByKey(row, 'value', value);
      onChange?.(ancestors, newRow);
    };

    return (
      <StyledBasicSwitch
        label={!options?.hideLabel ? t(row.label, true) : ''}
        labelCol={{ span: 20 }}
        wrapperCol={{ span: 4 }}
      >
        <Switch
          size="small"
          {...rest}
          {...options}
          checked={row.value}
          onChange={handleSwitchChange}
        />
      </StyledBasicSwitch>
    );
  },
  itemLayoutComparer,
);

export default BasicSwitch;

const StyledBasicSwitch = styled(BW)`
  flex-direction: row;
`;
