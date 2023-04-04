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

import { FONT_FAMILY, G90 } from 'styles/StyleConstants';
import { IFontDefault } from 'types';

export enum TenantManagementMode {
  Team = 'TEAM',
  Platform = 'PLATFORM',
}

export enum ControllerFacadeTypes {
  DropdownList = 'dropdownList',
  MultiDropdownList = 'multiDropdownList',
  DropDownTree = 'dropDownTree',

  RadioGroup = 'radioGroup',
  CheckboxGroup = 'checkboxGroup',
  Text = 'text',

  Value = 'value',
  RangeValue = 'rangeValue',
  Slider = 'slider',
  RangeSlider = 'rangeSlider',

  Time = 'time',
  RangeTime = 'rangeTime',
  RangeTimePicker = 'rangeTimePicker',
  RecommendTime = 'recommendTime',

  Tree = 'tree',
}

export enum ControllerRadioFacadeTypes {
  Default = 'default',
  Button = 'button',
}

export enum TimeFilterValueCategory {
  Relative = 'relative',
  Exact = 'exact',
}

export enum ControllerVisibilityTypes {
  Hide = 'hide',
  Show = 'show',
  Condition = 'condition',
}

export enum ChartLifecycle {
  Mounted = 'mounted',
  Updated = 'updated',
  Resize = 'resize',
  UnMount = 'unmount',
}

export enum DataViewFieldType {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  DATE = 'DATE',
}

export enum ChartDataViewSubType {
  UnCategorized = 'UNCATEGORIZED',
  Country = 'COUNTRY',
  ProvinceOrState = 'PROVINCEORSTATE',
  City = 'CITY',
  County = 'COUNTY',
}

export enum ChartDataViewFieldCategory {
  Field = 'field',
  Hierarchy = 'hierarchy',
  Variable = 'variable',
  ComputedField = 'computedField',
  AggregateComputedField = 'aggregateComputedField',
  DateLevelComputedField = 'dateLevelComputedField',
}

export enum SortActionType {
  None = 'NONE',
  ASC = 'ASC',
  DESC = 'DESC',
  Customize = 'CUSTOMIZE',
}

export enum FieldFormatType {
  Default = 'default',
  Numeric = 'numeric',
  Currency = 'currency',
  Percentage = 'percentage',
  Scientific = 'scientificNotation',
  Date = 'date',
  Custom = 'custom',
}

export enum AggregateFieldActionType {
  None = 'NONE',
  Sum = 'SUM',
  Avg = 'AVG',
  Count = 'COUNT',
  Count_Distinct = 'COUNT_DISTINCT',
  Max = 'MAX',
  Min = 'MIN',
}

export enum ChartDataSectionType {
  Group = 'group',
  Aggregate = 'aggregate',
  Mixed = 'mixed',
  Filter = 'filter',
  Color = 'color',
  Info = 'info',
  Size = 'size',
}

export enum FilterConditionType {
  // Real Filters
  List = 1 << 1,
  Customize = 1 << 2,
  Condition = 1 << 3,
  RangeValue = 1 << 4,
  Value = 1 << 5,
  RangeTime = 1 << 6,
  RecommendTime = 1 << 7,
  Time = 1 << 8,
  Tree = 1 << 9,

  // Logic Filters, and type of `Filter` includes all Real Filters
  Filter = List |
    Condition |
    Customize |
    RangeValue |
    Value |
    RangeTime |
    RecommendTime |
    Time |
    Tree,
  Relation = 1 << 50,
}

export const ChartDataSectionFieldActionType = {
  Sortable: 'sortable',
  Alias: 'alias',
  Format: 'format',
  Aggregate: 'aggregate',
  AggregateLimit: 'aggregateLimit',
  Filter: 'filter',
  CategoryFilter: 'categoryFilter',
  Colorize: 'colorize',
  ColorRange: 'colorRange',
  ColorizeSingle: 'colorSingle',
  Size: 'size',
  DateLevel: 'dateLevel',
};

