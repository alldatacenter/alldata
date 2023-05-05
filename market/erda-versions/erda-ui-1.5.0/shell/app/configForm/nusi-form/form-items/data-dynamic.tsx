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
import { getLabel, noop } from './common';
import i18n from 'i18n';
import './data-dynamic.scss';

interface IProps {
  value: {
    api: string;
    dataPath: string;
    valueKey: string;
    nameKey: string;
  };
  onChange: (arg: any) => void;
}

export const DataDynamic = ({ value, onChange }: IProps) => {
  const [data, setData] = React.useState(value);

  const updateData = (val: any) => {
    setData({ ...data, ...val });
  };
  const setChange = () => {
    onChange(data);
  };

  return (
    <div className="dice-form-nusi-data-dynamic">
      <Input
        value={data.api}
        onChange={(e) => updateData({ api: e.target.value })}
        onBlur={setChange}
        placeholder="api路径，若存在动态参数及其他特殊逻辑，请直接填写描述"
      />
      <div className="data-path">
        <Input
          value={data.dataPath}
          onChange={(e) => updateData({ dataPath: e.target.value })}
          onBlur={setChange}
          placeholder="数据路径，如data.list"
        />
        <Input
          value={data.nameKey}
          onChange={(e) => updateData({ nameKey: e.target.value })}
          onBlur={setChange}
          placeholder="展示字段"
        />
        <Input
          value={data.valueKey}
          onChange={(e) => updateData({ valueKey: e.target.value })}
          onBlur={setChange}
          placeholder="取值字段"
        />
      </div>
    </div>
  );
};

const FormItem = Form.Item;

export const FormDataDynamic = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
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
        <DataDynamic value={fixIn(value)} onChange={handleChange} />
      </FormItem>
    );
  });

export const config = {
  name: 'dataDynamic',
  Component: FormDataDynamic, // 某React组件，props中必须有value、onChange
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
