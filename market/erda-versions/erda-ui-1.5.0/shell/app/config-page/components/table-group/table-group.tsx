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
import { map } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import Title from '../title/title';
import Text from '../text/text';
import { Table } from '../table/table';
import i18n from 'i18n';
import './table-group.scss';

const noop = () => {};
const TableBoard = (props: CP_TABLE_GROUP.ITableBoardProps) => {
  const { props: configProps, execOperation = noop, updateState = noop } = props;
  const { title, subtitle, description, table, extraInfo } = configProps;
  const extraProps = { execOperation, updateState };

  return (
    <div className="table-board">
      <Text props={title?.props} operations={title?.operations} type="Text" {...extraProps} />
      <div className="table-board-card mt-2">
        <div className="ml-1">
          <Title props={subtitle} type="Title" {...extraProps} />
        </div>
        <div className="mt-3">
          <div className="mb-3 ml-1">
            <Text props={description} type="Text" {...extraProps} />
          </div>
          <Table props={table?.props} data={table?.data} operations={table?.operations} {...extraProps} />
          <div className="mt-3 ml-1">
            <Text props={extraInfo?.props} operations={extraInfo?.operations} type="Text" {...extraProps} />
          </div>
        </div>
      </div>
    </div>
  );
};

const TableGroup = (props: CP_TABLE_GROUP.Props) => {
  const {
    props: configProps,
    state: propsState,
    data = {} as CP_TABLE_GROUP.IData,
    operations,
    execOperation = noop,
    updateState = noop,
  } = props;
  const stateRef = React.useRef(propsState);
  const [{ pageNo, combineList = [], total, pageSize }, updater, update] = useUpdate(
    {
      pageNo: propsState?.pageNo || 1,
      total: propsState?.total || 0,
      pageSize: propsState?.pageSize || 3,
      combineList: data.list,
    } || {},
  ) as any;
  const { visible } = configProps;
  const showLoadMore = total > Math.max(combineList?.length, 0);

  React.useEffect(() => {
    stateRef.current = propsState;
  }, [propsState]);

  // 将接口返回的list和之前的list进行拼接
  React.useEffect(() => {
    // if isLoadMore is true, the data will be set undefined, combineList don't need to do anything
    if (data === undefined) {
      return;
    }
    update((pre) => {
      const newState = {
        ...pre,
        ...stateRef.current,
      };
      return {
        ...newState,
        combineList:
          (newState || {}).pageNo === 1 ? data?.list || [] : (newState?.combineList || []).concat(data?.list || []),
      };
    });
  }, [data, data?.list, update, stateRef]);

  // 加载更多
  const loadMore = () => {
    operations?.changePageNo && execOperation(operations.changePageNo, { pageNo: pageNo + 1 });
  };

  if (!visible) {
    return null;
  }
  return (
    <div className="cp-dice-table-group">
      {map(combineList, (item, index) => {
        return (
          <TableBoard
            type="TableBoard"
            key={`${index}`}
            props={item}
            execOperation={execOperation}
            updateState={updateState}
            operations={operations}
          />
        );
      })}
      {showLoadMore && (
        <div className="load-more hover-active" onClick={loadMore}>
          {i18n.t('load more')}...
        </div>
      )}
    </div>
  );
};

export default TableGroup;
