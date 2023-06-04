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

import { FormInstance } from 'antd';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { FC, memo } from 'react';
import FilterControlPanel from '../FilterControlPanel';

const FilterAction: FC<{
  config: ChartDataSectionField;
  dataset?: ChartDataSetDTO;
  dataView?: ChartDataView;
  dataConfig?: ChartDataConfig;
  aggregation?: boolean;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
  form?: FormInstance;
}> = memo(
  ({
    config,
    dataset,
    dataView,
    dataConfig,
    onConfigChange,
    aggregation,
    form,
  }) => {
    const handleFetchDataFromField = async fieldId => {
      // TODO: to be implement to get fields
      return await Promise.resolve(['a', 'b', 'c'].map(f => `${fieldId}-${f}`));
    };
    return (
      <FilterControlPanel
        aggregation={aggregation}
        config={config}
        dataset={dataset}
        dataConfig={dataConfig}
        dataView={dataView}
        onConfigChange={onConfigChange}
        fetchDataByField={handleFetchDataFromField}
        form={form}
      />
    );
  },
);

export default FilterAction;
