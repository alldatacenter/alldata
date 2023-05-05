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

import React, { useRef } from 'react';
import { useDeepCompareEffect, useMedia, useSetState, useSize, useUpdateEffect } from 'react-use';
import { notify } from 'common/utils';
import { DragSourceMonitor, DropTargetMonitor, useDrag, useDrop, XYCoord } from 'react-dnd';
import FormModal from './components/form-modal';
import { Pagination } from 'antd';
import { FilterBarHandle } from 'common/components/filter-group';
import { setSearch } from './utils';
import { debounce, every, forIn, get, isEmpty, isEqual, isFunction, mapValues, omit, set, some, sortBy } from 'lodash';
import moment, { Moment } from 'moment';
import routeInfoStore from 'core/stores/route';
import { IUseFilterProps, IUseMultiFilterProps } from 'app/interface/common';
import { PAGINATION } from 'app/constants';
import screenfull from 'screenfull';

export enum ScreenSize {
  xs = 768,
  sm = 1024,
  md = 1440,
  lg = 1680,
  xl = 1920,
}

const defaultIgnoreWidth = 48 + 200 + 32; // nav(48px) + sidebar(200px) + main Padding(32px)

/**
 * 批量观察断点响应, 返回从xs到xl的检查值
 * xs:768  sm:1024  md:1440  lg:1680  xl:1920
 * @param main 只观察主区域（不含nav和sidebar）
 * @param ignoreWidth 忽略的宽度（默认为nav+sidebar）
 * @return [largeThanXs, largeThanSm, ...]
 */
export const useMediaRange = (main = false, ignoreWidth = defaultIgnoreWidth) => {
  const extraWidth = main ? ignoreWidth : 0;

  const largeThanXs = useMedia(`(min-width: ${ScreenSize.xs + extraWidth}px)`);
  const largeThanSm = useMedia(`(min-width: ${ScreenSize.sm + extraWidth}px)`);
  const largeThanMd = useMedia(`(min-width: ${ScreenSize.md + extraWidth}px)`);
  const largeThanLg = useMedia(`(min-width: ${ScreenSize.lg + extraWidth}px)`);
  const largeThanXl = useMedia(`(min-width: ${ScreenSize.xl + extraWidth}px)`);
  return [largeThanXs, largeThanSm, largeThanMd, largeThanLg, largeThanXl];
};

/**
 * 返回小、中、大屏的检查值, 为了配合设计给的三个响应点
 *
 * 内容区宽度 > 600 | 1024 | 1440
 * @return [largeThan600, largeThan1024, largeThan1440]
 */
export const useMediaSize = () => {
  const largeThan600 = useMedia(`(min-width: ${600 + defaultIgnoreWidth}px)`);
  const largeThan1024 = useMedia(`(min-width: ${1024 + defaultIgnoreWidth}px)`);
  const largeThan1440 = useMedia(`(min-width: ${1440 + defaultIgnoreWidth}px)`);
  return [largeThan600, largeThan1024, largeThan1440];
};

export const useMediaGt = (num: number, main = false, ignoreWidth = defaultIgnoreWidth) =>
  useMedia(`(min-width: ${num + (main ? ignoreWidth : 0)}px)`);
export const useMediaLt = (num: number, main = false, ignoreWidth = defaultIgnoreWidth) =>
  useMedia(`(max-width: ${num + (main ? ignoreWidth : 0)}px)`);

export const useComponentWidth = () => {
  const [sized, { width }] = useSize(() => <div style={{ width: '100%' }} />);
  return [sized, width];
};

type on = () => void;
type off = () => void;
type toggle = () => void;
/**
 * 切换boolean值
 * @param initValue
 * @return [ value, on, off, toggle ]
 */
export const useSwitch = (initValue: boolean): [boolean, on, off, toggle] => {
  const [bool, setBool] = React.useState(Boolean(initValue));

  return [
    bool,
    function on() {
      setBool(true);
    },
    function off() {
      setBool(false);
    },
    function toggle() {
      setBool(!bool);
    },
  ];
};

interface DragItem {
  index: number;
  type: string;
}

