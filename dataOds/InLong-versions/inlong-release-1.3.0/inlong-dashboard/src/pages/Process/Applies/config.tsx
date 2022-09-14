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
import { statusList, genStatusTag } from './status';
import { timestampFormat } from '@/utils';

export const getFilterFormContent = defaultValues => [
  {
    type: 'inputnumber',
    name: 'id',
    props: {
      style: { width: 150 },
      min: 1,
      max: 100000000,
      placeholder: i18n.t('pages.Approvals.ProcessID'),
    },
  },
  {
    type: 'select',
    label: i18n.t('basic.Status'),
    name: 'status',
    initialValue: defaultValues.state,
    props: {
      dropdownMatchSelectWidth: false,
      options: statusList,
      allowClear: true,
    },
  },
];

export const getColumns = activedName => [
  {
    title: i18n.t('pages.Approvals.ProcessID'),
    dataIndex: 'id',
    width: 90,
    render: text => <Link to={`/process/${activedName}/${text}`}>{text}</Link>,
  },
  {
    title: i18n.t('pages.Approvals.ApplicationType'),
    width: 120,
    dataIndex: 'displayName',
  },
  {
    title: i18n.t('pages.Approvals.GroupId'),
    dataIndex: 'inlongGroupId',
    width: 200,
    render: (text, record) => record.showInList?.inlongGroupId,
  },
  {
    title: i18n.t('pages.Approvals.ApplicationTime'),
    dataIndex: 'startTime',
    width: 200,
    render: text => timestampFormat(text),
  },
  {
    title: i18n.t('pages.Approvals.Approver'),
    dataIndex: 'currentTasks',
    width: 220,
    render: text => text?.map(item => item.approvers)?.join(', '),
  },
  {
    title: i18n.t('basic.Status'),
    dataIndex: 'status',
    width: 100,
    render: text => genStatusTag(text),
  },
  {
    title: i18n.t('basic.Operating'),
    dataIndex: 'action',
    width: 100,
    render: (text, record) => (
      <Link to={`/process/${activedName}/${record.id}`}>{i18n.t('basic.Detail')}</Link>
    ),
  },
];
