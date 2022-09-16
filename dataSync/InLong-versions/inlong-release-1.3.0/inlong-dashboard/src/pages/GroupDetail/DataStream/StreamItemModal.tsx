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
import { useUpdateEffect, useRequest } from '@/hooks';
import i18n from '@/i18n';
import { groupForm } from '@/metas/group';
import getStreamFields from '@/metas/stream';
import request from '@/utils/request';
import { pickObjectArray } from '@/utils';
import { dataToValues, valuesToData } from './helper';

export interface Props extends ModalProps {
  inlongGroupId: string;
  inlongStreamId?: string;
  record?: Record<string, any>;
  mqType: string;
}

export const genFormContent = (isCreate, mqType) => {
  return [
    ...getStreamFields([
      {
        type: (
          <Divider orientation="left">{i18n.t('pages.GroupDetail.Stream.config.Basic')}</Divider>
        ),
      },
      'inlongStreamId',
      'name',
      'description',
      {
        type: (
          <Divider orientation="left">{i18n.t('pages.GroupDetail.Stream.config.DataInfo')}</Divider>
        ),
      },
      'dataType',
      'dataEncoding',
      'dataSeparator',
      'rowTypeFields',
      {
        type: (
          <Divider orientation="left">{i18n.t('pages.GroupDetail.Stream.config.Scale')}</Divider>
        ),
        visible: mqType === 'PULSAR',
      },
    ]),
    ...pickObjectArray(['dailyRecords', 'dailyStorage', 'peakRecords', 'maxLength'], groupForm).map(
      item => ({
        ...item,
        visible: mqType === 'PULSAR',
      }),
    ),
  ].map(item => {
    const obj = { ...item };

    if (!isCreate && (obj.name === 'inlongStreamId' || obj.name === 'dataType')) {
      obj.type = 'text';
    }

    return obj;
  });
};

const Comp: React.FC<Props> = ({ inlongGroupId, inlongStreamId, mqType, ...modalProps }) => {
  const [form] = useForm();

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
      onSuccess: result => form.setFieldsValue(dataToValues([result])?.[0]),
    },
  );

  const onOk = async () => {
    const values = {
      ...savedData,
      ...(await form.validateFields()),
    };

    const submitData = valuesToData(values ? [values] : [], inlongGroupId);
    await request({
      url: inlongStreamId ? '/stream/update' : '/stream/save',
      method: 'POST',
      data: submitData?.[0],
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
      title={i18n.t('pages.GroupDetail.Stream.StreamItemModal.DataFlowConfiguration')}
      width={1000}
      onOk={onOk}
    >
      <FormGenerator
        labelCol={{ span: 4 }}
        wrapperCol={{ span: 20 }}
        content={genFormContent(!inlongStreamId, mqType)}
        form={form}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