interface IDragProps {
  type: string;
  index: number;
  onMove: (dragIndex: number, hoverIndex: number) => any;
  collect?: (monitor: DragSourceMonitor) => any;
}

/**
 * 列表项拖拽排序
 * @param type {string} 类型标记
 * @param index {number} 序号
 * @param onMove 参数(fromIndex, toIndex) 移动后的回调
 * @param collect 参数(monitor) 获得拖拽中的相关信息
 * @return [dragRef, previewRef, collectedProps]
 */
export const useListDnD = ({ type, index, onMove, collect }: IDragProps) => {
  const dragRef = useRef<HTMLDivElement>(null);

  const [, drop] = useDrop({
    accept: type,
    hover(item: DragItem, monitor: DropTargetMonitor) {
      if (!dragRef.current) {
        return;
      }
      const dragIndex = item.index;
      const hoverIndex = index;

      // Don't replace items with themselves
      if (dragIndex === hoverIndex) {
        return;
      }

      // Determine rectangle on screen
      const hoverBoundingRect = dragRef.current!.getBoundingClientRect();

      // Get vertical middle
      const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;

      // Determine mouse position
      const clientOffset = monitor.getClientOffset();

      // Get pixels to the top
      const hoverClientY = (clientOffset as XYCoord).y - hoverBoundingRect.top;

      // Only perform the move when the mouse has crossed half of the items height
      // When dragging downwards, only move when the cursor is below 50%
      // When dragging upwards, only move when the cursor is above 50%

      // Dragging downwards
      if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
        return;
      }

      // Dragging upwards
      if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
        return;
      }

      // Time to actually perform the action
      onMove(dragIndex, hoverIndex);

      // Note: we're mutating the monitor item here!
      // Generally it's better to avoid mutations,
      // but it's good here for the sake of performance
      // to avoid expensive index searches.
      item.index = hoverIndex;
    },
  });

  const [collectedProps, drag, previewRef] = useDrag({
    item: { index, type },
    collect,
  });

  drag(drop(dragRef));

  return [dragRef, previewRef, collectedProps];
};

type UpdateFn<T> = (patch: Partial<T> | ((prevState: T) => Partial<T>)) => void;
type UpdatePartFn<U> = (patch: U | Function) => void;

type UpdaterFn<T> = {
  [K in keyof T]: UpdatePartFn<T[K]>;
};

// 测试 useUpdate 类型
// const Test = () => {
//   const [state, updater] = useUpdate({
//     str: '',
//     num: 0,
//     bool: true,
//     undef: undefined,
//     nul: null,
//     strList: ['one', 'two'],
//     numList: [1, 2],
//     multiList: ['one', 1, false], // 允许包含的类型
//     emptyList: [], // 允许任何数组对象
//     emptyObj: {}, // 允许任何对象
//     obj: { k: 'v', n: 2 }, // 允许相同形状对象
//     objList: [{ ok: true, msg: 'yes' }], // 允许设置任意对象
//   });
//   updater.nul({ any: true });
//   updater.multiList([1, '']);
//   updater.emptyList([1, '']);
//   updater.emptyObj({ a: 2 });
//   updater.objList([{ ok: false, msg: '' }]);
//   return null;
// };

type NullableValue<T> = {
  [K in keyof Required<T>]: T[K] extends null
    ? null | Obj // 初始状态里对象值可能是null
    : T[K] extends never[]
    ? any[] // 初始值是空数组，则认为可放任意结构数组
    : T[K] extends { [p: string]: never }
    ? Obj // 初始值是空对象，不限制内部结构，是object类型即可
    : T[K];
};

type ResetFn = () => void;

/**
 * 状态更新
 * @param initState 初始状态
 * @return [state, updateAll, updater]
 */
