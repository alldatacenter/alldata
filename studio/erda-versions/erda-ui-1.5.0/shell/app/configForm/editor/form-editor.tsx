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
import { Form as DefaultForm } from '../form';
import { isEmpty, map, reduce, filter } from 'lodash';
import i18n from 'i18n';
import { Tabs } from './common';

import './form-editor.scss';

interface IProps {
  field: IField;
  allField: IField[];
  fieldConfig: any;
  onChange: (data: any, field: IField) => void;
  Form?: any;
}

export const FormEditor = React.forwardRef((props: IProps, ref: any) => {
  const { field, allField, fieldConfig = {}, onChange, Form = DefaultForm } = props;
  const form = React.useRef();

  React.useEffect(() => {
    ref && (ref.current = form.current);
  }, [ref]);

  const onFieldChange = (data: any) => {
    onChange(data, field);
  };

  // 整合一份总的field传递给Form，同时给field通过category分类，在Tab中使用
  const fields = reduce(
    fieldConfig,
    (sum: any[], item, fKey) => {
      return sum.concat(map(item.fields, (fItem) => ({ ...fItem, category: fKey })));
    },
    [],
  );

  return (
    <div className="dice-form-editor">
      <h4>{i18n.t('common:form edit')}</h4>
      <div className="content">
        {isEmpty(field) ? (
          <div className="tip">
            <div className="tip-title mb-4">{i18n.t('common:how to edit a form')}</div>
            <div className="tip-desc mb-4">1、{i18n.t('common:add form items on the left')}</div>
            <div className="tip-desc mb-4">2、{i18n.t('common:click edit to complete the form')}</div>
          </div>
        ) : (
          <Form
            key={field.key}
            value={field}
            formRef={form}
            onChange={onFieldChange}
            fields={fields}
            formRender={({ RenderFields, form: formRef, fields: totalFields }: any) => {
              return (
                <Tabs
                  tabs={map(fieldConfig, (item) => {
                    // tab根据fieldConfig分类，再根据key对应上面注入的category收集对应分类下的field，这样达到同一个formRef控制多个RenderField
                    const { key, name } = item;
                    const curField = filter(totalFields, (fItem) => fItem.category === key);
                    return {
                      key,
                      name,
                      content: <RenderFields key={key} allField={allField} form={formRef} fields={curField} />,
                    };
                  })}
                />
              );
            }}
          />
        )}
      </div>
    </div>
  );
});
