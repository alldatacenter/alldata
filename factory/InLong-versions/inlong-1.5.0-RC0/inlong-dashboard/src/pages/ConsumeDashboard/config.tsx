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
import { Button } from 'antd';
import { Link } from 'react-router-dom';
import i18n from '@/i18n';
import { DashTotal, DashToBeAssigned, DashPending, DashRejected } from '@/components/Icons';
import { useDefaultMeta, useLoadMeta, ConsumeMetaType } from '@/metas';

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

export const useColumns = ({ onDelete }) => {
  const { defaultValue } = useDefaultMeta('consume');

  const { Entity } = useLoadMeta<ConsumeMetaType>('consume', defaultValue);

  const entityColumns = useMemo(() => {
    return Entity ? new Entity().renderList() : [];
  }, [Entity]);

  const genCreateUrl = record => `/consume/create/${record.id}`;
  const genDetailUrl = record =>
    [0, 10].includes(record.status) ? genCreateUrl(record) : `/consume/detail/${record.id}`;

  return entityColumns
    ?.map(item => {
      if (item.dataIndex === 'consumerGroup') {
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
    ]);
};
