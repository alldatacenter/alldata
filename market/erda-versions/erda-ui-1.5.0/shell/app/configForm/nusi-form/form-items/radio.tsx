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

import { Form, Radio } from 'antd';
import React from 'react';
import { get, map, isEmpty } from 'lodash';
import { getLabel, noop } from './common';
import { commonFields, checkWhen } from './common/config';
import i18n from 'i18n';

const FormItem = Form.Item;

export const FormRadio = ({
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
    const handleChange = (e: any) => {
      form.setFieldValue(key, curFixOut(e.target.value));
      (componentProps.onChange || noop)(e);
    };

    const { radioType, options: cOptions, displayDesc, ...rest } = componentProps;
    const RadioItem = radioType === 'button' ? Radio.Button : Radio;
    const options = cOptions || get(dataSource, 'static') || [];

    const renderOptions = () => {
      if (typeof options === 'function') {
        return options();
      }

      if (isEmpty(options)) {
        return <div>请补充备选数据</div>;
      }

      if (displayDesc) {
        return map(options, (item: any) => (
          <div className="h-16">
            <RadioItem key={item.value} value={item.value}>
              {item.name}
              <div className="text-darkgray">{item.desc}</div>
            </RadioItem>
          </div>
        ));
      }

      return map(options, (item: any) => (
        <RadioItem key={item.value} value={item.value}>
          {item.name}
        </RadioItem>
      ));
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
        <Radio.Group id={key} {...rest} disabled={disabled} value={curFixIn(value)} onChange={handleChange}>
          {renderOptions()}
        </Radio.Group>
      </FormItem>
    );
  });

export const config = {
  name: 'radio',
  Component: FormRadio, // 某React组件，props中必须有value、onChange
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
  radio: {
    name: '单选',
    value: 'radio',
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
            label: '选框类型',
            key: 'componentProps.radioType',
            type: 'radio',
            component: 'radio',
            defaultValue: 'radio',
            dataSource: {
              static: [
                { name: '单选框', value: 'radio' },
                { name: '单选按钮', value: 'button' },
              ],
            },
          },
          {
            label: '显示描述',
            key: 'componentProps.displayDesc',
            type: 'switch',
            component: 'switch',
          },
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
