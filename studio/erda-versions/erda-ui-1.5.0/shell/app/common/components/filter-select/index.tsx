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

import { Select, Spin } from 'antd';
import React from 'react';
import './index.scss';

interface IProps {
  multiple: boolean;
  options: Array<{
    value: string | number;
    text: string;
    hlName?: string;
  }>;
  fetching: boolean;
  value: number | [];
  initFetch?: () => void;
}
interface IState {
  filtering: boolean;
  filteredList: object[];
}

class FilterSelect extends React.PureComponent<IProps, IState> {
  state = {
    filtering: false,
    filteredList: [],
  };

  componentDidMount() {
    const noop = () => {};
    (this.props.initFetch || noop)();
  }

  onSearch = (value: string) => {
    const { options } = this.props;

    const filter = (list: Array<{ text: string }>) => {
      const iFiltered: object[] = [];
      list.forEach((item) => {
        const index = item.text.indexOf(value);
        if (index > -1) {
          const prefix = item.text.slice(0, index);
          const suffix = item.text.slice(index + value.length);
          const hlName = (
            <span>
              {prefix}
              <span className="hl">{value}</span>
              {suffix}
            </span>
          );
          iFiltered.push({ ...item, hlName });
        }
      });
      return iFiltered;
    };

    if (value.length) {
      this.setState({
        filteredList: filter(options),
        filtering: true,
      });
    } else {
      this.setState({
        filteredList: [],
        filtering: false,
      });
    }
  };

  render() {
    const { multiple, options, fetching, ...otherProps } = this.props;
    const { filtering, filteredList } = this.state;
    let _value: string | string[] = '';
    if (otherProps.value !== undefined && otherProps.value !== null) {
      if (typeof otherProps.value === 'number') {
        _value = `${otherProps.value}`; // value must be string
      }
      if (Array.isArray(otherProps.value)) {
        _value = otherProps.value.map((i) => String(i));
      }
    }

    return (
      <Select
        {...otherProps}
        mode={multiple ? 'multiple' : undefined}
        value={_value}
        filterOption={false}
        onSearch={this.onSearch}
        showSearch
        allowClear
        notFoundContent={fetching ? <Spin size="small" /> : null}
      >
        {(filtering ? filteredList : options).map(({ value, text, hlName }) => (
          <Select.Option key={value} value={value} className="suggestions-option">
            {hlName || text}
          </Select.Option>
        ))}
      </Select>
    );
  }
}

export default FilterSelect;
