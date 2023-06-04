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

import FieldActions from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction';
import { ChartDataSectionField, ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';

const YAxisNumberFormatPanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, data, onChange }) => {
    const onConfigChange = (config: ChartDataSectionField) => {
      onChange?.(ancestors, config.format);
    };

    const props = {
      config: {
        colName: '',
        type: 'NUMERIC',
        category: 'field',
        format: data.value,
      } as ChartDataSectionField,
      onConfigChange,
    };

    return <FieldActions.NumberFormatAction {...props} />;
  },
  itemLayoutComparer,
);

export default YAxisNumberFormatPanel;