export const useUpdate = <T extends object>(
  initState: NullableValue<T>,
): [NullableValue<T>, UpdaterFn<NullableValue<T>>, UpdateFn<NullableValue<T>>, ResetFn] => {
  const [state, _update] = useSetState<NullableValue<T>>(initState || {});
  // 使用ref，避免updater的更新方法中，在闭包里使用上次的state
  const ref = React.useRef(state);
  ref.current = state;

  const update: any = React.useCallback(
    (args: any) => {
      // 扩展 update 的使用, 使用方法同 useState((preState) => preState + 1)
      if (isFunction(args)) {
        return _update((prev) => args(prev));
      } else {
        return _update(args);
      }
    },
    [_update],
  );

  const updater: any = React.useMemo(() => {
    const result = {};
    Object.keys(ref.current).forEach((k) => {
      result[k] = (patch: Function | any) => {
        const newPart = patch instanceof Function ? patch(ref.current[k], ref.current) : patch;
        ref.current[k] = newPart;
        return _update({ [k]: newPart } as Partial<NullableValue<T>>);
      };
    });
    return result;
  }, [_update]);

  const reset = React.useCallback(() => _update(initState), [_update, initState]);

  return [state, updater, update, reset];
};

/**
 * FormModal包装hook，内部封装visible状态
 * @param param.visible 初始状态
 * @param param.Form 分离的FormModal
 * @return [FormModal, toggle]
 */
export const useFormModal = (
  { visible: initVisible = false, Form = FormModal } = {
    visible: false,
    Form: FormModal,
  },
) => {
  const [visible, toggleVisible] = React.useState(!!initVisible);
  const toggle = (vi: boolean) => {
    const newVal = typeof vi === 'boolean' ? vi : !visible;
    toggleVisible(newVal);
    return newVal;
  };
  const Modal = (props: any) => {
    const onCancel = () => toggleVisible(false);
    return <Form onCancel={onCancel} {...props} visible={visible} />;
  };

  return [Modal, toggle as any];
};

const defaultPaging = {
  pageNo: 1,
  pageSize: 15,
  total: 0,
  hasMore: true,
};

interface ITempPagingParams<T> {
  initList?: T[];
  append?: boolean;
  listKey?: string;
  basicParams?: Obj;
  service: (params?: any) => Promise<IPagingResp<T>>;
}

const emptyList: any[] = [];
const emptyObj = {};

/**
 * 用于Modal等地方的临时分页
 * @param param.service 接口
 * @param param.initList 初始列表数据
 * @param param.basicParams 基础请求参数
 * @param param.append 追加模式
 * @param param.listKey 默认为'list',特殊情况时自定义response里list的字段名
 * @return [list, paging, loading, load, clear]
 */
export function useTempPaging<T>({
  initList = emptyList,
  append = false,
  basicParams = emptyObj,
  service,
  listKey = 'list',
}: ITempPagingParams<T>): [T[], IPaging, boolean, (params?: any) => Promise<any>, () => void] {
  const [loading, setLoading] = React.useState(false);
  const [list, setList] = React.useState(initList);
  const [paging, setPaging] = React.useState(defaultPaging);

  const basic = React.useRef(basicParams);

  const load = React.useCallback(
    (params: Obj = {}) => {
      setLoading(true);
      return service({ pageNo: paging.pageNo, pageSize: paging.pageSize, ...basic.current, ...params })
        .then((result: any) => {
          if (result.success && result.data) {
            const newList = result.data[listKey];
            const { total } = result.data;
            if (Array.isArray(newList)) {
              if (append) {
                setList(list.concat(newList));
              } else {
                setList(newList);
              }
            }
            const nextPageNo = params.pageNo || paging.pageNo;
            const nextPageSize = params.pageSize || paging.pageSize;
            setPaging({
              pageNo: nextPageNo,
              pageSize: nextPageSize,
              total,
              hasMore: Math.ceil(total / nextPageSize) > nextPageNo,
            });
            return result.data;
          } else {
            notify('error', result.err.msg);
          }
        })
        .finally(() => {
          setLoading(false);
        });
    },
    [append, list, listKey, paging, service],
  );

  const clear = React.useCallback(() => {
    setList([]);
    setPaging(defaultPaging);
  }, []);

  return [list, paging, loading, load, clear];
}

/**
 * 用于调试打印prop变化
 * @param list watch的变量列表
 * @param name 可选，变量名称列表
 * @usage useDiff([a, b], ['a', 'b']);
 */
