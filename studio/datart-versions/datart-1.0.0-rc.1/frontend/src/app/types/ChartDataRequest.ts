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

import { AggregateFieldActionType, SortActionType } from 'app/constants';
import { ChartDatasetPageInfo } from 'app/types/ChartDataSet';

export type ChartDataRequestFilter = {
  aggOperator?: AggregateFieldActionType | null;
  column: string[];
  sqlOperator: string;
  values?: Array<{
    value: string;
    valueType: string;
  }>;
};

export type PendingChartDataRequestFilter = {
  aggOperator?: AggregateFieldActionType | null;
  column: string;
  sqlOperator: string;
  values?: Array<{
    value: string;
    valueType: string;
  }>;
};

export type ChartDataRequest = {
  viewId: string;
  aggregators: Array<{ column: string[]; sqlOperator: string }>;
  expired?: number;
  filters: ChartDataRequestFilter[];
  flush?: boolean;
  groups?: Array<{ column: string[] }>;
  functionColumns?: Array<{ alias: string; snippet: string }>;
  limit?: any;
  nativeQuery?: boolean;
  orders: Array<{
    column: string[];
    operator: SortActionType;
    aggOperator?: AggregateFieldActionType;
  }>;
  pageInfo?: ChartDatasetPageInfo;
  columns?: Array<{ alias: string; column: string[] }>;
  script?: boolean;
  keywords?: string[];
  cache?: boolean;
  cacheExpires?: number;
  concurrencyControl?: boolean;
  concurrencyControlMode?: string;
  params?: Record<string, string[]>;
  vizId?: string;
  vizName?: string;
  vizType?: string;
  analytics?: Boolean;
};
