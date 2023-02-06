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

import React, { useEffect, useState } from 'react';
import moment from 'moment';
import { IF, CardContainer } from 'common';
import { Row, Col } from 'antd';
import { MonitorChartNew } from 'charts';
import { ALARM_REPORT_CHART_MAP } from 'app/modules/dcos/common/config';
import topChartList from './config/topChartList';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import alarmReportStore from 'cmp/stores/alarm-report';

const { ChartContainer } = CardContainer;

const AlarmReport = () => {
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { x_filter_host_ip: filter_host_ip, x_timestamp: timestamp } = query as {
    x_filter_host_ip: string;
    x_timestamp: string;
  };
  const alarmReport = alarmReportStore.useStore((s) => s.alarmReport);
  const { getAlarmReport } = alarmReportStore.effects;
  const filter_cluster_name = params.clusterName;
  const alarmType = params.chartUniqId;
  const [selectedTime, setSelectedTime] = useState(timestamp);
  const topChartAttrs = {
    shouldLoad: filter_cluster_name && selectedTime,
    query: {
      filter_host_ip,
      timestamp: selectedTime,
      filter_cluster_name,
    },
  };

  useEffect(() => {
    getAlarmReport({
      type: alarmType,
      query: {
        filter_host_ip,
        timestamp,
      },
    });
  }, [alarmType, filter_host_ip, getAlarmReport, timestamp]);

  const onEvents = {
    click: ({ name }: any) => {
      setSelectedTime(name);
    },
  };

  return (
    <>
      <ChartContainer title={ALARM_REPORT_CHART_MAP[alarmType || ''].cnName}>
        <MonitorChartNew data={alarmReport} onEvents={onEvents} />
      </ChartContainer>
      <IF check={!['disk', 'cpu', 'load'].includes(alarmType || '')}>
        <h3 className="mb-4 mt-8">{`${i18n.t('cmp:process')} TOP（${moment(Number(selectedTime)).format(
          'YYYY-MM-DD HH:mm:ss',
        )}）`}</h3>
        <Row gutter={20}>
          {topChartList.map((TopChart: any, key) => (
            <Col span={8} key={String(key)}>
              <TopChart {...topChartAttrs} />
            </Col>
          ))}
        </Row>
      </IF>
    </>
  );
};

export default AlarmReport;
