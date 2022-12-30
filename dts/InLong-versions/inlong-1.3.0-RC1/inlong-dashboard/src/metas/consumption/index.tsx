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
import UserSelect from '@/components/UserSelect';
import type { FieldItemType } from '@/metas/common';
import { genFields, genForm, genTable } from '@/metas/common';
import i18n from '@/i18n';
import { timestampFormat } from '@/utils';
import {
  statusList,
  lastConsumerStatusList,
  genStatusTag,
  genLastConsumerStatusTag,
} from './status';
import { consumptionExtends } from './extends';

const consumptionDefault: FieldItemType[] = [
  {
    type: 'input',
    label: i18n.t('meta.Consumption.ConsumerGroupName'),
    name: 'consumerGroup',
    extra: i18n.t('meta.Consumption.ConsumerGroupNameRules'),
    rules: [
      { required: true },
      {
        pattern: /^[0-9a-z_\d]+$/,
        message: i18n.t('meta.Consumption.ConsumerGroupNameRules'),
      },
    ],
    _renderTable: true,
  },
  {
    type: <UserSelect mode="multiple" currentUserClosable={false} />,
    label: i18n.t('meta.Consumption.Owner'),
    name: 'inCharges',
    extra: i18n.t('meta.Consumption.OwnersExtra'),
    rules: [
      {
        required: true,
      },
    ],
    _renderTable: true,
  },
  {
    type: 'select',
    label: i18n.t('meta.Consumption.ConsumerTargetBusinessID'),
    name: 'inlongGroupId',
    extraNames: ['mqType'],
    rules: [{ required: true }],
    props: {
      showSearch: true,
      filterOption: false,
      options: {
        requestTrigger: ['onOpen', 'onSearch'],
        requestService: keyword => ({
          url: '/group/list',
          method: 'POST',
          data: {
            keyword,
            pageNum: 1,
            pageSize: 20,
            status: 130,
          },
        }),
        requestParams: {
          formatResult: result =>
            result?.list?.map(item => ({
              ...item,
              label: `${item.inlongGroupId} (${item.mqType})`,
              value: item.inlongGroupId,
            })),
        },
      },
      onChange: (value, option) => ({
        topic: undefined,
        mqType: option.mqType,
      }),
    },
    _renderTable: true,
  },
  {
    type: 'select',
    label: 'Topic',
    name: 'topic',
    rules: [{ required: true }],
    props: values => ({
      mode: values.mqType === 'PULSAR' ? 'multiple' : '',
      options: {
        requestService: `/group/getTopic/${values.inlongGroupId}`,
        requestParams: {
          formatResult: result =>
            result.mqType === 'TUBEMQ'
              ? [
                  {
                    label: result.mqResource,
                    value: result.mqResource,
                  },
                ]
              : result.streamTopics?.map(item => ({
                  ...item,
                  label: item.mqResource,
                  value: item.mqResource,
                })) || [],
        },
      },
      onChange: (value, option) => {
        if (typeof value !== 'string') {
          return {
            inlongStreamId: option.map(item => item.streamTopics).join(','),
          };
        }
      },
    }),
    visible: values => !!values.inlongGroupId,
    _renderTable: true,
  },
  {
    type: 'text',
    label: 'MQ Type',
    name: 'mqType',
    visible: false,
    _renderTable: true,
  },
  {
    type: 'select',
    label: i18n.t('basic.Status'),
    name: 'status',
    props: {
      allowClear: true,
      options: statusList,
      dropdownMatchSelectWidth: false,
    },
    visible: false,
    _renderTable: {
      render: text => genStatusTag(text),
    },
  },
  {
    type: 'input',
    label: i18n.t('pages.ConsumeDashboard.config.RecentConsumptionTime'),
    name: 'lastConsumptionTime',
    visible: false,
    _renderTable: {
      render: text => text && timestampFormat(text),
    },
  },
  {
    type: 'select',
    label: i18n.t('pages.ConsumeDashboard.config.OperatingStatus'),
    name: 'lastConsumptionStatus',
    props: {
      allowClear: true,
      dropdownMatchSelectWidth: false,
      options: lastConsumerStatusList,
    },
    visible: false,
    _renderTable: {
      render: text => text && genLastConsumerStatusTag(text),
    },
  },
  {
    type: 'radio',
    label: i18n.t('meta.Consumption.filterEnabled'),
    name: 'filterEnabled',
    initialValue: 0,
    props: {
      options: [
        {
          label: i18n.t('meta.Consumption.Yes'),
          value: 1,
        },
        {
          label: i18n.t('meta.Consumption.No'),
          value: 0,
        },
      ],
    },
    rules: [{ required: true }],
    visible: values => !!values.mqType && values.mqType !== 'PULSAR',
  },
  {
    type: 'input',
    label: i18n.t('meta.Consumption.ConsumerDataStreamID'),
    name: 'inlongStreamId',
    extra: i18n.t('meta.Consumption.DataStreamIDsHelp'),
    rules: [{ required: true }],
    visible: values => values.mqType === 'PULSAR' || values.filterEnabled,
  },
  {
    type: 'text',
    label: i18n.t('meta.Consumption.MasterAddress'),
    name: 'masterUrl',
  },
  {
    type: 'radio',
    label: 'isDlq',
    name: 'mqExtInfo.isDlq',
    initialValue: 0,
    rules: [{ required: true }],
    props: {
      options: [
        {
          label: i18n.t('meta.Consumption.Yes'),
          value: 1,
        },
        {
          label: i18n.t('meta.Consumption.No'),
          value: 0,
        },
      ],
    },
    visible: values => values.mqType === 'PULSAR',
  },
  {
    type: 'input',
    label: 'deadLetterTopic',
    name: 'mqExtInfo.deadLetterTopic',
    rules: [{ required: true }],
    visible: values => values.mqExtInfo?.isDlq && values.mqType === 'PULSAR',
  },
  {
    type: 'radio',
    label: 'isRlq',
    name: 'mqExtInfo.isRlq',
    initialValue: 0,
    rules: [{ required: true }],
    props: {
      options: [
        {
          label: i18n.t('meta.Consumption.Yes'),
          value: 1,
        },
        {
          label: i18n.t('meta.Consumption.No'),
          value: 0,
        },
      ],
    },
    visible: values => values.mqExtInfo?.isDlq && values.mqType === 'PULSAR',
  },
  {
    type: 'input',
    label: 'retryLetterTopic',
    name: 'mqExtInfo.retryLetterTopic',
    rules: [{ required: true }],
    visible: values =>
      values.mqExtInfo?.isDlq && values.mqExtInfo?.isRlq && values.mqType === 'PULSAR',
  },
];

export const consumption = genFields(consumptionDefault, consumptionExtends);

export const consumptionForm = genForm(consumption);

export const consumptionTable = genTable(consumption);
