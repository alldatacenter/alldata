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
import BaseFilter, { IFilterProps, FilterItemConfig } from './base-filter';
import { get, map, has, set, isEmpty, debounce } from 'lodash';
import routeInfoStore from 'core/stores/route';
import { useMount } from 'react-use';
import { updateSearch as _updateSearch } from 'common/utils';

enum FILTER_TIRGGER {
  onChange = 'onChange',
  onSubmit = 'onSubmit',
}

const noop = () => {};
interface IPureFilterProps extends Omit<IFilterProps, 'config'> {
  config: FilterItemConfig[];
  filterTirgger?: FILTER_TIRGGER;
  connectUrlSearch?: boolean;
  urlExtra?: Obj;
  query?: Obj;
  updateSearch: (arg: Obj) => void;
  formatFormData?: (arg: Obj) => Obj;
  onFilter?: (value: Obj, arg?: any) => void;
}

const PureFilter = (props: IPureFilterProps) => {
  const {
    filterTirgger = 'onChange',
    connectUrlSearch = false,
    updateSearch,
    onFilter = noop,
    query = {},
    className = '',
    formatFormData,
    urlExtra,
    ...rest
  } = props;
  // const query = routeInfoStore.getState(s => s.query);
  const filterRef: any = React.useRef(null as any);
  useMount(() => {
    const { pageNo: _, ...fieldsValue } = query;
    // 关联url, 将query初始化到表单
    if (connectUrlSearch && !isEmpty(fieldsValue)) {
      setTimeout(() => {
        setFieldsValue(fieldsValue);
      }, 0);
    } else if (filterTirgger === 'onChange') {
      setTimeout(() => {
        // 只能在setTimeout中拿到初始请求的值
        const formData = filterRef.current?.form.getFieldsValue();
        changeFilterData(formData);
      }, 0);
    }
  });

  React.useEffect(() => {
    if (!isEmpty(urlExtra)) {
      const filterForm = get(filterRef, 'current.form');
      const filterData = filterForm ? filterForm.getFieldsValue() : {};
      updateSearch({ ...urlExtra, ...filterData });
    }
  }, [updateSearch, urlExtra]);

  const debounceFilter = debounce((filterData: Obj) => {
    if (connectUrlSearch) {
      updateSearch(filterData);
    }
    onFilter(filterData);
  }, 1000);

  // filter变化的时候调用
  const changeFilterData = (filterData: Obj) => {
    debounceFilter(filterData);
  };

  const setFieldsValue = (obj: Obj) => {
    const filterForm = get(filterRef, 'current.form');
    if (filterForm) {
      const filterFields = filterForm.getFieldsValue();
      const formValue = {};
      map(filterFields, ({ name: _key }) => has(obj, _key) && set(formValue, _key, obj[_key]));
      filterForm.setFieldsValue(formatFormData ? formatFormData(formValue) : formValue);
      changeFilterData(obj); // 带上除filter之外的其他参数，如pageNo
    }
  };

  const onFieldChange = (_: string, params: any) => {
    if (filterTirgger === 'onChange') {
      changeFilterData(params.formData);
    }
  };

  const filterProps = {
    onChange: {
      onFieldChange,
      actions: [],
    },
    onSubmit: {},
  };

  return (
    <BaseFilter
      onSubmit={changeFilterData}
      className={`dice-filter my-3 ${className}`}
      {...rest}
      {...(filterProps[filterTirgger] || {})}
      ref={filterRef}
    />
  );
};

interface IDiceFilterProps extends Omit<IPureFilterProps, 'query' | 'updateSearch'> {
  updateSearch?: (arg: Obj) => void;
}
const Filter = (props: IDiceFilterProps) => {
  const query = routeInfoStore.getState((s) => s.query);
  const updateSearch = React.useCallback((searchObj = {}) => _updateSearch(searchObj, { replace: true }), []);

  return <PureFilter query={query} updateSearch={updateSearch} {...props} />;
};

Filter.Pure = PureFilter;
export default Filter;
