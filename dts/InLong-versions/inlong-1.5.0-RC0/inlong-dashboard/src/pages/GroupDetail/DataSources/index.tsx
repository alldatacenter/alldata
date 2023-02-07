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

import React, { useState, forwardRef, useMemo, useCallback } from 'react';
import { Badge, Button, Card, Modal, List, Tag, Radio, message } from 'antd';
import { PaginationConfig } from 'antd/lib/pagination';
import {
  UnorderedListOutlined,
  TableOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import { useDefaultMeta, useLoadMeta, SourceMetaType } from '@/metas';
import DetailModal from './DetailModal';
import i18n from '@/i18n';
import request from '@/utils/request';
import { pickObjectArray } from '@/utils';
import { CommonInterface } from '../common';

interface Props extends CommonInterface {
  inlongStreamId?: string;
}

const Comp = ({ inlongGroupId, inlongStreamId, readonly }: Props, ref) => {
  const [mode, setMode] = useState('list');

  const { defaultValue } = useDefaultMeta('source');

  const defaultOptions = {
    // keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    sourceType: defaultValue,
  };

  const [options, setOptions] = useState(defaultOptions);

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
        inlongStreamId,
      },
    },
    {
      refreshDeps: [options],
    },
  );

  const onEdit = useCallback(({ id }) => {
    setCreateModal({ visible: true, id });
  }, []);

  const onDelete = useCallback(
    ({ id }) => {
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
    },
    [getList, options.sourceType],
  );

  const onChange = useCallback(({ current: pageNum, pageSize }) => {
    setOptions(prev => ({
      ...prev,
      pageNum,
      pageSize,
    }));
  }, []);

  const onFilter = useCallback(allValues => {
    setOptions(prev => ({
      ...prev,
      ...allValues,
      pageNum: 1,
    }));
  }, []);

  const pagination: PaginationConfig = {
    pageSize: options.pageSize,
    current: options.pageNum,
    total: data?.total,
    simple: true,
    size: 'small',
  };

  const { Entity } = useLoadMeta<SourceMetaType>('source', options.sourceType);

  const entityColumns = useMemo(() => {
    return Entity ? new Entity().renderList() : [];
  }, [Entity]);

  const entityFields = useMemo(() => {
    return Entity ? new Entity().renderRow() : [];
  }, [Entity]);

  const getFilterFormContent = useCallback(
    defaultValues =>
      [
        {
          type: 'inputsearch',
          name: 'keyword',
          initialValue: defaultValues.keyword,
          props: {
            allowClear: true,
          },
        },
      ].concat(
        pickObjectArray(['sourceType', 'status'], entityFields).map(item => ({
          ...item,
          visible: true,
          initialValue: defaultValues[item.name],
        })),
      ),
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
      },
    ]);
  }, [entityColumns, onDelete, onEdit, readonly]);

  return (
    <>
      <Card
        size="small"
        title={
          <Badge size="small" count={data?.total} offset={[15, 0]}>
            {i18n.t('pages.GroupDetail.Sources')}
          </Badge>
        }
        style={{ height: '100%' }}
        extra={[
          !readonly && (
            <Button key="create" type="link" onClick={() => setCreateModal({ visible: true })}>
              {i18n.t('pages.GroupDetail.Sources.Create')}
            </Button>
          ),
          <Radio.Group
            key="mode"
            onChange={e => {
              setMode(e.target.value);
              setOptions(defaultOptions);
            }}
            defaultValue={mode}
            size="small"
          >
            <Radio.Button value="list">
              <UnorderedListOutlined />
            </Radio.Button>
            <Radio.Button value="table">
              <TableOutlined />
            </Radio.Button>
          </Radio.Group>,
        ]}
      >
        {mode === 'list' ? (
          <List
            size="small"
            loading={loading}
            dataSource={data?.list as Record<string, any>[]}
            pagination={pagination}
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
                className="test"
              >
                <span>
                  <span style={{ marginRight: 10 }}>{item.sourceName}</span>
                  <Tag>{item.sourceType}</Tag>
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
        defaultType={options.sourceType}
        inlongGroupId={inlongGroupId}
        inlongStreamId={inlongStreamId}
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
