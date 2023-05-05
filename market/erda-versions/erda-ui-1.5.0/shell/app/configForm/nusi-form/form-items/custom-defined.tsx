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
import i18n from 'i18n';

const FormItem = Form.Item;

export const FormCustomDefined = ({
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
      getComp,
      labelTip,
      fixIn: itemFixIn,
      fixOut: itemFixOut,
    } = fieldConfig || {};
    registerRequiredCheck(requiredCheck);
    const curFixIn = itemFixIn || fixIn;
    const curFixOut = itemFixOut || fixOut;
    const Comp = typeof getComp === 'function' ? getComp({ form, fieldConfig }) : null;
    const handleChange = (e: any) => {
      const fixFun = Comp.props.onChange || curFixOut;
      key && form.setFieldValue(key, fixFun(e));
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
        {Comp
          ? React.cloneElement(Comp, {
              ...componentProps,
              value: curFixIn(value),
              onChange: handleChange,
              disabled,
            })
          : null}
      </FormItem>
    );
  });

export const config = {
  name: 'customDefined',
  Component: FormCustomDefined, // 某React组件，props中必须有value、onChange
  requiredCheck: (value: any) => {
    // 必填校验时，特殊的校验规则
    return [value !== undefined && value !== '', i18n.t('can not be empty')];
  },
  fixOut: (value: any, options: any) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value: any, options: any) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data: any, options: any) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
};
