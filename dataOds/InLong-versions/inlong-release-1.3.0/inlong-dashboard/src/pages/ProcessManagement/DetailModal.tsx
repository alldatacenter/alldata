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
import UserSelect from '@/components/UserSelect';

export interface Props extends ModalProps {
  id?: number;
}

const content = [
  {
    type: 'input',
    label: i18n.t('pages.ApprovalManagement.ProcessName'),
    name: 'processName',
    rules: [{ required: true }],
  },
  {
    type: 'input',
    label: i18n.t('pages.ApprovalManagement.TaskName'),
    name: 'taskName',
    rules: [{ required: true }],
  },
  {
    type: <UserSelect mode="multiple" />,
    label: i18n.t('pages.ApprovalManagement.Approvers'),
    name: 'approvers',
    rules: [{ required: true }],
  },
];

const Comp: React.FC<Props> = ({ id, ...modalProps }) => {
  const [form] = useForm();

  const { data: savedData, run: getData } = useRequest(
    id => ({
      url: `/workflow/approver/get/${id}`,
    }),
    {
      manual: true,
      formatResult: result => ({
        ...result,
        approvers: result.approvers?.split(','),
      }),
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
      values.version = savedData?.version;
    }
    await request({
      url: isUpdate ? '/workflow/approver/update' : '/workflow/approver/save',
      method: 'POST',
      data: {
        ...values,
        approvers: values.approvers?.join(','),
      },
    });
    await modalProps?.onOk(values);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      id ? getData(id) : form.resetFields();
    }
  }, [modalProps.visible]);

  return (
    <Modal
      {...modalProps}
      title={id ? i18n.t('basic.Edit') : i18n.t('pages.UserManagement.DetailModal.New')}
      onOk={onOk}
    >
      <FormGenerator content={content} form={form} useMaxWidth />
    </Modal>
  );
};

export default Comp;
