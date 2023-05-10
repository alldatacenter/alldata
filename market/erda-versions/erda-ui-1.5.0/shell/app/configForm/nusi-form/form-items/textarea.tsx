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

import { Form, Input } from 'antd';
import React from 'react';
import { getLabel, noop } from './common';
import { commonFields, rulesField, checkWhen } from './common/config';
import i18n from 'i18n';

const FormItem = Form.Item;
const { TextArea } = Input;

export const FormTextArea = ({
  fixOut = noop,
  fixIn = noop,
  extensionFix,
  requiredCheck,
  trigger = 'onChange',
}: any = {}) =>
  React.memo(({ fieldConfig, form }: any = {}) => {
    const {
      key,
      value,
      label,
      visible,
      valid = [],
      disabled,
      required,
      registerRequiredCheck = noop,
      componentProps,
      wrapperProps,
      labelTip,
      fixIn: itemFixIn,
      fixOut: itemFixOut,
      requiredCheck: _requiredCheck,
    } = fieldConfig || {};
    const curFixIn = itemFixIn || fixIn;
    const curFixOut = itemFixOut || fixOut;

    registerRequiredCheck(_requiredCheck || requiredCheck);
    const handleChange = (e: any) => {
      form.setFieldValue(key, curFixOut(e.target.value));
      (componentProps.onChange || noop)(e);
    };

    const { placeholder } = componentProps || {};
    const _placeholder = placeholder || i18n.t('please enter {name}', { name: label });
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
        <TextArea
          id={key}
          {...componentProps}
          placeholder={_placeholder}
          disabled={disabled}
          value={curFixIn(value)}
          onChange={handleChange}
        />
      </FormItem>
    );
  });

export const config = {
  name: 'textarea',
  Component: FormTextArea, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [value !== undefined && value !== '', i18n.t('can not be empty')];
  },
  fixOut: (value, options) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value, options) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data, options) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};

export const formConfig = {
  textarea: {
    name: '多行文本',
    value: 'textarea',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...commonFields, rulesField, ...checkWhen],
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
            label: '自适应内容高度',
            key: 'componentProps.autoSize',
            type: 'switch',
            component: 'switch',
          },
          {
            label: '最大长度',
            key: 'componentProps.maxLength',
            type: 'inputNumber',
            component: 'inputNumber',
            componentProps: {
              min: 0,
            },
          },
          {
            label: '最小行数',
            key: 'componentProps.autoSize.minRows',
            type: 'inputNumber',
            component: 'inputNumber',
            defaultValue: 2,
            componentProps: {
              min: 1,
            },
          },
          {
            label: '最大行数',
            key: 'componentProps.autoSize.maxRows',
            type: 'inputNumber',
            component: 'inputNumber',
            defaultValue: 4,
            componentProps: {
              min: 1,
            },
          },
        ],
      },
    },
  },
};
