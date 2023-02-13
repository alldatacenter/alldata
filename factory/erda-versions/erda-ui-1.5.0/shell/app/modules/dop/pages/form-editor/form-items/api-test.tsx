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
import { ApiItem } from '../components/api-test';
import React from 'react';
import { commonFields, exceptField } from 'app/configForm/nusi-form/form-items';

const FormItem = Form.Item;
const noop = (d: any) => d;
export const FormApiTest = ({
  fixOut = noop,
  fixIn = noop,
  extensionFix,
  requiredCheck,
  trigger = 'onChange',
}: any = {}) =>
  React.memo(({ fieldConfig, form, getLabel }: any = {}) => {
    const {
      key,
      value,
      label,
      visible,
      valid = [],
      disabled,
      registerRequiredCheck = noop,
      componentProps,
      wrapperProps,
      labelTip,
      requiredCheck: _requiredCheck,
    } = fieldConfig || {};
    registerRequiredCheck(_requiredCheck || requiredCheck);
    const handleChange = (e: any) => {
      form.setFieldValue(key, fixOut(e));
      (componentProps.onChange || noop)(e);
    };
    return (
      <FormItem
        colon
        label={getLabel(label, labelTip)}
        className={visible ? '' : 'hidden'}
        validateStatus={valid[0]}
        help={valid[1]}
        required={false}
        {...wrapperProps}
      >
        <ApiItem id={key} {...componentProps} disabled={disabled} value={fixIn(value)} onChange={handleChange} />
      </FormItem>
    );
  });

export const config = {
  name: 'apiTest',
  Component: FormApiTest, // 某React组件，props中必须有value、onChange
  // requiredCheck: (value: Obj) => {
  //   // 必填校验时，特殊的校验规则
  //   return [value !== undefined && isEmpty(value), i18n.t('can not be empty')];
  // },
};

export const formConfig = {
  apiTest: {
    name: '接口测试',
    value: 'apiTest',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: '基本配置',
        fields: [...exceptField(commonFields, ['required', 'disabled', 'defaultValue', 'validateTrigger'])],
      },
    },
  },
};
