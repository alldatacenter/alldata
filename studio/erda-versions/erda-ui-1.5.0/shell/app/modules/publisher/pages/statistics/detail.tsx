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
import { Select, Radio, Table } from 'antd';
import { SelectValue, ColumnProps } from 'core/common/interface';
import { map, get } from 'lodash';
import { useEffectOnce, useUnmount } from 'react-use';
import moment from 'moment';
import { TimeSelector, BoardGrid } from 'common';
import { useUpdate } from 'common/use-hooks';
import statisticsStore from 'app/modules/publisher/stores/statistics';
import routeInfoStore from 'core/stores/route';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import monitorCommonStore from 'common/stores/monitorCommon';
import './index.scss';

const RadioGroup = Radio.Group;
const { Option } = Select;

interface IFilterTabProp {
  tabs: Array<{
    label: string;
    value: string | number;
  }>;
  defaultValue?: string;
  className?: string;
  onChange: (val: string | undefined) => void;
}
const FilterTab = ({ tabs, defaultValue, onChange, className }: IFilterTabProp) => {
  const [value, setValue] = React.useState(defaultValue);
  const onChangeFilterTabRef = React.useRef(onChange);

  React.useEffect(() => {
    onChangeFilterTabRef.current(value);
  }, [value]);
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

const FilterGroup = ({
  onChange,
  className,
  groups,
  ...rest
}: {
  onChange: (val: string | undefined) => void;
  className?: string;
  groups: Array<{ [pro: string]: any; label: string; value: string }>;
}) => {
  const [value, setValue] = React.useState(undefined as undefined | string);
  const onChangeFilterGroupRef = React.useRef(onChange);

  React.useEffect(() => {
    onChangeFilterGroupRef.current(value);
  }, [value]);
  return (
    <Select
      className={`${className || ''}`}
      onChange={(val: SelectValue) => {
        setValue(val as string);
      }}
      allowClear
      value={value}
      {...rest}
    >
      {map(groups, ({ value: val, label }) => {
        return (
          <Option key={val} value={val}>
            {label}
          </Option>
        );
      })}
    </Select>
  );
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

const formatFilterData = (data: any) => {
  const result = get(data, 'results[0].data');
  const list = [] as Array<{ label: string; value: string }>;
  map(result, (item) => {
    map(item, ({ tag }) => list.push({ label: tag, value: tag }));
  });
  return list;
};

const StatisticList = ({ artifactsId, monitorKey }: { artifactsId: string; monitorKey: PUBLISHER.MonitorKey }) => {
  const TODAY = 'today';
  const YESTERDAY = 'yesterday';
  const versionStatisticList = statisticsStore.useStore((s) => s.versionStatisticList);
  const { getVersionStatistics } = statisticsStore.effects;
  const { clearVersionStatistic } = statisticsStore.reducers;
  const [loading] = useLoading(statisticsStore, ['getVersionStatistics']);
  const [{ endAt }, updater] = useUpdate({
    endAt: TODAY,
  });
  React.useEffect(() => {
    const endAtStr = endAt === TODAY ? String(moment().valueOf()) : String(moment().startOf('day').valueOf());
    monitorKey?.ai && getVersionStatistics({ artifactsId, endTime: endAtStr, ...monitorKey });
  }, [artifactsId, endAt, getVersionStatistics, monitorKey]);

  useUnmount(() => {
    return () => clearVersionStatistic();
  });

  const columns: Array<ColumnProps<PUBLISHER.VersionStatistic>> = [
    {
      title: i18n.t('version'),
      dataIndex: 'versionOrChannel',
      width: 140,
    },
    {
      title: `${i18n.t("publisher:as of today's cumulative users")}(%)`,
      dataIndex: 'totalUsers',
      width: 230,
      render: (text, record) => `${text}(${record.totalUsersGrowth})`,
    },
    {
      title: i18n.t('publisher:new users'),
      dataIndex: 'newUsers',
      width: 180,
    },
    {
      title: `${i18n.t('publisher:active user')}(%)`,
      dataIndex: 'activeUsers',
      width: 180,
      render: (text, record) => `${text}(${record.activeUsersGrowth})`,
    },
    {
      title: i18n.t('publisher:number of starts'),
      width: 120,
      dataIndex: 'launches',
    },
    {
      title: i18n.t('publisher:upgrade user'),
      width: 120,
      dataIndex: 'upgradeUser',
    },
  ];

  return (
    <>
      <FilterTab
        tabs={[
          { label: i18n.t('publisher:today'), value: TODAY },
          { label: i18n.t('publisher:yesterday'), value: YESTERDAY },
        ]}
        onChange={(val: string) => updater.endAt(val)}
        defaultValue={TODAY}
        className="m-2"
      />
      <Table
        columns={columns}
        loading={loading}
        dataSource={versionStatisticList.map((item, i) => ({ ...item, key: i }))}
        pagination={false}
        scroll={{ x: '100%' }}
      />
    </>
  );
};

const StatisticsDetail = () => {
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);
  const [{ publisherItemId, topType }, monitorKey] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { getAllGroup, getTopLineChart } = statisticsStore.effects;

  const [{ lineGroup, lineData, lineFilter, groups }, updater] = useUpdate({
    lineGroup: '',
    lineFilter: '' as string | undefined,
    lineData: {},
    groups: [],
  });
  useEffectOnce(() => {
    getAllGroup({
      publisherItemId,
      group: topType === 'version' ? 'tags.av' : 'tags.ch',
      count: 'tags.cid',
      start: moment().subtract(32, 'days').valueOf(),
      end: new Date().getTime(),
      ...monitorKey,
    }).then((res) => updater.groups(formatFilterData(res)));
  });

  const lineChartQuery = React.useMemo(() => {
    const key = topType === 'channel' ? 'filter_ch' : 'filter_av';
    const query = {} as any;
    if (lineGroup === 'fields.firstDayUserId_value') {
      query['match_fields.firstDayUserId_value'] = '?*';
    } else if (lineGroup === 'tags.uid') {
      query.match_uid = '?*';
    }
    return {
      start: timeSpan.startTimeMs,
      end: timeSpan.endTimeMs,
      cardinality: lineGroup,
      [key]: lineFilter || undefined,
      align: false,
      points: 7,
      ...query,
    };
  }, [topType, lineGroup, timeSpan, lineFilter]);

  React.useEffect(() => {
    const getLineChartData = (q: any) => {
      monitorKey?.ai &&
        q.cardinality &&
        getTopLineChart({ ...q, publisherItemId, ...monitorKey }).then((res) => updater.lineData(res));
    };

    getLineChartData(lineChartQuery);
  }, [getTopLineChart, lineChartQuery, monitorKey, publisherItemId, updater]);

  const lineChart = React.useMemo(
    () => [
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
          hideHeader: true,
          staticData: getLineChartLayout(lineData),
        },
      },
    ],
    [lineData],
  );
  const topTypeMap = {
    version: {
      title: i18n.t('publisher:top10 version'),
      groupPlaceholder: i18n.t('publisher:please select a version'),
    },
    channel: {
      title: i18n.t('publisher:top10 channel'),
      groupPlaceholder: i18n.t('publisher:please select a channel'),
    },
  };

  return (
    <div className="artifacts-statistics">
      <div className="mt-4 block-container">
        <div className="title flex justify-between items-center">
          <span className="font-bold text-base">{get(topTypeMap, `${topType}.title`)}</span>
          <div>
            <FilterGroup
              className="mr-2 version-selector"
              onChange={(val: string | undefined) => updater.lineFilter(val)}
              groups={groups}
              placeholder={get(topTypeMap, `${topType}.groupPlaceholder`)}
            />
            <TimeSelector inline disabledDate={() => false} />
          </div>
        </div>
        <div>
          <FilterTab
            tabs={[
              { label: i18n.t('publisher:new users'), value: 'fields.firstDayUserId_value' },
              { label: i18n.t('publisher:active user'), value: 'tags.uid' },
            ]}
            defaultValue="fields.firstDayUserId_value"
            onChange={(val: string) => updater.lineGroup(val)}
            className="m-2"
          />
          <BoardGrid.Pure layout={lineChart} />
        </div>
      </div>
      <div className="mt-4 block-container">
        <div className="title flex justify-between items-center">
          <span className="font-bold text-base">{i18n.t('publisher:detailed data')}</span>
        </div>
        <div>
          <StatisticList artifactsId={publisherItemId} monitorKey={monitorKey} />
        </div>
      </div>
    </div>
  );
};

export default StatisticsDetail;
