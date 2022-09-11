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
import { Button } from 'antd';
import { Link } from 'react-router-dom';
import i18n from '@/i18n';
import { DashTotal, DashToBeAssigned, DashPending, DashRejected } from '@/components/Icons';
import {
  statusList,
  genStatusTag,
  lastConsumerStatusList,
  genLastConsumerStatusTag,
} from './status';
import { timestampFormat } from '@/utils';

export const dashCardList = [
  {
    desc: i18n.t('pages.ConsumeDashboard.config.Total'),
    dataIndex: 'totalCount',
    icon: <DashTotal />,
  },
  {
    desc: i18n.t('pages.ConsumeDashboard.config.Pending'),
    dataIndex: 'waitingAssignCount',
    icon: <DashToBeAssigned />,
  },
  {
    desc: i18n.t('pages.ConsumeDashboard.config.BeAllocated'),
    dataIndex: 'waitingApproveCount',
    icon: <DashPending />,
  },
  {
    desc: i18n.t('pages.ConsumeDashboard.config.Reject'),
    dataIndex: 'rejectedCount',
    icon: <DashRejected />,
  },
];

export const getFilterFormContent = defaultValues =>
  [
    {
      type: 'inputsearch',
      name: 'keyword',
    },
    {
      type: 'select',
      label: i18n.t('pages.ConsumeDashboard.config.ApplicationStatus'),
      name: 'status',
      initialValue: defaultValues.status,
      props: {
        dropdownMatchSelectWidth: false,
        options: statusList,
      },
    },
    {
      type: 'select',
      label: i18n.t('pages.ConsumeDashboard.config.OperatingStatus'),
      name: 'lastConsumerStatus',
      initialValue: defaultValues.lastConsumerStatusList,
      props: {
        dropdownMatchSelectWidth: false,
        options: lastConsumerStatusList,
      },
    },
  ].map(item => {
    if (item.type === 'radio' || item.type === 'select') {
      return {
        type: 'select',
        label: item.label,
        name: item.name,
        initialValue: defaultValues[item.name as string],
        props: {
          allowClear: true,
          options: item.props.options,
          dropdownMatchSelectWidth: false,
        },
      };
    }
    return item;
  });

export const getColumns = ({ onDelete }) => {
  const genCreateUrl = record => `/consume/create?id=${record.id}`;
  const genDetailUrl = record =>
    record.status === 10 ? genCreateUrl(record) : `/consume/detail/${record.id}`;

  return [
    {
      title: 'Topic',
      dataIndex: 'topic',
      width: 180,
      render: (text, record) => <Link to={genDetailUrl(record)}>{text}</Link>,
    },
    {
      title: i18n.t('pages.ConsumeDashboard.config.ConsumerGroup'),
      dataIndex: 'consumerGroupId',
      width: 180,
    },
    {
      title: i18n.t('pages.ConsumeDashboard.config.Middleware'),
      dataIndex: 'mqType',
      width: 120,
    },
    {
      title: i18n.t('pages.ConsumeDashboard.config.ConsumptionInlongGroupId'),
      dataIndex: 'inlongGroupId',
    },
    {
      title: i18n.t('pages.ConsumeDashboard.config.RecentConsumptionTime'),
      dataIndex: 'lastConsumerTime',
      render: text => text && timestampFormat(text),
    },
    {
      title: i18n.t('pages.ConsumeDashboard.config.ApplicationStatus'),
      dataIndex: 'status',
      render: text => genStatusTag(text),
      width: 120,
    },
    {
      title: i18n.t('pages.ConsumeDashboard.config.OperatingStatus'),
      dataIndex: 'lastConsumerStatus',
      render: text => text && genLastConsumerStatusTag(text),
      width: 120,
    },
    {
      title: i18n.t('basic.Operating'),
      dataIndex: 'action',
      render: (text, record) => (
        <>
          <Button type="link">
            <Link to={genDetailUrl(record)}>{i18n.t('basic.Detail')}</Link>
          </Button>
          {[20, 22].includes(record?.status) && (
            <Button type="link">
              <Link to={genCreateUrl(record)}>{i18n.t('basic.Edit')}</Link>
            </Button>
          )}
          <Button type="link" onClick={() => onDelete(record)}>
            {i18n.t('basic.Delete')}
          </Button>
        </>
      ),
    },
  ];
};
