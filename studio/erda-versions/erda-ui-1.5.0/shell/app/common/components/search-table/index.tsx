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
import { Input } from 'antd';
import { debounce } from 'lodash';
import { OperationBar } from 'common';

import './index.scss';

export interface IProps {
  needDebounce?: boolean;
  placeholder?: string;
  triggerByEnter?: boolean;
  searchFullWidth?: boolean;
  extraItems?: React.ReactNode[] | React.ReactNode;
  searchPosition?: 'left' | 'right';
  extraPosition?: 'left' | 'right';
  searchValue?: string;
  onSearch?: (searchKey: string) => void | any;
  searchListOps?: {
    list: object[];
    onUpdateOps?: (opsVal: any) => void;
  };
}
interface IState {
  searchValue: string;
}

const { Search } = Input;

/**
 * usage:
 * <SearchTable placeholder="通过应用名称或所属项目过滤搜索" onSearch={this.onSearch} searchPosition="right" extraPosition="left" needDebounce>
 *  <Table {...props}>
 * </SearchTable>
 */
class SearchTable extends React.PureComponent<IProps, IState> {
  debounceSearch = debounce((key: string) => {
    const { onSearch } = this.props;
    onSearch && onSearch(key);
  }, 1000);

  constructor(props: IProps) {
    super(props);

    this.state = {
      searchValue: props.searchValue || '',
    };
  }

  changeSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.target;
    const { onSearch, needDebounce, triggerByEnter } = this.props;
    this.setState(
      {
        searchValue: value,
      },
      () => {
        if (triggerByEnter) {
          return;
        }
        if (needDebounce) {
          this.debounceSearch(value);
        } else {
          onSearch && onSearch(value);
        }
      },
    );
  };

  onPressEnter = () => {
    const { triggerByEnter, onSearch } = this.props;
    if (triggerByEnter) {
      onSearch && onSearch(this.state.searchValue);
    }
  };

  onSearchClick = (value: string) => {
    const { onSearch } = this.props;
    onSearch && onSearch(value);
  };

  render() {
    const {
      placeholder = '',
      extraItems,
      searchPosition = 'left',
      searchFullWidth = false,
      extraPosition = 'left',
      children,
      searchListOps,
    } = this.props;
    const { searchValue } = this.state;
    const searchStyleName = `search-input search-input-${searchPosition} ${searchFullWidth ? 'w-full' : ''}`;
    const extraStyleName = `extra-items-${extraPosition}`;

    return (
      <section className="search-table-section">
        <div className="search-table-header">
          {searchListOps ? (
            <OperationBar searchList={searchListOps.list} onUpdateOps={searchListOps.onUpdateOps} />
          ) : (
            <Search
              className={searchStyleName}
              onSearch={this.onSearchClick}
              value={searchValue}
              placeholder={placeholder}
              onChange={this.changeSearch}
              onPressEnter={this.onPressEnter}
            />
          )}
          <div className={extraStyleName}>{extraItems}</div>
        </div>
        <div className="search-table-content">{children}</div>
      </section>
    );
  }
}

export default SearchTable;
