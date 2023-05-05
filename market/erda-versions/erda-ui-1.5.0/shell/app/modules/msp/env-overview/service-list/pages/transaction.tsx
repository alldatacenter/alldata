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

import React, { useCallback, useMemo } from 'react';
import { differenceBy, map } from 'lodash';
import i18n from 'i18n';
import DC from '@erda-ui/dashboard-configurator/dist';
import { Drawer, Radio, Select, Tag, Tooltip } from 'antd';
import Table from 'common/components/table';
import { IActions } from 'common/components/table/interface';
import { DebounceSearch, Ellipsis, SimpleLog } from 'common';
import { useUpdate } from 'common/use-hooks';
import monitorCommonStore from 'common/stores/monitorCommon';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import topologyServiceStore from 'msp/stores/topology-service-analyze';
import TraceSearchDetail from 'trace-insight/pages/trace-querier/trace-search-detail';
import mspStore from 'msp/stores/micro-service';
import ServiceListDashboard from './service-list-dashboard';
import { TimeSelectWithStore } from 'msp/components/time-select';
import serviceAnalyticsStore from 'msp/stores/service-analytics';
import NoServicesHolder from 'msp/env-overview/service-list/pages/no-services-holder';

const { Button: RadioButton, Group: RadioGroup } = Radio;

enum DASHBOARD_TYPE {
  http = 'http',
  rpc = 'rpc',
  cache = 'cache',
  database = 'database',
  mq = 'mq',
}

const defaultTimeSort: TOPOLOGY_SERVICE_ANALYZE.SORT_TYPE = 'duration:DESC';

const defaultSort = 0;

const sortButtonMap: { [key in TOPOLOGY_SERVICE_ANALYZE.SORT_TYPE]: string } = {
  'timestamp:DESC': i18n.t('msp:time desc'),
  'timestamp:ASC': i18n.t('msp:time ascending'),
  'duration:DESC': i18n.t('msp:duration desc'),
  'duration:ASC': i18n.t('msp:duration asc'),
};

const dashboardIdMap = {
  [DASHBOARD_TYPE.http]: {
    id: 'translation_analysis_http',
    name: i18n.t('msp:HTTP call'),
  },
  [DASHBOARD_TYPE.rpc]: {
    id: 'translation_analysis_rpc',
    name: i18n.t('msp:RPC call'),
  },
  [DASHBOARD_TYPE.cache]: {
    id: 'translation_analysis_cache',
    name: i18n.t('msp:Cache call'),
  },
  [DASHBOARD_TYPE.database]: {
    id: 'translation_analysis_database',
    name: i18n.t('msp:Database call'),
  },
  [DASHBOARD_TYPE.mq]: {
    id: 'translation_analysis_mq',
    name: i18n.t('msp:MQ call'),
  },
};

const limits = [100, 200, 500, 1000];

const sortList = [
  {
    name: i18n.t('msp:AVG TIME DELAY DESC'),
    value: 0,
  },
  {
    name: i18n.t('msp:REQUEST COUNT DESC'),
    value: 1,
  },
];

const sortHasErrorList = [
  {
    name: i18n.t('msp:AVG TIME DELAY DESC'),
    value: 0,
  },
  {
    name: i18n.t('msp:REQUEST COUNT DESC'),
    value: 1,
  },
  {
    name: i18n.t('msp:ERROR COUNT DESC'),
    value: 2,
  },
];
const differSortList = differenceBy(sortHasErrorList, sortList, 'value');
const hasErrorListTypes = [DASHBOARD_TYPE.http, DASHBOARD_TYPE.rpc];

const callTypes = [
  {
    name: i18n.t('msp:producer'),
    value: 'producer',
  },
  {
    name: i18n.t('consumer'),
    value: 'consumer',
  },
];

// 变量为正则需要转义字符
const REG_CHARS = ['*', '.', '?', '+', '$', '^', '[', ']', '(', ')', '{', '}', '|', '/'];

interface IState {
  type: DASHBOARD_TYPE;
  search?: string;
  topic?: string;
  subSearch?: string;
  sort?: number;
  url?: string;
  traceSlowTranslation?: TOPOLOGY_SERVICE_ANALYZE.TranslationSlowResp;
  traceId?: string;
  startTime: number;
  visible: boolean;
  detailVisible: boolean;
  logVisible: boolean;
  sortType: TOPOLOGY_SERVICE_ANALYZE.SORT_TYPE;
  callType?: string;
  limit: number;
}

