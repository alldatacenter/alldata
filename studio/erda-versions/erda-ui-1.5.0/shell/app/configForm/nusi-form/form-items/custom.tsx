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
import { getLabel, noop } from './common';
import { commonFields } from './common/config';
import i18n from 'i18n';

const FormItem = Form.Item;

export const FormInput = ({
  fixOut = noop,
  fixIn = noop,
  extensionFix,
  requiredCheck,
  trigger = 'onChange',
}: any = {}) =>
  React.memo(({ fieldConfig, form }: any = {}) => {
    const {
      label,
      visible,
      valid = [],
      required,
      registerRequiredCheck = noop,
      wrapperProps,
      labelTip,
      requiredCheck: _requiredCheck,
    } = fieldConfig || {};
    registerRequiredCheck(_requiredCheck || requiredCheck);

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
        <div>自定义组件将由前端同学根据您的描述实现，并添加后才可预览</div>
      </FormItem>
    );
  });

export const config = {
  name: 'custom',
  Component: FormInput, // 某React组件，props中必须有value、onChange
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
  custom: {
    name: '自定义组件',
    value: 'custom',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [
          ...commonFields.slice(0, 3),
          {
            label: '自定义组件描述',
            key: 'description',
            type: 'textarea',
            component: 'textarea',
            componentProps: {
              placeholder: '请描述自定义组件逻辑',
            },
          },
        ],
      },
    },
  },
};
