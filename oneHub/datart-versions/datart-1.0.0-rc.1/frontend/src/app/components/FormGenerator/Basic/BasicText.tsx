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

import { Input } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateByKey } from 'app/utils/mutation';
import { FC, memo } from 'react';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
const { TextArea } = Input;

const BasicText: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, data: row, onChange }) => {
    const { value } = row;

    const handleTextChange = e => {
      const newRow = updateByKey(row, 'value', e?.target?.value);
      onChange?.(ancestors, newRow);
    };

    return (
      <TextArea
        autoSize={true}
        showCount={true}
        onChange={handleTextChange}
        value={value}
      ></TextArea>
    );
  },
  itemLayoutComparer,
);

export default BasicText;