const Transaction = () => {
  const { getTraceSlowTranslation } = topologyServiceStore;
  const { startTimeMs, endTimeMs } = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan.range);
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const currentProject = mspStore.useStore((s) => s.currentProject);
  const [isFetching] = useLoading(topologyServiceStore, ['getTraceSlowTranslation']);
  const { setIsShowTraceDetail } = monitorCommonStore.reducers;
  const [serviceId, serviceName, _applicationId, requestCompleted] = serviceAnalyticsStore.useStore((s) => [
    s.serviceId,
    s.serviceName,
    s.applicationId,
    s.requestCompleted,
  ]);
  const [
    {
      type,
      search,
      topic,
      subSearch,
      sort,
      url,
      visible,
      traceSlowTranslation,
      detailVisible,
      traceId,
      startTime,
      logVisible,
      sortType,
      callType,
      limit,
    },
    updater,
  ] = useUpdate<IState>({
    type: query.type || DASHBOARD_TYPE.http,
    search: undefined,
    topic: undefined,
    subSearch: undefined,
    sort: defaultSort,
    url: undefined,
    traceSlowTranslation: undefined,
    traceId: undefined,
    startTime: 0,
    visible: false,
    detailVisible: false,
    logVisible: false,
    sortType: defaultTimeSort,
    callType: undefined,
    limit: limits[0],
  });

  React.useEffect(() => {
    // there are different sort type groups, the count of they is not match, so reset sort when switch type
    if (!hasErrorListTypes.includes(type) && differSortList.find((item) => item.value === sort)) {
      updater.sort(undefined);
    }
  }, [type, sort, updater]);

  const handleToggleType = (e: any) => {
    updater.type(e.target.value);
    updater.subSearch(undefined);
  };

  const queryTraceSlowTranslation = (
    sort_type: TOPOLOGY_SERVICE_ANALYZE.SORT_TYPE,
    queryLimit: number,
    cellValue: string,
  ) => {
    const { terminusKey } = params;
    updater.sortType(sort_type);
    updater.limit(queryLimit);
    if (serviceId) {
      getTraceSlowTranslation({
        sort: sort_type,
        start: startTimeMs,
        end: endTimeMs,
        terminusKey,
        serviceName,
        limit: queryLimit,
        serviceId,
        operation: cellValue,
      }).then((res) => updater.traceSlowTranslation(res));
    }
  };

  const handleChangeLimit = React.useCallback(
    (v) => {
      queryTraceSlowTranslation(sortType, v, url);
    },
    [sortType, url],
  );

  const handleChangeSortType = React.useCallback(
    (e) => {
      queryTraceSlowTranslation(e, limit, url);
    },
    [limit, url],
  );

  const handleBoardEvent = useCallback(
    ({ eventName, cellValue }: DC.BoardEvent) => {
      if (eventName === 'searchTranslation') {
        updater.subSearch(cellValue);
      }
      if (eventName === 'traceSlowTranslation') {
        updater.url(cellValue);
        updater.visible(true);
        queryTraceSlowTranslation(defaultTimeSort, limits[0], cellValue);
      }
    },
    [getTraceSlowTranslation, params, updater, startTimeMs, endTimeMs, serviceId],
  );

  const [columns, dataSource] = useMemo(() => {
    const c = [
      ...map(traceSlowTranslation?.cols, (col) => ({
        title: col.title,
        dataIndex: col.index,
      })),
    ];

    return [c, traceSlowTranslation?.data];
  }, [traceSlowTranslation, updater]);

  const tracingDrawerTitle = (
    <div className="w-60">
      <Ellipsis title={`${i18n.t('msp:tracing details')}(${url})`}>{`${i18n.t(
        'msp:tracing details',
      )}(${url})`}</Ellipsis>
    </div>
  );

  const extraGlobalVariable = useMemo(() => {
    let _subSearch = subSearch || search || topic;
    // 动态注入正则查询变量需要转义字符
    _subSearch &&
      REG_CHARS.forEach((char) => {
        _subSearch = _subSearch?.replaceAll(char, `\\${char}`);
      });
    // Backend requires accurate value, need to pass the value like this
    let _condition = '';
    if (callType === 'consumer') {
      _condition = `(target_service_id::tag='${serviceId}' and span_kind::tag='consumer')`;
    } else if (callType === 'producer') {
      _condition = `(source_service_id::tag='${serviceId}' and span_kind::tag='producer')`;
    } else {
      _condition = `(target_service_id::tag='${serviceId}' and span_kind::tag='consumer') or (source_service_id::tag='${serviceId}' and span_kind::tag='producer')`;
    }
    return {
      topic,
      search,
      sort,
      type: callType,
      condition: type === DASHBOARD_TYPE.mq ? _condition : undefined,
      subSearch: _subSearch || undefined,
      serviceId,
    };
  }, [subSearch, search, topic, callType, sort, type, serviceId]);

  if (!serviceId && requestCompleted) {
    return <NoServicesHolder />;
  }

  const traceSlowSlot = (
    <div className="flex items-center flex-wrap justify-start">
      <span>{i18n.t('msp:maximum number of queries')}：</span>
      <Select className="mr-3" value={limit} onChange={handleChangeLimit}>
        {limits.map((item) => (
          <Select.Option key={item} value={item}>
            {item}
          </Select.Option>
        ))}
      </Select>
      <span>{i18n.t('msp:sort method')}：</span>
      <Select value={sortType} onChange={handleChangeSortType}>
        {map(sortButtonMap, (v, k) => (
          <Select.Option key={k} value={k}>
            {v}
          </Select.Option>
        ))}
      </Select>
    </div>
  );

  const renderMenu = (record: TOPOLOGY_SERVICE_ANALYZE.TranslationSlowRecord) => {
    const { viewLog } = {
      viewLog: {
        title: i18n.t('check log'),
        onClick: () => {
          updater.traceId(record.requestId);
          updater.logVisible(true);
        },
      },
    };

    return [viewLog];
  };

  const actions: IActions<TOPOLOGY_SERVICE_ANALYZE.TranslationSlowRecord> = {
    render: (record: TOPOLOGY_SERVICE_ANALYZE.TranslationSlowRecord) => renderMenu(record),
  };

  return (
    <div className="service-analyze flex flex-col h-full">
      <div>
        <div className="flex justify-between items-center flex-wrap mb-1">
          <div className="left flex justify-between items-center mb-2">
            <If condition={type === DASHBOARD_TYPE.mq}>
              <Select
                placeholder={i18n.t('msp:call type')}
                allowClear
                style={{ width: '150px' }}
                onChange={updater.callType}
                value={callType}
              >
                {callTypes.map(({ name, value }) => (
                  <Select.Option key={value} value={value}>
                    {name}
                  </Select.Option>
                ))}
              </Select>
            </If>
            <Select
              className="ml-3"
              placeholder={i18n.t('msp:select sorting method')}
              allowClear
              style={{ width: '180px' }}
              onChange={(v) => updater.sort(v === undefined ? undefined : Number(v))}
              value={sort}
            >
              {(hasErrorListTypes.includes(type) ? sortHasErrorList : sortList).map(({ name, value }) => (
                <Select.Option key={value} value={value}>
                  {name}
                </Select.Option>
              ))}
            </Select>
            <If condition={type === DASHBOARD_TYPE.mq}>
              <DebounceSearch
                className="ml-3 w-48"
                placeholder={i18n.t('msp:search by Topic')}
                onChange={(v) => updater.topic(v)}
              />
            </If>
            <If condition={type !== DASHBOARD_TYPE.mq}>
              <DebounceSearch
                className="ml-3 w-48"
                placeholder={i18n.t('msp:search by transaction name')}
                onChange={(v) => updater.search(v)}
              />
            </If>
          </div>
          <div className="right flex justify-between items-center mb-2">
            <RadioGroup value={type} onChange={handleToggleType}>
              {map(dashboardIdMap, (v, k) => (
                <RadioButton key={k} value={k}>
                  {v.name}
                </RadioButton>
              ))}
            </RadioGroup>
            <TimeSelectWithStore className="ml-3" />
          </div>
        </div>
        <If condition={!!subSearch}>
          <Tag className="mb-2" closable onClose={() => updater.subSearch(undefined)}>
            <Tooltip title={subSearch}>
              <span>{subSearch && subSearch.length > 60 ? `${subSearch?.slice(0, 60)}...` : subSearch}</span>
            </Tooltip>
          </Tag>
        </If>
      </div>
      <div className="overflow-auto flex-1">
        <ServiceListDashboard
          key={dashboardIdMap[type].id}
          dashboardId={dashboardIdMap[type].id}
          extraGlobalVariable={extraGlobalVariable}
          onBoardEvent={handleBoardEvent}
        />
      </div>
      <Drawer
        title={tracingDrawerTitle}
        width="80%"
        className="z-50"
        visible={visible}
        onClose={() => updater.visible(false)}
      >
        <Table
          slot={traceSlowSlot}
          loading={isFetching}
          actions={currentProject?.type !== 'MSP' ? actions : null}
          rowKey="requestId"
          onRow={(record) => {
            return {
              onClick: () => {
                updater.traceId(record.requestId);
                setIsShowTraceDetail(true);
              },
            };
          }}
          columns={columns}
          dataSource={dataSource}
          onChange={() => queryTraceSlowTranslation(sortType, limit, url)}
        />
        <Drawer
          destroyOnClose
          title={i18n.t('runtime:monitor log')}
          width="80%"
          visible={logVisible}
          onClose={() => updater.logVisible(false)}
        >
          <SimpleLog requestId={traceId} applicationId={params?.applicationId || _applicationId} />
        </Drawer>
      </Drawer>
      <TraceSearchDetail traceId={traceId} startTime={startTime} />
    </div>
  );
};

export default Transaction;