export function useDiff(list: any[], name: string[]) {
  const { current: checkList } = React.useRef(list || []);
  const { current: nameList } = React.useRef(name || []);
  const { current: prevList } = React.useRef(Array(list.length).fill(undefined));

  React.useEffect(() => {
    checkList.forEach((item, i) => {
      if (prevList[i] !== item) {
        prevList[i] = item;
      }
    });
  }, [prevList, checkList, nameList]);
}

/**
 * Filter功能的包装hook, 可以实现自动管理分页跳转、查询条件贮存url，当页面中仅有一个Filter时使用此hook
 * @param getData 必传 用于取列表数据的effect
 * @param fieldConvertor 选传 用于对特殊自定义类型的域的值进行转换
 * @param pageSize 选传 不传默认为10
 * @param extraQuery 选传 查询时需要加入但不存在Filter组件中的参数 注意!!! extraQuery不能直接依赖于路由参数本身,如果必须依赖那就建立一个本地state来替代。否则在前进后退中会额外触发路由变化引起崩溃。另外extraQuery不要和searchQuery混在一起，更新searchQuery用onSubmit, 更新extraQuery直接更新对象, extraQuery的优先级大于searchQuery
 * @param excludeQuery 选传 在映射url时，将查询条件以外的query保留
 * @param fullRange 选传 date类型是否无视时间（仅日期） 时间从前一天的0点到后一天的23:59:59
 * @param dateFormat 选传 日期类型域的toString格式
 * @param requiredKeys 选传 当requiredKeys中的任何参数为空时，终止search行为，当下一次参数有值了才查询。用于页面初始化时某key参数需要异步拿到，不能直接查询的场景
 * @param loadMore 选传 当列表是使用loadMore组件加载时，不在url更新pageNo, LoadMore组件需要关闭initialLoad
 * @param debounceGap 选传 当需要在输入时搜索，加入debounce的时间间隔，不传默认为0
 * @param localMode 选传 为true时，与url切断联系，状态只保存在state中
 * @param lazy 选传 为true时，首次query必须由onSubmit触发
 * @param initQuery 选传 当初次加载页面时filter组件的默认值，如果url上的query不为空，则此值无效
 * @return {queryCondition, onSubmit, onReset, onPageChange, pageNo, fetchDataWithQuery }
 */
