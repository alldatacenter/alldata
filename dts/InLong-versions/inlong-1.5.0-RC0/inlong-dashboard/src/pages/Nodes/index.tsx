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
import HighTable from '@/components/HighTable';
import { PageContainer } from '@/components/PageContainer';
import { defaultSize } from '@/configs/pagination';
import { dao } from '@/metas/nodes';
import { useDefaultMeta, useLoadMeta, NodeMetaType } from '@/metas';
import DetailModal from './DetailModal';

const { useListNodeDao, useDeleteNodeDao } = dao;

const Comp: React.FC = () => {
  const { defaultValue, options: nodes } = useDefaultMeta('node');

  const [options, setOptions] = useState({
    keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    type: defaultValue,
  });

  const [detailModal, setDetailModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const { data, loading, run: getList } = useListNodeDao({ options });

  const { runAsync: destory } = useDeleteNodeDao();

  const onEdit = ({ id }) => {
    setDetailModal({ visible: true, id });
  };

  const onDelete = useCallback(
    ({ id }) => {
      Modal.confirm({
        title: i18n.t('basic.DeleteConfirm'),
        onOk: async () => {
          await destory(id);
          await getList();
          message.success(i18n.t('basic.DeleteSuccess'));
        },
      });
    },
    [getList, destory],
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

  const getFilterFormContent = useCallback(
    defaultValues => [
      {
        type: 'inputsearch',
        name: 'keyword',
      },
      {
        type: 'select',
        name: 'type',
        label: i18n.t('meta.Nodes.Type'),
        initialValue: defaultValues.type,
        props: {
          dropdownMatchSelectWidth: false,
          options: nodes,
        },
      },
    ],
    [nodes],
  );

  const { Entity } = useLoadMeta<NodeMetaType>('node', options.type);

  const entityColumns = useMemo(() => {
    return Entity ? new Entity().renderList() : [];
  }, [Entity]);

  const columns = useMemo(() => {
    return entityColumns
      ?.map(item => ({
        ...item,
        ellipsisMulti: 2,
      }))
      .concat([
        {
          title: i18n.t('basic.Operating'),
          dataIndex: 'action',
          width: 200,
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
  }, [entityColumns, onDelete]);

  return (
    <PageContainer useDefaultBreadcrumb={false}>
      <HighTable
        filterForm={{
          content: getFilterFormContent(options),
          onFilter,
        }}
        suffix={
          <Button type="primary" onClick={() => setDetailModal({ visible: true })}>
            {i18n.t('basic.Create')}
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

      <DetailModal
        {...detailModal}
        defaultType={options.type}
        visible={detailModal.visible as boolean}
        onOk={async () => {
          await getList();
          setDetailModal({ visible: false });
        }}
        onCancel={() => setDetailModal({ visible: false })}
      />
    </PageContainer>
  );
};

export default Comp;
