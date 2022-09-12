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

import React, { useState, useMemo, forwardRef } from 'react';
import { Button, Modal, message } from 'antd';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import i18n from '@/i18n';
import { DataStorageDetailModal } from '@/components/AccessHelper';
import { Storages } from '@/components/MetaData';
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
    type: 'select',
    name: 'sinkType',
    label: i18n.t('pages.AccessDetail.DataStorage.Type'),
    initialValue: defaultValues.sinkType,
    props: {
      dropdownMatchSelectWidth: false,
      options: Storages.map(item => ({
        label: item.label,
        value: item.value,
      })),
    },
  },
];

const Comp = ({ inlongGroupId }: Props, ref) => {
  const [options, setOptions] = useState({
    keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    sinkType: 'HIVE',
  });

  const [curDataStreamIdentifier, setCurDataStreamIdentifier] = useState<string>();

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const { data, loading, run: getList } = useRequest(
    {
      url: '/sink/list',
      params: {
        ...options,
        inlongGroupId,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const { data: streamList = [] } = useRequest(
    {
      url: '/stream/list',
      method: 'POST',
      data: {
        pageNum: 1,
        pageSize: 1000,
        inlongGroupId,
        sinkType: options.sinkType,
      },
    },
    {
      ready: !!createModal.visible,
      formatResult: result => result?.list || [],
    },
  );

  const onSave = async values => {
    const isUpdate = createModal.id;
    const submitData = {
      ...values,
      sinkType: options.sinkType,
      inlongGroupId: inlongGroupId,
    };
    if (isUpdate) {
      submitData.id = createModal.id;
    }
    await request({
      url: isUpdate ? '/sink/update' : '/sink/save',
      method: 'POST',
      data: submitData,
    });
    await getList();
    message.success(i18n.t('basic.OperatingSuccess'));
  };

  const onEdit = ({ id, inlongStreamId }) => {
    setCreateModal({ visible: true, id });
    setCurDataStreamIdentifier(inlongStreamId);
  };

  const onDelete = ({ id }) => {
    Modal.confirm({
      title: i18n.t('basic.DeleteConfirm'),
      onOk: async () => {
        await request({
          url: `/sink/delete/${id}`,
          method: 'DELETE',
          params: {
            sinkType: options.sinkType,
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
    pageSize: options.pageSize,
    current: options.pageNum,
    total: data?.total,
  };

  const columnsMap = useMemo(
    () =>
      Storages.reduce(
        (acc, cur) => ({
          ...acc,
          [cur.value]: cur.tableColumns,
        }),
        {},
      ),
    [],
  );

  const createContent = useMemo(
    () => [
      {
        type: 'select',
        label: i18n.t('pages.AccessDetail.DataStorage.DataStreams'),
        name: 'inlongStreamId',
        props: {
          notFoundContent: i18n.t('pages.AccessDetail.DataStorage.NoDataStreams'),
          disabled: !!createModal.id,
          options: streamList.map(item => ({
            label: item.inlongStreamId,
            value: item.inlongStreamId,
          })),
        },
        rules: [{ required: true }],
      },
    ],
    [createModal.id, streamList],
  );

  const streamItem = streamList.find(item => item.inlongStreamId === curDataStreamIdentifier);

  const columns = [
    {
      title: i18n.t('pages.AccessDetail.DataStorage.DataStreams'),
      dataIndex: 'inlongStreamId',
    },
  ]
    .concat(columnsMap[options.sinkType])
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

  return (
    <>
      <HighTable
        filterForm={{
          content: getFilterFormContent(options),
          onFilter,
        }}
        suffix={
          <Button type="primary" onClick={() => setCreateModal({ visible: true })}>
            {i18n.t('pages.AccessDetail.DataStorage.New')}
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

      <DataStorageDetailModal
        {...createModal}
        inlongGroupId={inlongGroupId}
        content={createContent}
        sinkType={options.sinkType as any}
        visible={createModal.visible as boolean}
        dataType={streamItem?.dataType}
        onValuesChange={(c, v) => setCurDataStreamIdentifier(v?.inlongStreamId)}
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
