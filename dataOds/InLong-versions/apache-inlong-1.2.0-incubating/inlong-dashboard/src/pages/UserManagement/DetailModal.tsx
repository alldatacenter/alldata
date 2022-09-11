/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import React from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import i18n from '@/i18n';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useUpdateEffect, useRequest } from '@/hooks';
import request from '@/utils/request';

export interface Props extends ModalProps {
  id?: number;
}

const content = [
  {
    type: 'radio',
    label: i18n.t('pages.UserManagement.config.AccountRole'),
    name: 'type',
    initialValue: 1,
    rules: [{ required: true }],
    props: {
      options: [
        {
          label: i18n.t('pages.UserManagement.config.GeneralUser'),
          value: 1,
        },
        {
          label: i18n.t('pages.UserManagement.config.Admin'),
          value: 0,
        },
      ],
    },
  },
  {
    type: 'input',
    label: i18n.t('pages.UserManagement.config.UserName'),
    name: 'username',
    rules: [{ required: true }],
  },
  {
    type: 'password',
    label: i18n.t('pages.UserManagement.DetailModal.UserPassword'),
    name: 'password',
    rules: [{ required: true }],
  },
  {
    type: 'inputnumber',
    label: i18n.t('pages.UserManagement.DetailModal.EffectiveTime'),
    name: 'validDays',
    suffix: i18n.t('pages.UserManagement.DetailModal.Day'),
    rules: [{ required: true }],
    props: {
      min: 1,
    },
  },
];

const Comp: React.FC<Props> = ({ id, ...modalProps }) => {
  const [form] = useForm();

  const { run: getData } = useRequest(
    id => ({
      url: `/user/get/${id}`,
    }),
    {
      manual: true,
      onSuccess: result => {
        form.setFieldsValue(result);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const isUpdate = id;
    if (isUpdate) {
      values.id = id;
    }
    await request({
      url: isUpdate ? '/user/update' : '/user/register',
      method: 'POST',
      data: values,
    });
    await modalProps?.onOk(values);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      id ? getData(id) : form.resetFields();
    }
  }, [modalProps.visible]);

  return (
    <Modal
      {...modalProps}
      title={id ? i18n.t('basic.Edit') : i18n.t('pages.UserManagement.DetailModal.New')}
      onOk={onOk}
    >
      <FormGenerator
        content={id ? content.filter(item => item.name !== 'password') : content}
        form={form}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
