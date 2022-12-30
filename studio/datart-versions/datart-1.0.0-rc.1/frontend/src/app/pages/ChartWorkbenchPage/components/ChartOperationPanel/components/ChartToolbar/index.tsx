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

import { FC, memo, useContext } from 'react';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_MD } from 'styles/StyleConstants';
import ChartAggregationContext from '../../../../contexts/ChartAggregationContext';
import Aggregation from './Aggregation';

const ChartToolbar: FC<{}> = memo(() => {
  const { aggregation, onChangeAggregation } = useContext(
    ChartAggregationContext,
  );

  return (
    <Toolbar>
      <Aggregation
        defaultValue={aggregation}
        onChangeAggregation={() => {
          onChangeAggregation?.();
        }}
      ></Aggregation>
    </Toolbar>
  );
});

export default ChartToolbar;

const Toolbar = styled.div`
  padding: ${SPACE_MD};
  margin-bottom: ${SPACE_MD};
  background-color: ${p => p.theme.componentBackground};
  border-radius: ${BORDER_RADIUS};
`;
