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

import { ChartConfig, SelectedItem } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { ChartDTO } from 'app/types/ChartDTO';

export type ChartConfigPayloadType = {
  init?: ChartConfig;
  ancestors?: number[];
  value?: any;
  needRefresh?: boolean;
};

export type WorkbenchState = {
  lang: string;
  dateFormat: string;
  dataviews?: ChartDataView[];
  currentDataView?: ChartDataView;
  dataset?: ChartDataSetDTO;
  chartConfig?: ChartConfig;
  shadowChartConfig?: ChartConfig;
  backendChart?: ChartDTO;
  backendChartId?: string;
  aggregation?: boolean;
  datasetLoading: boolean;
  chartEditorDownloadPolling: boolean;
  availableSourceFunctions?: string[];
  selectedItems: SelectedItem[];
};

export interface renderMataProps extends Omit<ChartDataViewMeta, 'children'> {
  dateLevelFields?: Array<dateLevelFieldsProps>;
  children?: Array<renderMataProps>;
  selectedItems?: Array<renderMataProps>;
  displayName?: string;
}

export interface dateLevelFieldsProps {
  name: string;
  category: string;
  expression: string;
  field: string;
  type: string;
  displayName: string;
}
