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

import React, { useMemo } from 'react';
import i18n from '@/i18n';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useUpdateEffect } from '@/hooks';
import request from '@/utils/request';

export interface NodeEditModalProps extends ModalProps {
  id?: number;
  type: string;
  clusterId: number;
}

const NodeEditModal: React.FC<NodeEditModalProps> = ({ id, type, clusterId, ...modalProps }) => {
  const [form] = useForm();

  const { data: savedData, run: getData } = useRequest(
    id => ({
      url: `/cluster/node/get/${id}`,
    }),
    {
      manual: true,
      formatResult: result => ({
        ...result,
        // inCharges: result.inCharges.split(','),
      }),
      onSuccess: result => {
        form.setFieldsValue(result);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const isUpdate = id;
    const submitData = {
      ...values,
      type,
      parentId: savedData?.parentId || clusterId,
      // inCharges: values.inCharges?.join(','),
    };
    if (isUpdate) {
      submitData.id = id;
    }
    await request({
      url: `/cluster/node/${isUpdate ? 'update' : 'save'}`,
      method: 'POST',
      data: submitData,
    });
    await modalProps?.onOk(submitData);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      form.resetFields();
      if (id) {
        getData(id);
      }
    }
  }, [modalProps.visible]);

  const content = useMemo(() => {
    return [
      {
        type: 'input',
        label: 'IP',
        name: 'ip',
      },
      {
        type: 'input',
        label: i18n.t('pages.Clusters.Node.Port'),
        name: 'port',
      },
    ];
  }, []);

  return (
    <Modal {...modalProps} title={i18n.t('pages.Clusters.Node.Name')} onOk={onOk}>
      <FormGenerator content={content} form={form} useMaxWidth />
    </Modal>
  );
};

export default NodeEditModal;
