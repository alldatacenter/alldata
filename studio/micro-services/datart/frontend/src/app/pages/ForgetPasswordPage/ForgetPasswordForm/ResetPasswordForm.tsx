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

import { Button, Form, Input, message } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, useCallback, useState } from 'react';
import { useHistory } from 'react-router';
import {
  getConfirmPasswordValidator,
  getPasswordValidator,
} from 'utils/validators';
import { resetPassword } from '../service';

interface ResetPasswordFormProps {
  token: string;
}
export const ResetPasswordForm: FC<ResetPasswordFormProps> = ({ token }) => {
  const [form] = Form.useForm();
  const history = useHistory();
  const [submiting, setSubmiting] = useState(false);
  const t = useI18NPrefix('forgotPassword');
  const tg = useI18NPrefix('global');

  const onFinish = useCallback(
    values => {
      setSubmiting(true);
      const params = {
        newPassword: values?.newPassword,
        token,
        verifyCode: values?.verifyCode,
      };
      resetPassword(params)
        .then(res => {
          if (res) {
            message.success(t('resetSuccess'));
            history.replace('/login');
          }
        })
        .finally(() => {
          setSubmiting(false);
        });
    },
    [token, history, t],
  );

  return (
    <Form form={form} onFinish={onFinish} size="large">
      <Form.Item
        name="newPassword"
        rules={[
          {
            required: true,
            message: `${t('password')}${tg('validation.required')}`,
          },
          { validator: getPasswordValidator(tg('validation.invalidPassword')) },
        ]}
      >
        <Input.Password placeholder={t('enterNewPassword')} />
      </Form.Item>
      <Form.Item
        name="confirmPassword"
        rules={[
          {
            required: true,
            message: `${t('password')}${tg('validation.required')}`,
          },
          getConfirmPasswordValidator(
            'newPassword',
            tg('validation.invalidPassword'),
            tg('validation.passwordNotMatch'),
          ),
        ]}
      >
        <Input.Password placeholder={t('confirmNewPassword')} />
      </Form.Item>
      <Form.Item
        name="verifyCode"
        rules={[
          {
            required: true,
            message: `${t('verifyCode')}${tg('validation.required')}`,
          },
        ]}
      >
        <Input placeholder={t('verifyCode')} />
      </Form.Item>
      <Button htmlType="submit" loading={submiting} type="primary" block>
        {t('reset')}
      </Button>
    </Form>
  );
};
