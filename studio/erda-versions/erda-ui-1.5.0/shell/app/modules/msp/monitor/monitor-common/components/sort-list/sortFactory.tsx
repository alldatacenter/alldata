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
import { get, has, isEqual } from 'lodash';
import { useEffectOnce } from 'react-use';
import PureSubTab from './sub-tab';
import PureSortTab from './sort-tab';
import PureSortList from './sort-list';
import monitorCommonStore from 'common/stores/monitorCommon';
import monitorChartStore from 'app/modules/msp/monitor/monitor-common/stores/monitorChart';
import routeInfoStore from 'core/stores/route';

interface ICreateParams {
  moduleName: string;
  chartName: string;
  type: string;
  tabList: ITab[];
  dataHandler?: Function;
}

interface ITab {
  [pro: string]: any;
  name: string;
  key: string;
}

interface IListProps {
  [pro: string]: any;
  shouldLoad?: boolean;
  loadData: (name: string, args: any) => Promise<object>;
  setCommonState?: (args: any) => Promise<any>;
}

export default {
  create: ({ moduleName, chartName, dataHandler, type, tabList }: ICreateParams) => {
    if (type === 'subTab') {
      const SubTab = () => {
        const { updateState: setCommonState } = monitorCommonStore.reducers;
        const onChange = (subTab: string) => {
          setCommonState && setCommonState({ subTab });
        };
        return <PureSubTab tabList={tabList} onChange={onChange} />;
      };
      return SubTab;
    } else if (type === 'sortTab') {
      const SortTab = () => {
        const { updateState: setCommonState } = monitorCommonStore.reducers;
        const onChange = (sortTab: string) => {
          setCommonState && setCommonState({ sortTab });
        };
        return <PureSortTab tabList={tabList} onChange={onChange} />;
      };
      return SortTab;
    } else if (type === 'sortList') {
      const getQuery = (props: any) => {
        const { getFetchObj, fetchApi, query, chosenApp, timeSpan, sortTab, subTab, terminusKey, params } = props;
        let reQuery = { fetchApi };
        let reextendHandler = {};
        if (getFetchObj) {
          // 从fetchObj中获取
          const { fetchApi: api, extendQuery = {}, extendHandler } = getFetchObj(props);
          extendHandler && (reextendHandler = { ...extendHandler });
          reQuery = { fetchApi: api ? (api.startsWith('/api') ? api : `${fetchApi}${api}`) : fetchApi, ...extendQuery };
        }
        const commonState = { ...timeSpan, chosenApp, sortTab, subTab, terminusKey, ...params };
        const { constQuery = {}, dependentKey, ...rest } = query;
        query.extendHandler && (reextendHandler = { ...reextendHandler, ...query.extendHandler });
        if (dependentKey) {
          // 接口中所需参数名命名不一致，如{tk:'terminusKey'}
          Object.keys(dependentKey).forEach((key) => {
            const curKey = dependentKey[key];
            if (has(commonState, curKey)) {
              const val = get(commonState, curKey);
              val !== undefined && val !== '' && (reQuery[key] = commonState[curKey]);
            } else {
              // eslint-disable-next-line no-console
              console.error(`there has no key:${curKey} in chartFactory, or the value of the key is undefined.`);
            }
          });
        }
        return { extendHandler: reextendHandler, ...reQuery, ...rest, ...constQuery }; // 有静态查询参数，覆盖前面的参数，如固定的时间
      };
      const isEqualQuery = (preQuery: object, nextQuery: object) => {
        // query中包含extendHandler，但extendHandler不是真正请求的参数，不需要比较
        const preObj = { ...preQuery, extendHandler: '', dataHandler: '' };
        const nextObj = { ...nextQuery, extendHandler: '', dataHandler: '' };
        return isEqual(preObj, nextObj);
      };
      const SortList = (props: IListProps) => {
        const { shouldLoad = true, viewProps, fetchApi, query } = props;
        const { loadChart } = monitorChartStore.effects;
        const { updateState: setCommonState } = monitorCommonStore.reducers;
        const [timeSpan, chosenSortItem, chosenApp = {}, sortTab, subTab] = monitorCommonStore.useStore((s) => [
          s.globalTimeSelectSpan.range,
          s.chosenSortItem,
          s.chosenApp,
          s.sortTab,
          s.subTab,
        ]);
        const terminusKey = routeInfoStore.useStore((s) => s.params.terminusKey);
        const sortListRef = React.useRef(null as any);
        const [curQuery, setCurQuery] = React.useState(
          getQuery({ ...props, terminusKey, timeSpan, chosenSortItem, chosenApp, sortTab, subTab }) as any,
        );
        const chart = monitorChartStore.useStore((s) => s);
        const data = get(chart, `${moduleName}.${chartName}`, {});

        useEffectOnce(() => {
          if (shouldLoad) {
            const q = getQuery({ ...props, terminusKey, timeSpan, chosenSortItem, chosenApp, sortTab, subTab });
            loadData(q);
            setCurQuery(q);
          }
        });

        React.useEffect(() => {
          const preQuery = curQuery;
          const nextQuery = getQuery({ ...props, terminusKey, timeSpan, chosenSortItem, chosenApp, sortTab, subTab });
          if (shouldLoad && !isEqualQuery(preQuery, nextQuery)) {
            setCurQuery(nextQuery);
            loadData(nextQuery);
          }
          if (!shouldLoad) {
            setCurQuery({});
          }
          // eslint-disable-next-line react-hooks/exhaustive-deps
        }, [curQuery, fetchApi, query, chosenApp, timeSpan, sortTab, subTab, chosenSortItem, terminusKey, shouldLoad]);

        const loadData = (q: any) => {
          loadChart({ query: q, chartName, moduleName, dataHandler }).then(() => {
            if (sortListRef && sortListRef.current) {
              sortListRef.current.handleClose();
            }
          });
        };
        const onClickItem = (curChosenItem: string) => {
          setCommonState && setCommonState({ chosenSortItem: curChosenItem });
        };
        return (
          <PureSortList
            ref={sortListRef}
            onClickItem={onClickItem}
            {...viewProps}
            data={shouldLoad ? data : { loading: false }}
          />
        );
      };
      return SortList;
    }
    return () => null;
  },
};
