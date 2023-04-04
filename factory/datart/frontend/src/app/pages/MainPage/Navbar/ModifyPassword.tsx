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

import { Button, Form, Input, message, Modal } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectLoggedInUser,
  selectModifyPasswordLoading,
} from 'app/slice/selectors';
import { modifyAccountPassword } from 'app/slice/thunks';
import { ModifyUserPassword } from 'app/slice/types';
import { FC, useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  getConfirmPasswordValidator,
  getPasswordValidator,
} from 'utils/validators';
const FormItem = Form.Item;
interface ModifyPasswordProps {
  visible: boolean;
  onCancel: () => void;
}
export const ModifyPassword: FC<ModifyPasswordProps> = ({
  visible,
  onCancel,
}) => {
  const dispatch = useDispatch();
  const loggedInUser = useSelector(selectLoggedInUser);
  const loading = useSelector(selectModifyPasswordLoading);
  const [form] = Form.useForm();
  const t = useI18NPrefix('main.nav.account.changePassword');
  const tg = useI18NPrefix('global');

  const reset = useCallback(() => {
    form.resetFields();
  }, [form]);

  useEffect(() => {
    if (visible) {
      reset();
    }
  }, [visible, reset]);

  const formSubmit = useCallback(
    ({ confirmPassword, ...params }: ModifyUserPassword) => {
      dispatch(
        modifyAccountPassword({
          params,
          resolve: () => {
            message.success(tg('operation.updateSuccess'));
            onCancel();
          },
        }),
      );
    },
    [dispatch, onCancel, tg],
  );

  return (
    <Modal
      title={t('title')}
      footer={false}
      visible={visible}
      onCancel={onCancel}
      afterClose={reset}
    >
      <Form<ModifyUserPassword>
        form={form}
        initialValues={loggedInUser || void 0}
        labelCol={{ span: 7 }}
        wrapperCol={{ span: 12 }}
        onFinish={formSubmit}
      >
        <FormItem
          label={t('oldPassword')}
          name="oldPassword"
          rules={[
            {
              required: true,
              message: `${t('oldPassword')}${tg('validation.required')}`,
            },
            {
              validator: getPasswordValidator(tg('validation.invalidPassword')),
            },
          ]}
        >
          <Input.Password type="password" />
        </FormItem>
        <FormItem
          label={t('newPassword')}
          name="newPassword"
          rules={[
            {
              required: true,
              message: `${t('newPassword')}${tg('validation.required')}`,
            },
            {
              validator: getPasswordValidator(tg('validation.invalidPassword')),
            },
          ]}
        >
          <Input.Password type="password" />
        </FormItem>
        <FormItem
          label={t('confirmPassword')}
          name="confirmPassword"
          dependencies={['newPassword']}
          rules={[
            {
              required: true,
              message: `${t('confirmPassword')}${tg('validation.required')}`,
            },
            getConfirmPasswordValidator(
              'newPassword',
              tg('validation.invalidPassword'),
              tg('validation.passwordNotMatch'),
            ),
          ]}
        >
          <Input.Password type="password" placeholder="" />
        </FormItem>
        <Form.Item wrapperCol={{ offset: 7, span: 12 }}>
          <Button type="primary" htmlType="submit" loading={loading} block>
            {tg('button.save')}
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
};
