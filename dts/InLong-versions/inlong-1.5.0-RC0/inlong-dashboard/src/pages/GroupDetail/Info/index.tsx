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

import React, { useEffect, useMemo, useImperativeHandle, forwardRef, useState } from 'react';
import { Button, Space, message } from 'antd';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useBoolean, useSelector } from '@/hooks';
import { useTranslation } from 'react-i18next';
import { useDefaultMeta } from '@/metas';
import request from '@/utils/request';
import { State } from '@/models';
import { CommonInterface } from '../common';
import { useFormContent } from './config';

type Props = CommonInterface;

const Comp = ({ inlongGroupId, readonly, isCreate }: Props, ref) => {
  const { t } = useTranslation();
  const [editing, { setTrue, setFalse }] = useBoolean(isCreate);

  const { defaultValue } = useDefaultMeta('consume');

  const [mqType, setMqType] = useState(defaultValue);

  const { userName } = useSelector<State, State>(state => state);

  const [form] = useForm();

  const isUpdate = useMemo(() => {
    return !!inlongGroupId;
  }, [inlongGroupId]);

  const { data, run: getData } = useRequest(`/group/get/${inlongGroupId}`, {
    ready: isUpdate,
    refreshDeps: [inlongGroupId],
    formatResult: data => ({
      ...data,
      inCharges: data.inCharges.split(','),
    }),
    onSuccess: data => {
      form.setFieldsValue(data);
      setMqType(data.mqType);
    },
  });

  const onOk = async () => {
    const values = await form.validateFields();

    const submitData = {
      ...values,
      version: data?.version,
      inCharges: values.inCharges?.join(','),
    };

    if (isUpdate) {
      submitData.inlongGroupId = inlongGroupId;
    }

    const result = await request({
      url: isUpdate ? '/group/update' : '/group/save',
      method: 'POST',
      data: submitData,
    });

    return {
      ...values,
      inlongGroupId: result,
    };
  };

  useEffect(() => {
    const values = {} as Record<string, unknown>;
    if (!isUpdate) {
      if (userName) values.inCharges = [userName];
      form.setFieldsValue(values);
    }
  }, [isUpdate, form, userName]);

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  const onSave = async () => {
    await onOk();
    await getData();
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
    isUpdate,
  });

  return (
    <div style={{ position: 'relative' }}>
      <FormGenerator
        form={form}
        content={formContent}
        initialValues={data}
        onValuesChange={(c, values) => setMqType(values.mqType)}
        useMaxWidth={1400}
        col={12}
      />

      {!isCreate && !readonly && (
        <div>
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
