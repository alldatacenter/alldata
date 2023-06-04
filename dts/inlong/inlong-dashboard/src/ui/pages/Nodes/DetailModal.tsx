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
import { Modal, message, Button } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useUpdateEffect } from '@/ui/hooks';
import { dao } from '@/plugins/nodes';
import { useDefaultMeta, useLoadMeta, NodeMetaType } from '@/plugins';
import i18n from '@/i18n';
import request from '@/core/utils/request';

const { useFindNodeDao, useSaveNodeDao } = dao;

export interface Props extends ModalProps {
  // Require when edit
  id?: string;
  defaultType?: string;
}

const Comp: React.FC<Props> = ({ id, defaultType, ...modalProps }) => {
  const [form] = useForm();

  const { defaultValue } = useDefaultMeta('node');

  const [type, setType] = useState(defaultValue);

  const { data: savedData, run: getData } = useFindNodeDao({
    onSuccess: result => {
      form.setFieldsValue(result);
      setType(result.type);
    },
  });

  const { runAsync: save } = useSaveNodeDao();

  const onOk = async e => {
    const values = await form.validateFields();
    const isUpdate = Boolean(id);
    const data = { ...values };
    if (isUpdate) {
      data.id = id;
      data.version = savedData?.version;
    }
    await save(data);
    await modalProps?.onOk(e);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  const testConnection = async () => {
    const values = await form.validateFields();
    const submitData = {
      ...values,
      inCharges: values.inCharges?.join(','),
      clusterTags: values.clusterTags?.join(','),
    };
    await request({
      url: '/node/testConnection',
      method: 'POST',
      data: submitData,
    });
    message.success(i18n.t('basic.ConnectionSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.open) {
      // open
      if (id) {
        getData(id);
      } else {
        form.setFieldsValue({ type: defaultType });
        setType(defaultType);
      }
    } else {
      form.resetFields();
    }
  }, [modalProps.open]);

  const { Entity } = useLoadMeta<NodeMetaType>('node', type);

  const content = useMemo(() => {
    return Entity ? new Entity().renderRow() : [];
  }, [Entity]);

  return (
    <Modal
      {...modalProps}
      width={720}
      title={id ? i18n.t('basic.Detail') : i18n.t('basic.Create')}
      footer={[
        <Button key="cancel" onClick={e => modalProps.onCancel(e)}>
          {i18n.t('basic.Cancel')}
        </Button>,
        <Button key="save" type="primary" onClick={onOk}>
          {i18n.t('basic.Save')}
        </Button>,
        <Button key="run" type="primary" onClick={testConnection}>
          {i18n.t('pages.Nodes.TestConnection')}
        </Button>,
      ]}
    >
      <FormGenerator
        content={content}
        form={form}
        onValuesChange={(c, values) => setType(values.type)}
        initialValues={id ? savedData : {}}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