export function useFilter<T>(props: ISingleFilterProps<T>): IUseFilterProps<T> {
  const {
    getData,
    excludeQuery = [],
    fieldConvertor,
    extraQuery = {},
    fullRange,
    dateFormat,
    requiredKeys = [],
    loadMore = false,
    debounceGap = 0,
    localMode = false,
    lazy = false,
    initQuery = {},
  } = props;
  const [query, currentRoute] = routeInfoStore.useStore((s) => [s.query, s.currentRoute]);
  const { pageNo: pNo, ...restQuery } = query;

  const [state, update, updater] = useUpdate({
    searchQuery: localMode
      ? {}
      : isEmpty(restQuery)
      ? initQuery
      : !isEmpty(excludeQuery)
      ? omit(restQuery, excludeQuery)
      : { ...initQuery, ...restQuery },
    pageNo: Number(localMode ? 1 : pNo || 1),
    loaded: false,
    pageSize: PAGINATION.pageSize,
    currentPath: currentRoute.path,
  });

  const { searchQuery, pageNo, pageSize, loaded, currentPath } = state;

  const fetchData = React.useCallback(
    debounce((q: any) => {
      const { [FilterBarHandle.filterDataKey]: _Q_, ...restQ } = q;
      getData({ ...restQ });
    }, debounceGap),
    [getData],
  );

  const updateSearchQuery = React.useCallback(() => {
    // 这里异步处理一把是为了不出现dva报错。dva移除后可以考虑放开
    setTimeout(() => {
      setSearch({ ...searchQuery, ...extraQuery, pageNo: loadMore ? undefined : pageNo }, excludeQuery, true);
    });
  }, [excludeQuery, extraQuery, loadMore, pageNo, searchQuery]);

  useDeepCompareEffect(() => {
    const payload = { pageSize, ...searchQuery, ...extraQuery, pageNo };
    const unableToSearch = some(requiredKeys, (key) => payload[key] === '' || payload[key] === undefined);
    if (unableToSearch || (lazy && !loaded)) {
      return;
    }
    pageNo && fetchData(payload);
    updateSearchQuery();
  }, [pageNo, pageSize, searchQuery, extraQuery]);

  // useUpdateEffect 是当mounted之后才会触发的effect
  // 这里的目的是当用户点击当前页面的菜单路由时，url的query会被清空。此时需要reset整个filter
  useUpdateEffect(() => {
    // 当点击菜单时，href会把query覆盖，此时判断query是否为空并且路径没有改变的情况下重新初始化query
    if (isEmpty(query) && currentPath === currentRoute.path) {
      onReset();
      updateSearchQuery();
    }
  }, [query, currentPath, currentRoute]);

  const fetchDataWithQuery = (pageNum?: number) => {
    if (pageNum && pageNum !== pageNo && !loadMore) {
      update.pageNo(pageNum);
    } else {
      const { [FilterBarHandle.filterDataKey]: _Q_, ...restSearchQuery } = searchQuery;
      return getData({
        pageSize,
        ...extraQuery,
        ...restSearchQuery,
        pageNo: pageNum || pageNo,
      });
    }
  };

  const onSubmit = (condition: { [prop: string]: any }) => {
    const formatCondition = convertFilterParamsToUrlFormat(fullRange, dateFormat)(condition, fieldConvertor);
    if (isEqual(formatCondition, searchQuery)) {
      // 如果查询条件没有变化，重复点击查询，还是要强制刷新
      fetchDataWithQuery(1);
    } else {
      update.searchQuery({ ...formatCondition, pageNo: 1 }); // 参数变化时始终重置pageNo
      update.pageNo(1);
    }
    update.loaded(true);
  };

  const onReset = () => {
    if (isEmpty(searchQuery)) {
      fetchDataWithQuery(1);
    } else {
      // reset之后理论上值要变回最开始？
      update.searchQuery(
        localMode
          ? {}
          : isEmpty(restQuery)
          ? initQuery
          : !isEmpty(excludeQuery)
          ? omit(restQuery, excludeQuery)
          : restQuery,
      );
      update.pageNo(1);
    }
  };

  const onPageChange = (currentPageNo: number, currentPageSize?: number) => {
    updater({
      pageNo: currentPageNo,
      pageSize: currentPageSize,
    });
  };

  const onTableChange = (pagination: PaginationConfig, _filters: any, sorter: SorterResult<any>) => {
    if (!isEmpty(sorter)) {
      const { field, order } = sorter;
      if (order) {
        update.searchQuery({ ...searchQuery, orderBy: field, asc: order === 'ascend' });
      } else {
        update.searchQuery({ ...searchQuery, orderBy: undefined, asc: undefined });
      }
    }
    if (!isEmpty(pagination)) {
      const { pageSize: pSize, current } = pagination;
      update.searchQuery({ ...searchQuery, pageSize: pSize, pageNo: current });
      update.pageNo(current || 1);
    }
  };

  const sizeChangePagination = (paging: IPaging) => {
    const { pageSize: pSize, total } = paging;
    let sizeOptions = PAGINATION.pageSizeOptions;
    if (!sizeOptions.includes(`${pageSize}`)) {
      // 备选项中不存在默认的页数
      sizeOptions.push(`${pageSize}`);
    }
    sizeOptions = sortBy(sizeOptions, (item) => +item);
    return (
      <div className="mt-4 flex items-center flex-wrap justify-end">
        <Pagination
          current={pageNo}
          pageSize={+pSize}
          total={total}
          onChange={onPageChange}
          showSizeChanger
          pageSizeOptions={sizeOptions}
        />
      </div>
    );
  };
  return {
    queryCondition: searchQuery,
    onSubmit, // 包装原始onSubmit, 当搜索时自动更新url
    onReset, // 包装原始onReset, 当重置时自动更新url
    onPageChange, // 当Table切换页码时记录PageNo并发起请求
    pageNo, // 返回当前pageNo，与paging的PageNo理论上相同
    fetchDataWithQuery, // 当页面表格发生操作（删除，启动，编辑）后，进行刷新页面，如不指定pageNum则使用当前页码
    autoPagination: (paging: IPaging) => ({
      total: paging.total,
      current: paging.pageNo,
      pageSize: paging.pageSize,
      // hideOnSinglePage: true,
      onChange: (currentPageNo: number, currentPageSize: number) => onPageChange(currentPageNo, currentPageSize),
    }),
    onTableChange, // Filter/Sort/Paging变化
    sizeChangePagination,
  };
}

