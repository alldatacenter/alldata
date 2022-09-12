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

import React, { useState } from 'react';
import { Button, Modal, message } from 'antd';
import HighTable from '@/components/HighTable';
import { PageContainer } from '@/components/PageContainer';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import i18n from '@/i18n';
// import { tableColumns as dataSourcesFileColumns } from './FileConfig';
import { tableColumns as dataSourcesDbColumns } from './DbConfig';
import DataSourcesCreateModal from './CreateModal';
import request from '@/utils/request';
import { timestampFormat } from '@/utils';
import { genStatusTag } from './status';

const getFilterFormContent = defaultValues => [
  {
    type: 'inputsearch',
    name: 'keyword',
  },
  {
    type: 'radiobutton',
    name: 'type',
    label: i18n.t('pages.Datasources.Type'),
    initialValue: defaultValues.type,
    props: {
      buttonStyle: 'solid',
      options: [
        // {
        //   label: 'FILE',
        //   value: 'FILE',
        // },
        {
          label: 'DB',
          value: 'DB',
        },
      ],
    },
  },
];

const Comp: React.FC = () => {
  const [options, setOptions] = useState({
    // keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    type: 'DB',
  });

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const { data, loading, run: getList } = useRequest(
    {
      url: `/commonserver/db/list`,
      method: 'POST',
      params: {
        ...options,
        serverType: options.type,
        type: undefined,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onEdit = ({ id }) => {
    setCreateModal({ visible: true, id });
  };

  const onDelete = ({ id }) => {
    Modal.confirm({
      title: i18n.t('basic.DeleteConfirm'),
      onOk: async () => {
        await request({
          url: `/commonserver/db/deleteById/${id}`,
          method: 'DELETE',
          params: {
            serverType: options.type,
          },
        });
        await getList();
        message.success(i18n.t('basic.DeleteSuccess'));
      },
    });
  };

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

  const columns = (options.type === 'FILE' ? [] : dataSourcesDbColumns).concat([
    {
      title: i18n.t('pages.Datasources.LastModifier'),
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
      title: i18n.t('basic.Status'),
      dataIndex: 'status',
      width: 80,
      render: text => genStatusTag(text),
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
    } as any,
  ]);

  return (
    <PageContainer useDefaultBreadcrumb={false}>
      <HighTable
        filterForm={{
          content: getFilterFormContent(options),
          onFilter,
        }}
        suffix={
          <Button type="primary" onClick={() => setCreateModal({ visible: true })}>
            {i18n.t('pages.Datasources.Create')}
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

      <DataSourcesCreateModal
        {...createModal}
        type={options.type as any}
        visible={createModal.visible as boolean}
        onOk={async values => {
          await getList();
          setCreateModal({ visible: false });
        }}
        onCancel={() => setCreateModal({ visible: false })}
      />
    </PageContainer>
  );
};

export default Comp;
