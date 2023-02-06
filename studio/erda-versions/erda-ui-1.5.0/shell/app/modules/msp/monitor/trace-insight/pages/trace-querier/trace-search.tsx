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
import { debounce, isNumber } from 'lodash';
import moment, { Moment } from 'moment';
import { BoardGrid, ContractiveFilter, Copy, TagsRow } from 'common';
import { useUpdate } from 'common/use-hooks';
import { ColumnProps, IActions } from 'common/components/table/interface';
import Table from 'common/components/table';
import { message } from 'antd';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import i18n from 'i18n';
import { getDashboard } from 'msp/services';
import { getFormatter } from 'charts/utils/formatter';
import { useLoading } from 'core/stores/loading';
import traceStore from '../../../../stores/trace';
import TraceSearchDetail from './trace-search-detail';
import { ICondition } from 'common/components/contractive-filter';
import { getQueryConditions } from 'trace-insight/services/trace-querier';
import Duration, { transformDuration } from 'trace-insight/components/duration';
import { TimeSelectWithStore } from 'msp/components/time-select';
import monitorCommonStore from 'common/stores/monitorCommon';
import routeInfoStore from 'core/stores/route';

const DashBoard = React.memo(BoardGrid.Pure);

const name = {
  sort: i18n.t('msp:sort method'),
  limit: i18n.t('msp:number of queries'),
  traceStatus: i18n.t('msp:tracking status'),
};

const convertData = (
  data: MONITOR_TRACE.TraceConditions,
): [any[], { [k in MONITOR_TRACE.IFixedConditionType]: string }] => {
  const { others, ...rest } = data;
  const list: ICondition[] = [];
  const defaultValue = {};
  const fixC = Object.keys(rest);
  fixC.forEach((key, index) => {
    const option = data[key];
    defaultValue[key] = option?.[0]?.value;
    list.push({
      type: 'select',
      fixed: true,
      showIndex: index + 2,
      key,
      label: name[key],
      options: option.map((t) => ({ ...t, label: t.displayName })),
      customProps: {
        mode: 'single',
      },
    });
  });
  others?.forEach(({ paramKey, displayName, type }) => {
    list.push({
      type,
      showIndex: 0,
      fixed: false,
      placeholder: i18n.t('please enter {name}', { name: displayName }),
      key: paramKey,
      label: displayName,
    });
  });
  return [list, defaultValue];
};

interface RecordType {
  id: string;
  duration: number;
  startTime: number;
  services: string[];
}

const initialFilter = [
  {
    label: i18n.t('msp:duration'),
    key: 'duration',
    showIndex: 1,
    fixed: true,
    getComp: (props) => {
      return <Duration {...props} />;
    },
  },
];

interface IState {
  filter: ICondition[];
  traceId?: string;
  defaultQuery: Obj;
  query: Obj;
  layout: DC.Layout;
  startTime: number;
}

type IQuery = {
  [k in MONITOR_TRACE.IFixedConditionType]: string;
} & {
  time: [Moment, Moment];
  duration: Array<{ timer: number; unit: 'ms' | 's' }>;
} & {
  [k: string]: string;
};

