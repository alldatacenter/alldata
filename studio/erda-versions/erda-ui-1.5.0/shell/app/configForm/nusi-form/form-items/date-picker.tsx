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

import { Form, DatePicker } from 'antd';
import React from 'react';
import { isEmpty, isArray, isString } from 'lodash';
import { getLabel, noop } from './common';
import { commonFields, checkWhen } from './common/config';
import i18n from 'i18n';

const { MonthPicker, RangePicker, WeekPicker } = DatePicker;

const FormItem = Form.Item;

const CompMap = {
  date: DatePicker,
  month: MonthPicker,
  range: RangePicker,
  week: WeekPicker,
};

const DatePickerComp = (props: any) => {
  const { componentProps, id, disabled, fixIn, handleChange, value } = props;
  const { dateType = 'date', placeholder, ...restCompProps } = componentProps || {};
  const Comp = CompMap[dateType];

  const plcholder = dateType === 'range' && isString(placeholder) ? placeholder.split(',') : placeholder;
  return (
    <Comp
      id={id}
      key={dateType}
      placeholder={plcholder}
      {...restCompProps}
      disabled={disabled}
      value={fixIn(value, { dateType })}
      onChange={handleChange}
    />
  );
};

export const FormDatePicker = ({
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
      form.setFieldValue(key, curFixOut(e));
      (componentProps.onChange || noop)(e);
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
        <DatePickerComp
          componentProps={componentProps}
          id={key}
          disabled={disabled}
          key={componentProps.dateType || 'date'}
          fixIn={curFixIn}
          handleChange={handleChange}
          value={value}
        />
      </FormItem>
    );
  });

export const config = {
  name: 'datePicker',
  Component: FormDatePicker, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [!isEmpty(value), i18n.t('can not be empty')];
  },
  fixOut: (value, options) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value: any, options: any) => {
    const { dateType = 'date' } = options || {};
    if (dateType === 'range' && !isArray(value)) return undefined;
    if (dateType !== 'range' && isArray(value)) return undefined;
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data, options) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};

export const formConfig = {
  datePicker: {
    name: '日期选择器',
    value: 'datePicker',
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
            label: '日期类型',
            key: 'componentProps.dateType',
            type: 'radio',
            component: 'radio',
            defaultValue: 'date',
            dataSource: {
              static: [
                { name: '日期', value: 'date' },
                { name: '月份', value: 'month' },
                { name: '日期范围', value: 'range' },
                { name: '星期', value: 'week' },
              ],
            },
          },
          {
            label: '支持清除',
            key: 'componentProps.allowClear',
            type: 'switch',
            component: 'switch',
          },
          {
            label: 'placeholder',
            key: 'componentProps.placeholder',
            type: 'input',
            component: 'input',
            componentProps: {
              placeholder: '日期范围用,分隔两个placehoder',
            },
          },
          {
            label: '时间选择',
            key: 'componentProps.showTime',
            type: 'switch',
            component: 'switch',
            defaultValue: true,
            removeWhen: [[{ field: 'componentProps.dateType', operator: 'includes', value: ['week', 'month'] }]],
            componentProps: {
              labelTip: '增加时间选择功能',
            },
          },
        ],
      },
    },
  },
};
