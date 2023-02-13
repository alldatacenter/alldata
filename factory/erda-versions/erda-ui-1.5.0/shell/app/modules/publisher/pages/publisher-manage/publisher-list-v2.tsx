// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { Input, Spin, Button, Table } from 'antd';
import { useDebounce, useUnmount, useUpdateEffect } from 'react-use';
import i18n from 'i18n';
import { CustomFilter } from 'common';
import { useUpdate } from 'common/use-hooks';
import { connectCube, goTo, insertWhen } from 'common/utils';
import './publisher-list-v2.scss';
import routeInfoStore from 'core/stores/route';
import ArtifactsFormModal from 'publisher/pages/artifacts/artifacts-form-modal';
import { ColumnProps } from 'core/common/interface';
import { WithAuth, usePerm } from 'user/common';

interface IMapperProps {
  list: PUBLISHER.IPublisher[];
  paging: IPaging;
  isFetching: boolean;
  getList: (payload: PUBLISHER.PublisherListQuery) => Promise<{
    list: PUBLISHER.IPublisher[];
    total: number;
  }>;
  clearList: () => void;
  deleteItem?: (payload: any) => Promise<any>;
  operationAuth?: { delete: boolean; edit: boolean; add: boolean };
  onItemClick: (item: PUBLISHER.IPublisher) => void;
}

export interface IPubliserListProps extends IMapperProps {
  placeHolderMsg?: string;
  getList: (p: any) => Promise<any>;
}

export const publisherTabs = () => {
  return [
    {
      key: 'MOBILE',
      name: i18n.t('dop:mobile app'),
    },
    {
      key: 'LIBRARY',
      name: i18n.t('dop:library/module'),
    },
  ];
};

export const PurePublisherList = ({
  list = [],
  paging,
  getList,
  clearList,
  isFetching,
  onItemClick,
}: IPubliserListProps) => {
  const [{ q, formVisible }, updater] = useUpdate({
    q: undefined as string | undefined,
    formVisible: false,
  });
  const { mode } = routeInfoStore.useStore((s) => s.params);
  const publishOperationAuth = usePerm((s) => s.org.publisher.operation.pass);

  useUnmount(clearList);

  useUpdateEffect(() => {
    updater.q(undefined);
  }, [mode]);

  useDebounce(
    () => {
      getList({
        q,
        pageNo: 1,
        type: mode,
      });
    },
    300,
    [q, mode],
  );

  const onSubmit = ({ q: value }: { q: string }) => {
    updater.q(value);
  };

  const goToPublisher = (item: PUBLISHER.IPublisher) => {
    onItemClick(item);
  };

  const handlePageChange = (pageNo: number) => {
    getList({
      q,
      pageNo,
      type: mode,
    });
  };

  const openFormModal = () => {
    updater.formVisible(true);
  };
  const closeFormModal = () => {
    updater.formVisible(false);
  };

  const afterSubmitAction = (_isUpdate: boolean, data: PUBLISHER.IArtifacts) => {
    const { id, type } = data;
    goTo(`../${type}/${id}`);
  };

  const column: Array<ColumnProps<PUBLISHER.IPublisher>> = [
    {
      title: i18n.t('publisher:publisher content name'),
      dataIndex: 'name',
      width: 240,
    },
    {
      title: i18n.t('description'),
      dataIndex: 'desc',
    },
    ...insertWhen<ColumnProps<PUBLISHER.IPublisher>>(mode === 'LIBRARY', [
      {
        title: i18n.t('version number'),
        width: 160,
        dataIndex: 'latestVersion',
        render: (text) => text || '-',
      },
      {
        title: i18n.t('publisher:subscriptions'),
        width: 120,
        dataIndex: 'refCount',
        render: (text) => text || 0,
      },
    ]),
    {
      title: i18n.t('default:status'),
      width: 120,
      dataIndex: 'public',
      render: (bool) => {
        return (
          <span className={`item-status ${bool ? 'on' : 'off'}`}>
            {bool ? i18n.t('publisher:published') : i18n.t('publisher:withdrawn')}
          </span>
        );
      },
    },
  ];
  const config = React.useMemo(
    () => [
      {
        type: Input,
        name: 'q',
        customProps: {
          placeholder: i18n.t('filter by {name}', { name: i18n.t('publisher:publisher content name') }),
          autoComplete: 'off',
        },
      },
    ],
    [],
  );
  return (
    <Spin spinning={isFetching}>
      <CustomFilter
        key={mode}
        config={config}
        onSubmit={onSubmit}
        onReset={() => {
          updater.q('');
        }}
      />
      <div className="publisher-list-section">
        <div className="top-button-group">
          <WithAuth pass={publishOperationAuth} tipProps={{ placement: 'bottom' }}>
            <Button type="primary" onClick={() => openFormModal()}>
              {i18n.t('publisher:add content')}
            </Button>
          </WithAuth>
        </div>
        <Table
          rowKey="id"
          columns={column}
          dataSource={list}
          onRow={(record: PUBLISHER.IPublisher) => {
            return {
              onClick: () => {
                goToPublisher(record);
              },
            };
          }}
          pagination={{
            current: paging.pageNo,
            ...paging,
            onChange: handlePageChange,
          }}
          scroll={{ x: 800 }}
        />
        <ArtifactsFormModal visible={formVisible} onCancel={closeFormModal} afterSubmit={afterSubmitAction} />
      </div>
    </Spin>
  );
};

export const createPublisherList = (Mapper: () => IMapperProps) => connectCube(PurePublisherList, Mapper);
