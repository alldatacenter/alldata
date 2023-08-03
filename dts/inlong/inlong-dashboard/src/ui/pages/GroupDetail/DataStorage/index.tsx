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

import React, { useState, useMemo, forwardRef, useCallback } from 'react';
import { Badge, Button, Card, Modal, List, Tag, Segmented, message, PaginationProps } from 'antd';
import { PaginationConfig } from 'antd/lib/pagination';
import {
  UnorderedListOutlined,
  TableOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import HighTable from '@/ui/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/ui/hooks';
import i18n from '@/i18n';
import DetailModal from './DetailModal';
import { useDefaultMeta, useLoadMeta, SinkMetaType } from '@/plugins';
import request from '@/core/utils/request';
import { pickObjectArray } from '@/core/utils';
import { CommonInterface } from '../common';
import { sinks } from '@/plugins/sinks';

interface Props extends CommonInterface {
  inlongStreamId?: string;
}

const Comp = ({ inlongGroupId, inlongStreamId, readonly }: Props, ref) => {
  const [mode, setMode] = useState('list');

  const { defaultValue } = useDefaultMeta('sink');

  const defaultOptions = {
    // keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    sinkType: defaultValue,
  };

  const [options, setOptions] = useState(defaultOptions);

  const [createModal, setCreateModal] = useState<Record<string, unknown>>({
    open: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/sink/list',
      method: 'POST',
      data: {
        ...options,
        inlongGroupId,
        inlongStreamId,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onEdit = useCallback(({ id }) => {
    setCreateModal({ open: true, id });
  }, []);

  const onDelete = useCallback(
    ({ id }) => {
      Modal.confirm({
        title: i18n.t('basic.DeleteConfirm'),
        onOk: async () => {
          await request({
            url: `/sink/delete/${id}`,
            method: 'DELETE',
            params: {
              sinkType: options.sinkType,
              startProcess: false,
            },
          });
          await getList();
          message.success(i18n.t('basic.DeleteSuccess'));
        },
      });
    },
    [getList, options.sinkType],
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

  const onChangeList: PaginationProps['onChange'] = page => {
    setOptions({ pageSize: defaultSize, pageNum: page, sinkType: defaultValue });
  };

  const pagination: PaginationConfig = {
    pageSize: options.pageSize,
    current: options.pageNum,
    total: data?.total,
    simple: true,
    size: 'small',
  };

  const { Entity } = useLoadMeta<SinkMetaType>('sink', options.sinkType);

  const entityColumns = useMemo(() => {
    return Entity ? new Entity().renderList() : [];
  }, [Entity]);

  const entityFields = useMemo(() => {
    return Entity ? new Entity().renderRow() : [];
  }, [Entity]);

  const getFilterFormContent = useCallback(
    defaultValues => [
      {
        type: 'inputsearch',
        name: 'keyword',
      },
      ...pickObjectArray(['sinkType', 'status'], entityFields).map(item => ({
        ...item,
        type: 'select',
        visible: true,
        initialValue: defaultValues[item.name],
      })),
    ],
    [entityFields],
  );

  const columns = useMemo(() => {
    return entityColumns?.concat([
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
  }, [entityColumns, onDelete, onEdit, readonly]);

  return (
    <>
      <Card
        size="small"
        title={
          <Badge size="small" count={data?.total} offset={[12, 3]}>
            {i18n.t('pages.GroupDetail.Sinks')}
          </Badge>
        }
        style={{ height: '100%' }}
        extra={[
          !readonly && (
            <Button key="create" type="link" onClick={() => setCreateModal({ open: true })}>
              {i18n.t('pages.GroupDetail.Sink.New')}
            </Button>
          ),
          <Segmented
            key="mode"
            onChange={(value: string) => {
              setMode(value);
              setOptions(defaultOptions);
            }}
            options={[
              {
                value: 'list',
                icon: <UnorderedListOutlined />,
              },
              {
                value: 'table',
                icon: <TableOutlined />,
              },
            ]}
            defaultValue={mode}
            size="small"
          />,
        ]}
      >
        {mode === 'list' ? (
          <List
            size="small"
            loading={loading}
            dataSource={data?.list as Record<string, any>[]}
            pagination={{
              pageSize: defaultSize,
              current: options.pageNum,
              total: data?.total,
              simple: true,
              size: 'small',
              onChange: onChangeList,
            }}
            renderItem={item => (
              <List.Item
                actions={[
                  <Button key="edit" type="link" onClick={() => onEdit(item)}>
                    <EditOutlined />
                  </Button>,
                  <Button key="del" type="link" onClick={() => onDelete(item)}>
                    <DeleteOutlined />
                  </Button>,
                ]}
              >
                <span>
                  <span style={{ marginRight: 10 }}>{item.sinkName}</span>
                  <Tag>{sinks.find(c => c.value === item.sinkType)?.label}</Tag>
                </span>
              </List.Item>
            )}
          />
        ) : (
          <HighTable
            filterForm={{
              content: getFilterFormContent(options),
              onFilter,
            }}
            table={{
              columns,
              rowKey: 'id',
              size: 'small',
              dataSource: data?.list,
              pagination,
              loading,
              onChange,
            }}
          />
        )}
      </Card>
      <DetailModal
        {...createModal}
        defaultType={options.sinkType}
        inlongGroupId={inlongGroupId}
        inlongStreamId={inlongStreamId}
        open={createModal.open as boolean}
        onOk={async () => {
          await getList();
          setCreateModal({ open: false });
        }}
        onCancel={() => setCreateModal({ open: false })}
      />
    </>
  );
};

export default forwardRef(Comp);
