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
import { Form as NForm } from 'antd';
import { registComponents, Form as FForm } from '../form';
import { groupBy, map, filter } from 'lodash';
import { FormField } from '../form/form';
import { components } from './form-items';
import { FORM_GROUP } from './config';
import { getLabel } from './form-items/common/index';

// export const layout = {
//   labelCol: {
//     xs: { span: 24 },
//     sm: { span: 8 },
//   },
//   wrapperCol: {
//     xs: { span: 24 },
//     sm: { span: 16 },
//   },
// };

// 中间层，接入antd或其他form，把field上的配置映射到组件的字段上
export const renderField =
  (formProps: Obj = {}) =>
  (ComponentMap: any) =>
  ({ fields, form, ...rest }: any) => {
    const groupFields = groupBy(fields, 'group');
    return (
      <NForm {...{ layout: 'vertical', ...formProps }}>
        {fields.map((f: FormField, index: number) => {
          const { component, getComp, group } = f;
          if (group) {
            if (component === FORM_GROUP) {
              const Component = ComponentMap[component];
              return (
                <Component key={f.key || `${index}`} fieldConfig={f} {...rest}>
                  {map(
                    filter(groupFields[group], (i) => i.component !== FORM_GROUP),
                    (gItem, subIndex) => {
                      const { component: subComp, getComp: subGetComp } = gItem;
                      const subCompType = typeof subGetComp === 'function' ? 'customDefined' : subComp;
                      const SubComponent = ComponentMap[subCompType];
                      return SubComponent ? (
                        <SubComponent
                          key={gItem.key || `${index}-${subIndex}`}
                          fieldConfig={gItem}
                          form={form}
                          getLabel={getLabel}
                          {...rest}
                        />
                      ) : null;
                    },
                  )}
                </Component>
              );
            }
            return null;
          } else {
            const compType = typeof getComp === 'function' ? 'customDefined' : component;
            const Component = ComponentMap[compType];
            return Component ? (
              <Component key={f.key || `${index}`} fieldConfig={f} form={form} getLabel={getLabel} {...rest} />
            ) : null;
          }
        })}
      </NForm>
    );
  };

registComponents(components);

export { registComponents };

export const Form = (props: any) => {
  const { formProps, ...rest } = props;

  const _renderField = React.useMemo(() => {
    return renderField(formProps);
  }, [formProps]);

  return <FForm renderField={_renderField} {...rest} />;
};

const { Submit, Reset } = FForm;

Form.Submit = Submit;
Form.Reset = Reset;
