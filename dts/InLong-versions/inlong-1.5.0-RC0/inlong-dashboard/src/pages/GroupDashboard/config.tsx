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

import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import i18n from '@/i18n';
import { DashTotal, DashToBeAssigned, DashPending, DashRejected } from '@/components/Icons';
import { Button } from 'antd';
import { useDefaultMeta, useLoadMeta, GroupMetaType } from '@/metas';

export const dashCardList = [
  {
    desc: i18n.t('pages.GroupDashboard.config.Total'),
    dataIndex: 'totalCount',
    icon: <DashTotal />,
  },
  {
    desc: i18n.t('pages.GroupDashboard.config.WaitAssignCount'),
    dataIndex: 'waitAssignCount',
    icon: <DashToBeAssigned />,
  },
  {
    desc: i18n.t('pages.GroupDashboard.config.WaitApproveCount'),
    dataIndex: 'waitApproveCount',
    icon: <DashPending />,
  },
  {
    desc: i18n.t('pages.GroupDashboard.config.Reject'),
    dataIndex: 'rejectCount',
    icon: <DashRejected />,
  },
];

export const useColumns = ({ onDelete, openModal, onRestart, onStop }) => {
  const { defaultValue } = useDefaultMeta('group');

  const { Entity } = useLoadMeta<GroupMetaType>('group', defaultValue);

  const entityColumns = useMemo(() => {
    return Entity ? new Entity().renderList() : [];
  }, [Entity]);

  const genCreateUrl = record => `/group/create/${record.inlongGroupId}`;
  const genDetailUrl = record =>
    [0, 100].includes(record.status)
      ? genCreateUrl(record)
      : `/group/detail/${record.inlongGroupId}`;

  return entityColumns
    ?.map(item => {
      if (item.dataIndex === 'inlongGroupId') {
        return { ...item, render: (text, record) => <Link to={genDetailUrl(record)}>{text}</Link> };
      }
      return item;
    })
    .concat([
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
              <Button type="link" onClick={() => onRestart(record)}>
                {i18n.t('pages.GroupDashboard.config.Restart')}
              </Button>
            )}
            {record?.status && (record?.status === 120 || record?.status === 130) && (
              <Button type="link" onClick={() => onStop(record)}>
                {i18n.t('pages.GroupDashboard.config.Stop')}
              </Button>
            )}
            {record?.status && (record?.status === 120 || record?.status === 130) && (
              <Button type="link" onClick={() => openModal(record)}>
                {i18n.t('pages.GroupDashboard.config.ExecuteLog')}
              </Button>
            )}
          </>
        ),
      },
    ]);
};
