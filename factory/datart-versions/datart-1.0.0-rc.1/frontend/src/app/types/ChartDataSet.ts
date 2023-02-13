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

import { PageInfo } from '../pages/MainPage/pages/ViewPage/slice/types';
import { ChartDataConfig, ChartDataSectionField } from './ChartConfig';

export interface IChartDataSetRow<T> extends Array<T> {
  getCell(field: ChartDataSectionField): T;

  getMultiCell(...fields: ChartDataSectionField[]): string;

  getCellByKey(key: string): T;

  getFieldKey(field: ChartDataSectionField): string;

  getFieldIndex(field: ChartDataSectionField): number;

  convertToObject(): object;

  convertToCaseSensitiveObject(): object;
}

export interface IChartDataSet<T> extends Array<IChartDataSetRow<T>> {
  getFieldKey(field: ChartDataSectionField): string;

  getOriginFieldInfo(key: string): ChartDataSectionField;

  getFieldOriginKey(field: ChartDataSectionField): string;

  getFieldIndex(field: ChartDataSectionField): number;

  sortBy(dataConfigs: ChartDataConfig[]): void;

  groupBy(field: ChartDataSectionField): {
    [groupKey in string]: IChartDataSetRow<T>[];
  };
}

export type ChartDataSetDTO = {
  id?: string;
  name?: string;
  columns?: ChartDatasetMeta[];
  rows?: string[][];
  pageInfo?: ChartDatasetPageInfo;
  script?: string;
};

export type ChartDatasetPageInfo = Partial<PageInfo>;

export type ChartDatasetMeta = {
  name?: string;
  type?: string;
  primaryKey?: boolean;
};

export default ChartDataSetDTO;
