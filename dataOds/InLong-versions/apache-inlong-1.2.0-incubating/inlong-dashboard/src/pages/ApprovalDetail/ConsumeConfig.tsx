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
import { Divider } from 'antd';
import i18n from '@/i18n';
import { genBasicFields } from '@/components/ConsumeHelper';

const getConsumerContent = initialValues => {
  return genBasicFields(
    [
      {
        type: 'text',
        label: i18n.t('pages.ApprovalDetail.ConsumeConfig.ConsumerGroup'),
        name: 'consumerGroupId',
        initialValue: initialValues.consumerGroupId,
      },
      'inCharges',
      'masterUrl',
      'inlongGroupId',
      'mqExtInfo.isDlq',
      'mqExtInfo.deadLetterTopic',
      'mqExtInfo.isRlq',
      'mqExtInfo.retryLetterTopic',
      {
        type: 'text',
        label: 'Topic',
        name: 'topic',
        initialValue: initialValues.topic,
      },
    ],
    initialValues,
  ).map(item => {
    const obj = { ...item };
    if (typeof obj.suffix !== 'string') {
      delete obj.suffix;
    }
    delete obj.extra;
    delete obj.rules;
    if (typeof obj.type === 'string' || obj.name === 'inlongGroupId') {
      obj.type = 'text';
    } else if (obj.name === 'inCharges') {
      obj.type = <span>{initialValues.inCharges}</span>;
    }

    return obj;
  });
};

export const getFormContent = (
  isViwer: boolean,
  isAdminStep: boolean,
  isFinished: boolean,
  noExtraForm: boolean,
  formData: Record<string, any> = {},
  suffixContent,
) => {
  const array = [
    {
      type: (
        <Divider orientation="left">
          {i18n.t('pages.ApprovalDetail.ConsumeConfig.BasicInfo')}
        </Divider>
      ),
    },
    ...(getConsumerContent(formData.consumptionInfo) || []),
  ];

  const extraForm =
    isAdminStep && !noExtraForm
      ? [
          {
            type: 'input',
            label: i18n.t('pages.ApprovalDetail.ConsumeConfig.ConsumerGroup'),
            name: ['form', 'consumerGroupId'],
            initialValue: formData.consumptionInfo?.consumerGroupId,
            rules: [{ required: true }],
            props: {
              disabled: isFinished,
            },
          },
        ]
      : [];

  return isViwer
    ? array
    : array.concat([
        {
          type: (
            <Divider orientation="left">
              {i18n.t('pages.ApprovalDetail.ConsumeConfig.ApprovalInfo')}
            </Divider>
          ),
        },
        ...extraForm,
        ...suffixContent,
      ]);
};
