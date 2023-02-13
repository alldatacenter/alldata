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
import { Table, Popconfirm, Spin, Button, Input } from 'antd';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import moment from 'moment';
import { CustomFilter, TableActions } from 'common';
import { goTo } from 'common/utils';
import { useLoading } from 'core/stores/loading';

const formatData = (data: API_ACCESS.AccessListItem[]): API_ACCESS.ITableData[] => {
  return data.map(({ children, ...rest }) => {
    return {
      subData: children.map((child) => ({ ...child, parents: rest })),
      ...rest,
    };
  });
};

const AccessList = () => {
  const [keyword, setKeyword] = React.useState('');
  const [accessList] = apiAccessStore.useStore((s) => [s.accessList]);
  const { getAccess, deleteAccess } = apiAccessStore.effects;
  const { clearAccess } = apiAccessStore.reducers;
  const [isFetch, isDelete] = useLoading(apiAccessStore, ['getAccess', 'deleteAccess']);
  React.useEffect(() => {
    getAccess({ pageNo: 1, paging: true, keyword });
    return () => {
      clearAccess();
    };
  }, [clearAccess, getAccess, keyword]);
  const handleSearch = (query: Record<string, any>) => {
    setKeyword(query.keyword);
  };
  const handleEdit = (record: API_ACCESS.SubAccess, e: React.MouseEvent<HTMLSpanElement>) => {
    e.stopPropagation();
    goTo(`./access/edit/${record.id}`);
  };
  const handleDelete = (record: API_ACCESS.SubAccess, e?: React.MouseEvent<HTMLElement>) => {
    e && e.stopPropagation();
    deleteAccess({ accessID: record.id }).then(() => {
      getAccess({ pageNo: 1, paging: true, keyword });
    });
  };
  const filterConfig = React.useMemo(
    (): FilterItemConfig[] => [
      {
        type: Input.Search,
        name: 'keyword',
        customProps: {
          placeholder: i18n.t('default:search by keywords'),
          autoComplete: 'off',
        },
      },
    ],
    [],
  );
  const columns: Array<ColumnProps<API_ACCESS.ITableData>> = [
    {
      title: i18n.t('API name'),
      dataIndex: 'assetName',
    },
    {
      title: i18n.t('number of versions'),
      dataIndex: 'totalChildren',
      width: 160,
    },
  ];
  const subColumns: Array<ColumnProps<API_ACCESS.ITableSubAccess>> = [
    {
      title: i18n.t('version'),
      dataIndex: 'swaggerVersion',
    },
    {
      title: i18n.t('number of related clients'),
      dataIndex: 'appCount',
      width: 200,
    },
    {
      title: i18n.t('create time'),
      dataIndex: 'createdAt',
      width: 176,
      render: (date) => moment(date).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'permission',
      width: 120,
      fixed: 'right',
      render: ({ edit, delete: canDelete }: API_ACCESS.AccessPermission, record) => {
        return (
          <TableActions>
            {edit ? (
              <span
                onClick={(e) => {
                  handleEdit(record, e);
                }}
              >
                {i18n.t('edit')}
              </span>
            ) : null}
            {canDelete ? (
              <Popconfirm
                title={i18n.t('confirm to {action}', { action: i18n.t('delete') })}
                onConfirm={(e) => {
                  handleDelete(record, e);
                }}
              >
                <span>{i18n.t('delete')}</span>
              </Popconfirm>
            ) : null}
          </TableActions>
        );
      },
    },
  ];
  const expandedRowRender = (record: API_ACCESS.ITableData) => {
    return (
      <Table
        rowKey="swaggerVersion"
        columns={subColumns}
        dataSource={record.subData}
        pagination={false}
        onRow={(data: API_ACCESS.ITableSubAccess) => {
          return {
            onClick: () => {
              goToDetail(data);
            },
          };
        }}
        scroll={{ x: 800 }}
      />
    );
  };
  const goToDetail = (record: API_ACCESS.ITableSubAccess) => {
    goTo(`./detail/${record.id}`);
  };
  const dataSource = formatData(accessList);
  return (
    <Spin spinning={isFetch || isDelete}>
      <div className="top-button-group">
        <Button
          type="primary"
          onClick={() => {
            goTo('./access/create');
          }}
        >
          {i18n.t('establish')}
        </Button>
      </div>
      <CustomFilter config={filterConfig} onSubmit={handleSearch} />
      <Table
        rowKey="assetID"
        columns={columns}
        dataSource={dataSource}
        expandedRowRender={expandedRowRender}
        scroll={{ x: '100%' }}
      />
    </Spin>
  );
};

export default AccessList;
