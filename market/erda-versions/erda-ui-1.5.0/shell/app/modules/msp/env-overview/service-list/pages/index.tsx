/* eslint-disable array-callback-return */
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
import { PAGINATION } from 'app/constants';
import { Button, Input, Tag, Tooltip } from 'antd';
import Pagination from 'common/components/pagination';
import { debounce, get, isNil, reduce } from 'lodash';
import EChart from 'charts/components/echarts';
import { CardColumnsProps, CardList, ErdaAlert, ErdaIcon, RadioTabs } from 'common';
import { goTo } from 'common/utils';
import { genLinearGradient, newColorMap } from 'app/charts/theme';
import './service-list.scss';
import routeInfoStore from 'core/stores/route';
import mspStore from 'msp/stores/micro-service';
import unknownIcon from 'app/images/default-project-icon.png';
import { getFormatter } from 'charts/utils';
import moment from 'moment';
import { getAnalyzerOverview, getServiceCount, getServices } from 'msp/services/service-list';
import i18n from 'i18n';
import DiceConfigPage from 'app/config-page';
import { functionalColor } from 'common/constants';
import { useUpdate } from 'common/use-hooks';

const defaultSeriesConfig = (color?: string) => ({
  type: 'line',
  showSymbol: false,
  itemStyle: {
    normal: {
      lineStyle: {
        color,
      },
    },
  },
  areaStyle: {
    normal: {
      color: genLinearGradient(color),
    },
  },
});

const option = {
  backgroundColor: 'rgba(48, 38, 71, 0.01)',
  xAxis: {
    show: false,
  },
  yAxis: {
    splitLine: {
      show: true,
    },
  },
  grid: { top: 0, bottom: 0, left: 0, right: 0 },
};

enum ERDA_ICON {
  java = 'java',
  golang = 'go',
  python = 'python',
  nodejs = 'nodejs',
  c = 'C-1',
  cpp = 'C++',
  php = 'php',
  dotnet = 'net',
  csharp = 'C',
}

const dataConvert: {
  [k in MSP_SERVICES.SERVICE_LIST_CHART_TYPE]: (value?: number) => { count: string | number; unit?: string };
} = {
  RPS: (value?: number) => (isNil(value) ? { count: '-' } : { count: value, unit: 'reqs/s' }),
  AvgDuration: (value?: number) => (isNil(value) ? { count: '-' } : getFormatter('TIME', 'ns').formatPro(value)),
  ErrorRate: (value?: number) => (isNil(value) ? { count: '-' } : { count: value, unit: '%' }),
};

const CHART_MAP: {
  [k in MSP_SERVICES.SERVICE_LIST_CHART_TYPE]: Array<{
    key: string;
    name: string;
    tips: string;
  }>;
} = {
  RPS: [
    {
      key: 'aggregateMetric.avgRps',
      name: i18n.t('msp:average throughput'),
      tips: i18n.t('msp:definition of average rps'),
    },
    {
      key: 'aggregateMetric.maxRps',
      name: i18n.t('msp:maximum throughput'),
      tips: i18n.t('msp:definition of maximum rps'),
    },
  ],
  AvgDuration: [
    {
      key: 'aggregateMetric.avgDuration',
      name: i18n.t('msp:average delay'),
      tips: i18n.t('msp:definition of average delay'),
    },
    {
      key: 'aggregateMetric.maxDuration',
      name: i18n.t('msp:maximum delay'),
      tips: i18n.t('msp:definition of maximum delay'),
    },
  ],
  ErrorRate: [
    {
      key: 'aggregateMetric.errorRate',
      name: i18n.t('msp:error rate'),
      tips: i18n.t('msp:definition of error rate'),
    },
  ],
};

const topNConfig = [
  {
    key: 'rpsMaxTop5',
    color: functionalColor.actions,
    icon: 'zuida',
  },
  {
    key: 'rpsMinTop5',
    color: functionalColor.success,
    icon: 'zuixiao',
  },
  {
    key: 'avgDurationTop5',
    color: functionalColor.warning,
    icon: 'yanshi',
  },
  {
    key: 'errorRateTop5',
    color: functionalColor.error,
    icon: 'cuowushuai',
  },
];

const topNMap = reduce(
  topNConfig,
  (prev, next) => {
    return {
      ...prev,
      [`service-list@${next.key}`]: {
        op: {
          clickRow: (item: CP_DATA_RANK.IItem) => {
            listDetail(item.id, item.name);
          },
        },
        props: {
          theme: [
            {
              titleIcon: next.icon,
              color: next.color,
            },
          ],
        },
      },
    };
  },
  {},
);

type IListItem = Merge<MSP_SERVICES.SERVICE_LIST_ITEM, { views: MSP_SERVICES.SERVICE_LIST_CHART['views'] }>;
type ServiceStatus = MSP_SERVICES.ServiceStatus | 'allService';

