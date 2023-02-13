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
import FormEditor, { registerFormComponent } from 'app/configForm/nusi-form/form-editor';
import { Form as NusiForm } from 'app/configForm/nusi-form/form';
import { config as memberSelectorConfig, formConfig as memberSelectorFormConfig } from './form-items/member-selector';
import {
  config as dataSourceSelectorConfig,
  formConfig as dataSourceSelectorFormConfig,
} from './form-items/datasource-selector';
import { config as apiTestConfig, formConfig as apiTestFormConfig } from './form-items/api-test';

const myForms = [
  { key: 'memberSelector', componentConfig: memberSelectorConfig, formConfig: memberSelectorFormConfig },
  { key: 'dataSourceSelector', componentConfig: dataSourceSelectorConfig, formConfig: dataSourceSelectorFormConfig },
  { key: 'apiTest', componentConfig: apiTestConfig, formConfig: apiTestFormConfig },
];

// 项目中自定义组件
registerFormComponent(myForms);

export const Form = (p: Obj) => <NusiForm {...p} />;

const { Submit, Reset } = NusiForm;

Form.Submit = Submit;
Form.Reset = Reset;

export default FormEditor;
export { registerFormComponent };
