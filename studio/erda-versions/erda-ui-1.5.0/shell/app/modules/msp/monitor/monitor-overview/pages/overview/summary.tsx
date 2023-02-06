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
import { Row, Col } from 'antd';
import { floor } from 'lodash';
import { getFormatter } from 'charts/utils/formatter';
import { GridPanel } from './overview-types';
import monitorOverviewStore from 'monitor-overview/stores/monitor-overview';
import { useEffectOnce } from 'react-use';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';

const constQuery = {
  // 当前一小时内
  end: Date.now(),
  start: Date.now() - 60 * 60 * 1000,
};

const Summary = () => {
  const [aiCapacityData, biCapacityData] = monitorOverviewStore.useStore((s) => [s.aiCapacityData, s.biCapacityData]);

  const { getAiCapacityData, getBiCapacityApdex, getBiCapacityAjaxErr, getBiCapacityAjaxInfo } =
    monitorOverviewStore.effects;

  const [isBiCapacityFetching, isAiCapacityFetching] = useLoading(monitorOverviewStore, [
    'getBiCapacityAjaxInfo',
    'getAiCapacityData',
  ]);

  useEffectOnce(() => {
    getAiCapacityData({ avg: 'elapsed_mean', sumCpm: 'elapsed_count', ...constQuery });
    getBiCapacityApdex({ ...constQuery, range: 'plt', ranges: '0:2000,2000:' });
    getBiCapacityAjaxErr({ ...constQuery, range: 'status', split: 2, rangeSize: 400 });
    getBiCapacityAjaxInfo({ ...constQuery, avg: 'tt', cpm: 'tt' });
  });

  const aiCapacityConf = {
    webAvg: {
      name: i18n.t('response time'),
      value: getFormatter('TIME').format(aiCapacityData.webAvg),
      color: 'primary',
    },
    webCpm: {
      name: i18n.t('msp:throughput'),
      value: `${floor(aiCapacityData.webCpm, 2)} cpm`,
      color: 'danger',
    },
  };
  const pageCapacityConf = {
    apdex: {
      name: `Apdex ${i18n.t('msp:index')}`,
      value: `${floor(biCapacityData.apdex, 2)} T`,
      color: 'primary',
    },
    ajaxResp: {
      name: `Ajax ${i18n.t('response time')}`,
      value: getFormatter('TIME').format(biCapacityData.ajaxResp),
      color: 'primary',
    },
    ajaxCpm: {
      name: `Ajax ${i18n.t('msp:throughput')}`,
      value: `${floor(biCapacityData.ajaxCpm, 2)} cpm`,
      color: 'info',
    },
    ajaxErr: {
      name: `Ajax ${i18n.t('msp:error rate')}`,
      value: biCapacityData.ajaxErr ? `${floor(biCapacityData.ajaxErr, 2)} %` : biCapacityData.ajaxErr,
      color: 'danger',
    },
  };

  return (
    <Row gutter={24}>
      <Col span={12}>
        <GridPanel
          className="little-panel"
          title={i18n.t('msp:page performance indicator (within 1 hour)')}
          isFetching={isBiCapacityFetching}
          data={pageCapacityConf}
        />
      </Col>
      <Col span={12}>
        <GridPanel
          className="little-panel"
          title={i18n.t('msp:application performance indicators (within 1 hour)')}
          isFetching={isAiCapacityFetching}
          data={aiCapacityConf}
        />
      </Col>
    </Row>
  );
};

export default Summary;
