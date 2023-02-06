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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import ReactDOM from 'react-dom';
import { Dropdown, Input, Menu, Checkbox, Tag, Empty, Spin } from 'antd';
import { map, isEmpty, isNumber, filter, find, isArray, get, isEqual } from 'lodash';
import { useEffectOnce, useDebounce, useDeepCompareEffect } from 'react-use';
import { Icon as CustomIcon, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { isPromise } from 'common/utils';
import i18n from 'i18n';

import './index.scss';
import { PAGINATION } from 'app/constants';

const { Item: MenuItem } = Menu;

export const SelectType = {
  Category: 'Category',
  Normal: 'Normal',
};
interface IProps {
  q?: string;
  className?: string;
  dropdownClassName?: string;
  category?: IOption[];
  list?: IOption[];
  forwardedRef?: any;
  LoadMoreRender?: React.ReactNode | null;
  type?: 'Category' | 'Normal';
  mode?: 'multiple';
  disabled?: boolean;
  loading?: boolean;
  value?: Array<IOption | string | number> | string | number;
  showSearch?: boolean;
  allowClear?: boolean;
  placeholder?: string;
  notFoundContent?: React.ReactNode;
  dropdownMatchSelectWidth?: boolean;
  onChangeCategory?: Function;
  size?: 'small' | 'normal';
  valueChangeTrigger?: 'onChange' | 'onClose';
  quickSelect?: React.ReactNode | React.ReactNode[];
  resultsRender?: (
    displayName: IOption[],
    deleteValue: (item: IOption) => void,
    isMultiple?: boolean,
    list?: IOption[],
  ) => React.ReactNode;
  onChange?: (arg: string[] | string, options?: IOption | IOption[]) => void;
  onClickItem?: (arg: IOption[] | IOption) => void;
  valueItemRender?: (
    item: IOption,
    deleteValue: (item: IOption) => void,
    isMultiple?: boolean,
    list?: IOption[],
  ) => React.ReactNode;
  chosenItemConvert?: (value: any, list: IOption[]) => any;
  changeQuery?: (q: string | undefined) => void;
  optionRender?: (option: any, type: string) => React.ReactNode;
  onDropdownVisible?: (visible: boolean) => void;
  onVisibleChange?: (visible: boolean, innerValue: any[][]) => void;
}

export interface IOption {
  label: string;
  value: string | number;
  [pro: string]: any;
}

const emptyFun = () => {};
const emptyArray = [] as IOption[];

const changeValue = (
  value: Array<IOption | string | number> | string | number = emptyArray,
  list: IOption[] = [],
  chosenItem: IOption[] = [],
) => {
  let v = [] as IOption[];
  if (isArray(value)) {
    v = map(value, (item) => {
      if (get(item, 'value') !== undefined) {
        return item;
      } else {
        const curItem = find([...list, ...chosenItem], (cItem) => `${cItem.value}` === `${item}`) || ({} as any);
        return { ...curItem, value: item, label: curItem.label };
      }
    }) as IOption[];
  } else {
    const curItem = find([...list, ...chosenItem], (cItem) => `${cItem.value}` === `${value}`) || ({} as any);
    v = [{ ...curItem, value, label: curItem.label }];
  }
  return v;
};

const defaultValueItemRender = (
  item: IOption,
  deleteValue: (item: IOption) => void,
  isMultiple?: boolean,
  list?: IOption[],
) => {
  const value =
    get(
      find(list, (cItem) => `${cItem.value}` === `${item.value}`),
      'label',
    ) ||
    item.label ||
    item.value;
  return isMultiple ? (
    <Tag key={item.value} size="small" closable onClose={() => deleteValue(item)}>
      {value}
    </Tag>
  ) : (
    value
  );
};

const PureLoadMoreSelector = (props: IProps) => {
  const {
    className = '',
    dropdownClassName = '',
    type: initType = SelectType.Normal,
    mode,
    value,
    onChange = emptyFun,
    onClickItem = emptyFun,
    placeholder = i18n.t('please select'),
    list = emptyArray,
    showSearch = true,
    allowClear = false,
    disabled = false,
    valueItemRender = defaultValueItemRender,
    chosenItemConvert,
    changeQuery = emptyFun,
    quickSelect = null,
    onDropdownVisible,
    onVisibleChange,
    dropdownMatchSelectWidth = true,
    valueChangeTrigger = 'onChange',
    forwardedRef,
    resultsRender,
    size = '',
    q: propsQ,
  } = props;
  const isMultiple = mode === 'multiple';
  const [visible, setVisible] = React.useState(false);
  const [chosenItem, setChosenItem] = React.useState([] as IOption[]);
  const [q, setQ] = React.useState(undefined as string | undefined);
  const [type, setType] = React.useState(initType);
  const [displayValue, setDisplayValue] = React.useState([] as IOption[]);
  const [contentWidth, setContentWidth] = React.useState('');
  const [innerValue, setInnerValue] = React.useState([value] as any[]);
  const [valueChanged, setValueChanged] = React.useState(false);
  const reqRef = React.useRef(null as any);

  const searchRef = React.useRef(null);
  const menuRef = React.useRef(null);
  const valueRef = React.useRef(null);
  useEffectOnce(() => {
    document.body.addEventListener('click', dropdownHide);

    if (forwardedRef) {
      forwardedRef.current = {
        show: (vis: boolean) => setVisible(vis),
      };
    }

    return () => {
      document.body.removeEventListener('click', dropdownHide);
    };
  });

  React.useEffect(() => {
    if (propsQ === undefined && q !== undefined) setQ(propsQ);
  }, [propsQ]);

  const searchRefCur = searchRef && searchRef.current;
  React.useEffect(() => {
    onDropdownVisible && onDropdownVisible(visible);
    if (visible && searchRefCur) {
      searchRefCur.focus();
    }
  }, [visible, searchRefCur]);

  // 带上select的dropdownMatchSelectWidth特性
  const dropdownMinWidth = get(document.querySelector('.load-more-selector-dropdown'), 'style.minWidth');
  React.useEffect(() => {
    if (dropdownMatchSelectWidth && dropdownMinWidth) {
      setContentWidth(dropdownMinWidth);
    }
  }, [dropdownMinWidth, dropdownMatchSelectWidth]);

  useDebounce(
    () => {
      // 如果是category时，清除q时不需要changeQuery，因为在changeCategory里会自动清理，此处阻止避免触发两个修改引起请求两次
      if (!(initType === SelectType.Category && !q)) changeQuery(q);
    },
    600,
    [q],
  );

  React.useEffect(() => {
    if (initType === SelectType.Category) {
      // 模式为category，内部根据q是否有值来切换模式
      if (type === SelectType.Category && q) {
        setType(SelectType.Normal);
      }
      if (type === SelectType.Normal && !q) {
        setType(SelectType.Category);
      }
    }
  }, [q, initType, type]);

  React.useEffect(() => {
    setInnerValue([value]);
  }, [value]);

  React.useEffect(() => {
    !visible && valueChangeTrigger === 'onClose' && valueChanged && onChange(...innerValue);
    setValueChanged(false);
  }, [visible]);

  React.useEffect(() => {
    if (isNumber(innerValue[0])) return setChosenItem(changeValue(innerValue[0], list, chosenItem));
    !isEmpty(innerValue[0]) ? setChosenItem(changeValue(innerValue[0], list, chosenItem)) : setChosenItem([]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [innerValue, list]);

  React.useEffect(() => {
    if (chosenItemConvert) {
      const val = map(chosenItem, (item) => {
        if (item.label) return item;
        const curVal = find(displayValue, (cItem) => `${cItem.value}` === `${item.value}`) || {};
        return !isEmpty(curVal) ? { ...item, ...curVal } : item;
      });

      const getNewValue = () => {
        reqRef.current = chosenItemConvert(isMultiple ? val : val[0], list);
        if (isPromise(reqRef.current)) {
          reqRef.current.then((res: IOption[]) => {
            setDisplayValue(Array.isArray(res) ? res : [res]);
          });
        } else {
          reqRef.current
            ? setDisplayValue(Array.isArray(reqRef.current) ? reqRef.current : [reqRef.current])
            : setDisplayValue([]);
        }
      };

      if (isPromise(reqRef.current)) {
        reqRef.current.then(() => {
          getNewValue();
        });
      } else {
        getNewValue();
      }
    } else {
      setDisplayValue(chosenItem);
    }
  }, [chosenItem]);

  const dropdownHide = (e: any) => {
    // 点击外部，隐藏选项
    const menuEl = menuRef && menuRef.current;
    const valueEl = valueRef && valueRef.current;
    // eslint-disable-next-line react/no-find-dom-node
    const el1 = ReactDOM.findDOMNode(menuEl) as HTMLElement;
    // eslint-disable-next-line react/no-find-dom-node
    const el2 = ReactDOM.findDOMNode(valueEl) as HTMLElement;
    if (!((el1 && el1.contains(e.target)) || (el2 && el2.contains(e.target)))) {
      setVisible(false);
    }
  };

  const setValue = (v: IOption[]) => {
    const newValue = isMultiple ? map(v, 'value') : get(v, '[0].value');
    if (isEqual(newValue, innerValue[0])) return;
    const [vals, opts] = isMultiple ? [map(v, 'value'), v] : [get(v, '[0].value'), v[0]];
    setInnerValue([vals, opts]);
    setValueChanged(true);
    (!visible || valueChangeTrigger === 'onChange') && onChange(vals, opts);
  };

  const clearValue = () => {
    setValue([]);
  };

  const deleteValue = (item: IOption) => {
    // 如果单选且不允许清除，则不能删数据
    if (!isMultiple && !allowClear) return;
    setValue(filter(chosenItem, (c) => `${c.value}` !== `${item.value}`));
  };
  const addValue = (item: IOption) => {
    if (isMultiple) {
      if (find(chosenItem, (cItem) => `${cItem.value}` === `${item.value}`)) {
        setValue(
          map(chosenItem, (cItem) => {
            return cItem.value === item.value ? { ...item } : { ...cItem };
          }),
        );
      } else {
        setValue([...chosenItem, item]);
      }
    } else {
      setValue([item]);
    }
  };

  const clickItem = (item: IOption, check: boolean) => {
    check ? addValue(item) : deleteValue(item);
    if (!isMultiple && check) setVisible(false); // 非多选模式，选中后隐藏
    onClickItem(item);
  };

  const getOverlay = () => {
    const Comp = CompMap[type];

    const curQuickSelect = isArray(quickSelect) ? quickSelect : quickSelect ? [quickSelect] : [];
    return (
      <Menu className="load-more-dropdown-menu" ref={menuRef} style={{ width: contentWidth }}>
        {showSearch
          ? [
              <MenuItem key="_search-item">
                <div className="search">
                  <Input
                    ref={searchRef}
                    size="small"
                    prefix={<CustomIcon type="search" />}
                    placeholder={i18n.t('search by keywords')}
                    value={q}
                    onChange={(e) => setQ(e.target.value)}
                  />
                </div>
              </MenuItem>,
              <Menu.Divider key="_search-divider" />,
            ]
          : null}
        {isMultiple
          ? [
              <MenuItem className="chosen-info" key="_chosen-info-item">
                <div className={''}>
                  {i18n.t('common:selected')}
                  &nbsp;
                  <span>{chosenItem.length}</span>
                  &nbsp;
                  {i18n.t('common:item')}
                </div>
                <span className="fake-link ml-2" onClick={clearValue}>
                  {i18n.t('common:clear selected')}
                </span>
              </MenuItem>,
              <Menu.Divider key="_chosen-info-divider" />,
            ]
          : null}
        {curQuickSelect.map((quickSelectItem, idx) => {
          return [<MenuItem key={`quick-select-${idx}`}>{quickSelectItem}</MenuItem>, <Menu.Divider />];
        })}
        <MenuItem className="options" key="options">
          <Comp {...props} width={contentWidth} clickItem={clickItem} value={chosenItem} isMultiple={isMultiple} />
          {/* {
            isMultiple && (
              <div className={`chosen-info ${type === SelectType.Normal ? 'border-top' : ''}`}>
                {i18n.t('common:selected')}
                <span>{chosenItem.length}</span>
                {i18n.t('common:item')}
              </div>
            )
          } */}
        </MenuItem>
      </Menu>
    );
  };
  return (
    <div className={`load-more-selector ${className}`} ref={valueRef} onClick={(e) => e.stopPropagation()}>
      <Dropdown
        overlay={getOverlay()}
        visible={visible}
        overlayClassName={`${visible ? 'load-more-selector-dropdown' : ''} ${dropdownClassName}`}
        onVisibleChange={(visible) => onVisibleChange?.(visible, innerValue)}
      >
        <div
          className={`results cursor-pointer ${disabled ? 'not-allowed' : ''} ${size}`}
          onClick={() => {
            !disabled && !visible && setVisible(true);
          }}
        >
          {resultsRender ? (
            resultsRender(displayValue, deleteValue, isMultiple, list)
          ) : (
            <div className="values">
              {map(displayValue, (item) => (
                <div key={item.value} className="value-item">
                  {valueItemRender(item, deleteValue, isMultiple, list)}
                </div>
              ))}
              {placeholder && isEmpty(chosenItem) ? <span className="placeholder">{placeholder}</span> : null}
            </div>
          )}
          {allowClear && !isEmpty(chosenItem) ? (
            <ErdaIcon
              type="close-one"
              className="close"
              size="14px"
              onClick={(e: any) => {
                e.stopPropagation();
                clearValue();
              }}
            />
          ) : null}
        </div>
      </Dropdown>
    </div>
  );
};

interface ICompProps extends IProps {
  clickItem: (item: IOption, check: boolean) => void;
  value: IOption[];
  isMultiple: boolean;
  viewType: string;
  width: string;
}

const OptionContainer = ({ list, value, clickItem, optionRender, isMultiple, viewType }: ICompProps) => {
  return (
    <>
      {map(list, (item) => {
        const curOpt = find(value, (cItem) => `${cItem.value}` === `${item.value}`) as IOption;
        const checked = !!curOpt;
        const options = optionRender ? optionRender(item, (viewType || '').toLowerCase()) : item.label || item.value;
        return isMultiple ? (
          <Checkbox
            className="load-more-list-item"
            key={item.value}
            checked={checked}
            onChange={(e: any) => clickItem(item, e.target.checked)}
          >
            {options}
          </Checkbox>
        ) : (
          <div
            className={`load-more-list-item radio-item ${checked ? 'checked' : ''}`}
            key={item.value}
            onClick={() => clickItem(item, !checked)}
          >
            {options}
          </div>
        );
      })}
    </>
  );
};

const CompMap = {
  Category: (props: ICompProps) => {
    const { category = [], list, onChangeCategory, LoadMoreRender, width, loading } = props;
    const [chosenCategory, setChosenCategory] = React.useState('' as string | number);
    React.useEffect(() => {
      if (!chosenCategory && !isEmpty(category)) {
        setChosenCategory(category[0].value);
      }
    }, [category, chosenCategory]);
    React.useEffect(() => {
      chosenCategory && onChangeCategory && onChangeCategory(chosenCategory);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [chosenCategory]);
    return (
      <div className="load-more-selector-container">
        <div className="content" style={width ? { width } : {}}>
          <div className="menu">
            <Menu selectedKeys={[`${chosenCategory}`]} className="h-full">
              {map(category, (item) => (
                <MenuItem className="menu-item" key={item.value} onClick={() => setChosenCategory(item.value)}>
                  {item.label}
                </MenuItem>
              ))}
              {isEmpty(category) && <Empty />}
            </Menu>
          </div>
          <div className="list">
            <OptionContainer {...props} viewType="category" />
            {isEmpty(list) ? (
              <Spin spinning={loading}>
                <Empty />
              </Spin>
            ) : (
              LoadMoreRender
            )}
          </div>
        </div>
      </div>
    );
  },
  Normal: (props: ICompProps) => {
    const { list, LoadMoreRender, notFoundContent, width, loading } = props;
    return (
      <div className="load-more-selector-container" style={width ? { width } : {}}>
        <div className="content normal">
          <div className="list">
            <OptionContainer {...props} viewType="normal" />
            {isEmpty(list) ? <Spin spinning={loading}>{notFoundContent || <Empty />}</Spin> : LoadMoreRender}
          </div>
        </div>
      </div>
    );
  },
};

export interface ILoadMoreSelectorProps extends IProps {
  pageSize?: number;
  list?: IOption[];
  forwardedRef?: any;
  extraQuery?: object;
  getData?: (arg: any) => Promise<{ list: IOption[]; total: number }>;
  dataFormatter?: (data: { list: any[]; total: number }) => { list: IOption[]; total: number };
}

const DefaultLoadMoreRender = ({ onLoadMore, loading }: { onLoadMore: () => void; loading: boolean }) => {
  return (
    <div
      className="cursor-pointer load-more load-more-list-item"
      onClick={(e) => {
        e.stopPropagation();
        onLoadMore();
      }}
    >
      <ErdaIcon type="loading" className="align-middle" spin={loading} />
      {i18n.t('load more')}
    </div>
  );
};

const defaultDataFormatter = ({ list, total }: { list: any[]; total: number }) => ({
  total,
  list: map(list, ({ name, id, ..._rest }) => ({
    ..._rest,
    name,
    id,
    label: name,
    value: id,
  })),
});

const LoadMoreSelector = (props: ILoadMoreSelectorProps) => {
  const {
    getData,
    pageSize = PAGINATION.pageSize,
    list: allList = emptyArray,
    LoadMoreRender = DefaultLoadMoreRender,
    onDropdownVisible: propsOnDropdownVisible,
    onChangeCategory: changeCategory,
    type,
    dataFormatter = defaultDataFormatter,
    extraQuery = {},
    ...rest
  } = props;

  const [{ q, pageNo, hasMore, chosenCategory, firstReq, list, visible, loading }, updater, update] = useUpdate({
    pageNo: 1,
    q: undefined as string | undefined,
    hasMore: false,
    chosenCategory: undefined as string | undefined,
    firstReq: false,
    list: allList as IOption[],
    visible: false,
    loading: false,
  });

  const isStaticData = !getData; // 是否是静态数据模式
  const isCategoryMode = type === SelectType.Category;

  React.useEffect(() => {
    if (isStaticData) {
      // 静态数据
      updater.list(q ? filter(allList, ({ label }) => (label || '').toLowerCase().includes(q.toLowerCase())) : allList);
    }
  }, [allList, q, isStaticData]);

  React.useEffect(() => {
    propsOnDropdownVisible?.(visible);
  }, [visible]);

  React.useEffect(() => {
    if (!isStaticData) {
      if (visible && isEmpty(list) && !firstReq && !isCategoryMode) {
        // 第一次请求，在visible的时候请求，但如果是category模式，会自动changeCategory触发
        updater.firstReq(true);
        getList({ pageNo: 1 });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isStaticData, list, visible, firstReq]);

  useDeepCompareEffect(() => {
    // 外部查询条件改变，重置state，在visible改变的时候，重新出发上面的请求
    if (!isEmpty(extraQuery)) {
      update({
        firstReq: false,
        list: [],
        hasMore: false,
        chosenCategory: undefined,
        q: undefined,
        pageNo: 1,
      });
    }
  }, [extraQuery]);

  const getList = (params?: any) => {
    const query = { pageSize, pageNo: 1, q, category: chosenCategory, ...extraQuery, ...params };
    if (query.q) query.category = undefined; // 类查询和关键字查询对立
    updater.loading(true);
    const res = getData && getData(query);
    if (res && isPromise(res)) {
      res.then((resData) => {
        const { total, list: curList } = dataFormatter(resData);
        updater.hasMore(Math.ceil(total / pageSize) > query.pageNo);
        let newList = [];
        if (query.pageNo === 1) {
          newList = [...curList];
        } else {
          newList = [...list, ...curList];
        }
        updater.list(newList);
        updater.loading(false);
      });
    }
  };

  const changeQuery = (qStr?: string) => {
    update({
      pageNo: 1,
      q: qStr || undefined,
    });
    !isStaticData && visible && getList({ pageNo: 1, q: qStr || undefined });
  };

  const onDropdownVisible = (vis: boolean) => {
    updater.visible(vis);
  };
  const onLoadMore = () => {
    updater.pageNo(pageNo + 1);
    getList({ pageNo: pageNo + 1, category: chosenCategory });
  };

  const onChangeCategory = (category: string) => {
    if (isCategoryMode && category) {
      changeCategory && changeCategory(category);
      updater.chosenCategory(category);
      update({
        q: undefined,
        pageNo: 1,
        chosenCategory: category,
      });
      getList({ pageNo: 1, q: undefined, category });
    }
  };

  const LoadMoreComp = (LoadMoreRender || DefaultLoadMoreRender) as React.ReactType;

  const LoadMore = hasMore ? <LoadMoreComp onLoadMore={onLoadMore} loading={loading} /> : null;
  return (
    <PureLoadMoreSelector
      onDropdownVisible={onDropdownVisible}
      list={list}
      type={type}
      loading={loading}
      LoadMoreRender={LoadMore}
      changeQuery={changeQuery}
      onChangeCategory={onChangeCategory}
      q={q}
      {...rest}
    />
  );
};
export default LoadMoreSelector;

/** *
 * LoadMoreSelector @usage
 * 1、在formModal中使用：
 * ```
    const fieldList = [{
        label: i18n.t('dop:choose certificate'),
        name: 'certificateId',
        getComp: () => {
          const getData = (q: any) => {
            const { q: searchKey, ...qRest } = q;
            return getCertificateList({ ...qRest, name: searchKey, appId }).then((res: any) => res.data);
          };
          return (
            <LoadMoreSelector
              getData={getData}
            />
          );
        },
      }],
  ```
 * 注意：
1、展开后开始第一次请求
2、dataFormatter: ({list,total}) => ({list,total})，转换后的list需要包含label和value，默认转换id => value，name => label
3、chosenItemConvert: 选中展示的值（避免编辑的时候，展示id），接收普通值或promise，具体使用参考member-selector
4、条件查询：若展开后的查询有前置条件，比如前一个表单选中后，则在getData方法里根据条件返回undefined或promise，见 app-version-push
      ```
      const getData = (q:any)=>{
        if(!chosenApp) return;
        return getJoinedApps({...q,appId: chosenApp})
          .then(res=>res.data)
      }
      ```
5、若需要监听4中的条件查询会变化，需传入extraQuery={{appId: chosenApp}}

 * */
