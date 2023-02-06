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
import { Row, Col, Radio, Input, Select } from 'antd';
import { map, get, isEmpty } from 'lodash';
import { Icon as CustomIcon, BoardGrid, TimeSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo } from 'common/utils';
import moment from 'moment';
import statisticsStore from 'app/modules/publisher/stores/statistics';
import monitorCommonStore from 'common/stores/monitorCommon';
import publisherStore from 'publisher/stores/publisher';

import i18n from 'i18n';

import './index.scss';

interface IProps {
  artifacts: PUBLISHER.IArtifacts;
}

const RadioGroup = Radio.Group;

const layout = {
  xl: { span: 6 },
  lg: { span: 12 },
  sm: { span: 24 },
};

const formatTrendData = (data?: PUBLISHER.IStatisticsTrend) => {
  return [
    {
      label: i18n.t('publisher:7-day average new users'),
      data: get(data, '7dAvgNewUsers') || 0,
      subData: get(data, '7dAvgNewUsersGrowth') || 0,
    },
    {
      label: i18n.t('publisher:7-day average active users'),
      data: get(data, '7dAvgActiveUsers') || 0,
      subData: get(data, '7dAvgActiveUsersGrowth') || 0,
    },
    {
      label: i18n.t('publisher:7-day average new user retention rate for the next day'),
      data: get(data, '7dAvgNewUsersRetention') || 0,
      subData: get(data, '7dAvgNewUsersRetentionGrowth') || 0,
    },
    {
      label: i18n.t('publisher:7-day average usage time'),
      data: get(data, '7dAvgDuration') || 0,
      subData: get(data, '7dAvgDurationGrowth') || 0,
    },
    {
      label: i18n.t('publisher:total active users in the last 7 days'),
      data: get(data, '7dTotalActiveUsers') || 0,
      subData: get(data, '7dTotalActiveUsersGrowth') || 0,
    },
    {
      label: i18n.t('publisher:total active users in the last 30 days'),
      data: get(data, 'monthTotalActiveUsers') || 0,
      subData: get(data, 'monthTotalActiveUsersGrowth') || 0,
    },
    {
      label: i18n.t('publisher:cumulative users'),
      data: get(data, 'totalUsers') || 0,
    },
    {
      label: i18n.t('publisher:total crash rate'),
      data: get(data, 'totalCrashRate') || 0,
    },
  ];
};

const getLineChartLayout = (data: any) => {
  const { time, results } = data;
  const xData = map(time, (item) => moment(item).format('MM-DD HH:mm'));
  const resultData = get(results, '[0].data[0]');
  const metricData = [] as any[];
  map(resultData, (item) => {
    metricData.push({
      name: item.tag || item.name,
      type: 'line',
      data: item.data,
      unit: item.unit,
    });
  });
  return {
    xData,
    metricData,
  };
};

const getPieChartLayout = (data: any) => {
  const curDataList = get(data, 'results[0].data') || [];
  const legendData = [] as string[];
  const metricData = [
    {
      name: i18n.t('publisher:proportion'),
      radius: '60%',
      data: [] as Array<{ value: number; name: string }>,
    },
  ];
  map(curDataList, (dataItem) => {
    map(dataItem, (item) => {
      metricData[0].data.push({ value: item.data, name: item.tag });
      legendData.push(item.tag);
    });
  });
  return {
    legendData,
    metricData: isEmpty(metricData[0].data) ? [] : metricData,
  };
};

const tabs = [
  { label: i18n.t('publisher:new users'), value: 'fields.firstDayUserId_value' },
  { label: i18n.t('publisher:number of starts'), value: 'tags.cid' },
  { label: i18n.t('publisher:cumulative users'), value: 'tags.uid' },
];
const FilterTab = ({ onChange, className }: { onChange: (val: string) => void; className?: string }) => {
  const [value, setValue] = React.useState('fields.firstDayUserId_value');

  React.useEffect(() => {
    onChange(value);
  }, [onChange, value]);

  return (
    <RadioGroup
      className={`${className || ''}`}
      size="small"
      value={value}
      onChange={(e: any) => setValue(e.target.value)}
    >
      {map(tabs, ({ label, value: val }) => {
        return (
          <Radio.Button key={val} value={val}>
            {label}
          </Radio.Button>
        );
      })}
    </RadioGroup>
  );
};

