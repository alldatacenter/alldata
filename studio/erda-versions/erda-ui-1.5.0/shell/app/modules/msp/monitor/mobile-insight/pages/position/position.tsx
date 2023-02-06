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
import { Row, Col, Button } from 'antd';
import { find } from 'lodash';
import { Link } from 'react-router-dom';
import { resolvePath } from 'common/utils';
import { SortTab } from 'monitor-common';
import './position.scss';
import PositionMap from './config/chartMap';
import i18n from 'i18n';
import { TimeSelectWithStore } from 'msp/components/time-select';

interface ITab {
  [pro: string]: any;
  fetchApi: string;
  query?: object;
}

const sortTabList = [
  { name: i18n.t('msp:user experience apdex'), key: 'apdex' },
  {
    name: i18n.t('msp:the whole page is loaded'),
    key: 'plt',
    fetchApi: 'ta_m_plt_grade/range',
    query: { range: 'plt', split: 10, rangeSize: 1000, source: true },
  },
  { name: i18n.t('msp:performance analysis'), key: 'comparative' },
];

const ComparativePanel = () => {
  return (
    <div className="monitor-comparative-container">
      <div className="monitor-comparative-tip">
        {i18n.t(
          'msp:Choose different indicators and user features for comparative analysis for different business types and scenarios, to locate issues accurately.',
        )}
      </div>
      <Link to={resolvePath('./comparative')}>
        <Button>{i18n.t('msp:comparative analysis')}</Button>
      </Link>
    </div>
  );
};

const Position = () => {
  const [tabKey, setTabKey] = React.useState(sortTabList[0].key);

  const changeSortTab = (key: string) => {
    setTabKey(key);
  };
  const curSortObj = find(sortTabList, { key: tabKey }) as ITab;

  return (
    <div>
      <div className="flex justify-end mb-3">
        <TimeSelectWithStore />
      </div>
      <div className="position-bars">
        <SortTab tabList={sortTabList} onChange={changeSortTab} />
        {tabKey === 'comparative' ? (
          <ComparativePanel />
        ) : tabKey === 'apdex' ? (
          <PositionMap.apdex />
        ) : (
          <PositionMap.timing fetchApi={curSortObj.fetchApi} query={curSortObj.query} />
        )}
      </div>
      <Row gutter={20}>
        <Col span={12}>
          {
            <PositionMap.dimension
              titleText={i18n.t('msp:operating system')}
              fetchApi="ta_m_percent_os"
              query={{ group: 'os' }}
              chartName="os"
            />
          }
        </Col>
        <Col span={12}>
          {
            <PositionMap.dimension
              titleText={i18n.t('msp:device')}
              fetchApi="ta_m_percent_device"
              query={{ group: 'device' }}
              chartName="device"
            />
          }
        </Col>
      </Row>
      <Row gutter={20}>
        <Col span={12}>
          {
            <PositionMap.dimension
              titleText={i18n.t('msp:app version')}
              fetchApi="ta_m_percent_av"
              query={{ group: 'av' }}
              chartName="av"
            />
          }
        </Col>
        <Col span={12}>
          {
            <PositionMap.dimension
              titleText={i18n.t('msp:domain name')}
              fetchApi="ta_m_percent_host"
              query={{ group: 'host' }}
              chartName="host"
            />
          }
        </Col>
      </Row>
      <Row gutter={20}>
        <Col span={12}>
          {
            <PositionMap.dimension
              titleText={i18n.t('msp:page')}
              fetchApi="ta_m_percent_page"
              query={{ group: 'doc_path' }}
              chartName="page"
            />
          }
        </Col>
      </Row>
    </div>
  );
};

export default Position;
