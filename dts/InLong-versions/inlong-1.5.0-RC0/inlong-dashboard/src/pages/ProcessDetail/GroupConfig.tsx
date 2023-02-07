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
import { Divider, Table } from 'antd';
import i18n from '@/i18n';
import { useLoadMeta, GroupMetaType } from '@/metas';

export const useGroupFormContent = ({ mqType = '', isFinished, isViwer }) => {
  const { Entity } = useLoadMeta<GroupMetaType>('group', mqType);

  const entityFields = useMemo(() => {
    return Entity ? new Entity().renderRow() : [];
  }, [Entity]);

  return entityFields?.map(item => {
    const obj = { ...item, col: 12 };

    const canEditSet = new Set([
      'dataReportType',
      'ensemble',
      'writeQuorum',
      'ackQuorum',
      'retentionTime',
      'ttl',
      'retentionSize',
    ]);

    if (!canEditSet.has(obj.name as string) || isFinished || isViwer) {
      obj.type = 'text';
      delete obj.rules;
      delete obj.extra;
      if ((obj.suffix as any)?.type) {
        (obj.suffix as any).type = 'text';
        delete (obj.suffix as any).rules;
      }
    }

    return obj;
  });
};

export const getFormContent = ({
  isViwer,
  formData,
  suffixContent,
  noExtraForm,
  isFinished,
  groupFormContent = [],
}) => {
  const array = [
    {
      type: <Divider orientation="left">{i18n.t('pages.Approvals.Type.Group')}</Divider>,
    },
    ...groupFormContent,
    {
      type: (
        <Divider orientation="left">
          {i18n.t('pages.ApprovalDetail.GroupConfig.DataFlowInformation')}
        </Divider>
      ),
    },
    {
      type: (
        <Table
          size="small"
          columns={[
            { title: 'ID', dataIndex: 'inlongStreamId' },
            { title: 'mqResource', dataIndex: 'mqResource' },
            {
              title: i18n.t('pages.ApprovalDetail.GroupConfig.DataStorages'),
              dataIndex: 'sinkList',
              render: text => text.map(item => item.sinkType).join(','),
            },
          ]}
          dataSource={formData?.streamInfoList || []}
          rowKey="id"
        />
      ),
    },
  ];

  const extraForm = noExtraForm
    ? []
    : [
        {
          type: 'select',
          label: i18n.t('pages.ApprovalDetail.GroupConfig.BindClusterTag'),
          name: ['inlongClusterTag'],
          rules: [{ required: true }],
          props: {
            showSearch: true,
            disabled: isFinished,
            options: {
              requestTrigger: ['onOpen', 'onSearch'],
              requestService: keyword => ({
                url: '/cluster/tag/list',
                method: 'POST',
                data: {
                  keyword,
                  pageNum: 1,
                  pageSize: 20,
                },
              }),
              requestParams: {
                formatResult: result =>
                  result?.list?.map(item => ({
                    ...item,
                    label: item.clusterTag,
                    value: item.clusterTag,
                  })),
              },
            },
          },
        },
      ];

  return isViwer
    ? array
    : array.concat([
        {
          type: (
            <Divider orientation="left">
              {i18n.t('pages.ApprovalDetail.GroupConfig.ApprovalInformation')}
            </Divider>
          ),
        },
        ...extraForm.map(item => ({ ...item, col: 12 })),
        ...suffixContent.map(item => ({ ...item, col: 12 })),
      ]);
};
