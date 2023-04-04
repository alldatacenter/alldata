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

import { FONT_FAMILY } from 'styles/StyleConstants';

export const DATARTSEPERATOR = '@datart@';
export const CHARTCONFIG_FIELD_PLACEHOLDER_UID = '@placeholder@';
export const DATART_TRANSLATE_HOLDER = '@global@';
export const EVENT_ACTION_DELAY_MS = 200;
export const RUNTIME_FILTER_KEY = Symbol('@filters@');
export const BOARD_COPY_CHART_SUFFIX = '_copy';
export const BOARD_SELF_CHART_PREFIX = 'widget_';
export const TABLE_DATA_INDEX = '@datartTableIndex@';
export const DATE_LEVEL_DELIMITER = '@date_level_delimiter@';

export enum StorageKeys {
  AuthorizationToken = 'AUTHORIZATION_TOKEN',
  LoggedInUser = 'LOGGED_IN_USER',
  ShareClientId = 'SHARE_CLIENT_ID',
  AuthRedirectUrl = 'AUTH_REDIRECT_URL',
  Locale = 'LOCALE',
  Theme = 'THEME',
}

export const BASE_API_URL = '/api/v1';
export const BASE_RESOURCE_URL = '/';
// 1 hour
export const DEFAULT_AUTHORIZATION_TOKEN_EXPIRATION = 1000 * 60 * 60;

export enum CommonFormTypes {
  Add = 'add',
  Edit = 'edit',
  SaveAs = 'saveAs',
}

export const TITLE_SUFFIX = ['archived', 'unpublished'];

export const DEFAULT_DEBOUNCE_WAIT = 300;

export const FONT_SIZES = [
  12, 13, 14, 15, 16, 18, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72, 96, 128,
];
export const FONT_LINE_HEIGHT = [
  {
    name: 'viz.palette.style.lineHeight.default',
    value: 1,
  },
  1.5,
  2,
  2.5,
  3,
  3.5,
  4,
  4.5,
  5,
  5.5,
  6,
  6.5,
  7,
  7.5,
  8,
  8.5,
  9,
  9.5,
  10,
];

export const FONT_FAMILIES = [
  { name: 'viz.palette.style.fontFamily.default', value: FONT_FAMILY },
  {
    name: 'viz.palette.style.fontFamily.microsoftYaHei',
    value: 'Microsoft YaHei',
  },
  { name: 'viz.palette.style.fontFamily.simSun', value: 'SimSun' },
  { name: 'viz.palette.style.fontFamily.simHei', value: 'SimHei' },
  {
    name: 'viz.palette.style.fontFamily.helveticaNeue',
    value: 'Helvetica Neue',
  },
  { name: 'viz.palette.style.fontFamily.helvetica', value: 'Helvetica' },
  { name: 'viz.palette.style.fontFamily.arial', value: 'Arial' },
  { name: 'viz.palette.style.fontFamily.sansSerif', value: 'sans-serif' },
];

export const FONT_WEIGHT = [
  { name: 'viz.palette.style.fontWeight.normal', value: 'normal' },
  { name: 'viz.palette.style.fontWeight.bold', value: 'bold' },
  { name: 'viz.palette.style.fontWeight.bolder', value: 'bolder' },
  { name: 'viz.palette.style.fontWeight.lighter', value: 'lighter' },
  { name: 'viz.palette.style.fontWeight.100', value: '100' },
  { name: 'viz.palette.style.fontWeight.200', value: '200' },
  { name: 'viz.palette.style.fontWeight.300', value: '300' },
  { name: 'viz.palette.style.fontWeight.400', value: '400' },
  { name: 'viz.palette.style.fontWeight.500', value: '500' },
  { name: 'viz.palette.style.fontWeight.600', value: '600' },
  { name: 'viz.palette.style.fontWeight.700', value: '700' },
  { name: 'viz.palette.style.fontWeight.800', value: '800' },
  { name: 'viz.palette.style.fontWeight.900', value: '900' },
];

export const FONT_STYLE = [
  { name: 'viz.palette.style.fontStyle.normal', value: 'normal' },
  { name: 'viz.palette.style.fontStyle.italic', value: 'italic' },
  { name: 'viz.palette.style.fontStyle.oblique', value: 'oblique' },
];

