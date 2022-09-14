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
import UserSelect from '@/components/UserSelect';
import request from '@/utils/request';

export interface TagDetailModalProps extends ModalProps {
  id?: number;
}

const TagDetailModal: React.FC<TagDetailModalProps> = ({ id, ...modalProps }) => {
  const [form] = useForm();

  const { data: savedData, run: getData } = useRequest(
    id => ({
      url: `/cluster/tag/get/${id}`,
    }),
    {
      manual: true,
      formatResult: result => ({
        ...result,
        inCharges: result.inCharges ? result.inCharges.split(',') : [],
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
      inCharges: values.inCharges?.join(','),
    };
    if (isUpdate) {
      submitData.id = id;
      submitData.version = savedData?.version;
    }
    await request({
      url: `/cluster/tag/${isUpdate ? 'update' : 'save'}`,
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
        label: i18n.t('pages.ClusterTags.Name'),
        name: 'clusterTag',
        rules: [{ required: true }],
        tooltip: i18n.t('pages.ClusterTags.NameEditHelp'),
      },
      {
        type: <UserSelect mode="multiple" currentUserClosable={false} />,
        label: i18n.t('pages.ClusterTags.InCharges'),
        name: 'inCharges',
        rules: [{ required: true }],
      },
      {
        type: 'textarea',
        label: i18n.t('pages.ClusterTags.Description'),
        name: 'description',
      },
    ];
  }, []);

  return (
    <Modal
      {...modalProps}
      title={i18n.t(id ? 'basic.Edit' : 'basic.Create') + i18n.t('pages.ClusterTags.Name')}
      onOk={onOk}
    >
      <FormGenerator content={content} form={form} useMaxWidth />
    </Modal>
  );
};

export default TagDetailModal;
