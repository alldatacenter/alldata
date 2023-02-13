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

import { isEmpty } from 'lodash';
import moment from 'moment';
import React from 'react';
import { Row, Col, Spin } from 'antd';
import MonthUptime from './month-uptime';
import monitorStatusStore from 'status-insight/stores/status';
import { useLoading } from 'core/stores/loading';
import { useEffectOnce } from 'react-use';
import './status-detail.scss';

const resetStatus = (time: number[], status: string[]) => {
  const resStatus = [...status];
  const m3DayLen = moment().daysInMonth(); // 当前月天数
  const m2DayLen = moment()
    .month(moment().month() - 1)
    .daysInMonth(); // 上个月天数
  const m1DayLen = moment()
    .month(moment().month() - 2)
    .daysInMonth(); // 上上个月天数
  if (!isEmpty(time)) {
    const lastTime = time[time.length - 1];
    const momentObj = moment(lastTime);
    const curDate = momentObj.date(); // 当前天
    if (curDate !== m3DayLen) {
      // 补足最后一个月的数据
      for (let i = curDate + 1; i <= m3DayLen; i++) {
        resStatus.push('default');
      }
    }
  }
  const totalLen = resStatus.length;
  const m3StatusArr = resStatus.slice(totalLen - m3DayLen);

  const m2SlicePos = [totalLen - m3DayLen - m2DayLen, totalLen - m3DayLen];
  const m2StatusArr = resStatus.slice(...m2SlicePos);

  const m1SlicePos = [totalLen - m3DayLen - m2DayLen - m1DayLen, totalLen - m2DayLen - m3DayLen];
  const m1Status = resStatus.slice(...m1SlicePos);
  return [m1Status, m2StatusArr, m3StatusArr];
};

const ThreeMonthUptime = () => {
  const metricStatus = monitorStatusStore.useStore((s) => s.metricStatus);
  const [isFetching] = useLoading(monitorStatusStore, ['getMetricStatus']);
  const { getMetricStatus } = monitorStatusStore.effects;
  const { clearMetricStatus } = monitorStatusStore.reducers;

  useEffectOnce(() => {
    getMetricStatus();
    return () => {
      clearMetricStatus();
    };
  });

  if (!isFetching && isEmpty(metricStatus)) {
    return null;
  }

  const { time = [], status = [] } = metricStatus;
  const statusArr = resetStatus(time, status);

  const m1Time = moment()
    .month(moment().month() - 2)
    .startOf('month')
    .valueOf();
  const m2Time = moment()
    .month(moment().month() - 1)
    .startOf('month')
    .valueOf();
  const m3Time = moment().valueOf();
  return (
    <Spin spinning={isFetching}>
      <Row className="uptime-bar">
        <Col span={12} xl={8}>
          <MonthUptime timestamp={m1Time} data={statusArr[0]} />
        </Col>
        <Col span={12} xl={8}>
          <MonthUptime timestamp={m2Time} data={statusArr[1]} />
        </Col>
        <Col span={12} xl={8}>
          <MonthUptime timestamp={m3Time} data={statusArr[2]} />
        </Col>
      </Row>
    </Spin>
  );
};

export default ThreeMonthUptime;
