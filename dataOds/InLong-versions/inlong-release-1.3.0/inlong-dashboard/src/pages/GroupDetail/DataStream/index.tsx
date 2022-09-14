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

import React, { useState, useImperativeHandle, forwardRef } from 'react';
import { Button, Modal, message } from 'antd';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';
import { CommonInterface } from '../common';
import StreamItemModal from './StreamItemModal';
import { getFilterFormContent } from './config';
import { genStatusTag } from './status';

type Props = CommonInterface;

const Comp = ({ inlongGroupId, readonly, mqType }: Props, ref) => {
  const { t } = useTranslation();

  const [options, setOptions] = useState({
    pageSize: defaultSize,
    pageNum: 1,
  });

  const [streamItemModal, setStreamItemModal] = useState({
    visible: false,
    inlongStreamId: '',
    inlongGroupId,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/stream/list',
      method: 'POST',
      data: {
        ...options,
        inlongGroupId,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onOk = () => {
    return Promise.resolve();
  };

  useImperativeHandle(ref, () => ({
    onOk,
  }));

  const onCreate = () => {
    setStreamItemModal(prev => ({
      ...prev,
      visible: true,
      inlongStreamId: '',
    }));
  };

  const onEdit = ({ inlongStreamId }) => {
    setStreamItemModal(prev => ({ ...prev, visible: true, inlongStreamId }));
  };

  const onDelete = record => {
    Modal.confirm({
      title: t('basic.DeleteConfirm'),
      onOk: async () => {
        await request({
          url: '/stream/delete',
          method: 'DELETE',
          params: {
            groupId: inlongGroupId,
            streamId: record?.inlongStreamId,
          },
        });
        await getList();
        message.success(t('basic.DeleteSuccess'));
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
      title: 'ID',
      dataIndex: 'inlongStreamId',
    },
    {
      title: t('pages.GroupDetail.Stream.Name'),
      dataIndex: 'name',
    },
    {
      title: t('basic.Creator'),
      dataIndex: 'creator',
    },
    {
      title: t('basic.CreateTime'),
      dataIndex: 'createTime',
    },
    {
      title: t('basic.Status'),
      dataIndex: 'status',
      render: text => genStatusTag(text),
    },
    {
      title: t('basic.Operating'),
      dataIndex: 'action',
      render: (text, record) =>
        readonly ? (
          '-'
        ) : (
          <>
            <Button type="link" onClick={() => onEdit(record)}>
              {t('basic.Edit')}
            </Button>
            <Button type="link" onClick={() => onDelete(record)}>
              {t('basic.Delete')}
            </Button>
          </>
        ),
    },
  ];

  return (
    <>
      <HighTable
        filterForm={{
          content: getFilterFormContent(),
          onFilter,
        }}
        suffix={
          !readonly && (
            <Button type="primary" onClick={onCreate}>
              {t('pages.GroupDetail.Stream.CreateDataStream')}
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

      <StreamItemModal
        {...streamItemModal}
        mqType={mqType}
        onOk={async () => {
          await getList();
          setStreamItemModal(prev => ({ ...prev, visible: false }));
        }}
        onCancel={() => setStreamItemModal(prev => ({ ...prev, visible: false }))}
      />
    </>
  );
};

export default forwardRef(Comp);