interface IMultiModeProps extends IProps {
  // 是否单页面有多个Filter，一般为Tab切换模式
  multiGroupEnums: string[];
  groupKey?: string;
  getData: Array<(param?: any) => Promise<any>>;
  shareQuery?: boolean;
  activeKeyInParam?: boolean;
  extraQueryFunc?: (activeGroup: string) => Obj;
  checkParams?: string[];
}

const convertFilterParamsToUrlFormat =
  (fullRange?: boolean, dateFormat?: string) =>
  (
    condition: { [prop: string]: any },
    fieldConvertor?: {
      [k: string]: (value: any, allQuery?: any) => string | string[] | undefined;
    },
  ) => {
    const formatCondition = {};
    forIn(condition, (v, k) => {
      const fieldConvertFunc = get(fieldConvertor, k);
      if (Array.isArray(v) && v.length === 2 && every(v, (item) => moment.isMoment(item))) {
        // handle date range
        const [start, end] = v as [Moment, Moment];
        const format = dateFormat || 'YYYY-MM-DD HH:mm:ss';
        let startName = `${k}From`;
        let endName = `${k}To`;
        const rangeNames = k.split(',');
        if (rangeNames.length === 2) {
          [startName, endName] = rangeNames;
        }
        const startMoment = fullRange ? start.startOf('day') : start;
        const endMoment = fullRange ? end.endOf('day') : end;
        set(formatCondition, startName, format === 'int' ? startMoment.valueOf() : startMoment.format(format));
        set(formatCondition, endName, format === 'int' ? endMoment.valueOf() : endMoment.format(format));
      } else if (fieldConvertFunc) {
        // handle custom field
        set(formatCondition, k, fieldConvertFunc(v, condition));
      } else {
        set(formatCondition, k, v);
      }
    });
    return formatCondition;
  };

interface IProps {
  fieldConvertor?: {
    [k: string]: (value: any, allQuery?: any) => string | string[] | undefined;
  };
  excludeQuery?: string[];
  pageSize?: number;
  fullRange?: boolean;
  dateFormat?: string;
  requiredKeys?: string[];
}

interface ISingleFilterProps<T = any> extends IProps {
  getData: (query: any) => Promise<T> | void;
  extraQuery?: Obj;
  loadMore?: boolean;
  debounceGap?: number;
  localMode?: boolean;
  lazy?: boolean;
  initQuery?: Obj;
}

/**
 * Filter功能的包装hook, 可以实现自动管理分页跳转、查询条件贮存url，当页面中有多个Filter（不论是多个实例还是逻辑上的多个Filter）时使用此hook
 * @param getData 必传 用于取列表数据的effect
 * @param multiGroupEnums 必传 各个Filter的key
 * @param groupKey 选传 用于在url标示Filter Key属性的key， 默认是type
 * @param shareQuery 选传 多个Filter之间是否共享查询条件，默认是false
 * @param activeKeyInParam 选传 groupKey属性时存在于query还是params， 默认是在query
 * @param fieldConvertor 选传 用于对特殊自定义类型的域的值进行转换
 * @param pageSize 选传 不传默认为10
 * @param extraQueryFunc 选传 查询时需要加入但不存在Filter组件中的参数
 * @param excludeQuery 选传 在映射url时，将查询条件以外的query保留
 * @param fullRange 选传 date类型是否无视时间（仅日期） 时间从前一天的0点到后一天的23:59:59
 * @param dateFormat 选传 日期类型域的toString格式
 * @param requiredKeys 选传 当requiredKeys中的任何参数为空时，终止search行为，当下一次参数有值了才查询。用于页面初始化时某key参数需要异步拿到，不能直接查询的场景
 * @return {queryCondition, onSubmit, onReset, onPageChange, pageNo, fetchDataWithQuery }
 */
