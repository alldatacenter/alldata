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
import { Divider, Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useUpdateEffect } from '@/hooks';
import i18n from '@/i18n';
import { genBusinessFields, genDataFields } from '@/components/AccessHelper';
import request from '@/utils/request';
import { valuesToData } from './helper';
import { pickObject } from '@/utils';

export interface Props extends ModalProps {
  inlongGroupId: string;
  record?: Record<string, any>;
  mqType: string;
}

export const genFormContent = (currentValues, inlongGroupId, mqType) => {
  const extraParams = {
    inlongGroupId,
  };

  return [
    ...genDataFields(
      [
        {
          type: (
            <Divider orientation="left">
              {i18n.t('pages.AccessCreate.DataStream.config.Basic')}
            </Divider>
          ),
        },
        'inlongStreamId',
        'name',
        'description',
        {
          type: (
            <Divider orientation="left">
              {i18n.t('pages.AccessCreate.DataStream.config.DataInfo')}
            </Divider>
          ),
        },
        'dataType',
        'rowTypeFields',
        {
          type: (
            <Divider orientation="left">
              {i18n.t('pages.AccessCreate.Business.config.AccessScale')}
            </Divider>
          ),
          visible: mqType === 'PULSAR',
        },
      ],
      currentValues,
      extraParams,
    ),
    ...genBusinessFields(['dailyRecords', 'dailyStorage', 'peakRecords', 'maxLength']).map(
      item => ({
        ...item,
        visible: mqType === 'PULSAR',
      }),
    ),
  ].map(item => {
    const obj = { ...item };

    if (obj.name === 'inlongStreamId' || obj.name === 'dataSourceType' || obj.name === 'dataType') {
      obj.type = 'text';
    }

    return obj;
  });
};

const Comp: React.FC<Props> = ({ inlongGroupId, record, mqType, ...modalProps }) => {
  const [form] = useForm();
  const onOk = async () => {
    const values = {
      ...pickObject(['id', 'inlongGroupId', 'inlongStreamId', 'dataSourceBasicId'], record),
      ...(await form.validateFields()),
    };

    const data = valuesToData(values ? [values] : [], inlongGroupId);
    const submitData = data.map(item =>
      pickObject(['dbBasicInfo', 'fileBasicInfo', 'streamInfo'], item),
    );
    await request({
      url: '/stream/update',
      method: 'POST',
      data: submitData?.[0]?.streamInfo,
    });
    await modalProps?.onOk(values);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      form.resetFields(); // Note that it will cause the form to remount to initiate a select request
    }
    if (Object.keys(record || {})?.length) {
      form.setFieldsValue(record);
    }
  }, [modalProps.visible]);

  return (
    <Modal
      {...modalProps}
      title={i18n.t('pages.AccessDetail.DataStream.StreamItemModal.DataFlowConfiguration')}
      width={1000}
      onOk={onOk}
    >
      <FormGenerator
        labelCol={{ span: 4 }}
        wrapperCol={{ span: 20 }}
        content={genFormContent(record, inlongGroupId, mqType)}
        form={form}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
