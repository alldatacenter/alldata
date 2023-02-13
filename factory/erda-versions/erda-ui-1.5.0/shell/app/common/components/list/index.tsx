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
import Pagination from 'common/components/pagination';
import { EmptyHolder } from 'common';
import i18n from 'i18n';
import ListItem from 'common/components/list/list-item';
import './index.scss';

const List = (props: ERDA_LIST.IProps) => {
  const { dataSource, isLoadMore, onLoadMore, pagination, operations, onRow, getKey, size, alignCenter, noBorder } =
    props;

  return (
    <div className="erda-list">
      {dataSource.length ? (
        <>
          {dataSource.map((item: ERDA_LIST.IListData, idx: number) => {
            return (
              <ListItem
                size={size}
                key={getKey(item, idx)}
                data={item}
                alignCenter={alignCenter}
                noBorder={noBorder}
                onRow={onRow}
                operations={operations}
              />
            );
          })}
          {!isLoadMore && pagination ? <Pagination {...pagination} current={pagination.pageNo} /> : null}
          {isLoadMore && (pagination?.total || 0) > dataSource.length && (
            <div className="hover-active load-more" onClick={onLoadMore}>
              {i18n.t('more')}
            </div>
          )}
        </>
      ) : (
        <EmptyHolder relative />
      )}
    </div>
  );
};

List.ListItem = ListItem;

export default List;