export const useMultiFilter = (props: IMultiModeProps): IUseMultiFilterProps => {
  const {
    getData,
    excludeQuery = [],
    fieldConvertor,
    pageSize = PAGINATION.pageSize,
    multiGroupEnums,
    groupKey = 'type',
    extraQueryFunc = () => ({}),
    fullRange,
    dateFormat,
    shareQuery = false,
    activeKeyInParam = false,
    requiredKeys = [],
    checkParams = [],
  } = props;
  const wholeExcludeKeys = activeKeyInParam ? excludeQuery : excludeQuery.concat([groupKey]);
  const [query, params, currentRoute] = routeInfoStore.useStore((s) => [s.query, s.params, s.currentRoute]);
  const { pageNo: pNo, ...restQuery } = query;

  const pickQueryValue = React.useCallback(() => {
    return omit(restQuery, wholeExcludeKeys) || {};
  }, [restQuery, wholeExcludeKeys]);

  const activeType = (activeKeyInParam ? params[groupKey] : query[groupKey]) || multiGroupEnums[0];

  const [state, update] = useUpdate({
    groupSearchQuery: multiGroupEnums.reduce((acc, item) => {
      acc[item] = item === activeType || shareQuery ? pickQueryValue() : {};
      return acc;
    }, {}),
    groupPageNo: multiGroupEnums.reduce((acc, item) => {
      acc[item] = item === activeType ? Number(pNo || 1) : 1;
      return acc;
    }, {}),
    activeGroup: activeType,
    currentPath: currentRoute.path,
  });

  const { groupSearchQuery, groupPageNo, activeGroup, currentPath } = state;
  const extraQuery = extraQueryFunc(activeGroup);

  const pageNo = React.useMemo(() => {
    if (activeGroup) {
      return groupPageNo[activeGroup];
    }
    return 1;
  }, [activeGroup, groupPageNo]);

  useDeepCompareEffect(() => {
    // 因为multiGroupEnums随着渲染一直变化引用，所以使用useDeepCompareEffect
    if (activeType !== activeGroup) {
      update.activeGroup(activeType);
    }
  }, [multiGroupEnums, groupKey, activeKeyInParam, params, query, update]);
  useUpdateEffect(() => {
    // 当点击菜单时，href会把query覆盖，此时判断query是否为空并且路径没有改变的情况下重新初始化query
    if (isEmpty(query) && currentPath === currentRoute.path) {
      onReset();
      updateSearchQuery();
    }
  }, [query, currentPath, currentRoute]);

  const currentFetchEffect = getData.length === 1 ? getData[0] : getData[multiGroupEnums.indexOf(activeGroup)];

  const searchQuery = React.useMemo(() => {
    if (activeGroup) {
      return groupSearchQuery[activeGroup];
    }
    return {};
  }, [groupSearchQuery, activeGroup]);

  const updateSearchQuery = React.useCallback(() => {
    setTimeout(() => {
      setSearch({ ...searchQuery, pageNo }, wholeExcludeKeys, true);
    }, 0);
  }, [searchQuery, pageNo, wholeExcludeKeys]);

  const fetchData = (pageNum?: number) => {
    if (checkParams.length) {
      const checked = checkParams.every((key) => !isEmpty(extraQuery[key]));
      if (!checked) {
        return;
      }
    }
    currentFetchEffect({
      pageSize,
      ...extraQuery,
      ...searchQuery,
      pageNo: pageNum || pageNo,
    });
  };

  useDeepCompareEffect(() => {
    const payload = { pageSize, ...extraQuery, ...searchQuery, pageNo };
    const unableToSearch = some(requiredKeys, (key) => payload[key] === '' || payload[key] === undefined);
    if (unableToSearch) {
      return;
    }
    fetchData();
    updateSearchQuery();
  }, [pageNo, pageSize, searchQuery, extraQuery]);

  const fetchDataWithQuery = (pageNum?: number) => {
    if (pageNum && pageNum !== pageNo) {
      onPageChange(pageNum);
    } else {
      fetchData(pageNum);
    }
  };

  const onSubmit = (condition: { [prop: string]: any }) => {
    const formatCondition = convertFilterParamsToUrlFormat(fullRange, dateFormat)(condition, fieldConvertor);
    if (isEqual(formatCondition, searchQuery)) {
      // 如果查询条件没有变化，重复点击查询，还是要强制刷新
      fetchDataWithQuery(1);
    } else if (shareQuery) {
      update.groupSearchQuery(mapValues(groupSearchQuery, () => formatCondition));
      update.groupPageNo(mapValues(groupPageNo, () => 1));
    } else {
      update.groupSearchQuery({
        ...groupSearchQuery,
        [activeGroup]: formatCondition,
      });
      update.groupPageNo({ ...groupPageNo, [activeGroup]: 1 });
    }
  };

  const onReset = () => {
    if (isEmpty(searchQuery)) {
      fetchDataWithQuery(1);
    } else {
      update.groupSearchQuery({ ...groupSearchQuery, [activeGroup]: {} });
      update.groupPageNo({ ...groupPageNo, [activeGroup]: 1 });
    }
  };

  const onPageChange = (currentPageNo: number) => {
    update.groupPageNo({ ...groupPageNo, [activeGroup]: currentPageNo });
  };

  return {
    queryCondition: searchQuery,
    onSubmit, // 包装原始onSubmit, 当搜索时自动更新url
    onReset, // 包装原始onReset, 当重置时自动更新url
    onPageChange, // 当Table切换页码时记录PageNo并发起请求
    pageNo, // 返回当前pageNo，与paging的PageNo理论上相同
    fetchDataWithQuery, // 当页面表格发生操作（删除，启动，编辑）后，进行刷新页面，如不指定pageNum则使用当前页码
    activeType: activeGroup,
    onChangeType: (t: string | number) => setSearch({ [groupKey || 'type']: t }, wholeExcludeKeys, true),
    autoPagination: (paging: IPaging) => ({
      total: paging.total,
      current: paging.pageNo,
      // hideOnSinglePage: true,
      onChange: (n: number) => onPageChange(n),
    }),
  };
};

