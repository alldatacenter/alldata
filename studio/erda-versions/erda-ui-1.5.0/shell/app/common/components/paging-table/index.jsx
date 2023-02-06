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
import { Button, Table, Modal } from 'antd';
import i18n from 'i18n';
import { PAGINATION } from 'app/constants';
import './index.scss';

const { confirm } = Modal;
const noop = () => {};

/**
 * 配置项：
    basicOperation: false // 为true时会自动添加标准的操作列
 * Usage:
    <PagingTable
      columns={apiCols}
      pageSize={pageSize}
      total={total}
      rowKey='apiId'
      onAdd={this.toggleModal}
    />
 *
 */
class PagingTable extends React.PureComponent {
  constructor(props) {
    super(props);
    this.page = {
      pageNo: 1,
      pageSize: props.pageSize || PAGINATION.pageSize,
    };
    this.operation = {
      title: i18n.t('operation'),
      width: 100,
      render: (record) => {
        return (
          <div className="table-operations">
            {props.onEdit ? (
              <span className="table-operations-btn" onClick={() => props.onEdit(record)}>
                {i18n.t('edit')}
              </span>
            ) : null}
            {props.onDelete ? (
              <span
                className="table-operations-btn"
                onClick={() =>
                  confirm({ title: i18n.t('common:confirm deletion'), onOk: () => props.onDelete(record) })
                }
              >
                {i18n.t('delete')}
              </span>
            ) : null}
          </div>
        );
      },
    };
  }

  componentDidMount() {
    const { isForbidInitialFetch, getList } = this.props;
    !isForbidInitialFetch && (getList || noop)(this.page);
  }

  componentWillUnmount() {
    (this.props.clearList || noop)();
  }

  getList = () => {
    this.props.getList(this.page);
  };

  onChangePage = (pageNo) => {
    this.page = {
      ...this.page,
      pageNo,
    };
    this.getList();
  };

  render() {
    const {
      dataSource,
      isFetching = false,
      total,
      title = null,
      onAdd,
      columns,
      rowKey,
      basicOperation = false,
      buttonClass = '',
      tableProps = {},
    } = this.props;
    const { pageNo, pageSize } = this.page;
    const opRow =
      title || onAdd ? (
        <div className="op-row">
          {onAdd ? (
            <Button
              type="primary"
              className={`add-btn ${buttonClass}`}
              onClick={() => onAdd(/* 这里不要改动，避免传参数 */)}
            >
              {i18n.t('common:add')}
            </Button>
          ) : null}
          {title ? <span className="table-title">{title}</span> : null}
        </div>
      ) : null;

    return (
      <div className="paging-table">
        {opRow}
        <Table
          rowKey={rowKey || 'id'}
          loading={isFetching}
          columns={basicOperation ? columns.concat(this.operation) : columns}
          dataSource={dataSource}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            onChange: this.onChangePage,
            // hideOnSinglePage: true,
          }}
          scroll={{ x: '100%' }}
          tableLayout="auto" // fixed header/column or if column.ellipsis is used, the default value is fixed.
          {...tableProps}
        />
      </div>
    );
  }
}

export default PagingTable;
