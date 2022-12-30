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

import { DataViewFieldType } from 'app/constants';

export interface ConditionalStyleFormValues {
  uid: string;
  target: { name: string; type: any };
  range: ConditionalStyleRange;
  operator: OperatorTypes;
  value: string;
  color: { background: string; textColor: string };
}

export enum ConditionalStyleRange {
  Cell = 'cell',
  Row = 'row',
}

export enum OperatorTypes {
  Equal = '=',
  NotEqual = '!=',
  Contain = 'like',
  NotContain = 'not like',
  Between = 'between',
  In = 'in',
  NotIn = 'not in',
  LessThan = '<',
  GreaterThan = '>',
  LessThanOrEqual = '<=',
  GreaterThanOrEqual = '>=',
  IsNull = 'is null',
}

export const OperatorTypesLocale = {
  [OperatorTypes.Equal]: '等于',
  [OperatorTypes.NotEqual]: '不等于',
  [OperatorTypes.Contain]: '包含',
  [OperatorTypes.NotContain]: '不包含',
  [OperatorTypes.In]: '在……范围内',
  [OperatorTypes.NotIn]: '不在……范围内',
  [OperatorTypes.Between]: '在……之间',
  [OperatorTypes.LessThan]: '小于',
  [OperatorTypes.GreaterThan]: '大于',
  [OperatorTypes.LessThanOrEqual]: '小于等于',
  [OperatorTypes.GreaterThanOrEqual]: '大于等于',
  [OperatorTypes.IsNull]: '空值',
};

export const ConditionalOperatorTypes = {
  [DataViewFieldType.STRING]: [
    OperatorTypes.Equal,
    OperatorTypes.NotEqual,
    OperatorTypes.Contain,
    OperatorTypes.NotContain,
    OperatorTypes.In,
    OperatorTypes.NotIn,
    OperatorTypes.IsNull,
  ],
  [DataViewFieldType.NUMERIC]: [
    OperatorTypes.Equal,
    OperatorTypes.NotEqual,
    OperatorTypes.Between,
    OperatorTypes.LessThan,
    OperatorTypes.GreaterThan,
    OperatorTypes.LessThanOrEqual,
    OperatorTypes.GreaterThanOrEqual,
    OperatorTypes.IsNull,
  ],
  [DataViewFieldType.DATE]: [
    OperatorTypes.Equal,
    OperatorTypes.NotEqual,
    OperatorTypes.In,
    OperatorTypes.NotIn,
    OperatorTypes.IsNull,
  ],
};
