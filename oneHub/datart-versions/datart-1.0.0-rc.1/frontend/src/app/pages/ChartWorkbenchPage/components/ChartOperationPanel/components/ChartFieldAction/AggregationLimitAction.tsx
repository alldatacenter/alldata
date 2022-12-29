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

import { Menu, Radio, Space } from 'antd';
import {
  AggregateFieldSubAggregateType,
  ChartDataSectionFieldActionType,
} from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, useState } from 'react';

const AggregationLimitAction: FC<{
  config: ChartDataSectionField;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
  mode?: 'menu';
}> = ({ config, onConfigChange, mode }) => {
  const actionNeedNewRequest = true;
  const t = useI18NPrefix(`viz.common.enum.aggregateTypes`);
  const [aggregate, setAggregate] = useState(config?.aggregate);

  const onChange = selectedValue => {
    const newConfig = updateBy(config, draft => {
      draft.aggregate = selectedValue;
    });
    setAggregate(selectedValue);
    onConfigChange?.(newConfig, actionNeedNewRequest);
  };
  const renderOptions = mode => {
    if (mode === 'menu') {
      return (
        <>
          {AggregateFieldSubAggregateType[
            ChartDataSectionFieldActionType.AggregateLimit
          ]?.map(agg => {
            return (
              <Menu.Item key={agg} onClick={() => onChange(agg)}>
                {t(agg)}
              </Menu.Item>
            );
          })}
        </>
      );
    }

    return (
      <Radio.Group onChange={e => onChange(e.target?.value)} value={aggregate}>
        <Space direction="vertical">
          {AggregateFieldSubAggregateType[
            ChartDataSectionFieldActionType.AggregateLimit
          ]?.map(agg => {
            return (
              <Radio key={agg} value={agg}>
                {t(agg)}
              </Radio>
            );
          })}
        </Space>
      </Radio.Group>
    );
  };

  return renderOptions(mode);
};

export default AggregationLimitAction;
