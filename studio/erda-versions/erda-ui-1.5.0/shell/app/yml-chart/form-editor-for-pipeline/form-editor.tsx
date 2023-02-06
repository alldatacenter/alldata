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
import { Form } from 'dop/pages/form-editor/index';
import { createFormEditor, DefaultPreview } from 'app/configForm/editor';
import { FormEditor, basicField, previewRenderField } from 'app/configForm/nusi-form/form-editor';
import i18n from 'i18n';
import { map } from 'lodash';
import { componentFormConfig } from './form-editor-config';

// 此处表单预览不需要展示获取配置按钮
const FormPreview = React.forwardRef((props: any, ref: any) => {
  const nameField = {
    index: 0,
    label: i18n.t('field'),
    key: 'key',
    type: 'input',
    component: 'input',
    required: true,
    componentProps: {
      autoComplete: 'off',
      maxLength: 50,
    },
    rules: [
      {
        validator: (v: string) => {
          if (v === 'nodeName') {
            return [false, i18n.t('can not be nodeName')];
          }
          const reg = /^[a-zA-Z0-9_]*$/;
          if (!reg.test(v)) {
            return [false, i18n.t('includes letters, numbers and underscores')];
          }
          const curFields = (ref && ref.current && ref.current.getFields()) || [];
          const keyArr = map(curFields, 'key');
          return [!keyArr.includes(v), i18n.t('{name} already exists', { name: 'key' })];
        },
      },
    ],
  };

  return (
    <DefaultPreview
      {...props}
      Form={Form}
      renderField={previewRenderField}
      ref={ref}
      showGetConfig={false}
      nameField={nameField}
    />
  );
});

export default createFormEditor({ FormEditor, FormPreview, componentMap: componentFormConfig, basicField });
