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

import React, { useEffect, useMemo, forwardRef, useState } from 'react';
import { Divider, Table } from 'antd';
import FormGenerator, { useForm } from '@/ui/components/FormGenerator';
import { useRequest } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import { CommonInterface } from '../common';
import { clusters } from '@/plugins/clusters';

type Props = CommonInterface;

const Comp = ({ inlongGroupId, isCreate }: Props, ref) => {
  const { t } = useTranslation();

  const [form] = useForm();

  const isUpdate = useMemo(() => {
    return !!inlongGroupId;
  }, [inlongGroupId]);

  const { data, run: getData } = useRequest(`/group/detail/${inlongGroupId}`, {
    ready: isUpdate,
    refreshDeps: [inlongGroupId],
    formatResult: data => ({
      ...data,
    }),
  });

  useEffect(() => {
    getData();
  }, [getData]);

  const formContent = item => {
    let content = [];
    if (data[item].constructor === Object) {
      for (const key in data[item]) {
        content.push({
          label: key,
          name: key,
          type: 'text',
          initialValue: data[item][key],
        });
      }
      return content;
    }
  };

  const dividerInfo = data => {
    let info = [];
    for (const item in data) {
      if (data[item] !== null && item !== 'SortInfo' && item !== 'PULSAR') {
        info.push(item);
      }
    }
    return info;
  };

  return (
    <div style={{ position: 'relative' }}>
      {dividerInfo(data).map(item => {
        return (
          <>
            <Divider orientation="left">
              {clusters.find(c => c.value === item)?.label || item}{' '}
              {t('pages.GroupDetail.Resource.Info')}
            </Divider>
            <FormGenerator
              form={form}
              content={formContent(item)}
              initialValues={data}
              useMaxWidth={1400}
              col={12}
            />
          </>
        );
      })}
      {data?.hasOwnProperty('PULSAR') && (
        <>
          <Divider orientation="left" style={{ marginTop: 40 }}>
            Pulsar {t('pages.GroupDetail.Resource.Info')}
          </Divider>
          <Table
            size="small"
            columns={[
              { title: 'Default Tenant', dataIndex: 'defaultTenant' },
              { title: 'Server Url', dataIndex: 'serverUrl' },
              { title: 'Admin Url', dataIndex: 'adminUrl' },
            ]}
            style={{ marginTop: 20 }}
            dataSource={data?.PULSAR}
            pagination={false}
            rowKey="name"
          ></Table>
        </>
      )}
      {data?.hasOwnProperty('SortInfo') && (
        <>
          <Divider orientation="left" style={{ marginTop: 60 }}>
            Sort {t('pages.GroupDetail.Resource.Info')}
          </Divider>
          <Table
            size="small"
            columns={[
              { title: 'inlongStreamId', dataIndex: 'inlongStreamId' },
              { title: 'dataflowId', dataIndex: 'id' },
              { title: 'sinkName', dataIndex: 'sinkName' },
              { title: 'topoName', dataIndex: 'inlongClusterName' },
            ]}
            style={{ marginTop: 20 }}
            dataSource={data?.SortInfo}
            pagination={false}
            rowKey="name"
          ></Table>
        </>
      )}
    </div>
  );
};

export default forwardRef(Comp);
