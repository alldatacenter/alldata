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
import { Button, Select, InputNumber, Input, Row, Col, Drawer, Tooltip, Tag } from 'antd';
import { map, forEach, isArray, isPlainObject, isEmpty, compact, get, find } from 'lodash';
import { Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { updateSearch, qs } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import i18n from 'i18n';
import './index.scss';

const { Option } = Select;
const noop = () => {};

interface IFilterItem {
  name: string;
  type?: string;
  options?:
    | Array<{
        name: string;
        value: string | number;
      }>
    | Function;
  Comp?: React.ReactElement<any>;
  label?: string;
  onChange?: (...a: any) => any;
  filterBarValueConvert?: (...arg: any) => string;
  [prop: string]: any;
}

const defaultConvert = (val: string | any[]) => (isArray(val) ? val.join(',') : val);
const defaultConvertSelect = (_val: any, options: any) =>
  map(isArray(options) ? options : [options], (item) => item.props.children).join(',');
const Field = ({ type, update, options = [], onChange = noop, filterBarValueConvert, ...itemProps }: IFilterItem) => {
  let ItemComp = null;
  const specialConfig: any = {};
  let wrapOnChange = onChange;
  const { getComp, Comp, ...rest } = itemProps;
  switch (type) {
    case 'custom':
      wrapOnChange = (...args: any) => {
        const filterDisplayName = (filterBarValueConvert || defaultConvert)(...args);
        const newData = update(itemProps.name, args[0], filterDisplayName);
        onChange(...args, newData);
      };
      ItemComp =
        typeof getComp === 'function'
          ? getComp(wrapOnChange)
          : Comp
          ? React.cloneElement(Comp, {
              ...rest,
              onChange: wrapOnChange,
            })
          : null;
      break;
    case 'select':
      if (itemProps.mode === 'multiple') {
        specialConfig.valuePropType = 'array';
      }
      wrapOnChange = (...args: any) => {
        if (args[0] === undefined) {
          update(itemProps.name, args[0]);
          return onChange(...args);
        }
        const filterDisplayName = (filterBarValueConvert || defaultConvertSelect)(...args);
        const newData = update(itemProps.name, args[0], filterDisplayName);
        onChange(...args, newData);
      };
      ItemComp = (
        <Select {...rest} onChange={wrapOnChange}>
          {typeof options === 'function'
            ? options()
            : options.map((single) => (
                <Option key={single.value} value={`${single.value}`}>
                  {single.name}
                </Option>
              ))}
        </Select>
      );
      break;
    case 'inputNumber':
      wrapOnChange = (value: number | string) => {
        const newData = update(itemProps.name, value);
        onChange(value, newData);
      };
      ItemComp = <InputNumber {...rest} onChange={wrapOnChange} />;
      break;
    case 'input':
    default:
      wrapOnChange = (e: any) => {
        const newData = update(itemProps.name, e.target.value);
        onChange(e, newData);
      };
      ItemComp = <Input {...rest} onChange={wrapOnChange} />;
      break;
  }
  return ItemComp || null;
};

interface IBaseFilterProps {
  list: IFilterItem[];
  showReset?: boolean;
  query?: any;
  syncUrlOnSearch?: boolean;
  onChange?: (value: object) => any;
  onSearch?: (value: object) => any;
  onReset?: () => any;
}
interface IFilterCoreProps extends IBaseFilterProps {
  children: Function;
}

export const FilterBarHandle = {
  splitKey: '||',
  filterDataKey: '_Q_',
  queryToData: (str = '') => {
    const strArr = str.split(FilterBarHandle.splitKey);
    const _data = {};
    if (!isEmpty(compact(strArr))) {
      map(strArr, (item) => {
        const [key, val] = item.split('[');
        _data[key] = val.slice(0, -1);
      });
    }
    return _data;
  },
  dataToQuery: (_data: object = {}) => {
    const strArr = [] as string[];
    map(_data, (val, key) => {
      val && strArr.push(`${key}[${val}]`);
    });
    return strArr.join(FilterBarHandle.splitKey);
  },
};

export interface IFilterReturnProps {
  CompList: JSX.Element[];
  resetButton: JSX.Element;
  searchButton: JSX.Element;
  search: () => any;
  reset: () => any;
}

/**
 * 基础过滤组件
 * @param list 同RenderForm类似的field配置列表
 * @param onSearch
 * @param onChange
 * @param onReset
 * @param syncUrlOnSearch 是否将query自动同步到url上
 * @param query 初始数据
 * @param children 传入一个方法，参数包含：
  ```
    * CompList, // 字段组件列表
    * resetButton, // 重置按钮
    * searchButton, // 查询按钮
    * search, // 查询方法，自定义按钮时使用
    * reset, // 重置方法，自定义按钮时使用
  ```
 */
export const FilterCore = ({ list, onSearch, onChange, onReset, syncUrlOnSearch, children }: IFilterCoreProps) => {
  const query = routeInfoStore.useStore((s) => s.query);
  const fieldList = [] as IFilterItem[];
  const initData = {};
  forEach(list, ({ ...rest }) => {
    fieldList.push(rest);
    if (rest.name) {
      initData[rest.name] = query[rest.name] || rest.value || rest.defaultValue;
    }
  });
  if (query[FilterBarHandle.filterDataKey]) {
    initData[FilterBarHandle.filterDataKey] = query[FilterBarHandle.filterDataKey];
  }
  const [filterData, setFilterData] = React.useState(initData);

  const update = (name: string, value: any, displayName?: string) => {
    const newData = {
      ...filterData,
      [name]: value === '' ? undefined : value,
      [FilterBarHandle.filterDataKey]: FilterBarHandle.dataToQuery({
        ...FilterBarHandle.queryToData(filterData[FilterBarHandle.filterDataKey]),
        [name]: displayName,
      }),
    };

    setFilterData(newData);
    (onChange || noop)(newData);
    return newData;
  };

  const search = () => {
    (onSearch || noop)(filterData);
    const QKey = FilterBarHandle.filterDataKey;
    syncUrlOnSearch &&
      updateSearch(filterData, {
        sort: (a: string, b: string) => (a === QKey ? 1 : b === QKey ? -1 : 0),
      });
  };

  const reset = () => {
    setFilterData(initData);
    (onReset || noop)();
    if (syncUrlOnSearch) {
      const resetFields = list.reduce((acc, item) => {
        acc[item.name] = undefined;
        return acc;
      }, {});
      updateSearch(resetFields);
    }
  };

  const fixFun = (v: any) => v;
  const CompList = map(fieldList, (item) => {
    const valueFixOut = item.valueFixOut || fixFun;
    return (
      <Field
        key={item.name}
        {...item}
        value={filterData[item.name]}
        update={(name: string, value: any, displayName?: string) => update(name, valueFixOut(value), displayName)}
      />
    );
  });
  const resetButton = <Button onClick={reset}>{i18n.t('reset')}</Button>;
  const searchButton = (
    <Button type="primary" onClick={search}>
      {i18n.t('search')}
    </Button>
  );

  return children({
    CompList, // 字段组件列表
    resetButton, // 重置按钮
    searchButton, // 查询按钮
    search, // 查询方法，自定义按钮时使用
    reset, // 重置方法，自定义按钮时使用
  });
};

interface IFilterGroupProps extends IBaseFilterProps {
  reversePosition?: boolean; // 交换左右块位置
  children?: JSX.Element | null;
}

/**
 * 过滤项配置，左侧占满剩余空间
 * @param list 过滤项配置列表
 * @param reversePosition 是否切换左右块位置
 * @param onChange 内容变动时触发
 * @param onSearch 传入时会显示查询按钮，并在点击时触发
 * @param onReset 传入时会显示重置按钮，并在点击时触发
 */
export const FilterGroup = ({
  list,
  reversePosition = false,
  onSearch,
  onChange,
  onReset,
  syncUrlOnSearch,
  children,
}: IFilterGroupProps) => {
  return (
    <FilterCore list={list} onSearch={onSearch} onChange={onChange} onReset={onReset} syncUrlOnSearch={syncUrlOnSearch}>
      {({ CompList, resetButton, searchButton }: IFilterReturnProps) => {
        let left = CompList;
        let right = children || (
          <>
            {onReset ? resetButton : null}
            {onSearch ? searchButton : null}
          </>
        );
        if (reversePosition) {
          [left, right] = [right, left] as any[];
        }
        return (
          <div className="filter-group-bar flex justify-between items-center">
            <div className="filter-group-left flex-1">{left}</div>
            <div className="filter-group-right ml-6">{right}</div>
          </div>
        );
      }}
    </FilterCore>
  );
};

interface IToolBarWithFilter extends IBaseFilterProps {
  children: any;
  filterValue: object;
  className?: string;
  onClose?: () => any;
}
export const ToolBarWithFilter: any = React.forwardRef((props: IToolBarWithFilter, ref: any) => {
  const {
    list,
    children,
    onSearch: search = noop,
    onClose: close = noop,
    className = '',
    filterValue,
    ...rest
  } = props;
  const [{ visible }, updater] = useUpdate({ visible: false });
  const childrenWithProps = React.Children.map(children, (child) => {
    return child?.type === ToolBarWithFilter.FilterButton
      ? React.cloneElement(child, {
          onClick: () => updater.visible(true),
        })
      : child;
  });

  React.useEffect(() => {
    if (ref) {
      ref.current = {
        onSearchWithFilterBar: (sObj: any) => {
          const k = Object.keys(sObj)[0];
          const v = Object.values(sObj)[0] || undefined;
          const filterBarValueConvert = get(find(list, { name: k }), 'filterBarValueConvert');
          const newData = {
            ...filterValue,
            [k]: v,
            [FilterBarHandle.filterDataKey]: FilterBarHandle.dataToQuery({
              ...FilterBarHandle.queryToData(filterValue[FilterBarHandle.filterDataKey]),
              [k]: filterBarValueConvert ? filterBarValueConvert(v) : v,
            }),
          };
          onSearch(newData);
        },
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterValue, ref]);

  const onSearch = (val: object) => {
    const reVal = { ...val };
    if (!reVal[FilterBarHandle.filterDataKey]) reVal[FilterBarHandle.filterDataKey] = undefined; // 除去空字符串
    search(reVal);
    onClose();
  };

  const onClose = () => {
    updater.visible(false);
    close();
  };

  const getFilterValue = (val: any, name: string, convertFun: Function = noop) => {
    const curVal = get(val, name);
    const searchStr = qs.parse(location.search)[FilterBarHandle.filterDataKey] || ('' as string);
    const valueObj = { ...FilterBarHandle.queryToData(searchStr as string) };
    let finalVal = get(valueObj, name);
    if (isEmpty(finalVal) && curVal) {
      // filter有值但在_Q_中找不到，则直接调用convertFun,
      finalVal = convertFun(curVal) || curVal;
    }
    return isArray(finalVal) ? finalVal.join(', ') : finalVal;
  };

  const FilterBar = React.useMemo(() => {
    const filterBar = [] as any[];
    const filterBarKeys = [] as string[];
    map(list, (item) => {
      const curVal = getFilterValue(filterValue, item.name, item.filterBarValueConvert);
      if (!(curVal === undefined || curVal === '' || ((isArray(curVal) || isPlainObject(curVal)) && isEmpty(curVal)))) {
        filterBarKeys.push(item.name);
        filterBar.push(
          <Tag
            className="mb-1"
            closable
            key={item.name}
            onClose={(e: any) => {
              e.stopPropagation();
              const newData = {
                ...filterValue,
                [item.name]: undefined,
                [FilterBarHandle.filterDataKey]: FilterBarHandle.dataToQuery({
                  ...FilterBarHandle.queryToData(filterValue[FilterBarHandle.filterDataKey]),
                  [item.name]: undefined,
                }),
              };
              onSearch({ ...newData });
            }}
          >
            {item.label}: {curVal}
          </Tag>,
        );
      }
    });
    if (!isEmpty(filterBar)) {
      filterBar.push(
        <div
          key="_clear"
          className="clear"
          onClick={() => {
            let newData = { ...filterValue };
            map(filterBarKeys, (_k) => {
              newData = {
                ...newData,
                [_k]: undefined,
                [FilterBarHandle.filterDataKey]: FilterBarHandle.dataToQuery({
                  ...FilterBarHandle.queryToData(newData[FilterBarHandle.filterDataKey]),
                  [_k]: undefined,
                }),
              };
            });
            onSearch(newData);
          }}
        >
          {i18n.t('clear')}
        </div>,
      );
    }
    return filterBar;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterValue, location.search]);

  return (
    <div className={`toolbar-with-filter ${isEmpty(FilterBar) ? '' : 'with-data'}`}>
      <div className={`tools-container ${className}`}>{childrenWithProps}</div>
      <div className={'filter-bar'}>{FilterBar}</div>
      <FilterGroupDrawer {...rest} visible={visible} list={list} onSearch={onSearch} onClose={onClose} />
    </div>
  );
});

interface IFilterButtonProps {
  btnClassName?: string;
  onClick?: () => void;
}
ToolBarWithFilter.FilterButton = ({ onClick = noop, btnClassName = '' }: IFilterButtonProps) => {
  return (
    <Tooltip title={i18n.t('common:advanced filter')}>
      <Button onClick={onClick} className={btnClassName}>
        <CustomIcon type="filter" />
      </Button>
    </Tooltip>
  );
};

interface IIFilterDrawerProps extends IBaseFilterProps {
  visible: boolean;
  onClose: () => any;
}

export const FilterGroupDrawer = ({
  visible,
  onClose,
  list,
  onSearch,
  onChange,
  onReset,
  syncUrlOnSearch,
}: IIFilterDrawerProps) => {
  return (
    <Drawer
      width="520"
      onClose={onClose}
      visible={visible}
      destroyOnClose
      title={i18n.t('common:advanced filter')}
      className="dice-drawer advanced-filter-drawer"
    >
      <FilterCore
        list={list}
        onSearch={onSearch}
        onChange={onChange}
        onReset={onReset}
        syncUrlOnSearch={syncUrlOnSearch}
      >
        {({ CompList, search }: IFilterReturnProps) => {
          return (
            <div className="filter-group-drawer">
              {CompList.length === 1 ? (
                <div>
                  <div className="filter-label">{list[0].label}</div>
                  {CompList[0]}
                </div>
              ) : (
                CompList.map((item, i: number) => {
                  if (i % 2 === 1) {
                    return null;
                  }
                  const hasSecond = !!list[i + 1];
                  return (
                    <Row key={list[i].name} gutter={12}>
                      <Col span={12}>
                        <div className="filter-label">{list[i].label}</div>
                        {item}
                      </Col>
                      {hasSecond ? (
                        <Col span={12}>
                          <div className="filter-label">{list[i + 1].label}</div>
                          {CompList[i + 1]}
                        </Col>
                      ) : null}
                    </Row>
                  );
                })
              )}
              <div className="drawer-footer ml-3-group">
                <Button
                  onClick={() => {
                    onClose();
                  }}
                >
                  {i18n.t('cancel')}
                </Button>
                <Button type="primary" ghost onClick={search}>
                  {i18n.t('filter')}
                </Button>
              </div>
            </div>
          );
        }}
      </FilterCore>
    </Drawer>
  );
};

FilterGroup.Drawer = FilterGroupDrawer;
FilterGroup.FilterBarHandle = FilterBarHandle;
export default FilterGroup;
