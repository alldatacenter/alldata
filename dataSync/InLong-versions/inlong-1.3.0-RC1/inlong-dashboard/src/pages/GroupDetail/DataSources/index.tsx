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

import React, { useState, forwardRef, useMemo } from 'react';
import { Button, Modal, message } from 'antd';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import DetailModal from './DetailModal';
import { sources } from '@/metas/sources';
import i18n from '@/i18n';
import request from '@/utils/request';
import { CommonInterface } from '../common';
import { statusList, genStatusTag } from './status';

type Props = CommonInterface;

const getFilterFormContent = defaultValues => [
  {
    type: 'inputsearch',
    name: 'keyword',
  },
  {
    type: 'radiobutton',
    name: 'sourceType',
    label: i18n.t('pages.GroupDetail.Sources.Type'),
    initialValue: defaultValues.sourceType,
    props: {
      buttonStyle: 'solid',
      options: sources.map(item => ({
        label: item.label,
        value: item.value,
      })),
    },
  },
  {
    type: 'select',
    name: 'status',
    label: i18n.t('basic.Status'),
    props: {
      allowClear: true,
      dropdownMatchSelectWidth: false,
      options: statusList,
    },
  },
];

const Comp = ({ inlongGroupId, readonly }: Props, ref) => {
  const [options, setOptions] = useState({
    // keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    sourceType: sources[0].value,
  });

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
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

  const onEdit = ({ id }) => {
    setCreateModal({ visible: true, id });
  };

  const onDelete = ({ id }) => {
    Modal.confirm({
      title: i18n.t('pages.GroupDetail.Sources.DeleteConfirm'),
      onOk: async () => {
        await request({
          url: `/source/delete/${id}`,
          method: 'DELETE',
          params: {
            sourceType: options.sourceType,
          },
        });
        await getList();
        message.success(i18n.t('pages.GroupDetail.Sources.DeleteSuccessfully'));
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
      sources.reduce(
        (acc, cur) => ({
          ...acc,
          [cur.value]: cur.tableColumns,
        }),
        {},
      ),
    [],
  );

  const columns = [
    {
      title: i18n.t('pages.GroupDetail.Sources.DataStreams'),
      dataIndex: 'inlongStreamId',
    },
    {
      title: i18n.t('meta.Sources.Name'),
      dataIndex: 'sourceName',
    },
  ]
    .concat(columnsMap[options.sourceType])
    .concat([
      {
        title: i18n.t('basic.Status'),
        dataIndex: 'status',
        render: text => genStatusTag(text),
      },
      {
        title: i18n.t('basic.Operating'),
        dataIndex: 'action',
        render: (text, record) =>
          readonly ? (
            '-'
          ) : (
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
          !readonly && (
            <Button type="primary" onClick={() => setCreateModal({ visible: true })}>
              {i18n.t('pages.GroupDetail.Sources.Create')}
            </Button>
          )
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

      <DetailModal
        {...createModal}
        inlongGroupId={inlongGroupId}
        visible={createModal.visible as boolean}
        onOk={async () => {
          await getList();
          setCreateModal({ visible: false });
        }}
        onCancel={() => setCreateModal({ visible: false })}
      />
    </>
  );
};

export default forwardRef(Comp);
