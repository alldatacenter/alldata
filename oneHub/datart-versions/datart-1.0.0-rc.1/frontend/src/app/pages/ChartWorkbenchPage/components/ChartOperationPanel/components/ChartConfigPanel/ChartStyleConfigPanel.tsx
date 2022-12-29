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
import { ItemLayout } from 'app/components/FormGenerator';
import { FormGroupLayoutMode } from 'app/components/FormGenerator/constants';
import GroupLayout from 'app/components/FormGenerator/Layout/GroupLayout';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataConfig, ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';

const ChartStyleConfigPanel: FC<{
  configs?: ChartStyleConfig[];
  dataConfigs?: ChartDataConfig[];
  i18nPrefix: string;
  context?: any;
  onChange: (
    ancestors: number[],
    config: ChartStyleConfig,
    needRefresh?: boolean,
  ) => void;
}> = memo(
  ({ configs, dataConfigs, i18nPrefix, context, onChange }) => {
    const t = useI18NPrefix(i18nPrefix);

    return (
      <Collapse className="datart-config-panel" ghost>
        {configs
          ?.filter(c => !Boolean(c.hidden))
          ?.map((c, index) => {
            if (c.comType === 'group') {
              return (
                <Collapse.Panel header={t(c.label, true)} key={c.key}>
                  <GroupLayout
                    ancestors={[index]}
                    mode={
                      c.comType === 'group'
                        ? FormGroupLayoutMode.INNER
                        : FormGroupLayoutMode.OUTER
                    }
                    data={c}
                    translate={t}
                    dataConfigs={dataConfigs}
                    onChange={onChange}
                    context={context}
                    flatten
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
                  onChange={onChange}
                  context={context}
                />
              );
            }
          })}
      </Collapse>
    );
  },
  (prev, next) =>
    prev.configs === next.configs && prev.dataConfigs === next.dataConfigs,
);

export default ChartStyleConfigPanel;
