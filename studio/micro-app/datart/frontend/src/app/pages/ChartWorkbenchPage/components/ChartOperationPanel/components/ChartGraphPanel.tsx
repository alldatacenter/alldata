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

import ChartManager from 'app/models/ChartManager';
import ChartI18NContext from 'app/pages/ChartWorkbenchPage/contexts/Chart18NContext';
import { IChart } from 'app/types/Chart';
import { ChartConfig } from 'app/types/ChartConfig';
import { transferChartDataConfig } from 'app/utils/internalChartHelper';
import { FC, memo, useLayoutEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_MD, SPACE_XS } from 'styles/StyleConstants';
import { CloneValueDeep } from 'utils/object';
import ChartGraphIcon from './ChartGraphIcon';

const ChartGraphPanel: FC<{
  chart?: IChart;
  chartConfig?: ChartConfig;
  onChartChange: (chart: IChart) => void;
}> = memo(({ chart, chartConfig, onChartChange }) => {
  const chartManager = ChartManager.instance();
  const [allCharts] = useState<IChart[]>(chartManager.getAllCharts());
  const [requirementsStates, setRequirementStates] = useState<object>({});

  useLayoutEffect(() => {
    if (allCharts) {
      const dict = allCharts?.reduce((acc, cur) => {
        const transferedChartConfig = transferChartDataConfig(
          { datas: CloneValueDeep(cur?.config?.datas || []) },
          { datas: chartConfig?.datas },
        );
        acc[cur.meta.id] = cur?.isMatchRequirement(transferedChartConfig);
        return acc;
      }, {});
      setRequirementStates(dict);
    }
  }, [allCharts, chartConfig]);

  return (
    <StyledChartGraphPanel>
      {allCharts?.map(c => {
        return (
          <ChartI18NContext.Provider
            key={c?.meta?.id}
            value={{ i18NConfigs: c?.config?.i18ns }}
          >
            <ChartGraphIcon
              chart={c}
              isActive={c?.meta?.id === chart?.meta?.id}
              isMatchRequirement={!!requirementsStates?.[c?.meta?.id]}
              onChartChange={onChartChange}
            />
          </ChartI18NContext.Provider>
        );
      })}
    </StyledChartGraphPanel>
  );
});

export default ChartGraphPanel;

const StyledChartGraphPanel = styled.div`
  display: flex;
  flex-flow: row wrap;
  padding: ${SPACE_XS};
  margin-bottom: ${SPACE_MD};
  color: ${p => p.theme.textColorLight};
  background-color: ${p => p.theme.componentBackground};
  border-radius: ${BORDER_RADIUS};
`;
