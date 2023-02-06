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
import { map, isEmpty } from 'lodash';
import { Select, Input, Tooltip, Form } from 'antd';
import { getLabel, noop, createCombiner } from './common';
import i18n from 'i18n';

const { Option } = Select;

const ruleMap = {
  pattern: {
    key: 'pattern',
    name: '正则匹配',
    tip: '请输入正则表达式,否则校验不生效',
  },
  enum: {
    key: 'enum',
    name: '枚举',
    tip: '请输入枚举值，以`,`隔开',
    valueFixIn: (val: string[]) => val && val.join(','),
    valueFixOut: (val: string) => val && val.split(','),
  },
  validator: {
    key: 'validator',
    name: '自定义',
    tip: '请输入自定义校验的描述说明',
  },
  min: {
    key: 'min',
    name: '最小长度',
    tip: '',
  },
  max: {
    key: 'max',
    name: '最大长度',
    tip: '',
  },
  equalWith: {
    key: 'equalWith',
    name: '等于',
    tip: '设置该表单值等于其他表单的值（一般用于密码、密码确认）',
  },
};

interface IRule {
  type: string;
  value: any;
  msg: string;
}

interface IRuleValue {
  msg: string;
  [pro: string]: any;
}
const ruleItem: IRule = {
  type: 'pattern',
  value: '',
  msg: '',
};

interface IRuleItemProps {
  data: IRule;
  className?: string;
  operation?: any;
  updateItem: (arg: any) => void;
}

const RuleItem = ({ updateItem, data, className = '', operation = null }: IRuleItemProps) => {
  const curRule = { valueFixIn: noop, valueFixOut: noop, ...(ruleMap[data.type] || {}) };
  return (
    <div className={className}>
      <Select
        value={data.type}
        onChange={(val) => {
          updateItem({ type: val, value: undefined, msg: undefined });
        }}
      >
        {map(ruleMap, (item) => (
          <Option key={item.key} value={item.key}>
            {item.name}
          </Option>
        ))}
      </Select>
      <Tooltip title={curRule.tip}>
        <Input
          value={curRule.valueFixIn(data.value)}
          placeholder="请输入"
          onChange={(e) => updateItem({ value: curRule.valueFixOut(e.target.value) })}
        />
      </Tooltip>
      <Input value={data.msg} placeholder="请填写提示信息" onChange={(e) => updateItem({ msg: e.target.value })} />
      {operation}
    </div>
  );
};

const changeRulesToValue = (rules: IRule[]) => {
  return map(rules, (item) => {
    const { type, value, msg } = item;
    return { [type]: value, msg };
  });
};

const changeValueToRules = (value: Array<{ msg: string; [pro: string]: any }>) => {
  const curRules = [] as IRule[];
  map(value, (item) => {
    const { msg, ...rest } = item;
    const curKey = Object.keys(rest)[0];
    if (curKey && ruleMap[curKey]) {
      curRules.push({ type: curKey, value: Object.values(rest)[0], msg });
    }
  });
  return curRules;
};

export const RulesInput = createCombiner<IRuleValue, IRule>({
  valueFixIn: changeValueToRules,
  valueFixOut: changeRulesToValue,
  CombinerItem: RuleItem,
  defaultItem: ruleItem,
});

const FormItem = Form.Item;

export const FormRuleInput = ({ fixOut = noop, fixIn = noop, extensionFix, requiredCheck, trigger = 'onChange' }) =>
  React.memo(({ fieldConfig, form }: any) => {
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
        <RulesInput value={fixIn(value)} onChange={handleChange} />
      </FormItem>
    );
  });

export const config = {
  name: 'rulesInput',
  Component: FormRuleInput, // 某React组件，props中必须有value、onChange
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
