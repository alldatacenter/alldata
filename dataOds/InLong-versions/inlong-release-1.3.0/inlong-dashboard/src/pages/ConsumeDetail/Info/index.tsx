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

import React, { useMemo, useImperativeHandle, forwardRef } from 'react';
import { Button, Space, message } from 'antd';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useBoolean } from '@/hooks';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';
import { CommonInterface } from '../common';
import { getFormContent } from './config';

type Props = CommonInterface;

const Comp = ({ id, readonly, isCreate }: Props, ref) => {
  const { t } = useTranslation();
  const [editing, { setTrue, setFalse }] = useBoolean(false);

  const [form] = useForm();

  const isUpdate = useMemo(() => {
    return !!id;
  }, [id]);

  const { data, run: getDetail } = useRequest(
    {
      url: `/consumption/get/${id}`,
    },
    {
      ready: !!id,
      refreshDeps: [id],
      formatResult: result => ({
        ...result,
        inCharges: result.inCharges?.split(',') || [],
      }),
      onSuccess: data => {
        form.setFieldsValue(data);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const submitData = {
      ...values,
      inCharges: values.inCharges.join(','),
      consumerGroup: values.consumerGroup || data?.consumerGroup,
      topic: Array.isArray(values.topic) ? values.topic.join(',') : values.topic,
      version: data?.version,
      mqExtInfo: {
        ...values.mqExtInfo,
        mqType: values.mqType,
      },
    };

    const result = await request({
      url: isUpdate ? `/consumption/update/${id}` : '/consumption/save',
      method: 'POST',
      data: submitData,
    });
    return result;
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

  return (
    <div style={{ position: 'relative' }}>
      <FormGenerator
        form={form}
        content={getFormContent({
          editing,
          isCreate,
        })}
        allValues={data}
        useMaxWidth={800}
      />

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
