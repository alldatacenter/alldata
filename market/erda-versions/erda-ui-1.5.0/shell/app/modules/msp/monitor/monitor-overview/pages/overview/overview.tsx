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
import MonitorPanel from 'monitor-overview/common/components/monitor-panel';
import BIOverviewChartMap from 'browser-insight/pages/overview/config/chartMap';
import BIAjaxChartMap from 'browser-insight/pages/ajax/config/chartMap';
import AIWebChartMap from 'application-insight/pages/web/config/chartMap';
import Summary from './summary';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';
import './overview.scss';

interface IShowItem {
  name: string;
  Comp: any;
}

const constQuery = {
  // 当前一小时内
  end: Date.now(),
  start: Date.now() - 60 * 60 * 1000,
};
const curDay = new Date(new Date().toLocaleDateString()).getTime();
const wholeDayBefor = (num: number) => ({
  // num天（整点）
  end: curDay + 24 * 3600 * 1000,
  start: curDay - (num - 1) * 24 * 3600 * 1000,
});

const PVUVCharts = () => (
  <MonitorPanel className="main-panel" title={i18n.t('msp:visit (7 days)')}>
    <Row className="pvuv-container">
      <Col>
        <BIOverviewChartMap.loadPVUV query={{ constQuery: wholeDayBefor(7) }} />
      </Col>
    </Row>
  </MonitorPanel>
);

const BICharts = () => (
  <MonitorPanel className="main-panel" title={i18n.t('msp:browser access performance (within 1 hour)')}>
    <Row>
      <Col>
        <BIOverviewChartMap.performanceInterval query={{ constQuery }} />
      </Col>
    </Row>
    <Row className="last-line">
      <Col span={12}>
        <BIAjaxChartMap.ajaxPerformanceTrends query={{ constQuery }} />
      </Col>
      <Col span={12}>
        <BIAjaxChartMap.statusTopN query={{ constQuery }} />
      </Col>
    </Row>
  </MonitorPanel>
);

const AICharts = () => (
  <MonitorPanel className="main-panel" title={i18n.t('msp:application performance trends (within 1 hour)')}>
    <Row className="last-line">
      <Col span={12}>
        <AIWebChartMap.responseTimes query={{ constQuery }} />
      </Col>
      <Col span={12}>
        <AIWebChartMap.throughput query={{ constQuery }} />
      </Col>
    </Row>
  </MonitorPanel>
);

const list = [
  {
    name: 'Summary',
    Comp: Summary,
  },
  {
    name: 'PVUVCharts',
    Comp: PVUVCharts,
  },
  {
    name: 'BICharts',
    Comp: BICharts,
  },
  {
    name: 'AICharts',
    Comp: AICharts,
  },
];
let pauseTime = 100;
const Overview = () => {
  const [showList, setShowList] = React.useState([] as IShowItem[]);
  useEffectOnce(() => {
    lazyLoad();
  });

  const lazyLoad = (curList: IShowItem[] = []) => {
    const _list = curList.concat(list.shift() as IShowItem);
    setShowList(_list);
    if (list.length) {
      setTimeout(() => {
        lazyLoad(_list);
        pauseTime += 50;
      }, pauseTime);
    }
  };
  return (
    <div className="monitor-overview">
      {showList.map(({ Comp, name }: IShowItem) => (
        <Comp key={name} />
      ))}
    </div>
  );
};

export default Overview;
