/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Checkbox, Form, FormInstance, Input, Radio } from 'antd';
import { ModalForm, ModalFormProps } from 'app/components';
import { DateFormat } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import moment from 'moment';
import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { SPACE_XS } from 'styles/StyleConstants';
import { errorHandle } from 'utils/utils';
import { VariableHierarchy } from '../ViewPage/slice/types';
import { VariableScopes, VariableTypes, VariableValueTypes } from './constants';
import { DefaultValue } from './DefaultValue';
import { Variable } from './slice/types';
import { VariableFormModel } from './types';

interface VariableFormProps extends ModalFormProps {
  scope: VariableScopes;
  orgId: string;
  editingVariable: undefined | Variable;
  variables?: VariableHierarchy[];
}

export const VariableForm = memo(
  ({
    scope,
    orgId,
    editingVariable,
    variables,
    visible,
    formProps,
    onSave,
    afterClose,
    ...modalProps
  }: VariableFormProps) => {
    const [type, setType] = useState<VariableTypes>(
      scope === VariableScopes.Public
        ? VariableTypes.Permission
        : VariableTypes.Query,
    );
    const [valueType, setValueType] = useState<VariableValueTypes>(
      VariableValueTypes.String,
    );
    const [expression, setExpression] = useState(false);
    const [dateFormat, setDateFormat] = useState<DateFormat | undefined>();
    const formRef = useRef<FormInstance<VariableFormModel>>();
    const t = useI18NPrefix('variable');
    const tg = useI18NPrefix('global');

    useEffect(() => {
      if (visible && editingVariable) {
        try {
          const { type, valueType, expression, dateFormat } = editingVariable;
          let defaultValue = editingVariable.defaultValue
            ? JSON.parse(editingVariable.defaultValue)
            : [];
          if (valueType === VariableValueTypes.Date && !expression) {
            defaultValue = defaultValue.map(str => moment(str));
          }
          setType(type);
          setValueType(valueType);
          setExpression(expression || false);
          setDateFormat(dateFormat || DateFormat.DateTime);
          formRef.current?.setFieldsValue({
            ...editingVariable,
            defaultValue,
          });
        } catch (error) {
          errorHandle(error);
          throw error;
        }
      }
    }, [visible, editingVariable, formRef]);

    const onAfterClose = useCallback(() => {
      setType(
        scope === VariableScopes.Public
          ? VariableTypes.Permission
          : VariableTypes.Query,
      );
      setValueType(VariableValueTypes.String);
      setExpression(false);
      afterClose && afterClose();
    }, [scope, afterClose]);

    const typeChange = useCallback(e => {
      setType(e.target.value);
    }, []);

    const valueTypeChange = useCallback(e => {
      setValueType(e.target.value);
      formRef.current?.setFieldsValue({ defaultValue: [] });
    }, []);

    const expressionChange = useCallback(e => {
      setExpression(e.target.checked);
      formRef.current?.setFieldsValue({ defaultValue: [] });
    }, []);

    const save = useCallback(
      values => {
        onSave({ ...values, name: values.name.toUpperCase(), dateFormat });
      },
      [onSave, dateFormat],
    );

    const nameValidator = useMemo(
      () =>
        scope === VariableScopes.Private
          ? (_, value) => {
              if (value === editingVariable?.name) {
                return Promise.resolve();
              }
              if (variables?.find(({ name }) => name === value)) {
                return Promise.reject(new Error(t('duplicateName')));
              } else {
                return Promise.resolve();
              }
            }
          : debounce((_, value) => {
              if (!value || value === editingVariable?.name) {
                return Promise.resolve();
              }
              if (!value.trim()) {
                return Promise.reject(
                  `${t('name')}${tg('validation.required')}`,
                );
              }
              const data = { name: value, orgId };
              return fetchCheckName('variables', data);
            }, DEFAULT_DEBOUNCE_WAIT),
      [scope, editingVariable?.name, variables, orgId, t, tg],
    );
    const onChangeDateFormat = useCallback(
      dateFormat => {
        setDateFormat(dateFormat);
        formRef.current?.setFieldsValue({ dateFormat });
      },
      [formRef],
    );
    return (
      <ModalForm
        {...modalProps}
        visible={visible}
        formProps={{
          labelAlign: 'left',
          labelCol: { offset: 1, span: 6 },
          wrapperCol: { span: 16 },
          className: '',
        }}
        onSave={save}
        afterClose={onAfterClose}
        ref={formRef}
        destroyOnClose
      >
        <Form.Item
          name="name"
          label={t('name')}
          validateFirst
          getValueFromEvent={event => event.target.value?.trim()}
          rules={[
            {
              required: true,
              message: `${t('name')}${tg('validation.required')}`,
            },
            {
              validator: nameValidator,
            },
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="label" label={t('label')}>
          <Input />
        </Form.Item>
        <Form.Item name="type" label={t('type')} initialValue={type}>
          <Radio.Group onChange={typeChange}>
            {Object.values(VariableTypes).map(value => (
              <Radio.Button key={value} value={value}>
                {t(`variableType.${value.toLowerCase()}`)}
              </Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>
        <Form.Item
          name="valueType"
          label={t('valueType')}
          initialValue={valueType}
        >
          <Radio.Group onChange={valueTypeChange}>
            {Object.values(VariableValueTypes).map(value => (
              <Radio.Button key={value} value={value}>
                {t(`variableValueType.${value.toLowerCase()}`)}
              </Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>
        {scope === VariableScopes.Public && type === VariableTypes.Permission && (
          <Form.Item
            name="permission"
            label={t('permission.label')}
            initialValue={0}
          >
            <Radio.Group>
              <Radio.Button value={0}>{t('permission.hidden')}</Radio.Button>
              <Radio.Button value={1}>{t('permission.readonly')}</Radio.Button>
              <Radio.Button value={2}>{t('permission.editable')}</Radio.Button>
            </Radio.Group>
          </Form.Item>
        )}
        <Form.Item
          name="defaultValue"
          label={t('defaultValue')}
          css={`
            margin-bottom: ${SPACE_XS};
          `}
        >
          <DefaultValue
            type={valueType}
            expression={expression}
            onChangeDateFormat={onChangeDateFormat}
            dateFormat={dateFormat}
          />
        </Form.Item>

        {valueType !== VariableValueTypes.Expression && (
          <Form.Item
            name="expression"
            label=" "
            colon={false}
            valuePropName="checked"
            initialValue={expression}
          >
            <Checkbox onChange={expressionChange}>{t('expression')}</Checkbox>
          </Form.Item>
        )}
      </ModalForm>
    );
  },
);
