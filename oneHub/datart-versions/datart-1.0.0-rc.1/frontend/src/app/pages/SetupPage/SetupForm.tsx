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

import { Button, Form, Input } from 'antd';
import * as AuthLayout from 'app/components/styles/AuthLayout';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { setup } from 'app/slice/thunks';
import React, { FC, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { getPasswordValidator } from 'utils/validators';

interface RegisterFormProps {
  loading: boolean;
}
export const SetupForm: FC<RegisterFormProps> = ({ loading }) => {
  const dispatch = useDispatch();
  const [form] = Form.useForm();
  const t = useI18NPrefix('setup');
  const tg = useI18NPrefix('global');

  const onSetup = useCallback(
    values => {
      dispatch(setup({ params: values, resolve: () => {} }));
    },
    [dispatch],
  );

  return (
    <AuthLayout.Form>
      <Form form={form} onFinish={onSetup}>
        <Form.Item
          name="username"
          rules={[
            {
              required: true,
              message: `${t('username')}${tg('validation.required')}`,
            },
          ]}
        >
          <Input placeholder={t('username')} size="large" />
        </Form.Item>
        <Form.Item
          name="email"
          rules={[
            {
              required: true,
              message: `${t('email')}${tg('validation.required')}`,
            },
            { type: 'email' },
          ]}
        >
          <Input placeholder={t('email')} type="email" size="large" />
        </Form.Item>
        <Form.Item
          name="password"
          rules={[
            {
              required: true,
              message: `${t('password')}${tg('validation.required')}`,
            },
            {
              validator: getPasswordValidator(tg('validation.invalidPassword')),
            },
          ]}
        >
          <Input.Password placeholder={t('password')} size="large" />
        </Form.Item>
        <Form.Item name="name">
          <Input placeholder={t('name')} size="large" />
        </Form.Item>
        <Form.Item className="last" shouldUpdate>
          {() => (
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              loading={loading}
              disabled={
                loading ||
                !!form.getFieldsError().filter(({ errors }) => errors.length)
                  .length
              }
              block
            >
              {t('initialize')}
            </Button>
          )}
        </Form.Item>
      </Form>
    </AuthLayout.Form>
  );
};
