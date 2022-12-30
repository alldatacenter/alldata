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
import { ChartDataViewFieldCategory } from 'app/constants';
import i18n from 'i18next';

const prefix = 'viz.workbench.dataview.';

export const ChartConfigReducerActionType = {
  INIT: 'init',
  STYLE: 'style',
  DATA: 'data',
  SETTING: 'setting',
  INTERACTION: 'interaction',
  I18N: 'i18n',
};

export const DATE_LEVELS = [
  {
    category: ChartDataViewFieldCategory.DateLevelComputedField,
    expression: 'AGG_DATE_YEAR',
    name: i18n.t(prefix + 'AGG_DATE_YEAR'),
    type: 'DATE',
  },
  {
    category: ChartDataViewFieldCategory.DateLevelComputedField,
    expression: 'AGG_DATE_QUARTER',
    name: i18n.t(prefix + 'AGG_DATE_QUARTER'),
    type: 'DATE',
  },
  {
    category: ChartDataViewFieldCategory.DateLevelComputedField,
    expression: 'AGG_DATE_MONTH',
    name: i18n.t(prefix + 'AGG_DATE_MONTH'),
    type: 'DATE',
  },
  {
    category: ChartDataViewFieldCategory.DateLevelComputedField,
    expression: 'AGG_DATE_WEEK',
    name: i18n.t(prefix + 'AGG_DATE_WEEK'),
    type: 'DATE',
  },
  {
    category: ChartDataViewFieldCategory.DateLevelComputedField,
    expression: 'AGG_DATE_DAY',
    name: i18n.t(prefix + 'AGG_DATE_DAY'),
    type: 'DATE',
  },
];
