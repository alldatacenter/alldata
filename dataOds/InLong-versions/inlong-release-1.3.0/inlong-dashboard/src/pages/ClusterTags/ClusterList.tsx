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

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Modal, message } from 'antd';
import i18n from '@/i18n';
import HighTable from '@/components/HighTable';
import { defaultSize } from '@/configs/pagination';
import { useRequest } from '@/hooks';
import { Clusters } from '@/metas/clusters';
import ClusterBindModal from './ClusterBindModal';
import request from '@/utils/request';

export interface ClusterListProps {
  clusterTag: string;
}

const getFilterFormContent = defaultValues => [
  {
    type: 'inputsearch',
    name: 'keyword',
  },
  {
    type: 'select',
    name: 'type',
    label: i18n.t('pages.Clusters.Type'),
    initialValue: defaultValues.type,
    props: {
      options: [
        {
          label: i18n.t('pages.Clusters.TypeAll'),
          value: '',
        },
      ].concat(
        Clusters.map(item => ({
          label: item.label,
          value: item.value,
        })),
      ),
    },
  },
];

const Comp: React.FC<ClusterListProps> = ({ clusterTag }) => {
  const [options, setOptions] = useState({
    keyword: '',
    pageSize: defaultSize,
    pageNum: 1,
    type: '',
  });

  const [clusterBindModal, setClusterBindModal] = useState<Record<string, unknown>>({
    visible: false,
  });

  const {
    data,
    loading,
    run: getList,
  } = useRequest(
    {
      url: '/cluster/list',
      method: 'POST',
      data: {
        ...options,
        clusterTag,
      },
    },
    {
      manual: true,
    },
  );

  useEffect(() => {
    if (clusterTag) {
      getList();
    }
  }, [options, clusterTag, getList]);

  const onDelete = useCallback(
    ({ id }) => {
      Modal.confirm({
        title: i18n.t('pages.ClusterTags.DelClusterConfirm'),
        onOk: async () => {
          await request({
            url: '/cluster/bindTag',
            method: 'POST',
            data: {
              unbindClusters: [id],
              clusterTag,
            },
          });
          await getList();
          message.success(i18n.t('DelClusterSuccess'));
        },
      });
    },
    [clusterTag, getList],
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
    const defaultColumns = [
      {
        title: i18n.t('pages.Clusters.Name'),
        dataIndex: 'name',
        ellipsisMulti: 2,
      },
      {
        title: i18n.t('pages.Clusters.InCharges'),
        dataIndex: 'inCharges',
        ellipsisMulti: 2,
      },
      {
        title: i18n.t('pages.Clusters.Description'),
        dataIndex: 'description',
        ellipsisMulti: 2,
      },
    ];

    return defaultColumns.concat([
      {
        title: i18n.t('basic.Operating'),
        dataIndex: 'action',
        width: 200,
        render: (text, record) => (
          <>
            {/* <Button type="link">{i18n.t('basic.Detail')}</Button> */}
            <Button type="link" onClick={() => onDelete(record)}>
              {i18n.t('pages.ClusterTags.DelCluster')}
            </Button>
          </>
        ),
      } as any,
    ]);
  }, [onDelete]);

  return (
    <>
      <HighTable
        filterForm={{
          content: getFilterFormContent(options),
          onFilter,
        }}
        suffix={
          <Button type="primary" onClick={() => setClusterBindModal({ visible: true })}>
            {i18n.t('pages.ClusterTags.BindCluster')}
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

      <ClusterBindModal
        {...clusterBindModal}
        clusterTag={clusterTag}
        visible={clusterBindModal.visible as boolean}
        onOk={async () => {
          await getList();
          setClusterBindModal({ visible: false });
        }}
        onCancel={() => setClusterBindModal({ visible: false })}
      />
    </>
  );
};

export default Comp;
