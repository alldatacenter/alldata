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

import React, { forwardRef, useImperativeHandle } from 'react';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { CommonInterface } from './common';
import { getFormContent } from './ConsumeConfig';

type Props = CommonInterface;

const Comp = (
  { defaultData, isViwer, isAdminStep, isFinished, noExtraForm, suffixContent }: Props,
  ref,
) => {
  const [form] = useForm();

  const onOk = async (useValidate = true) => {
    if (!useValidate) {
      const values = await form.getFieldsValue();
      return values;
    }
    const values = await form.validateFields();
    return values;
  };

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  return (
    Object.keys(defaultData).length && (
      <FormGenerator
        form={form}
        content={getFormContent(
          isViwer,
          isAdminStep,
          isFinished,
          noExtraForm,
          defaultData?.processInfo?.formData,
          suffixContent,
        )}
      />
    )
  );
};

export default forwardRef(Comp);
