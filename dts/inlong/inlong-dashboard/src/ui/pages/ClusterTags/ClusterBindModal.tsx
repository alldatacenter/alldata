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

import React, { useCallback } from 'react';
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useUpdateEffect } from '@/ui/hooks';
import i18n from '@/i18n';
import request from '@/core/utils/request';
import { clusters } from '@/plugins/clusters';

export interface Props extends ModalProps {
  clusterTag: string;
}

const Comp: React.FC<Props> = ({ clusterTag, ...modalProps }) => {
  const [form] = useForm();

  const onOk = async () => {
    const values = await form.validateFields();
    const submitData = {
      clusterTag,
      bindClusters: values.clusters,
    };
    await request({
      url: '/cluster/bindTag',
      method: 'POST',
      data: submitData,
    });
    await modalProps?.onOk(values);
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  useUpdateEffect(() => {
    if (modalProps.open) {
      // open
      form.resetFields();
    }
  }, [modalProps.open]);

  const getCreateFormContent = useCallback(
    () => [
      {
        type: 'select',
        label: i18n.t('pages.Clusters.Name'),
        name: 'clusters',
        rules: [{ required: true }],
        props: {
          mode: 'multiple',
          filterOption: false,
          options: {
            requestTrigger: ['onOpen', 'onSearch'],
            requestService: keyword => ({
              url: '/cluster/list',
              method: 'POST',
              data: {
                keyword,
                pageNum: 1,
                pageSize: 20,
              },
            }),
            requestParams: {
              formatResult: result =>
                result?.list?.map(item => ({
                  ...item,
                  label: `${item.displayName === null ? item.name : item.displayName} (${
                    clusters.find(c => c.value === item.type)?.label || item.type
                  })`,
                  value: item.id,
                })),
            },
          },
        },
      },
    ],
    [],
  );

  return (
    <Modal {...modalProps} title={i18n.t('pages.ClusterTags.BindCluster')} onOk={onOk}>
      <FormGenerator content={getCreateFormContent()} form={form} useMaxWidth />
    </Modal>
  );
};

export default Comp;
