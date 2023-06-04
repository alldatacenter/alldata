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

import {
  AggregateFieldActionType,
  ChartDataSectionFieldActionType,
  ChartDataSectionType,
  ChartDataViewFieldCategory,
  ChartStyleSectionComponentType,
  ControllerFacadeTypes,
  ControllerVisibilityTypes,
  DataViewFieldType,
  DateFormat,
  FieldFormatType,
  FilterConditionType,
  FilterRelationType,
  SortActionType,
} from 'app/constants';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import {
  FilterSqlOperator,
  NumberUnitKey,
  RECOMMEND_TIME,
  RUNTIME_FILTER_KEY,
} from 'globalConstants';
import { ValueOf } from 'types';

export type FilterFieldAction = {
  condition?: FilterCondition;
  visibility?: FilterVisibility;
  facade?: FilterFacade;
  width?: string;
};

export type FilterVisibility =
  | Lowercase<ControllerVisibilityTypes>
  | {
      visibility: ControllerVisibilityTypes.Condition;
      fieldUid: string;
      relation: string;
      value: string;
    };

export type FilterFacade =
  | Uncapitalize<keyof typeof ControllerFacadeTypes>
  | {
      facade: Uncapitalize<keyof typeof ControllerFacadeTypes>;
      [key: string]: string | number;
    };

export type FilterCondition = {
  name: string;
  type: FilterConditionType;
  value?:
    | Lowercase<keyof typeof FilterRelationType>
    | string
    | number
    | [number, number]
    | string[]
    | Array<RelationFilterValue>
    | TimeFilterConditionValue;
  visualType: string;
  operator?:
    | string
    | Lowercase<keyof typeof FilterRelationType>
    | Uncapitalize<keyof typeof FilterSqlOperator>;
  children?: FilterCondition[];
};

export type TimeFilterConditionValue =
  | string
  | string[]
  | Lowercase<keyof typeof RECOMMEND_TIME>
  | Array<{
      unit;
      amount;
      direction?: string;
    }>;

export type RelationFilterValue = {
  key: string;
  label: string;
  index?: number;
  childIndex?: number;
  isSelected?: boolean;
  children?: RelationFilterValue[];
  selectable?: boolean;
};

export type AggregateLimit = Pick<typeof AggregateFieldActionType, 'Count'>;

export type ChartDataSectionField = {
  uid?: string;
  colName: string;
  desc?: string;
  type: DataViewFieldType;
  category: Uncapitalize<keyof typeof ChartDataViewFieldCategory>;
  expression?: string;
  field?: string;
  sort?: SortFieldAction;
  alias?: AliasFieldAction;
  format?: FormatFieldAction;
  aggregate?: AggregateFieldActionType;
  filter?: FilterFieldAction;
  color?: ColorFieldAction;
  size?: number;
  path?: string[];
  dateFormat?: DateFormat;
};

export type SortFieldAction = {
  type: SortActionType;
  value?: any;
};

export type ColorFieldAction = {
  start?: string;
  end?: string;
  colors?: Array<{ key: string; value: string }>;
};

export type FormatFieldAction = {
  type: FieldFormatType;
  [FieldFormatType.Numeric]?: {
    decimalPlaces: number;
    unitKey?: NumberUnitKey;
    useThousandSeparator?: boolean;
    prefix?: string;
    suffix?: string;
  };
  [FieldFormatType.Currency]?: {
    decimalPlaces: number;
    unitKey?: NumberUnitKey;
    useThousandSeparator?: boolean;
    currency?: string;
  };
  [FieldFormatType.Percentage]?: {
    decimalPlaces: number;
  };
  [FieldFormatType.Scientific]?: {
    decimalPlaces: number;
  };
  [FieldFormatType.Date]?: {
    format: string;
  };
  [FieldFormatType.Custom]?: {
    format: string;
  };
};

export type AliasFieldAction = {
  name?: string;
  desc?: string;
};

export type ChartConfigBase = {
  label?: string;
  key: string;
};

export type ChartDataConfig = ChartConfigBase & {
  type?: Lowercase<keyof typeof ChartDataSectionType>;
  allowSameField?: boolean;
  required?: boolean;
  rows?: ChartDataSectionField[];
  actions?: Array<ValueOf<typeof ChartDataSectionFieldActionType>> | object;
  limit?: null | number | string | number[] | string[];
  disableAggregate?: boolean;
  disableAggregateComputedField?: boolean;
  drillable?: boolean;
  drillContextMenuVisible?: boolean;
  options?: {
    [key in ValueOf<typeof ChartDataSectionFieldActionType>]: {
      backendSort?: boolean;
    };
  };
  replacedConfig?: ChartDataSectionField;
  // NOTE: keep field's filter relation for filter arrangement feature
  fieldRelation?: FilterCondition;
  // Runtime filters
  [RUNTIME_FILTER_KEY]?: PendingChartDataRequestFilter[];
};

export type ChartStyleSectionTemplate = {
  template?: ChartStyleSectionRow;
};

export type ChartStyleConfig = ChartConfigBase &
  ChartStyleSectionGroup &
  ChartStyleSectionTemplate;

export type ChartStyleSectionGroup = ChartStyleSectionRow & {
  rows?: ChartStyleSectionGroup[];
};