export const FilterRelationType = {
  AND: 'and',
  OR: 'or',
  BETWEEN: 'between',
  IN: 'in',
};

export const AggregateFieldSubAggregateType = {
  [ChartDataSectionFieldActionType.Aggregate]: [
    AggregateFieldActionType.Sum,
    AggregateFieldActionType.Avg,
    AggregateFieldActionType.Count,
    AggregateFieldActionType.Count_Distinct,
    AggregateFieldActionType.Max,
    AggregateFieldActionType.Min,
  ],
  [ChartDataSectionFieldActionType.AggregateLimit]: [
    AggregateFieldActionType.Count,
    AggregateFieldActionType.Count_Distinct,
  ],
};

export const ChartStyleSectionComponentType = {
  CHECKBOX: 'checkbox',
  INPUT: 'input',
  SWITCH: 'switch',
  SELECT: 'select',
  FONT: 'font',
  FONT_FAMILY: 'fontFamily',
  FONT_SIZE: 'fontSize',
  FONT_COLOR: 'fontColor',
  FONT_STYLE: 'fontStyle',
  FONT_WEIGHT: 'fontWeight',
  INPUT_NUMBER: 'inputNumber',
  INPUT_PERCENTAGE: 'inputPercentage',
  SLIDER: 'slider',
  GROUP: 'group',
  REFERENCE: 'reference',
  TABS: 'tabs',
  LIST_TEMPLATE: 'listTemplate',
  TABLE_HEADER: 'tableHeader',
  LINE: 'line',
  MARGIN_WIDTH: 'marginWidth',
  TEXT: 'text',
  CONDITIONAL_STYLE: 'conditionalStylePanel',
  RADIO: 'radio',

  // Customize Component
  FONT_ALIGNMENT: 'fontAlignment',
  NAME_LOCATION: 'nameLocation',
  LABEL_POSITION: 'labelPosition',
  LEGEND_TYPE: 'legendType',
  LEGEND_POSITION: 'legendPosition',
  SCORECARD_LIST_TEMPLATE: 'scorecardListTemplate',
  SCORECARD_CONDITIONAL_STYLE: 'scorecardConditionalStyle',
  PIVOT_SHEET_THEME: 'pivotSheetTheme',
  BACKGROUND: 'background',
  WIDGET_BORDER: 'widgetBorder',
  TIMER_FORMAT: 'timerFormat',
  CHECKBOX_MODAL: 'checkboxModal',
  INTERACTION_DRILL_THROUGH_PANEL: 'interaction.drillThrough',
  INTERACTION_CROSS_FILTERING: 'interaction.crossFiltering',
  INTERACTION_VIEW_DETAIL_PANEL: 'interaction.viewDetail',
  DATA_ZOOM_PANEL: 'dataZoomPanel',
  Y_AXIS_NUMBER_FORMAT_PANEL: 'YAxisNumberFormatPanel',
};

export enum DownloadFileType {
  Pdf = 'PDF',
  Excel = 'EXCEL',
  Image = 'IMAGE',
  Template = 'TEMPLATE',
}

export const RUNTIME_DATE_LEVEL_KEY = Symbol('DateLevel');

export const FONT_DEFAULT: IFontDefault = {
  fontFamily: FONT_FAMILY,
  fontSize: '14',
  fontWeight: 'normal',
  fontStyle: 'normal',
  color: G90,
};

export enum AuthorizationStatus {
  Initialized = 'initialized',
  Pending = 'pending',
  Success = 'success',
  Error = 'error',
}

export enum ChartInteractionEvent {
  Select = 'select',
  UnSelect = 'unSelect',
  Drilled = 'drilled',
  PagingOrSort = 'paging-sort-filter',
  ChangeContext = 'rich-text-change-context',
}

export enum DateFormat {
  DateTime = 'YYYY-MM-DD HH:mm:ss',
  Date = 'YYYY-MM-DD',
}
