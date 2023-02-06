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
import { get, map } from 'lodash';
import { ModuleSelector, AppGroupSelector, AppsSelector } from 'monitor-common';
import { IF } from 'common';
import { TimeSelectWithStore } from 'msp/components/time-select';
import i18n from 'i18n';
import './tab-right.scss';

interface IParams {
  timeSpan: ITimeSpan;
  choosenApp: {
    [pro: string]: any;
    id: string;
  };
}

// 三层group查询，数据为三层嵌套  runtime.service.instance，
const modulesHandler = (dataKey: string) => (originData: object) => {
  const results = get(originData, 'results[0].data') || [];
  const modules = map(results, (item) => {
    const { tag: runtimeName, data: serviceList } = item; // runtime层
    const runtimeChildren = map(serviceList, (service) => {
      const { tag: serviceName, data: instanceList } = service; // service层
      const children = map(instanceList, (instance, j) => {
        return { value: instance[dataKey].tag, label: `${i18n.t('instance')} ${j + 1}` }; // instance层
      });
      return { value: `${serviceName}`, label: `${serviceName}`, children };
    });
    return { value: runtimeName, label: runtimeName, children: runtimeChildren };
  });
  return modules;
};

// 二层group查询，数据为三层嵌套  runtime.service
const appGroupHandler = (originData: object) => {
  const results = get(originData, 'results[0].data') || [];
  const data = map(results, (item) => {
    const { tag: runtimeName, data: serviceList } = item; // runtime层
    const runtimeChildren = map(serviceList, (service) => {
      const { tag: serviceName } = service; // service层
      return { value: `${serviceName}`, label: `${serviceName}` };
    });
    return { value: runtimeName, label: runtimeName, children: runtimeChildren };
  });
  return data;
};

const reqUrlPrefix = '/api/spot/tmc/metrics';
const TabRight = ({ type = '' }: { type?: string }) => {
  const modulesMap = {
    jvm: {
      api: `${reqUrlPrefix}/jvm_memory`,
      query: {
        latestTimestamp: 'usage_percent',
        sort: ['count', 'latestTimestamp_usage_percent'],
        group: ['runtime_name', 'service_name', 'instance_id'],
      },
      dataHandler: modulesHandler('latestTimestamp.usage_percent'),
    },
    node: {
      api: `${reqUrlPrefix}/nodejs_memory`,
      query: {
        latestTimestamp: 'heap_total',
        sort: ['count', 'latestTimestamp_heap_total'],
        group: ['runtime_name', 'service_name', 'instance_id'],
      },
      dataHandler: modulesHandler('latestTimestamp.heap_total'),
    },
    web: {
      api: `${reqUrlPrefix}/application_http`,
      query: (params: IParams) => {
        const { timeSpan, choosenApp } = params;
        const { startTimeMs: start, endTimeMs: end } = timeSpan;
        const filter_target_application_id = get(choosenApp, 'id');
        return {
          sort: ['count', 'count'],
          group: ['target_runtime_name', 'target_service_name'],
          start,
          end,
          filter_target_application_id,
        };
      },
      dataHandler: appGroupHandler,
    },
    rpc: {
      api: `${reqUrlPrefix}/application_rpc`,
      query: (params: IParams) => {
        const { timeSpan, choosenApp } = params;
        const { startTimeMs: start, endTimeMs: end } = timeSpan;
        const filter_target_application_id = get(choosenApp, 'id');
        return {
          sort: ['count', 'count'],
          group: ['target_runtime_name', 'target_service_name'],
          start,
          end,
          filter_target_application_id,
        };
      },
      dataHandler: appGroupHandler,
    },
    database: {
      api: `${reqUrlPrefix}/application_db`,
      query: (params: IParams) => {
        const { timeSpan, choosenApp } = params;
        const { startTimeMs: start, endTimeMs: end } = timeSpan;
        const filter_source_application_id = get(choosenApp, 'id');
        return {
          sort: ['count', 'count'],
          group: ['source_runtime_name', 'source_service_name'],
          start,
          end,
          filter_source_application_id,
        };
      },
      dataHandler: appGroupHandler,
    },
    cache: {
      api: `${reqUrlPrefix}/application_cache`,
      query: (params: IParams) => {
        const { timeSpan, choosenApp } = params;
        const { startTimeMs: start, endTimeMs: end } = timeSpan;
        const filter_source_application_id = get(choosenApp, 'id');
        return {
          sort: ['count', 'count'],
          group: ['source_runtime_name', 'source_service_name'],
          start,
          end,
          filter_source_application_id,
        };
      },
      dataHandler: appGroupHandler,
    },
  };
  return (
    <div className="ai-top-nav-right filter-box flex justify-between">
      <div>
        <AppsSelector />
        <IF check={['web', 'rpc', 'database', 'cache'].includes(type)}>
          <AppGroupSelector {...modulesMap[type]} key={type} type={type} />
        </IF>
        <IF check={['jvm', 'node'].includes(type)}>
          <ModuleSelector
            {...modulesMap[type]}
            key={type}
            type={type}
            selectDefault
            viewProps={{ allowClear: false }}
          />
        </IF>
      </div>
      <TimeSelectWithStore />
    </div>
  );
};

export default TabRight;
