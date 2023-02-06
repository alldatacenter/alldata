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
import { Form } from './form';
import { get } from 'lodash';
import { Form as NForm } from 'antd';
import { createFormEditor, DefaultEditor, DefaultPreview } from '../editor';
import {
  componentFormConfig,
  basicField,
  commonFields,
  rulesField,
  checkWhen,
  registerFormComponent,
  exceptField,
} from './form-items';

import { FORM_GROUP } from './config';

import i18n from 'i18n';
import { getLabel } from './form-items/common/index';
import './form-editor.scss';

export { registerFormComponent, commonFields, basicField, rulesField, checkWhen, exceptField };

export const FormEditor = React.forwardRef((props: any, ref: any) => {
  return <DefaultEditor {...props} Form={Form} ref={ref} />;
});

export const previewRenderField =
  ({ changePos, onEdit, onDelete, onCopy, onAddGroup, currentEditField }: any) =>
  (compMap: any) =>
  ({ fields, form }: any) => {
    return (
      <NForm style={{ marginBottom: 0 }}>
        {fields.map((f: any, index: number) => {
          const Component = compMap[f.component];
          const isOnEdit = get(currentEditField, 'key') === f.key;
          return (
            <div key={`${f.key}`} className={`dice-form-nusi-preview-item ${isOnEdit ? 'on-edit' : ''}`}>
              <div className="form-view">
                <Component key={f.key} getLabel={getLabel} fieldConfig={f} form={form} />
              </div>
              <div className="form-operation">
                <span onClick={() => changePos(index, -1)}>{i18n.t('move up')}</span>
                <span onClick={() => changePos(index, 1)}>{i18n.t('move down')}</span>
                {/* <span onClick={() => onCopy(f)}>{i18n.t('copy')}</span> */}
                <span onClick={() => onEdit(f)}>{i18n.t('edit')}</span>
                <span onClick={() => onDelete(f)}>{i18n.t('delete')}</span>
                {f.component === FORM_GROUP ? <span onClick={() => onAddGroup(f)}>{i18n.t('common:add')}</span> : null}
              </div>
            </div>
          );
        })}
      </NForm>
    );
  };

export const FormPreview = React.forwardRef((props: any, ref: any) => {
  return <DefaultPreview {...props} Form={Form} renderField={previewRenderField} ref={ref} />;
});

export default createFormEditor({ FormEditor, FormPreview, componentMap: componentFormConfig, basicField });