const listDetail = (serviceId: string, serviceName: string) => {
  const currentProject = mspStore.getState((s) => s.currentProject);
  const params = routeInfoStore.getState((s) => s.params);
  goTo(goTo.pages.mspServiceMonitor, {
    ...params,
    applicationId: currentProject?.type === 'MSP' ? '-' : serviceId.split('_')[0],
    serviceName,
    serviceId: window.encodeURIComponent(serviceId || ''),
  });
};

const tabs: Array<{ label: string; value: ServiceStatus; countKey: keyof MSP_SERVICES.ServiceCount }> = [
  {
    label: i18n.t('msp:all service'),
    value: 'allService',
    countKey: 'totalCount',
  },
  {
    label: i18n.t('msp:unhealthy service'),
    value: 'hasError',
    countKey: 'hasErrorCount',
  },
  {
    label: i18n.t('msp:no traffic service'),
    value: 'withoutRequest',
    countKey: 'withoutRequestCount',
  },
];

interface IState {
  searchValue: string;
  startTime: number;
  endTime: number;
  serviceStatus: MSP_SERVICES.ServiceStatus | 'allService';
  pagination: {
    current: number;
    pageSize: number;
  };
}

const MicroServiceOverview = () => {
  const [data, dataLoading] = getServices.useState();
  const tenantId = routeInfoStore.useStore((s) => s.params.terminusKey);
  const overviewList = getAnalyzerOverview.useData();
  const serViceCount = getServiceCount.useData();
  const [{ pagination, searchValue, serviceStatus, startTime, endTime }, updater, update] =
    useUpdate<IState>({
      searchValue: '',
      startTime: moment().subtract(1, 'h').valueOf(),
      endTime: moment().valueOf(),
      serviceStatus: tabs[0].value,
      pagination: { current: 1, pageSize: PAGINATION.pageSize },
    });

  React.useEffect(() => {
    getServiceCount.fetch({ tenantId });
  }, [tenantId]);

  const getServicesList = React.useCallback(() => {
    if (tenantId) {
      getServices.fetch({
        tenantId,
        serviceName: searchValue || undefined,
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
        serviceStatus: serviceStatus === 'allService' ? undefined : serviceStatus,
      });
    }
  }, [tenantId, pagination, searchValue, serviceStatus]);

  React.useEffect(() => {
    getServicesList();
  }, [getServicesList]);

  React.useEffect(() => {
    const serviceIdList = data?.list.map((item) => item?.id);
    if (serviceIdList?.length) {
      getAnalyzerOverview.fetch({
        view: 'service_overview',
        tenantId,
        serviceIds: serviceIdList,
      });
    }
  }, [data]);

  const onPageChange = (current: number, pageSize?: number) => {
    updater.pagination({ current, pageSize: pageSize || PAGINATION.pageSize });
  };

  const handleChange = React.useCallback(
    debounce((keyword: string) => {
      update({
        searchValue: keyword,
        pagination: { current: 1, pageSize: PAGINATION.pageSize },
      });
    }, 1000),
    [],
  );

  const handleRefresh = React.useCallback(() => {
    update({
      startTime: moment().subtract(1, 'h').valueOf(),
      endTime: moment().valueOf(),
    });
    getServicesList();
    getServiceCount.fetch({ tenantId });
  }, [getServicesList]);

  const columns: Array<CardColumnsProps<IListItem>> = [
    {
      dataIndex: 'language',
      colProps: { span: 8, className: 'flex items-center' },
      render: (language, { name, lastHeartbeat }) => {
        return (
          <>
            <div className="rounded-sm w-14 h-14 mr-2 language-wrapper">
              {ERDA_ICON[language] ? (
                <ErdaIcon type={ERDA_ICON[language]} size="56" />
              ) : (
                <img src={unknownIcon} width={56} height={56} />
              )}
            </div>
            <div>
              <p className="mb-0.5 font-medium text-xl leading-8">{name}</p>
              <Tag color="#59516C" className="mb-0.5 text-xs leading-5 border-0">
                {i18n.t('msp:last active time')}: {lastHeartbeat}
              </Tag>
            </div>
          </>
        );
      },
    },
    {
      dataIndex: 'id',
      colProps: { span: 16, className: 'flex items-center' },
      children: {
        columns: Object.keys(CHART_MAP).map((key) => {
          const chartItems = CHART_MAP[key];
          const item: CardColumnsProps<IListItem> = {
            colProps: {
              span: 8,
              className: 'flex items-center',
            },
            dataIndex: chartItems[0][key],
            render: (_: number, { views, ...rest }) => {
              const { type, view } = views?.find((t) => t.type === key) || {};
              const timeStamp: number[] = [];
              const value: number[] = [];
              view?.forEach((v) => {
                timeStamp.push(v.timestamp);
                value.push(v.value);
              });
              const currentOption = {
                ...option,
                xAxis: { data: timeStamp, show: false },
                tooltip: {
                  trigger: 'axis',
                  formatter: (param: Obj[]) => {
                    const { data: count, axisValue: time } = param[0] ?? [];
                    const tips = dataConvert[key](count);
                    return `<div style="color:rgba(255,255,255,0.60);margin-bottom:8px;">${moment(+time).format(
                      'YYYY-MM-DD HH:mm:ss',
                    )}</div><div class="flex justify-between"><span>${
                      chartItems[0].name
                    }</span><span style='margin-left:10px;'>${tips.count}${tips.unit}</span></div>`;
                  },
                },
                series: [
                  {
                    ...defaultSeriesConfig(
                      value.find((val) => val !== 0) && type === 'ErrorRate'
                        ? newColorMap.warning4
                        : newColorMap.primary4,
                    ),
                    data: value,
                    type: 'line',
                    smooth: false,
                  },
                ],
              };
              return (
                <div className="p-2 w-full bg-default-01">
                  <div className="w-full flex">
                    {chartItems.map((chartItem) => {
                      const { count, unit } = dataConvert[key](get(rest, chartItem.key));
                      return (
                        <div className="w-1/2">
                          <p className="mb-0 whitespace-nowrap font-number">
                            <span>{count}</span>
                            <span className="text-xs text-desc ml-1">{unit}</span>
                          </p>
                          <p className="mb-2 flex text-xs  text-desc">
                            {chartItem.name}
                            <Tooltip title={chartItem.tips}>
                              <ErdaIcon className="ml-1" type="help" />
                            </Tooltip>
                          </p>
                        </div>
                      );
                    })}
                  </div>
                  <div className="chart-wrapper w-full">
                    <EChart style={{ width: '100%', height: '37px', minHeight: 0 }} option={currentOption} />
                  </div>
                </div>
              );
            },
          };
          return item;
        }),
      },
    },
  ];

  const list = React.useMemo(() => {
    return (data?.list ?? []).map((item) => {
      const views = overviewList?.list.find((t) => t.serviceId === item.id)?.views ?? [];
      return {
        ...item,
        aggregateMetric: {
          ...item.aggregateMetric,
          maxRps: views.find((t) => t.type === 'RPS')?.maxValue ?? 0,
          maxDuration: views.find((t) => t.type === 'AvgDuration')?.maxValue ?? 0,
        },
        views,
      };
    });
  }, [data?.list, overviewList?.list]);

  const tabsOptions = React.useMemo(
    () =>
      tabs.map((item) => {
        return {
          ...item,
          label: `${item.label}(${serViceCount?.[item.countKey] ?? 0})`,
        };
      }),
    [serViceCount],
  );

  return (
    <div>
      <div className="top-button-group">
        <Button type="default" className="flex items-center" onClick={handleRefresh}>
          <ErdaIcon type="refresh" className="mr-1" />
          {i18n.t('refresh data')}
        </Button>
      </div>
      <ErdaAlert
        showOnceKey="msp-service-list"
        message={i18n.t(
          'msp:show all connected services in the current environment, as well as the key request indicators of the service in the last hour',
        )}
      />
      <DiceConfigPage
        showLoading
        forceUpdateKey={['inParams']}
        scenarioType="service-list"
        scenarioKey="service-list"
        inParams={{ tenantId, startTime, endTime }}
        fullHeight={false}
        customProps={{
          grid: {
            props: {
              span: [6, 6, 6, 6],
            },
          },
          ...topNMap,
        }}
      />
      <RadioTabs
        defaultValue={tabs[0].value}
        options={tabsOptions}
        onChange={(v) => {
          update({ serviceStatus: v, searchValue: '', pagination: { current: 1, pageSize: PAGINATION.pageSize } });
        }}
        className="mb-2 mt-4"
      />
      <CardList<IListItem>
        loading={dataLoading}
        rowKey="id"
        size="small"
        columns={columns}
        dataSource={list}
        rowClick={({ id, name }) => {
          listDetail(id, name);
        }}
        slot={
          <Input
            key={serviceStatus}
            prefix={<ErdaIcon type="search1" />}
            bordered={false}
            allowClear
            placeholder={i18n.t('msp:search by service name')}
            className="bg-hover-gray-bg w-72"
            onChange={(e) => {
              handleChange(e.target.value);
            }}
          />
        }
      />
      <div className="flex flex-1 flex-col bg-white shadow pb-2">
        <Pagination
          {...pagination}
          onChange={onPageChange}
          className="flex justify-end mr-4 mb-2"
          total={data?.total ?? 0}
        />
      </div>
    </div>
  );
};

export default MicroServiceOverview;
