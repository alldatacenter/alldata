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

import AggregationAction from './AggregationAction';
import AggregationColorizeAction from './AggregationColorizeAction';
import AggregationLimitAction from './AggregationLimitAction';
import AliasAction from './AliasAction';
import ColorizeRangeAction from './ColorizeRangeAction';
import ColorizeSingleAction from './ColorizeSingleAction';
import FilterActions from './FilterAction';
import NumberFormatAction from './NumberFormatAction';
import SizeOptionsAction from './SizeAction';
import SortAction from './SortAction/SortAction';

const { FilterAction } = FilterActions;

const actions = {
  AggregationAction,
  AliasAction,
  NumberFormatAction,
  SortAction,
  AggregationLimitAction,
  FilterAction,
  AggregationColorizeAction,
  SizeOptionsAction,
  ColorizeRangeAction,
  ColorizeSingleAction,
};

export default actions;
