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

import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { FilterCondition } from 'app/types/ChartConfig';
import { ChartDTO } from 'app/types/ChartDTO';
import DropdownListFilter from './DropdownListFilter';
import MultiDropdownListFilter from './MultiDropdownListFilter';
import RadioGroupFilter from './RadioGroupFilter';
import RangeTimeFilter from './RangeTimeFilter';
import RangeTimePickerFilter from './RangeTimePickerFilter';
import RangValueFilter from './RangValueFilter';
import RecommendTimeFilter from './RecommendTimeFilter';
import SliderFilter from './SliderFilter';
import TextFilter from './TextFilter';
import TimeFilter from './TimeFilter';
import TreeFilter from './TreeFilter';
import ValueFilter from './ValueFilter';

export type PresentControllerFilterProps = {
  viewId?: string;
  view?: ChartDTO['view'];
  condition?: FilterCondition;
  onConditionChange: (condition: FilterCondition) => void;
  options: any;
  executeToken?: Record<string, ExecuteToken>;
};

const Filters = {
  DropdownListFilter,
  MultiDropdownListFilter,
  RadioGroupFilter,
  RangeTimeFilter,
  RangeTimePickerFilter,
  RecommendTimeFilter,
  RangValueFilter,
  SliderFilter,
  TextFilter,
  TimeFilter,
  TreeFilter,
  ValueFilter,
};

export default Filters;
