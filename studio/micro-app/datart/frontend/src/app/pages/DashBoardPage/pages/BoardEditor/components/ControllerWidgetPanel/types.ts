import { RadioGroupOptionType } from 'antd/lib/radio';
import { DataViewFieldType, TimeFilterValueCategory } from 'app/constants';
import {
  ControllerVisibleType,
  ValueOptionType,
} from 'app/pages/DashBoardPage/constants';
import { RelationFilterValue } from 'app/types/ChartConfig';
import { FilterSqlOperator } from 'globalConstants';
import i18next from 'i18next';
import { Moment, unitOfTime } from 'moment';
import { VariableValueTypes } from '../../../../../MainPage/pages/VariablePage/constants';

export interface ControllerVisibility {
  visibilityType: ControllerVisibleType;
  condition?: VisibilityCondition;
}
export type ValueTypes = DataViewFieldType | VariableValueTypes;
export interface ControlOption {
  label: string;
  value: string;
  id?: string;
  type?: string;
  variables?: {
    [viewId: string]: string;
  };
  children?: ControlOption[];
}
export interface VisibilityCondition {
  dependentControllerId: string;
  relation: FilterSqlOperator.Equal | FilterSqlOperator.NotEqual; //等于或这不等于
  value: any; // any type
}

export interface ControllerConfig {
  valueOptionType: ValueOptionType; //
  visibility: ControllerVisibility;
  sqlOperator: FilterSqlOperator;
  valueOptions: RelationFilterValue[];
  controllerValues: any[];
  required: boolean; // 是否允许空值
  canChangeSqlOperator?: boolean; // 是否显示 sqlOperator 切换
  assistViewFields?: string[]; //辅助添加view字段
  controllerDate?: ControllerDate; //存储时间
  parentFields?: string[]; //父节点字段
  buildingMethod?: 'byParent' | 'byHierarchy'; //树控制器类型

  minValue?: number; // slider min
  maxValue?: number; // slider max

  radioButtonType?: RadioGroupOptionType; //按钮样式

  sliderConfig?: SliderConfig;
}

export interface ControllerDate {
  pickerType: PickerType;
  startTime: ControllerDateType;
  endTime?: ControllerDateType;
}
export interface SliderConfig {
  step: number;
  range: boolean;
  vertical: boolean;
  showMarks: boolean;
}
export const enum PickerTypes {
  Year = 'year',
  Quarter = 'quarter',
  Month = 'month',
  Week = 'week',
  Date = 'date',
  DateTime = 'dateTime',
}
export type PickerType = Uncapitalize<keyof typeof PickerTypes>;

const td = (value: PickerTypes) => {
  const preStr = 'viz.date.';
  return i18next.t(preStr + value);
};
const getPickerTypeItem = (value: PickerTypes) => {
  return {
    name: td(value),
    value: value,
  };
};
export const PickerTypeOptions = [
  getPickerTypeItem(PickerTypes.Date),
  getPickerTypeItem(PickerTypes.DateTime),
  getPickerTypeItem(PickerTypes.Year),
  getPickerTypeItem(PickerTypes.Quarter),
  getPickerTypeItem(PickerTypes.Month),
  getPickerTypeItem(PickerTypes.Week),
];

export interface ControllerDateType {
  relativeOrExact: TimeFilterValueCategory;
  relativeValue?: RelativeDate;
  exactValue?: Moment | string | null;
}

export interface RelativeDate {
  amount: number;
  unit: unitOfTime.DurationConstructor;
  direction: '-' | '+' | '+0';
}
