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

import React, { useState, forwardRef, useImperativeHandle, useEffect, useMemo } from 'react';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useRequest, useSelector } from '@/hooks';
import { State } from '@/models';
import request from '@/utils/request';
import { getFormContent } from './config';

export interface Props {
  id?: number;
}

const Comp = ({ id }: Props, ref) => {
  const [form] = useForm();

  const { userName } = useSelector<State, State>(state => state);

  const [changedValues, setChangedValues] = useState<Record<string, unknown>>({});

  const isUpdate = useMemo(() => {
    return !!id;
  }, [id]);

  const { data: savedData } = useRequest(
    {
      url: `/consumption/get/${id}`,
    },
    {
      ready: !!id && !Object.keys(changedValues).length,
      refreshDeps: [id],
      formatResult: data => ({
        ...data,
        topic: data.topic.split(','),
        inCharges: data.inCharges?.split(',') || [],
      }),
      onSuccess: data => {
        form.setFieldsValue(data);
        setChangedValues(data);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const data = {
      ...values,
      inCharges: values.inCharges.join(','),
      consumerGroupId: values.consumerGroupName || savedData.consumerGroupId,
      topic: Array.isArray(values.topic) ? values.topic.join(',') : values.topic,
      mqExtInfo: {
        ...values.mqExtInfo,
        mqType: values.mqType,
      },
    };

    if (id) data.id = id;

    const result = await request({
      url: '/consumption/save',
      method: 'POST',
      data,
    });
    await request({
      url: `/consumption/startProcess/${result}`,
      method: 'POST',
      data,
    });
    return result;
  };

  useEffect(() => {
    const values = {} as Record<string, unknown>;
    if (!isUpdate) {
      if (userName) values.inCharges = [userName];
      form.setFieldsValue(values);
      setChangedValues(values);
    }
  }, [isUpdate, form, userName]);

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  return (
    <>
      <FormGenerator
        form={form}
        content={getFormContent({ changedValues })}
        useMaxWidth={800}
        onValuesChange={(c, v) => setChangedValues(prev => ({ ...prev, ...v }))}
        allValues={savedData}
      />
    </>
  );
};

export default forwardRef(Comp);
