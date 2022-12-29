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

import { ColorPickerPopover } from 'app/components/ColorPicker';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';

const COLORS = [
  '#B80000',
  '#DB3E00',
  '#FCCB00',
  '#008B02',
  '#006B76',
  '#1273DE',
  '#004DCF',
  '#5300EB',
  '#EB9694',
  '#FAD0C3',
  '#FEF3BD',
  '#C1E1C5',
  '#BEDADC',
  '#C4DEF6',
  '#BED3F3',
  '#D4C4FB',
  'transparent',
];
const BasicColorSelector: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data: row, onChange }) => {
    const { comType, options, ...rest } = row;

    const handlePickerSelect = value => {
      onChange?.(ancestors, value);
    };

    const getColor = () => {
      return row.value || row.default || 'whitesmoke';
    };

    return (
      <StyledVizBasicColorSelector
        label={!options?.hideLabel ? t(row.label, true) : ''}
        labelCol={{ span: 20 }}
        wrapperCol={{ span: 4 }}
      >
        <ColorPickerPopover
          {...rest}
          {...options}
          colors={COLORS}
          defaultValue={getColor()}
          onSubmit={handlePickerSelect}
        ></ColorPickerPopover>
      </StyledVizBasicColorSelector>
    );
  },
  itemLayoutComparer,
);

export default BasicColorSelector;

const StyledVizBasicColorSelector = styled(BW)`
  flex-direction: row;
`;
