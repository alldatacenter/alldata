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
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import { State } from '@/models';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useUpdateEffect, useSelector } from '@/hooks';
import { useTranslation } from 'react-i18next';
import request from '@/utils/request';
// import { getCreateFormContent as getFileCreateFormContent } from './FileConfig';
import { getCreateFormContent as getDbCreateFormContent } from './DbConfig';

export interface Props extends ModalProps {
  // Type
  type: 'DB' | 'FILE';
  // Required when edit
  id?: string;
}

const Comp: React.FC<Props> = ({ type, id, ...modalProps }) => {
  const { t } = useTranslation();

  const [form] = useForm();

  const { userName } = useSelector<State, State>(state => state);

  const onOk = async e => {
    const values = await form.validateFields();
    const isUpdate = id;
    const submitData = {
      ...values,
      serverType: type,
    };
    if (isUpdate) {
      submitData.id = id;
    }
    if (type === 'DB') {
      submitData.inCharges = values.inCharges.join(',');
    }
    await request({
      url: `/commonserver/db/${isUpdate ? 'update' : 'create'}`,
      method: 'POST',
      data: submitData,
    });
    await modalProps?.onOk(values);
    message.success(t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      form.resetFields();
      if (id) {
        getData(id);
      } else {
        userName && type === 'DB' && form.setFieldsValue({ inCharges: [userName] });
      }
    }
  }, [modalProps.visible]);

  const { data, run: getData } = useRequest(
    id => ({
      url: `/commonserver/db/getById/${id}`,
      params: {
        serverType: type,
      },
    }),
    {
      manual: true,
      onSuccess: result => {
        if (type === 'DB') {
          result.inCharges = result.inCharges.split(',');
        }
        form.setFieldsValue(result);
      },
    },
  );

  const getCreateFormContent = useMemo(() => {
    return {
      DB: getDbCreateFormContent,
      // FILE: getFileCreateFormContent,
    }[type];
  }, [type]);

  return (
    <Modal
      {...modalProps}
      title={`${type} ${t('pages.Datasources.CreateModal.Server')}`}
      onOk={onOk}
    >
      <FormGenerator content={getCreateFormContent(data)} form={form} useMaxWidth />
    </Modal>
  );
};

export default Comp;
