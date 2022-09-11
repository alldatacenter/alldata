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
import { genBasicFields } from '@/components/ConsumeHelper';
import i18n from '@/i18n';

export const getFormContent = ({ editing, initialValues }) =>
  genBasicFields(
    [
      {
        type: 'text',
        label: i18n.t('pages.ConsumeDetail.Info.config.ConsumerGroupID'),
        name: 'consumerGroupId',
        rules: [{ required: true }],
      },
      'consumerGroupName',
      'inCharges',
      'masterUrl',
      'inlongGroupId',
      'topic',
      'filterEnabled',
      'inlongStreamId',
      'mqExtInfo.isDlq',
      'mqExtInfo.deadLetterTopic',
      'mqExtInfo.isRlq',
      'mqExtInfo.retryLetterTopic',
    ],
    initialValues,
  ).map(item => {
    const obj = { ...item };
    if (typeof obj.suffix !== 'string') {
      delete obj.suffix;
    }
    delete obj.extra;
    if (!editing) {
      if (typeof obj.type === 'string') {
        obj.type = 'text';
      }
      if (obj.name === 'inCharges') {
        obj.type = <span>{initialValues?.inCharges?.join(', ')}</span>;
      }
    }

    if (
      [
        'consumerGroupId',
        'consumerGroupName',
        'inlongGroupId',
        'topic',
        'filterEnabled',
        'inlongStreamId',
      ].includes(obj.name as string)
    ) {
      obj.type = 'text';
    }

    return obj;
  });
