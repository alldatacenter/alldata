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
import { map, get } from 'lodash';
import i18n from 'i18n';
import './index.scss';

const noop = (a: any) => a;

const FormSelect = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }: any) =>
  React.memo(({ fieldConfig, form }: any) => {
    const {
      key,
      value,
      label,
      registerRequiredCheck = noop,
      componentProps,
      dataSource,
      requiredCheck: _requiredCheck,
    } = fieldConfig || {};
    registerRequiredCheck(_requiredCheck || requiredCheck);
    const handleChange = (e) => {
      form.setFieldValue(key, fixOut(e.target.value));
      (componentProps.onChange || noop)(e.target.value);
    };
    return (
      <div className="dice-form-item mb-4">
        <label htmlFor={key}>{label}:</label>
        <select
          id={key}
          {...componentProps}
          value={fixIn(value)}
          className={`${componentProps.className || ''} dice-form-select`}
          onChange={handleChange}
        >
          {map(get(dataSource, 'static', []), (item: any) => (
            <option key={item.value} value={item.value}>
              {item.name}
            </option>
          ))}
        </select>
      </div>
    );
  });

export const config = {
  name: 'select',
  Component: FormSelect, // 某React组件，props中必须有value、onChange
  requiredCheck: (value) => {
    // 必填校验时，特殊的校验规则
    return [value !== undefined, i18n.t('can not be empty')];
  },
  fixOut: (value, options) => {
    // 在获取表单数据时，将React组件的value格式化成需要的格式
    return value;
  },
  fixIn: (value = '', options) => {
    // 从schema到React组件映射时，修正传入React组件的value
    return value;
  },
  extensionFix: (data, options) => {
    // 从schema到React组件映射时，修正传入React组件的配置项
    return data;
  },
  // event: { // 表单事件机制的eventName，所对应的React组件的事件名
  //   eventName: {
  //     handleName: 'onFocus',
  //   },
  // },
};
