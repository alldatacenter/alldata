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
import i18n from 'i18n';

interface IData {
  name: string;
  desc?: string;
  value: string;
}

const dataItem: IData = {
  name: '',
  desc: '',
  value: '',
};

interface IDataItemProps {
  data: IData;
  hasDesc?: boolean;
  className?: string;
  operation?: any;
  updateItem: (arg: any) => void;
}

const DataItem = ({ updateItem, data, hasDesc = true, className = '', operation = null }: IDataItemProps) => {
  return (
    <div className={className}>
      <Input value={data.name} placeholder="请输入数据名称" onChange={(e) => updateItem({ name: e.target.value })} />
      {hasDesc && (
        <Input value={data.desc} placeholder="请输入数据描述" onChange={(e) => updateItem({ desc: e.target.value })} />
      )}
      <Input value={data.value} placeholder="请填写数据值" onChange={(e) => updateItem({ value: e.target.value })} />
      {operation}
    </div>
  );
};

export const DataStatic = createCombiner<IData, IData>({
  valueFixIn: noop,
  valueFixOut: noop,
  CombinerItem: DataItem,
  defaultItem: dataItem,
});

const FormItem = Form.Item;

export const FormDataStatic = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
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
        <DataStatic value={fixIn(value)} onChange={handleChange} />
      </FormItem>
    );
  });

export const config = {
  name: 'dataStatic',
  Component: FormDataStatic, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [!isEmpty(value), i18n.t('can not be empty')];
  },
  fixOut: (value, options) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value = [], options) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data, options) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};
