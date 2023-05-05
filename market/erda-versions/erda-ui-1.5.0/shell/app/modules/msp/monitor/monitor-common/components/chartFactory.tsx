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
import { MonitorChartNew, PieChart, MapChart, HollowPieChart } from 'charts';
import { CardContainer } from 'common';
import monitorChartStore from 'app/modules/msp/monitor/monitor-common/stores/monitorChart';
import routeInfoStore from 'core/stores/route';
import monitorCommonStore from 'common/stores/monitorCommon';

const { ChartContainer } = CardContainer;

interface ILoadObj {
  id: number;
  query: object;
  dataHandler?: Function;
  loadChart: (args: any) => void;
}

interface IChartProps {
  [pro: string]: any;
  titleText: string | boolean;
  viewType: string;
  viewRender: any;
  viewProps: object;
  groupId: string;
  shouldLoad: boolean;
  timeSpan: ITimeSpan;
  terminusKey: string;
  chosenApp: object;
  fetchApi: string;
  params: object;
}

interface ICreateChartProps {
  moduleName: string;
  chartName: string;
  dataHandler?: Function;
}

let chartId = 0;
const loadList: ILoadObj[] = [];
const timerMap = {};
const lazyLoad = (pauseTime = 200) => {
  if (loadList.length) {
    const { id, loadChart, query, dataHandler } = loadList.shift() as ILoadObj;
    loadChart({ ...query, dataHandler });
    if (loadList.length) {
      timerMap[id] = setTimeout(() => {
        lazyLoad();
      }, pauseTime);
    }
  }
};
const isEqualQuery = (preQuery: object, nextQuery: object) => {
  // query中包含extendHandler，但extendHandler不是真正请求的参数，不需要比较
  const preObj = { ...preQuery, extendHandler: '', dataHandler: '' };
  const nextObj = { ...nextQuery, extendHandler: '', dataHandler: '' };
  return isEqual(preObj, nextObj);
};
let loadStart: any = null;
const emptyObj = {};
const ChartBaseFactory = {
  create: ({ moduleName, chartName, dataHandler }: ICreateChartProps) => {
    const getQuery = (props: IChartProps) => {
      const { query = {}, timeSpan, terminusKey, chosenApp, fetchApi, params } = props;
      const commonState = {
        ...timeSpan,
        chosenApp,
        terminusKey,
        fetchApi,
        ...params,
      }; // 提供被查询的参数
      const { constQuery = {}, dependentKey, ...rest } = query;
      const reQuery = {};
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
      return {
        query: { fetchApi, ...reQuery, ...rest, ...constQuery },
        moduleName,
        chartName,
        dataHandler,
      }; // 有静态查询参数，覆盖前面的参数，如固定的时间
    };
    const ChartBase = (props: IChartProps) => {
      const { titleText, viewType, viewRender, viewProps, groupId, shouldLoad = true, ...otherProps } = props;
      const { query = emptyObj, fetchApi } = otherProps;
      const { loadChart } = monitorChartStore.effects;
      const { clearChartData } = monitorChartStore.reducers;
      const chart = monitorChartStore.useStore((s) => s);
      const data = get(chart, `${moduleName}.${chartName}`, {});
      const [timeSpan, chosenSortItem, chosenApp = {}] = monitorCommonStore.useStore((s) => [
        s.globalTimeSelectSpan.range,
        s.chosenSortItem,
        s.chosenApp,
      ]);
      const terminusKey = routeInfoStore.useStore((s) => s.params.terminusKey);
      const [curQuery, setCurQuery] = React.useState(
        getQuery({
          ...props,
          timeSpan,
          chosenSortItem,
          chosenApp,
          terminusKey,
        }) as any,
      );
      let id = chartId;
      useEffectOnce(() => {
        id = chartId;
        chartId += 1;
        if (shouldLoad) {
          const q = getQuery({
            ...props,
            timeSpan,
            chosenSortItem,
            chosenApp,
            terminusKey,
          });
          setCurQuery(q);
          loadList.push({ id, loadChart, dataHandler, query: q });
        }
        // only start load after the last chart mounted
        loadStart = setTimeout(() => {
          if (loadStart) clearTimeout(loadStart);
          loadStart = setTimeout(() => lazyLoad(), 0);
        }, 0);
        return () => {
          if (timerMap[id]) {
            clearTimeout(timerMap[id]);
          }
          clearChartData({ chartName, moduleName });
        };
      });
      React.useEffect(() => {
        if (!shouldLoad) {
          setCurQuery({});
        }
      }, [shouldLoad]);
      React.useEffect(() => {
        const preQuery = curQuery;
        const nextQuery = getQuery({
          ...props,
          timeSpan,
          chosenSortItem,
          chosenApp,
          terminusKey,
        });
        if (shouldLoad && !isEqualQuery(preQuery, nextQuery)) {
          setCurQuery(nextQuery);
          loadChart(nextQuery);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
      }, [shouldLoad, query, fetchApi, curQuery, timeSpan, chosenSortItem, chosenApp, terminusKey]);
      let Chart: any;
      switch (viewType) {
        case 'pie':
          Chart = PieChart;
          break;
        case 'map':
          Chart = MapChart;
          break;
        case 'hollow-pie':
          Chart = HollowPieChart;
          break;
        default:
          Chart = viewRender || MonitorChartNew;
      }
      const title = titleText === false ? '' : data.title || titleText;
      return (
        <ChartContainer title={title}>
          {
            <Chart
              {...otherProps}
              _moduleName={moduleName} // TODO: use a inner named prop to prevent effect, only used in slow-tract-panel, need to refactor
              timeSpan={timeSpan}
              data={shouldLoad ? data : { loading: false }}
              groupId={groupId}
              {...viewProps}
            />
          }
        </ChartContainer>
      );
    };
    return ChartBase;
  },
};
export default ChartBaseFactory;

export const commonChartRender = (obj: any) => {
  if (!obj) return null;
  let reQuery = { dependentKey: { start: 'startTimeMs', end: 'endTimeMs' } };
  const { fetchApi: api, ...rest } = obj;
  const Chart = ChartBaseFactory.create({ ...rest });
  return (props: any) => {
    obj.query && (reQuery = { ...reQuery, ...obj.query });
    const { query, fetchApi, ...ownProps } = props;
    const reApi = fetchApi || api;
    return <Chart {...rest} {...ownProps} fetchApi={reApi} query={{ ...query, ...reQuery }} />;
  };
};
