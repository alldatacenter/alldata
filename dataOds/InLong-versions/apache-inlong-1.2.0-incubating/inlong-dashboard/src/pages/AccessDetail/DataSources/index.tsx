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

import React, { useState, forwardRef } from 'react';
import { Button, Modal, message } from 'antd';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import { DataSourcesCreateModal } from '@/components/AccessHelper';
import { dataSourcesBinLogColumns } from '@/components/MetaData/DataSourcesBinLog';
import { dataSourcesFileColumns } from '@/components/MetaData/DataSourcesFile';
import i18n from '@/i18n';
import request from '@/utils/request';
import { CommonInterface } from '../common';
import { genStatusTag } from './status';

type Props = CommonInterface;

const getFilterFormContent = defaultValues => [
  {
    type: 'inputsearch',
    name: 'keyword',
  },
  {
    type: 'radiobutton',
    name: 'sourceType',
    label: i18n.t('pages.AccessDetail.DataSources.Type'),
    initialValue: defaultValues.sourceType,
    props: {
      buttonStyle: 'solid',
      options: [
        {
          label: i18n.t('pages.AccessDetail.DataSources.File'),
          value: 'FILE',
        },
        {
          label: 'BinLog',
          value: 'BINLOG',
        },
      ],
    },
  },
];

const Comp = ({ inlongGroupId }: Props, ref) => {
  const [options, setOptions] = useState({
    // keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    sourceType: 'FILE',
  });

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const { data, loading, run: getList } = useRequest(
    {
      url: '/source/list',
      params: {
        ...options,
        inlongGroupId,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onSave = async values => {
    const isUpdate = createModal.id;
    const submitData = {
      ...values,
      inlongGroupId: inlongGroupId,
      sourceType: options.sourceType,
    };
    if (isUpdate) {
      submitData.id = createModal.id;
    }

    await request({
      url: `/source/${isUpdate ? 'update' : 'save'}`,
      method: 'POST',
      data: submitData,
    });
    await getList();
    message.success(i18n.t('pages.AccessDetail.DataSources.SaveSuccessfully'));
  };

  const onEdit = ({ id }) => {
    setCreateModal({ visible: true, id });
  };

  const onDelete = ({ id }) => {
    Modal.confirm({
      title: i18n.t('pages.AccessDetail.DataSources.DeletConfirm'),
      onOk: async () => {
        await request({
          url: `/source/delete/${id}`,
          method: 'DELETE',
          params: {
            sourceType: options.sourceType,
          },
        });
        await getList();
        message.success(i18n.t('pages.AccessDetail.DataSources.DeleteSuccessfully'));
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
    pageSize: options.pageSize,
    current: options.pageNum,
    total: data?.total,
  };

  const columns = [
    {
      title: i18n.t('pages.AccessDetail.DataSources.DataStreams'),
      dataIndex: 'inlongStreamId',
    } as any,
  ]
    .concat(options.sourceType === 'FILE' ? dataSourcesFileColumns : dataSourcesBinLogColumns)
    .concat([
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

  const createContent = [
    {
      type: 'select',
      label: i18n.t('pages.AccessDetail.DataSources.DataStreams'),
      name: 'inlongStreamId',
      props: {
        notFoundContent: i18n.t('pages.AccessDetail.DataSources.NoDataStreams'),
        disabled: !!createModal.id,
        options: {
          requestService: {
            url: '/stream/list',
            method: 'POST',
            data: {
              pageNum: 1,
              pageSize: 1000,
              inlongGroupId,
              dataSourceType: options.sourceType,
            },
          },
          requestParams: {
            ready: !!(createModal.visible && !createModal.id),
            formatResult: result =>
              result?.list.map(item => ({
                label: item.inlongStreamId,
                value: item.inlongStreamId,
              })) || [],
          },
        },
      },
      rules: [{ required: true }],
    },
  ];

  return (
    <>
      <HighTable
        filterForm={{
          content: getFilterFormContent(options),
          onFilter,
        }}
        suffix={
          <Button type="primary" onClick={() => setCreateModal({ visible: true })}>
            {i18n.t('pages.AccessDetail.DataSources.Create')}
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
        type={options.sourceType as any}
        content={createContent}
        visible={createModal.visible as boolean}
        onOk={async values => {
          await onSave(values);
          setCreateModal({ visible: false });
        }}
        onCancel={() => setCreateModal({ visible: false })}
      />
    </>
  );
};

export default forwardRef(Comp);
