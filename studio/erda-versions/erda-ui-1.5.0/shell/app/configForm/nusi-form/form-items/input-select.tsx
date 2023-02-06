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

import { Form } from 'antd';
import React from 'react';
import { isEmpty, map, debounce } from 'lodash';
import { InputSelect } from 'common';
import { getLabel, noop } from './common';
import { commonFields, checkWhen } from './common/config';
import i18n from 'i18n';
import './input-select.scss';

const FormItem = Form.Item;

const empty = {};
const PureFormSelect = (props: any) => {
  const { fieldConfig, form, fixOut, fixIn, requiredCheck } = props || {};
  const [options, setOptions] = React.useState([] as any[] | Function);
  const {
    key,
    value,
    label,
    labelTip,
    dataSource = empty,
    visible,
    disabled,
    componentProps,
    wrapperProps,
    required,
    registerRequiredCheck = noop,
    valid,
    fixIn: itemFixIn,
    fixOut: itemFixOut,
    requiredCheck: _requiredCheck,
  } = fieldConfig || {};
  const curFixIn = itemFixIn || fixIn;
  const curFixOut = itemFixOut || fixOut;
  const { options: curOption } = componentProps;

  React.useEffect(() => {
    if (typeof curOption === 'function') {
      setOptions(() => map(curOption, (o) => ({ ...o, label: o.name })));
    } else if (Array.isArray(curOption)) {
      setOptions(map(curOption, (o) => ({ ...o, label: o.name })));
    }
  }, [dataSource, curOption]);

  const debounceChange = React.useRef(debounce(componentProps.onChange || noop, 300));

  const handleChange = (...args: any) => {
    form.setFieldValue(key, curFixOut(args[0]));
    debounceChange.current(...args);
  };
  registerRequiredCheck(_requiredCheck || requiredCheck);

  const { placeholder } = componentProps || {};
  const _placeholder = placeholder || i18n.t('please select {name}', { name: label || key });

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
      <InputSelect
        id={key}
        {...componentProps}
        placeholder={_placeholder}
        disabled={disabled}
        options={options}
        value={curFixIn(value)}
        onChange={handleChange}
      />
    </FormItem>
  );
};

export const FormInputSelect = ({
  fixOut = noop,
  fixIn = noop,
  requiredCheck,
  extensionFix,
  trigger = 'onChange',
}: any = {}) => {
  return React.memo((props: any) => (
    <PureFormSelect
      fixOut={fixOut}
      fixIn={fixIn}
      requiredCheck={requiredCheck}
      extensionFix={extensionFix}
      trigger={trigger}
      {...props}
    />
  ));
};

export const config = {
  name: 'inputSelect',
  Component: FormInputSelect,
  requiredCheck: (value: string) => {
    return [!isEmpty(value), i18n.t('can not be empty')];
  },
};

export const formConfig = {
  inputSelect: {
    name: '可输入选择框',
    value: 'inputSelect',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...commonFields, ...checkWhen],
      },
      componentProps: {
        key: 'componentProps',
        name: '组件配置',
        fields: [
          {
            label: 'placeholder',
            key: 'componentProps.placeholder',
            type: 'input',
            component: 'input',
          },
          {
            label: '支持清除',
            key: 'componentProps.allowClear',
            type: 'switch',
            component: 'switch',
          },
          {
            label: '选项数据',
            key: 'componentProps.options',
            type: 'dataStatic',
            component: 'dataStatic',
          },
        ],
      },
    },
  },
};
