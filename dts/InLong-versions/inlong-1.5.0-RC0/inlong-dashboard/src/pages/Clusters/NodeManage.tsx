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

import React, { useCallback, useMemo, useState } from 'react';
import { Button, Modal, message } from 'antd';
import i18n from '@/i18n';
import { parse } from 'qs';
import HighTable from '@/components/HighTable';
import { PageContainer } from '@/components/PageContainer';
import { defaultSize } from '@/configs/pagination';
import { useRequest, useLocation } from '@/hooks';
import NodeEditModal from './NodeEditModal';
import request from '@/utils/request';
import { timestampFormat } from '@/utils';

const getFilterFormContent = defaultValues => [
  {
    type: 'inputsearch',
    name: 'keyword',
  },
];

const Comp: React.FC = () => {
  const location = useLocation();
  const { type, clusterId } = useMemo<Record<string, string>>(
    () => (parse(location.search.slice(1)) as Record<string, string>) || {},
    [location.search],
  );

  const [options, setOptions] = useState({
    keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    type,
    parentId: +clusterId,
  });

  const [nodeEditModal, setNodeEditModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/cluster/node/list',
      method: 'POST',
      data: {
        ...options,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onEdit = ({ id }) => {
    setNodeEditModal({ visible: true, id });
  };

  const onDelete = useCallback(
    ({ id }) => {
      Modal.confirm({
        title: i18n.t('basic.DeleteConfirm'),
        onOk: async () => {
          await request({
            url: `/cluster/node/delete/${id}`,
            method: 'DELETE',
          });
          await getList();
          message.success(i18n.t('basic.DeleteSuccess'));
        },
      });
    },
    [getList],
  );

  const onChange = ({ current: pageNum, pageSize }) => {
    setOptions(prev => ({
      ...prev,
      pageNum,
      pageSize,
    }));
  };

  const onFilter = allValues => {
    setOptions(prev => ({
      ...prev,
      ...allValues,
      pageNum: 1,
    }));
  };

  const pagination = {
    pageSize: +options.pageSize,
    current: +options.pageNum,
    total: data?.total,
  };

  const columns = useMemo(() => {
    return [
      {
        title: 'IP',
        dataIndex: 'ip',
      },
      {
        title: i18n.t('pages.Clusters.Node.Port'),
        dataIndex: 'port',
      },
      {
        title: i18n.t('pages.Clusters.Node.ProtocolType'),
        dataIndex: 'protocolType',
      },
      {
        title: i18n.t('pages.Clusters.Node.LastModifier'),
        dataIndex: 'modifier',
        width: 150,
        render: (text, record: any) => (
          <>
            <div>{text}</div>
            <div>{record.modifyTime && timestampFormat(record.modifyTime)}</div>
          </>
        ),
      },
      {
        title: i18n.t('basic.Operating'),
        dataIndex: 'action',
        width: 120,
        render: (text, record) => (
          <>
            <Button type="link" onClick={() => onEdit(record)}>
              {i18n.t('basic.Edit')}
            </Button>
            <Button type="link" onClick={() => onDelete(record)}>
              {i18n.t('basic.Delete')}
            </Button>
          </>
        ),
      },
    ];
  }, [onDelete]);

  return (
    <PageContainer breadcrumb={[{ name: `${type} ${i18n.t('pages.Clusters.Node.Name')}` }]}>
      <HighTable
        filterForm={{
          content: getFilterFormContent(options),
          onFilter,
        }}
        suffix={
          <Button type="primary" onClick={() => setNodeEditModal({ visible: true })}>
            {i18n.t('pages.Clusters.Node.Create')}
          </Button>
        }
        table={{
          columns,
          rowKey: 'id',
          dataSource: data?.list,
          pagination,
          loading,
          onChange,
        }}
      />

      <NodeEditModal
        type={type}
        clusterId={+clusterId}
        {...nodeEditModal}
        visible={nodeEditModal.visible as boolean}
        onOk={async () => {
          await getList();
          setNodeEditModal({ visible: false });
        }}
        onCancel={() => setNodeEditModal({ visible: false })}
      />
    </PageContainer>
  );
};

export default Comp;
