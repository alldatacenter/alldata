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

import i18n from 'i18n';

const commonFields = [
  {
    label: i18n.t('field'),
    key: 'key',
    type: 'input',
    component: 'input',
    disabled: true,
  },
  {
    label: i18n.t('component'),
    key: 'component',
    type: 'input',
    component: 'input',
    defaultValue: 'input',
    disabled: true,
  },
  // { // 标签label,在这里等于key，不可修改
  //   label: '标签',
  //   key: 'label',
  //   type: 'input',
  //   component: 'input',
  //   disabled: true,
  // },
  {
    label: i18n.t('label tip'),
    key: 'labelTip',
    type: 'input',
    component: 'input',
    componentProps: {
      maxLength: 50,
    },
  },
  {
    label: i18n.t('is it required'),
    key: 'required',
    type: 'switch',
    component: 'switch',
    defaultValue: true,
  },
  {
    label: i18n.t('default value'),
    key: 'defaultValue',
    type: 'input',
    component: 'input',
    componentProps: {
      placeholder: i18n.t('common:default value only takes effect in the generated form'),
    },
  },
];

export const componentFormConfig = {
  input: {
    name: i18n.t('input'),
    value: 'input',
    dataType: 'string',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: i18n.t('basic configuration'),
        fields: commonFields,
      },
    },
  },
  inputNumber: {
    name: i18n.t('number input'),
    value: 'inputNumber',
    dataType: 'int',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: i18n.t('basic configuration'),
        fields: commonFields,
      },
    },
  },
  switch: {
    name: i18n.t('switch'),
    value: 'switch',
    dataType: 'boolean',
    fieldConfig: {
      basic: {
        key: 'basic',
        name: i18n.t('basic configuration'),
        fields: commonFields,
      },
    },
  },
};