type TargetValue<T> = T | undefined | null;

type TargetType = HTMLElement | Element | Window | Document;

export type BasicTarget<T extends TargetType = Element> =
  | (() => TargetValue<T>)
  | TargetValue<T>
  | React.MutableRefObject<TargetValue<T>>;

export const getTargetElement = <T extends TargetType>(target: BasicTarget<T>, defaultElement?: T) => {
  if (!target) {
    return defaultElement;
  }

  let targetElement: TargetValue<T>;

  if (typeof target === 'function') {
    targetElement = target();
  } else if ('current' in target) {
    targetElement = target.current;
  } else {
    targetElement = target;
  }

  return targetElement;
};

export interface Options {
  onExit?: () => void;
  onEnter?: () => void;
}

type IUseFullScreen = (
  target: BasicTarget,
  options?: Options,
) => [boolean, { exitFullscreen: () => void; toggleFullscreen: () => void; enterFullscreen: () => void }];

export const useFullScreen: IUseFullScreen = (target, options) => {
  const [state, setState] = React.useState(false);
  const latestStateRef = React.useRef(false);
  latestStateRef.current = state;

  React.useEffect(() => {
    const onChange = () => {
      if (screenfull.isEnabled) {
        const { isFullscreen } = screenfull;
        if (isFullscreen) {
          options?.onEnter?.();
        } else {
          options?.onExit?.();
        }
        setState(isFullscreen);
      }
    };
    if (screenfull.isEnabled) {
      screenfull.on('change', onChange);
    }
    return () => {
      if (screenfull.isEnabled) {
        screenfull.off('change', onChange);
      }
    };
  }, []);

  const enterFullscreen = () => {
    const el = getTargetElement(target);
    if (!el) {
      return;
    }
    if (screenfull.isEnabled) {
      screenfull.request(el);
    }
  };
  const exitFullscreen = () => {
    if (!latestStateRef.current) {
      return;
    }
    if (screenfull.isEnabled) {
      screenfull.exit();
    }
  };

  const toggleFullscreen = () => {
    if (latestStateRef.current) {
      exitFullscreen();
    } else {
      enterFullscreen();
    }
  };

  return [
    state,
    {
      exitFullscreen,
      enterFullscreen,
      toggleFullscreen,
    },
  ];
};
