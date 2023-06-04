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

import { Collapse } from 'antd';
import { CollapseHeader, ItemLayout } from 'app/components/FormGenerator';
import { FormGroupLayoutMode } from 'app/components/FormGenerator/constants';
import GroupLayout from 'app/components/FormGenerator/Layout/GroupLayout';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartI18NContext from 'app/pages/ChartWorkbenchPage/contexts/Chart18NContext';
import { WidgetContext } from 'app/pages/DashBoardPage/components/WidgetProvider/WidgetProvider';
import { ChartDataConfig, ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useContext } from 'react';
import styled from 'styled-components/macro';
import widgetManagerInstance from '../../../../components/WidgetManager';

const StyledWrapper = styled.div`
  width: 100%;
  min-height: 0;
  /* overflow-y: auto; */
`;

export const WidgetConfigPanel: FC<{
  configs: ChartStyleConfig[];
  dataConfigs?: ChartDataConfig[];
  context?: any;
  onChange: (
    ancestors: number[],
    config: ChartStyleConfig,
    needRefresh?: boolean,
  ) => void;
}> = memo(({ configs, dataConfigs, context, onChange }) => {
  const widget = useContext(WidgetContext);

  const widgetTypeId = widget.config.originalType;
  const i18ns = widgetManagerInstance.meta(widgetTypeId).i18ns;

  return (
    <ChartI18NContext.Provider value={{ i18NConfigs: i18ns }}>
      <StyledWrapper onClick={e => e.stopPropagation()}>
        <BoardConfigCollapse
          dataConfigs={dataConfigs}
          configs={configs || []}
          context={context}
          onChange={onChange}
        />
      </StyledWrapper>
    </ChartI18NContext.Provider>
  );
});

// TODO: merge and extract logic with ChartStyleConfigPanel.tsx component
export const BoardConfigCollapse: FC<{
  configs: ChartStyleConfig[];
  dataConfigs?: ChartDataConfig[];
  context?: any;
  onChange: (
    ancestors: number[],
    config: ChartStyleConfig,
    needRefresh?: boolean,
  ) => void;
}> = memo(({ configs, dataConfigs, context, onChange }) => {
  const t = useI18NPrefix();
  return (
    <Collapse className="datart-config-panel" ghost>
      {configs
        ?.filter(c => !Boolean(c.hidden))
        .map((c, index) => {
          if (c.comType === 'group') {
            return (
              <Collapse.Panel
                header={<CollapseHeader title={t(c.label, true)} />}
                key={c.key}
              >
                <GroupLayout
                  ancestors={[index]}
                  mode={
                    c.comType === 'group'
                      ? FormGroupLayoutMode.INNER
                      : FormGroupLayoutMode.OUTER
                  }
                  data={c}
                  translate={t}
                  dataConfigs={[]}
                  context={context}
                  onChange={onChange}
                />
              </Collapse.Panel>
            );
          } else {
            return (
              <ItemLayout
                ancestors={[index]}
                data={c}
                translate={t}
                dataConfigs={dataConfigs}
                context={context}
                onChange={onChange}
              />
            );
          }
        })}
    </Collapse>
  );
});
