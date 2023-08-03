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

import React, { useMemo, useImperativeHandle, forwardRef, useState } from 'react';
import { Button, Space, message } from 'antd';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useRequest, useBoolean } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import { useDefaultMeta } from '@/plugins';
import request from '@/core/utils/request';
import { useFormContent } from './config';
import { CommonInterface } from '../common';

type Props = CommonInterface;

const Comp = ({ inlongGroupId, inlongStreamId, readonly, isCreate }: Props, ref) => {
  const { t } = useTranslation();
  const [editing, { setTrue, setFalse }] = useBoolean(isCreate);

  const { defaultValue } = useDefaultMeta('sync');

  const [mqType, setMqType] = useState(defaultValue);

  const [form] = useForm();

  const isUpdate = useMemo(() => {
    return !!inlongGroupId;
  }, [inlongGroupId]);

  const isUpdateStream = useMemo(() => {
    return !!inlongStreamId;
  }, [inlongStreamId]);

  const { data, run: getData } = useRequest(`/group/get/${inlongGroupId}`, {
    ready: isUpdate,
    refreshDeps: [inlongGroupId],
    formatResult: data => ({
      ...data,
      inCharges: data.inCharges.split(','),
    }),
    onSuccess: data => {
      setMqType(data.mqType);
      form.setFieldsValue(data);
    },
  });
  const { data: streamDetail, run: getStreamDetail } = useRequest(
    streamId => ({
      url: `/stream/getBrief`,
      params: {
        groupId: inlongGroupId,
        streamId,
      },
    }),
    {
      manual: true,
    },
  );
  const { data: streamData, run: getDataStream } = useRequest(
    {
      url: '/stream/list',
      method: 'POST',
      data: {
        inlongGroupId,
      },
    },
    {
      ready: isUpdateStream,
      refreshDeps: [inlongGroupId],
      onSuccess: result => {
        getStreamDetail(result.list[0].inlongStreamId);
        form.setFieldValue('inlongStreamId', result.list[0].inlongStreamId);
        form.setFieldValue('streamName', result.list[0].name);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();

    const submitData = {
      ...values,
      version: data?.version,
      inCharges: values.inCharges?.join(','),
      inlongGroupMode: 1,
    };

    const submitDataStream = {
      inlongGroupId: values.inlongGroupId,
      inlongStreamId: values.inlongStreamId,
      name: values.streamName,
      fieldList: streamDetail?.fieldList,
    };
    if (streamData !== undefined) {
      submitDataStream['version'] = streamData?.list[0].version;
    }

    if (isUpdate) {
      submitData.inlongGroupId = inlongGroupId;
      submitData.inlongStreamId = inlongStreamId;
    }

    const result = await request({
      url: isUpdate ? '/group/update' : '/group/save',
      method: 'POST',
      data: submitData,
    });

    const resultStream = await request({
      url: isUpdateStream ? '/stream/update' : '/stream/save',
      method: 'POST',
      data: submitDataStream,
    });

    return {
      ...values,
      inlongGroupId: result,
      inlongStreamId: resultStream,
    };
  };

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  const onSave = async () => {
    await onOk();
    await getData();
    await getDataStream();
    setFalse();
    message.success(t('basic.OperatingSuccess'));
  };

  const onCancel = () => {
    form.setFieldsValue(data);
    // form.setFieldsValue(dataStream);
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
