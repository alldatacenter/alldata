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
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useUpdateEffect, useRequest } from '@/hooks';
import i18n from '@/i18n';
import { useLoadMeta, useDefaultMeta, StreamMetaType } from '@/metas';
import request from '@/utils/request';
import { dataToValues, valuesToData } from './helper';

export interface Props extends ModalProps {
  inlongGroupId: string;
  inlongStreamId?: string;
  record?: Record<string, any>;
  mqType: string;
}

const Comp: React.FC<Props> = ({ inlongGroupId, inlongStreamId, mqType, ...modalProps }) => {
  const [form] = useForm();

  const { defaultValue } = useDefaultMeta('stream');

  const { Entity } = useLoadMeta<StreamMetaType>('stream', defaultValue);

  const entityFields = useMemo(() => {
    return Entity ? new Entity().renderRow() : [];
  }, [Entity]);

  const formContent = useMemo(() => {
    return [
      ...(entityFields || []),
      {
        type: 'inputnumber',
        label: i18n.t('meta.Group.TubeMq.NumberOfAccess'),
        name: 'dailyRecords',
        initialValue: 1,
        isPro: true,
        rules: [{ required: true }],
        suffix: i18n.t('meta.Group.TubeMq.TenThousand/Day'),
        props: {
          min: 1,
          precision: 0,
        },
        visible: mqType === 'PULSAR',
      },
      {
        type: 'inputnumber',
        label: i18n.t('meta.Group.TubeMq.AccessSize'),
        name: 'dailyStorage',
        initialValue: 10,
        isPro: true,
        rules: [{ required: true }],
        suffix: i18n.t('meta.Group.TubeMq.GB/Day'),
        props: {
          min: 1,
          precision: 0,
        },
        visible: mqType === 'PULSAR',
      },
      {
        type: 'inputnumber',
        label: i18n.t('meta.Group.TubeMq.AccessPeakPerSecond'),
        name: 'peakRecords',
        initialValue: 100,
        isPro: true,
        rules: [{ required: true }],
        suffix: i18n.t('meta.Group.TubeMq.Stripe/Second'),
        props: {
          min: 1,
          precision: 0,
        },
        visible: mqType === 'PULSAR',
      },
      {
        type: 'inputnumber',
        label: i18n.t('meta.Group.TubeMq.SingleStripMaximumLength'),
        name: 'maxLength',
        initialValue: 1024,
        isPro: true,
        rules: [{ required: true }],
        suffix: 'Byte',
        props: {
          min: 1,
          precision: 0,
        },
        visible: mqType === 'PULSAR',
      },
    ];
  }, [entityFields, mqType]);

  const { data: savedData, run: getStreamData } = useRequest(
    {
      url: '/stream/get',
      params: {
        groupId: inlongGroupId,
        streamId: inlongStreamId,
      },
    },
    {
      manual: true,
      onSuccess: result => form.setFieldsValue(dataToValues(result)),
    },
  );

  const onOk = async () => {
    const isUpdate = !!inlongStreamId;
    const values = await form.validateFields();
    const submitData = valuesToData(values, inlongGroupId);
    if (isUpdate) {
      submitData.id = savedData?.id;
      submitData.version = savedData?.version;
    }

    await request({
      url: isUpdate ? '/stream/update' : '/stream/save',
      method: 'POST',
      data: submitData,
    });
    await modalProps?.onOk(values);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      form.resetFields(); // Note that it will cause the form to remount to initiate a select request
      if (inlongStreamId) {
        getStreamData();
      }
    }
  }, [modalProps.visible]);

  return (
    <Modal
      {...modalProps}
      title={i18n.t('pages.GroupDetail.Stream.StreamConfigTitle')}
      width={1000}
      onOk={onOk}
    >
      <FormGenerator
        labelCol={{ span: 4 }}
        wrapperCol={{ span: 20 }}
        initialValues={inlongStreamId ? savedData : {}}
        content={formContent}
        form={form}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
