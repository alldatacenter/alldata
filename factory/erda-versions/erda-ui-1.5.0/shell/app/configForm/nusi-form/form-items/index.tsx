// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { set, findIndex, map, filter, isArray } from 'lodash';

// 无formConfig，未对外提供
import { config as rulesInputConfig } from './rules-input';
import { config as clearWhenConfig } from './clear-when';
import { config as checkWhenConfig } from './check-when';
import { config as dataStaticConfig } from './data-static';
import { config as dataDynamicConfig } from './data-dynamic';
import { config as customDefinedConfig } from './custom-defined';

// 有formConfig,对外提供选择
import { config as inputConfig, formConfig as inputFormConfig } from './input';
import { config as selectConfig, formConfig as selectFormConfig } from './select';
import { config as inputSelectConfig, formConfig as inputSelectFormConfig } from './input-select';
import { config as switchConfig, formConfig as switchFormConfig } from './switch';
import { config as textareaConfig, formConfig as textareaFormConfig } from './textarea';
import { config as inputNumberConfig, formConfig as inputNumberFormConfig } from './inputNumber';
import { config as radioConfig, formConfig as radioFormConfig } from './radio';
import { config as checkboxConfig, formConfig as checkboxFormConfig } from './checkbox';
import { config as customConfig, formConfig as customFormConfig } from './custom';
import { config as datePickerConfig, formConfig as datePickerFormConfig } from './date-picker';
import { config as arrayObjConfig, formConfig as arrayObjFormConfig } from './array-obj';
import { config as inputArrayConfig, formConfig as inputArrayFormConfig } from './input-array';
import { config as groupConfig, formConfig as groupFormConfig } from './group';
import { config as mapConfig, formConfig as mapFormConfig } from './map';
import { config as uploadConfig, formConfig as uploadFormConfig } from './upload';

import { registComponents } from '../form';

export { basicField, commonFields, checkWhen, rulesField } from './common/config';

export const components = [
  inputConfig,
  textareaConfig,
  selectConfig,
  switchConfig,
  rulesInputConfig,
  clearWhenConfig,
  checkWhenConfig,
  dataStaticConfig,
  dataDynamicConfig,
  inputNumberConfig,
  radioConfig,
  checkboxConfig,
  customConfig,
  datePickerConfig,
  inputSelectConfig,
  customDefinedConfig,
  arrayObjConfig,
  inputArrayConfig,
  groupConfig,
  mapConfig,
  uploadConfig,
];

export const componentFormConfig = {
  ...inputFormConfig,
  ...selectFormConfig,
  ...switchFormConfig,
  ...textareaFormConfig,
  ...inputNumberFormConfig,
  ...radioFormConfig,
  ...checkboxFormConfig,
  ...datePickerFormConfig,
  ...inputSelectFormConfig,
  ...customFormConfig,
  ...arrayObjFormConfig,
  ...inputArrayFormConfig,
  ...groupFormConfig,
  ...mapFormConfig,
  ...uploadFormConfig,
};

interface IRegisterFormProps {
  key: string;
  componentConfig: {};
  formConfig: {};
}
export const registerFormComponent = (props: IRegisterFormProps[]) => {
  map(props, (item) => {
    const { componentConfig, formConfig, key } = item;
    componentFormConfig[key] = { ...formConfig[key] };
    let curIndex = findIndex(components, { name: key });
    if (curIndex === -1) curIndex = components.length;
    set(components, `[${curIndex}]`, { ...componentConfig, name: key });
  });
  registComponents(components);
};

export const exceptField = (f: any[], exceptKeys: string | string[]) => {
  const _exceptKeys = isArray(exceptKeys) ? exceptKeys : [exceptKeys];
  return filter(f, (item) => !_exceptKeys.includes(item.key));
};
