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
import { filter, isEmpty, map } from 'lodash';
import { Select, Form } from 'antd';
import { getLabel, noop } from './common';
import i18n from 'i18n';

const { Option } = Select;

interface IProps {
  value: string[];
  allField: any[];
  form: FormInstance;
  onChange: (arg: string[]) => void;
}
const ClearFieldSelector = (props: IProps) => {
  const { value, onChange, allField, form } = props;
  const [options, setOptions] = React.useState([] as string[]);
  const [optionInit, setOptionInit] = React.useState(false);

  React.useEffect(() => {
    if (optionInit) {
      setOptions(
        map(
          filter(allField, (item: any) => item.key !== form.getData().key),
          'key',
        ),
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [optionInit]);

  return (
    <Select
      mode="multiple"
      value={value}
      onChange={onChange}
      onDropdownVisibleChange={(isOpen) => isOpen && !optionInit && setOptionInit(true)}
    >
      {map(options, (item) => (
        <Option key={item} value={item}>
          {item}
        </Option>
      ))}
    </Select>
  );
};

const FormItem = Form.Item;

export const FormClearWhen = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
  React.memo(({ fieldConfig, form, allField }: any) => {
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
        <ClearFieldSelector value={fixIn(value)} onChange={handleChange} allField={allField} form={form} />
      </FormItem>
    );
  });

export const config = {
  name: 'clearWhen',
  Component: FormClearWhen, // 某React组件，props中必须有value、onChange
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
