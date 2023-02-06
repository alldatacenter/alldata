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

import { Form, Checkbox } from 'antd';
import React from 'react';
import { get, map, isEmpty } from 'lodash';
import i18n from 'i18n';
import { getLabel, noop } from './common';
import { commonFields, checkWhen } from './common/config';

const FormItem = Form.Item;

export const FormCheckBox = ({
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
      dataSource,
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
    const handleChange = (v: any[]) => {
      form.setFieldValue(key, curFixOut(v));
      (componentProps.onChange || noop)(v);
    };
    const options = get(dataSource, 'static') || [];

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
        <Checkbox.Group
          id={key}
          {...componentProps}
          disabled={disabled}
          value={curFixIn(value)}
          onChange={handleChange}
        >
          {isEmpty(options) ? (
            <div>请补充备选数据</div>
          ) : (
            map(options, (item: any) => (
              <Checkbox key={item.value} value={item.value}>
                {item.name}
              </Checkbox>
            ))
          )}
        </Checkbox.Group>
      </FormItem>
    );
  });

export const config = {
  name: 'checkbox',
  Component: FormCheckBox, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [!isEmpty(value), i18n.t('can not be empty')];
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
  checkbox: {
    name: '复选框',
    value: 'checkbox',
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
            label: '数据',
            key: 'dataSource.static',
            type: 'dataStatic',
            component: 'dataStatic',
          },
        ],
      },
    },
  },
};
