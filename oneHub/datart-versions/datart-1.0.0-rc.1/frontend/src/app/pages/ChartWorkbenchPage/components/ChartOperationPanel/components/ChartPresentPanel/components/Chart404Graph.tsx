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

import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { IChart } from 'app/types/Chart';
import { ChartConfig } from 'app/types/ChartConfig';
import { reachLowerBoundCount } from 'app/utils/internalChartHelper';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_TIMES } from 'styles/StyleConstants';

const Chart404Graph: FC<{
  chart?: IChart;
  chartConfig?: ChartConfig;
}> = memo(({ chart, chartConfig }) => {
  const t = useI18NPrefix(`viz.palette`);

  const renderChartLimitation = () => {
    const sections = chartConfig?.datas
      ?.filter(s => reachLowerBoundCount(s?.limit, s.rows?.length) > 0)
      .map(s => {
        return (
          <li key={s.key}>
            {t('present.needMore', false, {
              type: t('data.' + s.label),
              num: reachLowerBoundCount(s?.limit, s.rows?.length),
            })}
          </li>
        );
      });
    return sections;
  };

  return (
    <StyledChart404Graph>
      <StyledChartIcon>
        <i className={chart?.meta?.icon} />
      </StyledChartIcon>
      {renderChartLimitation()}
    </StyledChart404Graph>
  );
});

export default Chart404Graph;

const StyledChart404Graph = styled.div`
  display: flex;
  flex-flow: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: ${p => p.theme.normal};
  opacity: 0.3;
`;

const StyledChartIcon = styled.div`
  margin-bottom: ${SPACE_TIMES(10)};
  border-radius: ${BORDER_RADIUS};

  > i {
    font-size: ${SPACE_TIMES(60)};
    line-height: ${SPACE_TIMES(60)};
  }
`;
