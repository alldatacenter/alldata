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

import React, { useState, useMemo } from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useUpdateEffect } from '@/hooks';
import request from '@/utils/request';
import { Clusters } from '@/metas/clusters';
import i18n from '@/i18n';

export interface Props extends ModalProps {
  // Require when edit
  id?: string;
}

const Comp: React.FC<Props> = ({ id, ...modalProps }) => {
  const [form] = useForm();

  const [type, setType] = useState(Clusters[0].value);

  const { data: savedData, run: getData } = useRequest(
    id => ({
      url: `/cluster/get/${id}`,
    }),
    {
      manual: true,
      formatResult: result => ({
        ...result,
        inCharges: result.inCharges?.split(','),
        clusterTags: result.clusterTags?.split(','),
      }),
      onSuccess: result => {
        form.setFieldsValue(result);
        setType(result.type);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const isUpdate = id;
    const submitData = {
      ...values,
      inCharges: values.inCharges?.join(','),
      clusterTags: values.clusterTags?.join(','),
    };
    if (isUpdate) {
      submitData.id = id;
      submitData.version = savedData?.version;
    }
    await request({
      url: `/cluster/${isUpdate ? 'update' : 'save'}`,
      method: 'POST',
      data: submitData,
    });
    await modalProps?.onOk(submitData);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      if (id) {
        getData(id);
      }
    } else {
      form.resetFields();
      setType(Clusters[0].value);
    }
  }, [modalProps.visible]);

  const content = useMemo(() => {
    const current = Clusters.find(item => item.value === type);
    return current?.config;
  }, [type]);

  return (
    <Modal
      {...modalProps}
      title={id ? i18n.t('pages.Clusters.Edit') : i18n.t('pages.Clusters.Create')}
      onOk={onOk}
    >
      <FormGenerator
        content={content}
        form={form}
        onValuesChange={(c, values) => setType(values.type)}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
