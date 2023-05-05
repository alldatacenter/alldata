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

import React from 'react';
import { isEmpty } from 'lodash';
import { Input, Form } from 'antd';
import { getLabel, noop, createCombiner } from './common';
import { commonFields, checkWhen } from './common/config';
import i18n from 'i18n';
import './input-array.scss';

interface IInputArrayItemProps {
  keys: string[] | Array<{ key: string; name: string }>;
  data: string | undefined;
  className?: string;
  operation?: any;
  updateItem: (arg: any) => void;
  itemRender?: (data: string | undefined, updateItem: (arg: string) => void) => void;
}

const InputItem = ({ updateItem, data, className = '', operation = null, itemRender }: IInputArrayItemProps) => {
  return (
    <div className={`dice-form-input-array ${className}`}>
      {itemRender ? (
        itemRender(data, updateItem)
      ) : (
        <Input value={data} placeholder={i18n.t('please enter')} onChange={(e) => updateItem(e.target.value)} />
      )}
      {operation}
    </div>
  );
};

const changeValue = (obj: Obj[]) => obj;
export const InputArray = createCombiner<Obj, Obj>({
  valueFixIn: changeValue,
  valueFixOut: changeValue,
  CombinerItem: InputItem,
});

const FormItem = Form.Item;

export const FormInputArray = ({
  fixOut = noop,
  fixIn = noop,
  extensionFix,
  requiredCheck,
  trigger = 'onChange',
}: any) =>
  React.memo(({ fieldConfig, form }: any) => {
    const {
      key,
      value,
      label,
      visible,
      valid,
      registerRequiredCheck,
      componentProps,
      required,
      wrapperProps,
      labelTip,
      requiredCheck: _requiredCheck,
    } = fieldConfig;
    registerRequiredCheck(_requiredCheck || requiredCheck);
    const handleChange = (val: any) => {
      form.setFieldValue(key, fixOut(val));
      (componentProps.onChange || noop)(val);
    };
    return (
      <FormItem
        colon
        label={getLabel(label, labelTip)}
        className={visible ? '' : 'hidden'}
        validateStatus={valid[0]}
        help={valid[1]}
        required={required}
        {...wrapperProps}
      >
        <InputArray value={fixIn(value) || []} onChange={handleChange} {...componentProps} />
      </FormItem>
    );
  });

export const config = {
  name: 'inputArray',
  Component: FormInputArray, // 某React组件，props中必须有value、onChange
  requiredCheck: (value: any) => {
    // 必填校验时，特殊的校验规则
    return [!isEmpty(value), i18n.t('can not be empty')];
  },
  fixOut: (value: any) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value = [], options: any) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data: any, options: any) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};

export const formConfig = {
  inputArray: {
    name: '字符串数组',
    value: 'inputArray',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...commonFields, ...checkWhen],
      },
    },
  },
};
