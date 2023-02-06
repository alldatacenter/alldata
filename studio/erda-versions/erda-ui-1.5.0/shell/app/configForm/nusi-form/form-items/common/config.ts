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

export const basicField = {
  label: '标题',
  component: 'input',
  validateTrigger: ['onChange'],
  required: true,
};

export const rulesField = {
  label: '校验规则',
  key: 'rules',
  type: 'rulesInput',
  component: 'rulesInput',
};

export const clearField = {
  label: '清除其他字段',
  key: 'clearWhen',
  type: 'clearWhen',
  component: 'clearWhen',
  componentProps: {
    placeholder: '值变化的时候,将选中的其他字段值置为空',
  },
};

export const checkWhen = [
  clearField,
  {
    label: '隐藏条件',
    key: 'hideWhen',
    type: 'checkWhen',
    component: 'checkWhen',
    labelTip: '隐藏条件：设置条件组，满足条件表单将隐藏',
  },
  {
    label: '移除条件',
    key: 'removeWhen',
    type: 'checkWhen',
    component: 'checkWhen',
    labelTip: '移除条件：设置条件组，满足条件表单将移除',
  },
  {
    label: '禁用条件',
    key: 'disableWhen',
    type: 'checkWhen',
    component: 'checkWhen',
    labelTip: '禁用条件：设置条件组，满足条件表单将禁用',
  },
];

export const commonFields = [
  {
    label: '表单字段',
    key: 'key',
    type: 'input',
    component: 'input',
    labelTip: '字段唯一，支持对象嵌套：a.b.c',
    disabled: true,
  },
  {
    label: '组件',
    key: 'component',
    type: 'input',
    component: 'input',
    defaultValue: 'input',
    disabled: true,
  },
  {
    label: '标签',
    key: 'label',
    type: 'input',
    component: 'input',
  },
  {
    label: '标签提示',
    key: 'labelTip',
    type: 'input',
    component: 'input',
  },
  {
    label: '是否必填',
    key: 'required',
    type: 'switch',
    component: 'switch',
    defaultValue: true,
  },
  {
    label: '是否禁用',
    key: 'disabled',
    type: 'switch',
    component: 'switch',
    defaultValue: false,
  },
  {
    label: '是否显示',
    key: 'visible',
    type: 'switch',
    component: 'switch',
    defaultValue: true,
  },
  {
    label: '默认值',
    key: 'defaultValue',
    type: 'input',
    component: 'input',
    componentProps: {
      placeholder: '默认值只在生成后的表单中生效',
    },
  },
  {
    label: '校验触发',
    key: 'validateTrigger',
    type: 'select',
    component: 'select',
    dataSource: {
      static: [
        { name: 'onBlur', value: 'onBlur' },
        { name: 'onChange', value: 'onChange' },
      ],
    },
    componentProps: {
      mode: 'multiple',
    },
  },
];