export const CHART_LINE_STYLES = [
  { name: 'viz.palette.style.lineStyles.solid', value: 'solid' },
  { name: 'viz.palette.style.lineStyles.dashed', value: 'dashed' },
  { name: 'viz.palette.style.lineStyles.dotted', value: 'dotted' },
];

export const CHART_LINE_WIDTH = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

export const CHART_DRAG_ELEMENT_TYPE = {
  DATA_CONFIG_COLUMN: 'data_config_column',
  DATASET_COLUMN: 'dataset_column',
  DATASET_COLUMN_GROUP: 'dataset_column_group',
};

export const TIME_UNIT_OPTIONS = [
  { name: 'seconds', value: 's' },
  { name: 'minutes', value: 'm' },
  { name: 'hours', value: 'h' },
  { name: 'days', value: 'd' },
  { name: 'weeks', value: 'W' },
  { name: 'months', value: 'M' },
  { name: 'years', value: 'y' },
  { name: 'quarters', value: 'Q' },
];
export const TIME_DIRECTION = [
  { name: 'ago', value: '-' },
  { name: 'current', value: '+0' },
  { name: 'fromNow', value: '+' },
];

export const RECOMMEND_TIME = {
  TODAY: 'today',
  YESTERDAY: 'yesterday',
  THIS_WEEK: 'this_week',
  LAST_7_DAYS: 'last_7_days',
  LAST_30_DAYS: 'last_30_days',
  LAST_90_DAYS: 'last_90_days',
  LAST_1_MONTH: 'last_1_month',
  LAST_1_YEAR: 'last_1_year',
};

export enum FilterSqlOperator {
  Equal = 'EQ',
  NotEqual = 'NE',

  Null = 'IS_NULL',
  NotNull = 'NOT_NULL',

  Contain = 'LIKE',
  NotContain = 'NOT_LIKE',

  PrefixContain = 'PREFIX_LIKE',
  NotPrefixContain = 'PREFIX_NOT_LIKE',

  SuffixContain = 'SUFFIX_LIKE',
  NotSuffixContain = 'SUFFIX_NOT_LIKE',

  Between = 'BETWEEN',
  NotBetween = 'NOT_BETWEEN',
  In = 'IN',
  NotIn = 'NOT_IN',
  LessThan = 'LT',
  GreaterThan = 'GT',
  LessThanOrEqual = 'LTE',
  GreaterThanOrEqual = 'GTE',
}

export const DATE_FORMATTER = 'YYYY-MM-DD';
export const TIME_FORMATTER = 'YYYY-MM-DD HH:mm:ss';

export const CONTROLLER_WIDTH_OPTIONS = [
  { label: 'auto', value: 'auto' },
  { label: '100%', value: '24' },
  { label: '1/2', value: '12' },
  { label: '1/3', value: '8' },
  { label: '1/4', value: '6' },
  { label: '1/6', value: '4' },
  { label: '1/8', value: '3' },
  { label: '1/12', value: '2' },
];

export enum NumberUnitKey {
  None = 'none',
  // English Unit
  Thousand = 'thousand',
  Million = 'million',
  Billion = 'billion',
  // Chinese Unit
  Wan = 'wan',
  Yi = 'yi',
}

export const NumericUnitDescriptions = new Map<NumberUnitKey, [number, string]>(
  [
    [NumberUnitKey.None, [1, '']],
    [NumberUnitKey.Thousand, [10 ** 3, 'K']],
    [NumberUnitKey.Million, [10 ** 6, 'M']],
    [NumberUnitKey.Billion, [10 ** 9, 'B']],
    [NumberUnitKey.Wan, [10 ** 4, '万']],
    [NumberUnitKey.Yi, [10 ** 8, '亿']],
  ],
);

export const KEYBOARD_EVENT_NAME = {
  CTRL: 'Control',
  COMMAND: 'Meta',
};

// .drt = datart template file
// .drr = datart resources file
export enum DatartFileSuffixes {
  Template = '.drt',
  Resource = '.drr',
}

export enum CalculationType {
  ADD = 'add',
  SUBTRACT = 'subtract',
}