const Statistics = (props: IProps) => {
  const { artifacts } = props;
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);
  const publishItemMonitors = publisherStore.useStore((s) => s.publishItemMonitors);
  const publisherItemId = artifacts.id;
  const { getStatisticsTrend, getStatisticsChart, getStatisticsPieChart } = statisticsStore.effects;
  const [
    {
      totalTrend,
      lineType,
      lineData,
      pieVersionType,
      pieChannelType,
      pieVersionData,
      pieChannelData,
      selectMonitorKey,
    },
    updater,
  ] = useUpdate({
    totalTrend: formatTrendData(),
    lineType: '',
    pieVersionType: '',
    pieChannelType: '',
    lineData: {},
    pieVersionData: {},
    pieChannelData: {},
    selectMonitorKey: Object.keys(publishItemMonitors)[0],
  });

  const monitorKey = React.useMemo(() => {
    const { ak, ai } = publishItemMonitors[selectMonitorKey] || {};
    return { ak, ai };
  }, [publishItemMonitors, selectMonitorKey]);

  React.useEffect(() => {
    getStatisticsTrend({ publisherItemId, ...monitorKey }).then((res) => updater.totalTrend(formatTrendData(res)));
    /* eslint-disable react-hooks/exhaustive-deps */
  }, [monitorKey]);

  const lineChartQuery = React.useMemo(() => {
    let query = { cardinality: lineType } as any;
    if (lineType === 'tags.cid') {
      query = { count: lineType, match_date: '*' };
    } else if (lineType === 'fields.firstDayUserId_value') {
      query['match_fields.firstDayUserId_value'] = '?*';
    }
    return {
      start: timeSpan.startTimeMs,
      end: timeSpan.endTimeMs,
      ...query,
      align: false,
      points: 7,
      ...monitorKey,
    };
  }, [timeSpan, lineType, monitorKey]);

  React.useEffect(() => {
    (lineChartQuery.cardinality || lineChartQuery.count) && getLineChartData(lineChartQuery);
  }, [lineChartQuery]);

  const getLineChartData = (q: any) => {
    getStatisticsChart({ ...q, publisherItemId, ...monitorKey }).then((res) => {
      updater.lineData(res);
    });
  };

  React.useEffect(() => {
    let extraQuery = { cardinality: pieVersionType } as any;
    if (pieVersionType === 'tags.cid') {
      extraQuery = { count: pieVersionType, match_date: '*' };
    } else if (pieVersionType === 'fields.firstDayUserId_value') {
      extraQuery['match_fields.firstDayUserId_value'] = '?*';
    } else if (pieVersionType === 'tags.uid') {
      extraQuery.match_uid = '?*';
    }
    const query = {
      publisherItemId,
      group: 'tags.av',
      ...extraQuery,
      limit: 10,
      // 累计用户取一个月前开始时间，其他取当天零点开始数据
      start:
        pieVersionType === 'tags.uid' ? moment().subtract(32, 'days').valueOf() : moment().startOf('day').valueOf(),
      end: new Date().getTime(), // 当前时间毫秒
      ...monitorKey,
    };
    pieVersionType && getStatisticsPieChart(query).then((res) => updater.pieVersionData(res));
  }, [pieVersionType, monitorKey]);

  React.useEffect(() => {
    let extraQuery = { cardinality: pieChannelType } as any;
    if (pieChannelType === 'tags.cid') {
      extraQuery = { count: pieChannelType, match_date: '*' };
    } else if (pieChannelType === 'fields.firstDayUserId_value') {
      extraQuery['match_fields.firstDayUserId_value'] = '?*';
    } else if (pieChannelType === 'tags.uid') {
      extraQuery.match_uid = '?*';
    }
    const query = {
      publisherItemId,
      group: 'tags.ch',
      ...extraQuery,
      limit: 10,
      // 累计用户取一个月前开始时间，其他取当天零点开始数据
      start:
        pieChannelType === 'tags.uid' ? moment().subtract(32, 'days').valueOf() : moment().startOf('day').valueOf(),
      end: new Date().getTime(), // 当前时间毫秒
      ...monitorKey,
    };
    pieChannelType && getStatisticsPieChart(query).then((res) => updater.pieChannelData(res));
  }, [pieChannelType, monitorKey]);

  const OnChangeLineType = React.useCallback((val: string) => {
    updater.lineType(val);
  }, []);

  const OnChangePieVersionType = React.useCallback((val: string) => {
    updater.pieVersionType(val);
  }, []);

  const OnChangePieChannelType = React.useCallback((val: string) => {
    updater.pieChannelType(val);
  }, []);

  const lineChart = [
    {
      w: 24,
      h: 9,
      x: 0,
      y: 0,
      i: 'line',
      moved: false,
      static: false,
      view: {
        chartType: 'chart:line',
        hideReload: true,
        staticData: getLineChartLayout(lineData),
        title: () => (
          <div className="chart-title flex justify-between items-center  w-full">
            <FilterTab onChange={OnChangeLineType} />
            <TimeSelector inline disabledDate={() => false} />
          </div>
        ),
      },
    },
    {
      w: 12,
      h: 9,
      x: 0,
      y: 9,
      i: 'pie1',
      moved: false,
      static: false,
      view: {
        chartType: 'chart:pie',
        hideReload: true,
        title: () => (
          <div className="chart-title flex justify-between items-center w-full">
            <span className="font-bold text-base">{i18n.t('publisher:top10 version')}</span>
            <span
              className="text-primary cursor-pointer"
              onClick={() => {
                goTo('./topDetail/version', { query: monitorKey });
              }}
            >
              {i18n.t('publisher:see details')}
            </span>
          </div>
        ),
        staticData: getPieChartLayout(pieVersionData),
        config: {
          option: {
            legend: {
              show: false,
            },
            tooltip: {
              formatter: `{a} <br/>${i18n.t('version')}{b}: {c}({d}%)`,
            },
            grid: { left: 45, right: 45 },
          },
        },
        customRender: (chartNode: any) => {
          return (
            <div className="flex flex-col h-full">
              <FilterTab className="mb-2" onChange={OnChangePieVersionType} />
              <div className="flex-1">{chartNode}</div>
            </div>
          );
        },
      },
    },
    {
      w: 12,
      h: 9,
      x: 12,
      y: 9,
      i: 'pie2',
      moved: false,
      static: false,
      view: {
        chartType: 'chart:pie',
        hideReload: true,
        staticData: getPieChartLayout(pieChannelData),
        title: () => (
          <div className="chart-title flex justify-between items-center w-full">
            <span className="font-bold text-base">{i18n.t('publisher:top10 channel')}</span>
            <span
              className="text-primary cursor-pointer"
              onClick={() => {
                goTo('./topDetail/channel', { query: monitorKey });
              }}
            >
              {i18n.t('publisher:see details')}
            </span>
          </div>
        ),
        config: {
          option: {
            legend: {
              show: false,
            },
            tooltip: {
              formatter: `{a} <br/>${i18n.t('publisher:channel')}{b}: {c}({d}%)`,
            },
          },
        },
        customRender: (chartNode: any) => {
          return (
            <div className="flex flex-col h-full">
              <FilterTab className="mb-2" onChange={OnChangePieChannelType} />
              <div className="flex-1">{chartNode}</div>
            </div>
          );
        },
      },
    },
  ];

  const config: FilterItemConfig[] = React.useMemo(
    () => [
      {
        type: Input,
        name: 'ak',
        customProps: {
          placeholder: i18n.t('please enter {name}', { name: 'AK' }),
          autoComplete: 'off',
        },
      },
      {
        type: Input,
        name: 'sk',
        customProps: {
          placeholder: i18n.t('please enter {name}', { name: 'SK' }),
          autoComplete: 'off',
        },
      },
    ],
    [],
  );

  return (
    <>
      <div className="artifacts-statistics block-padding">
        <Select
          value={selectMonitorKey}
          style={{ width: 200 }}
          className="mb-2"
          onChange={(k) => {
            updater.selectMonitorKey(k);
          }}
        >
          {map(publishItemMonitors, (_, key) => (
            <Select.Option key={key} value={key}>
              {key}
            </Select.Option>
          ))}
        </Select>
        <div className="total-trend block-container">
          <div className="title font-bold text-base">{i18n.t('publisher:overall trend')}</div>
          <Row className="pb-4">
            {map(totalTrend, (info, idx) => {
              return (
                <Col key={`${idx}`} {...layout}>
                  <div className="info-block border-bottom">
                    <div className="data">
                      <span className="main-data">{info.data}</span>
                      <span className="sub-data">
                        {info.subData !== undefined ? (
                          <>
                            {info.subData}
                            {`${info.subData}`.startsWith('-') ? (
                              <CustomIcon className="text-red" type="arrow-down" />
                            ) : (
                              <CustomIcon className="text-green" type="arrow-up" />
                            )}
                          </>
                        ) : null}
                      </span>
                    </div>
                    <div className="label">{info.label}</div>
                  </div>
                </Col>
              );
            })}
          </Row>
        </div>
      </div>
      <div className="artifacts-statistics-chart">
        <BoardGrid.Pure layout={lineChart} />
      </div>
    </>
  );
};

export default Statistics;
