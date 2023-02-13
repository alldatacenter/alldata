/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useState } from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useUpdateEffect, useRequest } from '@/hooks';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';

export interface Props extends ModalProps {
  id?: number;
}

const Comp: React.FC<Props> = ({ id, ...modalProps }) => {
  const { t } = useTranslation();
  const [form] = useForm();
  const [userId, setId] = useState(null);
  const [userData, setData] = useState({
    id: null,
    name: '',
    version: null,
    accountType: null,
    validDays: null,
  }) as any;

  const content = [
    {
      type: 'password',
      label: t('components.Layout.NavWidget.Password'),
      name: 'password',
      rules: [{ required: true }],
    },
    {
      type: 'password',
      label: t('components.Layout.NavWidget.NewPassword'),
      name: 'newPassword',
      rules: [{ required: true }],
    },
    {
      type: 'password',
      label: t('components.Layout.NavWidget.ConfirmPassword'),
      name: 'confirmPassword',
      rules: [
        { required: true },
        ({ getFieldValue }) => ({
          validator(_, val) {
            if (val) {
              const newPassword = getFieldValue(['newPassword']);
              return newPassword === val
                ? Promise.resolve()
                : Promise.reject(new Error(t('components.Layout.NavWidget.Remind')));
            }
            return Promise.resolve();
          },
        }),
      ],
    },
  ];

  const { run: getData } = useRequest(
    {
      url: `/user/currentUser`,
      method: 'post',
    },
    {
      manual: true,
      onSuccess: result => {
        setId(result.id);
      },
    },
  );

  const { run: getDays } = useRequest(
    {
      url: `/user/get/${userId}`,
    },
    {
      manual: true,
      onSuccess: result => {
        setData({
          id: result.id,
          name: result.name,
          version: result.version,
          validDays: result.validDays,
          accountType: result.accountType,
        });
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const data = { ...userData, ...values };
    await request({
      url: '/user/update',
      method: 'POST',
      data: data,
    });
    message.success(t('basic.OperatingSuccess'));
    await modalProps?.onOk(data);
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      getData();
    } else {
      form.resetFields();
    }
  }, [modalProps.visible]);

  useUpdateEffect(() => {
    if (userId !== null) {
      getDays();
    }
  }, [userId]);

  return (
    <Modal {...modalProps} title={t('components.Layout.NavWidget.EditPassword')} onOk={onOk}>
      <FormGenerator content={content} form={form} useMaxWidth />
    </Modal>
  );
};

export default Comp;
