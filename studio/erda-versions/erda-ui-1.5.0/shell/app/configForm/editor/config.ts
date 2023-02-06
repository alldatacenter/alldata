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
};

export const commonFields = [
  {
    index: 0,
    label: '标签',
    key: 'label',
    type: 'input',
    component: 'input',
  },
  {
    index: 1,
    label: '组件',
    key: 'component',
    type: 'select',
    component: 'select',
    disabled: true,
    dataSource: {
      static: [
        { name: '输入框', value: 'input' },
        { name: '选择框', value: 'select' },
      ],
    },
  },
  {
    index: 2,
    label: '字段名',
    key: 'key',
    type: 'input',
    component: 'input',
  },
  {
    index: 3,
    label: 'placeholder',
    key: 'componentProps.placeholder',
    type: 'input',
    component: 'input',
  },
];

export const componentMap = {
  input: {
    name: '输入框',
    value: 'input',
    fieldConfig: [...commonFields],
  },
  select: {
    name: '选择框',
    value: 'select',
    fieldConfig: [...commonFields],
  },
};
