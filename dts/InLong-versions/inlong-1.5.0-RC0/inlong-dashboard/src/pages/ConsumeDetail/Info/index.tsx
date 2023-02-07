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

import React, { forwardRef, useImperativeHandle, useMemo, useState } from 'react';
import { Button, message, Space, Table } from 'antd';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useBoolean, useRequest } from '@/hooks';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';
import { useDefaultMeta } from '@/metas';
import { CommonInterface } from '../common';
import { useFormContent } from './config';

type Props = CommonInterface;

const Comp = ({ id, readonly, isCreate }: Props, ref) => {
  const { t } = useTranslation();
  const [editing, { setTrue, setFalse }] = useBoolean(false);

  const { defaultValue } = useDefaultMeta('consume');

  const [mqType, setMqType] = useState(defaultValue);

  const [clusterInfos, setClusterInfos] = useState([]);

  const [form] = useForm();

  const isUpdate = useMemo(() => {
    return !!id;
  }, [id]);

  const { data, run: getDetail } = useRequest(`/consume/get/${id}`, {
    ready: isUpdate,
    refreshDeps: [id],
    formatResult: result => ({
      ...result,
      inCharges: result.inCharges?.split(',') || [],
      topic: result.topic?.split(','),
    }),
    onSuccess: data => {
      form.setFieldsValue(data);
      setMqType(data.mqType);
      setClusterInfos(data.clusterInfos);
    },
  });

  const onOk = async () => {
    const values = await form.validateFields();
    const submitData = {
      ...values,
      inCharges: values.inCharges.join(','),
      consumerGroup: values.consumerGroup || data?.consumerGroup,
      topic: values.topic?.join(','),
    };

    if (isUpdate) {
      submitData.id = data?.id;
      submitData.version = data?.version;
    }

    return await request({
      url: isUpdate ? `/consume/update` : '/consume/save',
      method: 'POST',
      data: submitData,
    });
  };

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  const onSave = async () => {
    await onOk();
    await getDetail();
    setFalse();
    message.success(t('basic.OperatingSuccess'));
  };

  const onCancel = () => {
    form.setFieldsValue(data);
    setFalse();
  };

  const formContent = useFormContent({
    mqType,
    editing,
    isCreate,
  });

  return (
    <div style={{ position: 'relative' }}>
      <FormGenerator
        form={form}
        content={formContent}
        initialValues={data}
        onValuesChange={(c, values) => setMqType(values.mqType)}
        useMaxWidth={800}
      />
      {!isCreate && <label>{t('pages.ConsumeDetail.ClusterInfo')}</label>}
      {!isCreate && (
        <Table
          size="small"
          columns={[
            { title: 'name', dataIndex: 'name' },
            { title: 'type', dataIndex: 'type' },
            { title: 'serviceUrl', dataIndex: 'url' },
            { title: 'adminUrl', dataIndex: 'adminUrl' },
          ]}
          style={{ marginTop: 20 }}
          dataSource={clusterInfos}
          rowKey="name"
        ></Table>
      )}

      {!isCreate && !readonly && (
        <div style={{ position: 'absolute', top: 0, right: 0 }}>
          {editing ? (
            <Space>
              <Button type="primary" onClick={onSave}>
                {t('basic.Save')}
              </Button>
              <Button onClick={onCancel}>{t('basic.Cancel')}</Button>
            </Space>
          ) : (
            <Button type="primary" onClick={setTrue}>
              {t('basic.Edit')}
            </Button>
          )}
        </div>
      )}
    </div>
  );
};

export default forwardRef(Comp);
