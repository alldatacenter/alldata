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
import { selectRegisterLoading } from 'app/slice/selectors';
import { register } from 'app/slice/thunks';
import React, { FC, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import { LINE_HEIGHT_ICON_LG } from 'styles/StyleConstants';
import { getPasswordValidator } from 'utils/validators';

interface RegisterFormProps {
  onRegisterSuccess: (email: string) => void;
}
export const RegisterForm: FC<RegisterFormProps> = ({ onRegisterSuccess }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const loading = useSelector(selectRegisterLoading);
  const [form] = Form.useForm();
  const t = useI18NPrefix('register');
  const tg = useI18NPrefix('global');

  const onRegister = useCallback(
    values => {
      dispatch(
        register({
          data: values,
          resolve: () => {
            form.resetFields();
            onRegisterSuccess(values.email);
          },
        }),
      );
    },
    [dispatch, form, onRegisterSuccess],
  );

  const toLogin = useCallback(() => {
    history.push('/login');
  }, [history]);

  return (
    <AuthLayout.Form>
      <Form form={form} onFinish={onRegister}>
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
        <Form.Item className="last" shouldUpdate>
          {() => (
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              loading={loading}
              disabled={
                loading ||
                !form.isFieldsTouched(true) ||
                !!form.getFieldsError().filter(({ errors }) => errors.length)
                  .length
              }
              block
            >
              {t('register')}
            </Button>
          )}
        </Form.Item>
        <Links>
          {t('desc1')}
          <LinkButton onClick={toLogin}>{t('login')}</LinkButton>
        </Links>
      </Form>
    </AuthLayout.Form>
  );
};

const Links = styled.div`
  line-height: ${LINE_HEIGHT_ICON_LG};
  color: ${p => p.theme.textColorLight};
  text-align: right;
`;

const LinkButton = styled.a`
  line-height: ${LINE_HEIGHT_ICON_LG};
`;