const TraceSearch = () => {
  const range = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan.range);
  const [traceSummary] = traceStore.useStore((s) => [s.traceSummary]);
  const { getTraceSummary } = traceStore;
  const [loading] = useLoading(traceStore, ['getTraceSummary']);
  const { setIsShowTraceDetail } = monitorCommonStore.reducers;
  const [{ traceId, filter, defaultQuery, query, layout, startTime }, updater, update] = useUpdate<IState>({
    filter: [],
    traceId: undefined,
    defaultQuery: {},
    query: {},
    layout: [],
    startTime: 0,
  });
  const [routeQuery, params] = routeInfoStore.useStore((s) => [s.query, s.params]);
  const globalVariable = React.useMemo(() => {
    return {
      startTime: range.startTimeMs,
      endTime: range.endTimeMs,
      terminusKey: params.terminusKey,
      durationLeft: query.durationMin ? `trace_duration::field>'${query.durationMin}'` : undefined,
      durationRight: query.durationMax ? `trace_duration::field<'${query.durationMax}'` : undefined,
      serviceName: query.serviceName ? `service_names::field='${query.serviceName}'` : undefined,
      traceId: query.traceID ? `trace_id::tag='${query.traceID}'` : undefined,
      dubboMethod: query.dubboMethod ? `dubbo_methods::field='${query.dubboMethod}'` : undefined,
      httpPath: query.httpPath ? `http_paths::field='${query.httpPath}'` : undefined,
      statusSuccess: query.status === 'trace_success' ? `errors_sum::field='0'` : undefined,
      statusError: query.status === 'trace_error' ? `errors_sum::field>'0'` : undefined,
    };
  }, [
    params.terminusKey,
    range,
    query.dubboMethod,
    query.durationMin,
    query.durationMax,
    query.status,
    query.traceID,
    query.serviceName,
    query.httpPath,
  ]);

  useEffectOnce(() => {
    getQueryConditions().then((res) => {
      if (res.success) {
        const [list, defaultValue] = convertData(res.data);
        const handleDefaultValue = {
          ...defaultValue,
          traceStatus: routeQuery?.status || defaultValue.traceStatus,
        };
        update({
          defaultQuery: {
            ...handleDefaultValue,
          },
          query: {
            ...handleDefaultValue,
          },
          filter: [...initialFilter, ...list],
        });
        getData({
          startTime: range.startTimeMs,
          endTime: range.endTimeMs,
          status: defaultValue.traceStatus,
          ...handleDefaultValue,
        });
      }
    });
    getDashboard({ type: 'trace_count' }).then(({ success, data }) => {
      if (success) {
        updater.layout(data?.viewConfig);
      }
    });
  });

  useUpdateEffect(() => {
    getData({
      ...query,
      startTime: range.startTimeMs,
      endTime: range.endTimeMs,
    });
  }, [query, range]);

  const getData = React.useCallback(
    debounce((obj: Omit<MS_MONITOR.ITraceSummaryQuery, 'tenantId'>) => {
      getTraceSummary(obj);
    }, 500),
    [],
  );

  const handleSearch = (query: Partial<IQuery>) => {
    const { duration, traceStatus, ...rest } = query;
    const durationMin = transformDuration(duration?.[0]);
    const durationMax = transformDuration(duration?.[1]);
    let durations = {};
    if (isNumber(durationMin) && isNumber(durationMax)) {
      if (durationMin <= durationMax) {
        durations = {
          durationMin,
          durationMax,
        };
      } else {
        message.error(i18n.t('msp:wrong duration'));
      }
    }
    updater.query({
      status: traceStatus,
      ...durations,
      ...rest,
    });
  };

  const handleCheckTraceDetail = (id: string, time: number) => {
    updater.traceId(id);
    updater.startTime(time);
    setIsShowTraceDetail(true);
  };

  const columns: Array<ColumnProps<MS_MONITOR.ITraceSummary>> = [
    {
      title: i18n.t('msp:trace id'),
      dataIndex: 'id',
      render: (id: string) => <Copy>{id}</Copy>,
    },
    {
      title: i18n.t('msp:duration'),
      dataIndex: 'duration',
      width: 240,
      sorter: {
        compare: (a: MS_MONITOR.ITraceSummary, b: MS_MONITOR.ITraceSummary) => a.duration - b.duration,
      },
      render: (duration: number) => getFormatter('TIME', 'ns').format(duration),
    },
    {
      title: i18n.t('msp:start time'),
      dataIndex: 'startTime',
      width: 200,
      render: (time: number) => moment(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('service'),
      dataIndex: 'services',
      width: 240,
      render: (services: string[]) => <TagsRow labels={services.map((service) => ({ label: service }))} />,
    },
  ];

  return (
    <>
      <div className="flex justify-between items-start bg-white px-2 py-2 mb-3">
        <div className="flex-1">
          {filter.length > 0 ? (
            <ContractiveFilter delay={1000} conditions={filter} initValue={defaultQuery} onChange={handleSearch} />
          ) : null}
        </div>
        <TimeSelectWithStore />
      </div>
      <div className="mb-6">
        <DashBoard layout={layout} globalVariable={globalVariable} />
      </div>
      <Table
        loading={loading}
        rowKey="id"
        columns={columns}
        dataSource={traceSummary}
        onRow={(record) => {
          return {
            onClick: () => handleCheckTraceDetail(record.id, record.startTime),
          };
        }}
        scroll={{ x: 1100 }}
        onChange={() => {
          getData({ ...query, startTime: range.startTimeMs, endTime: range.endTimeMs });
        }}
      />
      <TraceSearchDetail traceId={traceId} startTime={startTime} />
    </>
  );
};

export default TraceSearch;
