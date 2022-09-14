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
import i18n from '@/i18n';
import StatusTag, { StatusTagProps } from '@/components/StatusTag';
import { ClockCircleFilled } from '@/components/Icons';

type StatusProp = {
  label: string;
  value: string | number;
  type: StatusTagProps['type'];
  icon?: StatusTagProps['icon'];
};

export const statusList: StatusProp[] = [
  {
    label: i18n.t('pages.GroupDetail.Sources.status.Disable'),
    value: 99,
    type: 'warning',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.Normal'),
    value: 101,
    type: 'success',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.Failure'),
    value: 102,
    type: 'error',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.Frozen'),
    value: 104,
    type: 'warning',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.New'),
    value: 110,
    type: 'primary',
    icon: <ClockCircleFilled />,
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.TobeAdd'),
    value: 200,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.TobeDelete'),
    value: 201,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.TobeRetry'),
    value: 202,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.TobeFrozen'),
    value: 204,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.TobeActive'),
    value: 205,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.BeenAdd'),
    value: 300,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.BeenDelete'),
    value: 301,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.BeenRetry'),
    value: 302,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.BeenFrozen'),
    value: 304,
    type: 'default',
  },
  {
    label: i18n.t('pages.GroupDetail.Sources.status.BeenActive'),
    value: 305,
    type: 'default',
  },
];

export const statusMap = statusList.reduce(
  (acc, cur) => ({
    ...acc,
    [cur.value]: cur,
  }),
  {},
);

export const genStatusTag = (value: StatusProp['value']) => {
  const item = statusMap[value] || {};

  return <StatusTag type={item.type || 'default'} title={item.label || value} icon={item.icon} />;
};
