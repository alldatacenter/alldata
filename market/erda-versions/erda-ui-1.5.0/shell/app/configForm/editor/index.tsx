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
import { map, cloneDeep, get, filter, isEmpty } from 'lodash';
import { FormEditor as DefaultEditor } from './form-editor';
import { FormPreview as DefaultPreview } from './form-preview';
import { basicField as defaultBasicField, componentMap as defaultComponentMap } from './config';
import { FORM_GROUP } from '../nusi-form/config';
import './index.scss';

const defaultProps = {
  FormEditor: DefaultEditor,
  FormPreview: DefaultPreview,
  componentMap: defaultComponentMap,
  basicField: defaultBasicField,
};

export { DefaultEditor, DefaultPreview, defaultComponentMap };

export interface IEditorProps {
  fields?: IField[];
}

export const createFormEditor = (props: any = defaultProps) =>
  React.forwardRef((p: IEditorProps, ref: any) => {
    const { FormEditor, FormPreview, componentMap, basicField = defaultBasicField } = props;
    const formRef = React.useRef(null as any);
    const [chosenField, setChosenField] = React.useState({} as IField);

    const { fields = [] } = p;

    React.useEffect(() => {
      ref && (ref.current = formRef.current);
    }, [ref]);

    React.useEffect(() => {
      if (formRef && formRef.current && !isEmpty(fields)) {
        formRef.current.setFields(fields);
        setChosenField(fields[0]);
      }
    }, [fields]);

    const addFormField = (data: any) => {
      if (formRef && formRef.current) {
        const curFields = formRef.current.getFields();
        const newField =
          data.component === FORM_GROUP ? { ...data, group: data.key } : { ...basicField, ...data, label: data.key };
        formRef.current.setFields(curFields.concat(newField));
        setChosenField(newField);
      }
    };

    const copyField = (data: any, key: string) => {
      if (formRef && formRef.current) {
        const curFields = formRef.current.getFields();
        const newField = { ...basicField, ...data, key };
        formRef.current.setFields(curFields.concat(newField));
        setChosenField(newField);
      }
    };

    const updateField = (d: any = {}, curField: IField) => {
      if (formRef && formRef.current) {
        const curFields = formRef.current.getFields();
        const newConfig = map(curFields, (c: IField) => {
          if (c.key !== curField.key) return c;
          const reC = { ...c, ...d };
          return reC;
        });
        formRef.current.setFields(newConfig);
      }
    };

    const changeFieldPos = (from: number, to: number) => {
      if (formRef && formRef.current) {
        const toPos = from + to;
        const curFields = formRef.current.getFields();

        if (toPos >= 0 && toPos < curFields.length) {
          const newFields = cloneDeep(curFields);
          const fieldItem = newFields.splice(from, 1);
          newFields.splice(toPos, 0, fieldItem[0]);
          formRef.current.setFields(newFields);
        }
      }
    };

    const deleteField = (field: IField) => {
      if (formRef && formRef.current) {
        const curFields = formRef.current.getFields();
        const { component } = field;
        const newFields = filter(curFields, (item: IField) =>
          component === FORM_GROUP ? item.group !== field.key : item.key !== field.key,
        );
        if (field.key === chosenField.key) {
          setChosenField(newFields[0] || ({} as IField));
        }
        formRef.current.setFields(newFields);
      }
    };

    const editField = (field: IField) => {
      setChosenField(field);
    };

    const allField = (formRef && formRef.current && formRef.current.getFields()) || [];

    return (
      <div className="dice-form-configuration h-full">
        <div className="form-preview-box">
          <FormPreview
            ref={formRef}
            componentMap={componentMap}
            addFormField={addFormField}
            copyField={copyField}
            deleteField={deleteField}
            editField={editField}
            changeFieldPos={changeFieldPos}
            updateField={updateField}
            currentEditField={chosenField}
          />
        </div>
        <div className="form-editor-box">
          <FormEditor
            field={chosenField}
            allField={allField}
            onChange={updateField}
            fieldConfig={get(componentMap, `${chosenField.component}.fieldConfig`, [])}
          />
        </div>
      </div>
    );
  });