export type ChartStyleSectionRow = {
  label: string;
  key: string;
  default?: any;
  value?: any;
  disabled?: boolean;
  hide?: boolean;
  options?: ChartStyleSectionRowOption;
  watcher?: ChartStyleSectionRowWatcher;
  template?: ChartStyleSectionRow;
  comType: ValueOf<typeof ChartStyleSectionComponentType>;
  hidden?: boolean;
};

export type ChartStyleSectionRowOption = {
  min?: number | string;
  max?: number | string;
  step?: number | string;
  dots?: boolean;
  type?: string;
  editable?: boolean;
  modalSize?: string | number;
  expand?: boolean;
  items?: Array<ChartStyleSelectorItem> | string[] | number[];
  hideLabel?: boolean;
  style?: React.CSSProperties;
  getItems?: (cols) => Array<ChartStyleSelectorItem>;
  needRefresh?: boolean;
  fontFamilies?: string[];
  showFontSize?: boolean;
  showLineHeight?: boolean;
  showFontStyle?: boolean;
  showFontColor?: boolean;

  /**
   * Support Components: @see BasicRadio, @see BasicSelector and etc
   * Default is false for now, will be change in future version
   */
  translateItemLabel?: boolean;

  /**
   * Only GroupLayout is used
   * */
  flatten?: boolean;
  title?: string;

  /**
   * Other Free Property
   */
  [key: string]: any;
};

export type ChartStyleSelectorItem = {
  key?: any;
  label: any;
  value: any;
};

export type ChartStyleSectionRowWatcher = {
  deps: string[];
  action: (props) => Partial<ChartStyleSectionRow>;
};

export type ChartI18NSectionConfig = {
  lang: string;
  translation: object;
};

export type ChartConfig = {
  datas?: ChartDataConfig[];
  styles?: ChartStyleConfig[];
  settings?: ChartStyleConfig[];
  interactions?: ChartStyleConfig[];
  i18ns?: ChartI18NSectionConfig[];
};

export interface LineStyle {
  color: string;
  type: string;
  width?: number;
}

export interface AxisLineStyle {
  show: boolean;
  lineStyle?: LineStyle;
}

export interface FontStyle {
  color?: string;
  fontFamily?: string;
  fontSize?: number | string;
  fontStyle?: string;
  fontWeight?: string;
  lineHeight?: number;
}

export type AxisLabel = {
  hideOverlap?: boolean;
  interval?: string | null;
  overflow?: string | null;
  rotate?: number | null;
  show: boolean;
  width?: string | number;
  formatter?: string;
  font?: FontStyle;
} & FontStyle;

export type EmphasisStyle = {
  label?: {
    show?: boolean;
  } & FontStyle;
};

export type LabelStyle = {
  label?: {
    position?: string;
    show: boolean;
    font?: FontStyle;
    formatter?: string | ((params) => string);
  } & FontStyle;
  labelLayout?: { hideOverlap: boolean };
  emphasis?: EmphasisStyle;
};

export interface LegendStyle {
  bottom?: number;
  right?: number;
  top?: number;
  left?: number;
  width?: number;
  height?: number | null;
  type: string;
  orient: string;
  show: boolean;
  textStyle: FontStyle;
  selected?: {
    [x: string]: boolean;
  };
  data?: string[];
  itemStyle?: {
    [x: string]: any;
  };
  lineStyle?: {
    [x: string]: any;
  };
}

export type MarkDataConfig = {
  yAxis?: number;
  xAxis?: number;
  name: string;
  lineStyle?: LineStyle;
  itemStyle?: {
    opacity: number;
    color: string;
  } & BorderStyle;
} & LabelStyle;

export interface MarkLine {
  data: MarkDataConfig[];
}

export interface MarkArea {
  data: MarkDataConfig[][];
}

export interface XAxisColumns {
  type: string;
  tooltip: { show: boolean };
  data: string[];
}

export interface SeriesStyle {
  smooth?: boolean;
  step?: boolean;
  symbol?: string;
  stack?: boolean;
  connectNulls?: boolean;
}

export interface YAxis {
  type: string;
  name: string | null;
  nameLocation: string;
  nameGap: string;
  nameRotate: string;
  inverse: boolean;
  min: number;
  max: number;
  axisLabel: AxisLabel;
  axisLine: AxisLineStyle;
  axisTick: AxisLineStyle;
  nameTextStyle: FontStyle;
  splitLine: AxisLineStyle;
}

export type XAxis = {
  axisPointer?: {
    show: boolean;
    type: string;
  };
  axisLabel: AxisLabel;
  axisLine: AxisLineStyle;
  axisTick: AxisLineStyle;
  data: string[];
  inverse: boolean;
  splitLine: AxisLineStyle;
  tooltip: { show: boolean };
  type: string;
};

export interface BorderStyle {
  borderType?: string;
  borderWidth?: number;
  borderColor?: string;
}

export interface GridStyle {
  left?: string;
  right?: string;
  bottom?: string;
  top?: string;
  containLabel?: boolean;
}

export interface SelectedItem {
  index: string;
  data: {
    rowData: {
      [p: string]: any;
    };
    [p: string]: any;
  };
}

export type IntervalScaleNiceTicksResult = {
  interval: number;
  intervalPrecision: number;
  niceTickExtent: [number, number];
  extent: [number, number];
};
