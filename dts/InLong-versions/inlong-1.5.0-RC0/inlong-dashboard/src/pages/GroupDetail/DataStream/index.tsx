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

import React, { useState, useImperativeHandle, forwardRef, useMemo } from 'react';
import { Button, Modal, message } from 'antd';
import { RightCircleTwoTone, DownCircleTwoTone } from '@ant-design/icons';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';
import { useLoadMeta, useDefaultMeta, StreamMetaType } from '@/metas';
import { GroupLogs } from '@/components/GroupLogs';
import { CommonInterface } from '../common';
import StreamItemModal from './StreamItemModal';
import SourceSinkCard from './SourceSinkCard';
import { getFilterFormContent } from './config';

type Props = CommonInterface;

const Comp = ({ inlongGroupId, readonly, mqType }: Props, ref) => {
  const { t } = useTranslation();

  const { defaultValue } = useDefaultMeta('stream');

  const [options, setOptions] = useState({
    pageSize: defaultSize,
    pageNum: 1,
  });

  const [streamItemModal, setStreamItemModal] = useState({
    visible: false,
    inlongStreamId: '',
    inlongGroupId,
  });

  const [groupLogs, setGroupLogs] = useState({
    visible: false,
    inlongGroupId,
    inlongStreamId: '',
  });

  const [groupStatus, setGroupStatus] = useState();

  const [expandedRowKeys, setExpandedRowKeys] = useState([]);

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
      onSuccess: result => {
        const [item] = result?.list || [];
        setExpandedRowKeys([item?.inlongStreamId]);
      },
    },
  );

  useRequest(`/group/get/${inlongGroupId}`, {
    onSuccess: result => setGroupStatus(result.status),
  });

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

  const onEdit = record => {
    setStreamItemModal(prev => ({ ...prev, visible: true, inlongStreamId: record.inlongStreamId }));
  };

  const openModal = record => {
    setGroupLogs({
      visible: true,
      inlongGroupId: inlongGroupId,
      inlongStreamId: record.inlongStreamId,
    });
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

  const onWorkflow = record => {
    Modal.confirm({
      title: t('meta.Stream.ExecuteConfirm'),
      onOk: async () => {
        await request({
          url: `/stream/startProcess/${inlongGroupId}/${record?.inlongStreamId}`,
          method: 'POST',
          params: {
            sync: false,
          },
        });
        await getList();
        message.success(t('meta.Stream.ExecuteSuccess'));
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

  const { Entity } = useLoadMeta<StreamMetaType>('stream', defaultValue);

  const entityColumns = useMemo(() => {
    return Entity ? new Entity().renderList() : [];
  }, [Entity]);

  const columns = entityColumns?.concat([
    {
      title: t('basic.Operating'),
      dataIndex: 'action',
      render: (text, record) =>
        readonly ? (
          '-'
        ) : (
          <div onClick={e => e.stopPropagation()}>
            <Button type="link" onClick={() => onEdit(record)}>
              {t('basic.Edit')}
            </Button>
            <Button type="link" onClick={() => onDelete(record)}>
              {t('basic.Delete')}
            </Button>
            {record?.status && (record?.status === 120 || record?.status === 130) && (
              <Button type="link" onClick={() => openModal(record)}>
                {t('pages.GroupDashboard.config.ExecuteLog')}
              </Button>
            )}
            {record?.status && (groupStatus === 120 || groupStatus === 130) && (
              <Button type="link" onClick={() => onWorkflow(record)}>
                {t('meta.Stream.ExecuteWorkflow')}
              </Button>
            )}
          </div>
        ),
    },
  ]);

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
          rowKey: 'inlongStreamId',
          dataSource: data?.list,
          pagination,
          loading,
          onChange,
          expandRowByClick: true,
          expandedRowKeys,
          onExpandedRowsChange: rows => setExpandedRowKeys(rows),
          expandedRowRender: record => (
            <SourceSinkCard inlongGroupId={inlongGroupId} inlongStreamId={record.inlongStreamId} />
          ),
          expandIcon: ({ expanded, onExpand, record }) =>
            expanded ? (
              <DownCircleTwoTone onClick={e => onExpand(record, e)} />
            ) : (
              <RightCircleTwoTone onClick={e => onExpand(record, e)} />
            ),
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

      <GroupLogs
        {...groupLogs}
        onOk={() => setGroupLogs({ visible: false, inlongGroupId: '', inlongStreamId: '' })}
        onCancel={() => setGroupLogs({ visible: false, inlongGroupId: '', inlongStreamId: '' })}
      />
    </>
  );
};

export default forwardRef(Comp);
