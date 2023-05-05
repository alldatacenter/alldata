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
import { Spin, Row, Col, Button, Tooltip } from 'antd';
import { useUpdate } from 'common/use-hooks';
import StatusChart from './status-detail-chart';
import MonthUptime from './3-month-uptime';
import AddModal from './add-modal';
import PastIncidents from './past-incidents';
import monitorStatusStore from 'status-insight/stores/status';
import { useLoading } from 'core/stores/loading';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import './status-detail.scss';
import routeInfoStore from 'core/stores/route';

const StatusDetail = () => {
  const params = routeInfoStore.useStore((s) => s.params);
  const [detail, pastIncidents] = monitorStatusStore.useStore((s) => [s.detail, s.pastIncidents]);
  const { getStatusDetail, getPastIncidents } = monitorStatusStore.effects;
  const { clearStatusDetail } = monitorStatusStore.reducers;
  const [isFetching, setDatumFetching] = useLoading(monitorStatusStore, ['getStatusDetail', 'setDatumPoint']);

  const [{ period, modalVisible }, updater] = useUpdate({
    period: 'hour',
    modalVisible: false,
  });

  useEffectOnce(() => {
    getStatusDetail();
    getPastIncidents();
    return () => {
      clearStatusDetail();
    };
  });

  const handePeriod = (_period: string) => {
    updater.period(_period);
    getStatusDetail({ period: _period });
  };

  const toggleModal = () => {
    updater.modalVisible(!modalVisible);
  };

  let data = {} as MONITOR_STATUS.IDashboard;
  if (detail.metrics && detail.metrics[params.metricId]) {
    data = detail.metrics[params.metricId];
  }

  const { time = [], status = [], latency = [0] } = data.chart || {};

  const formData = { ...data, id: params.metricId };

  const max = Math.max(...latency);
  const min = Math.min(...latency);
  const avg = Math.round(latency.reduce((all: number, item: number) => all + item, 0) / latency.length);

  const timeRange = [
    {
      text: i18n.t('msp:{num} month', { num: 1 }),
      value: 'month',
    },
    {
      text: i18n.t('msp:{num} week', { num: 1 }),
      value: 'week',
    },
    {
      text: i18n.t('msp:{num} day', { num: 1 }),
      value: 'day',
    },
    {
      text: i18n.t('msp:{num} hour', { num: 1 }),
      value: 'hour',
    },
  ];

  const emptyText = (value: any) => (value === undefined ? i18n.t('msp:no data') : value);

  const colorMap = {
    Operational: 'green',
    'Major Outage': 'red',
    Miss: 'grey',
  };

  return (
    <div className="status-detail-page">
      <Spin spinning={isFetching}>
        <div className="url-bar">
          <span className="mr-4 whitespace-nowrap overflow-ellipsis overflow-hidden ...">
            {data.name}&nbsp;&nbsp;
            <Tooltip arrowPointAtCenter title={data.config?.url}>
              ({data.config?.url})
            </Tooltip>
          </span>
          <div>
            {
              // data.mode === 'browser' ? ( // 暂时去掉
              //   <Button type="primary" className="mr-2" ghost loading={setDatumFetching} onClick={this.setDatumPoint}>{i18n.t('msp:set datum point')}</Button>
              // ) : null
            }
            <Button type="primary" ghost onClick={toggleModal}>
              {i18n.t('edit')}
            </Button>
          </div>
          <AddModal
            modalVisible={modalVisible}
            toggleModal={toggleModal}
            formData={formData}
            afterSubmit={getStatusDetail}
          />
        </div>
        <div className="row-space" />

        <div className="title-bar">
          <span className="title">{i18n.t('msp:availability and performance')}</span>
          <ul className="time-range">
            {timeRange.map((t) => {
              return (
                <li key={t.value} onClick={() => handePeriod(t.value)} className={period === t.value ? 'active' : ''}>
                  {t.text}
                </li>
              );
            })}
          </ul>
        </div>

        <Row gutter={16} className="summary-bar">
          <Col span={6}>
            <div className="summary-card nowrap">
              <span className="name">{i18n.t('msp:availability')}</span>
              <span className="value">{emptyText(data.uptime)}</span>
            </div>
          </Col>
          <Col span={6}>
            <div className="summary-card nowrap">
              <span className="name">{i18n.t('msp:downtime')}</span>
              <span className="value">
                <Tooltip title={emptyText(data.downtime)}>{data.downDuration}</Tooltip>
                <ErdaIcon type="tishi" size="16" className="info-icon" />
              </span>
            </div>
          </Col>
          <Col span={6}>
            <div className="summary-card nowrap">
              <span className="name">{i18n.t('msp:user experience index')}</span>
              <span className="value">{emptyText(data.apdex ? data.apdex.toFixed(2) : data.apdex)}</span>
            </div>
          </Col>
          <Col span={6}>
            <div className="summary-card nowrap">
              <span className="name">{i18n.t('msp:average response time')}</span>
              <span className="value">{emptyText(data.latency)} ms</span>
            </div>
          </Col>
        </Row>
        <div className="row-space" />

        <div className="chart-wrap">
          <div className="legend-bar">
            <div>
              <span className="left-item">
                <span className="status-point chart" />
                {i18n.t('response time')}
              </span>
              <span className="left-item">
                <span className="status-point success" />
                {i18n.t('msp:available')}
              </span>
              <span className="left-item">
                <span className="status-point danger" />
                {i18n.t('msp:downtime')}
              </span>
            </div>
            <div>
              <span className="right-item">
                {i18n.t('msp:maximum value')}: <span className="blod">{max} ms</span>
              </span>
              <span className="right-item">
                {i18n.t('msp:minimum')}: <span className="blod">{min} ms</span>
              </span>
              <span className="right-item">
                {i18n.t('msp:average value')}: <span className="blod">{avg} ms</span>
              </span>
            </div>
          </div>
          <StatusChart
            xAxisData={time}
            data={latency}
            period={period}
            style={{ minWidth: '520px', width: '100%', height: '340px' }}
          />
          <ul className="status-list">
            {status.map((item: string, i: number) => (
              <li key={String(i)} className={colorMap[item]} />
            ))}
          </ul>
        </div>
      </Spin>
      <div className="row-space" />
      <div className="title-bar">
        <span className="title">
          {i18n.t('msp:history available time')}
          <span className="sub">（{i18n.t('msp:historical downtime')}）</span>
        </span>
      </div>
      <MonthUptime />
      <div className="title-bar past-incident">
        <span className="title">{i18n.t('msp:the past 3 months')}</span>
      </div>
      <PastIncidents pastIncidents={pastIncidents} fetchData={getPastIncidents}/>
    </div>
  );
};

export default StatusDetail;
