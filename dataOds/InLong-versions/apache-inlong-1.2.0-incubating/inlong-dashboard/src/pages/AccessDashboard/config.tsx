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
import { Link } from 'react-router-dom';
import i18n from '@/i18n';
import { DashTotal, DashToBeAssigned, DashPending, DashRejected } from '@/components/Icons';
import { statusList, genStatusTag } from './status';
import { Button } from 'antd';

export const dashCardList = [
  {
    desc: i18n.t('pages.AccessDashboard.config.Total'),
    dataIndex: 'totalCount',
    icon: <DashTotal />,
  },
  {
    desc: i18n.t('pages.AccessDashboard.config.WaitAssignCount'),
    dataIndex: 'waitAssignCount',
    icon: <DashToBeAssigned />,
  },
  {
    desc: i18n.t('pages.AccessDashboard.config.WaitApproveCount'),
    dataIndex: 'waitApproveCount',
    icon: <DashPending />,
  },
  {
    desc: i18n.t('pages.AccessDashboard.config.Reject'),
    dataIndex: 'rejectCount',
    icon: <DashRejected />,
  },
];

export const getFilterFormContent = defaultValues => [
  {
    type: 'inputsearch',
    name: 'keyword',
    initialValue: defaultValues.keyword,
    props: {
      allowClear: true,
    },
  },
  {
    type: 'select',
    label: i18n.t('basic.Status'),
    name: 'status',
    initialValue: defaultValues.status,
    props: {
      allowClear: true,
      options: statusList,
      dropdownMatchSelectWidth: false,
    },
  },
];

export const getColumns = ({ onDelete, openModal }) => {
  const genCreateUrl = record => `/access/create/${record.inlongGroupId}`;
  const genDetailUrl = record =>
    [0, 100].includes(record.status)
      ? genCreateUrl(record)
      : `/access/detail/${record.inlongGroupId}`;

  return [
    {
      title: i18n.t('pages.AccessDashboard.config.GroupId'),
      dataIndex: 'inlongGroupId',
      render: (text, record) => <Link to={genDetailUrl(record)}>{text}</Link>,
    },
    {
      title: i18n.t('pages.AccessDashboard.config.Name'),
      dataIndex: 'name',
    },
    {
      title: i18n.t('pages.AccessDashboard.config.InCharges'),
      dataIndex: 'inCharges',
    },
    {
      title: i18n.t('basic.CreateTime'),
      dataIndex: 'createTime',
    },
    {
      title: i18n.t('basic.Status'),
      dataIndex: 'status',
      render: text => genStatusTag(text),
    },
    {
      title: i18n.t('basic.Operating'),
      dataIndex: 'action',
      render: (text, record) => (
        <>
          <Button type="link">
            <Link to={genDetailUrl(record)}>{i18n.t('basic.Detail')}</Link>
          </Button>
          {[102].includes(record?.status) && (
            <Button type="link">
              <Link to={genCreateUrl(record)}>{i18n.t('basic.Edit')}</Link>
            </Button>
          )}
          <Button type="link" onClick={() => onDelete(record)}>
            {i18n.t('basic.Delete')}
          </Button>
          {record?.status && (record?.status === 120 || record?.status === 130) && (
            <Button type="link" onClick={() => openModal(record)}>
              {i18n.t('pages.AccessDashboard.config.ExecuteLog')}
            </Button>
          )}
        </>
      ),
    },
  ];
};
