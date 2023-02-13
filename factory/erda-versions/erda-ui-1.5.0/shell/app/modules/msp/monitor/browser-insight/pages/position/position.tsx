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
import { TimeSelectWithStore } from 'msp/components/time-select';
import PositionMap from './config/chartMap';
import './position.scss';
import i18n from 'i18n';

interface IProps {
  sortTabList: ITab[];
}
interface IState {
  tabKey: string;
}
interface ITab {
  [pro: string]: any;
  fetchApi: string;
  query?: object;
}

const sortTabList = [
  { name: `${i18n.t('msp:user experience')} Apdex`, key: 'apdex' },
  {
    name: i18n.t('msp:the whole page is loaded'),
    key: 'plt',
    fetchApi: 'ta_plt_grade/range',
    query: { range: 'plt', split: 10, rangeSize: 1000, source: true },
  },
  {
    name: i18n.t('first paint time'),
    key: 'wst',
    fetchApi: 'ta_wst_grade/range',
    query: { range: 'wst', split: 10, rangeSize: 200, source: true },
  },
  {
    name: i18n.t('first contentful paint time'),
    key: 'fst',
    fetchApi: 'ta_fst_grade/range',
    query: { range: 'fst', split: 10, rangeSize: 400, source: true },
  },
  {
    name: i18n.t('msp:resource loading completed'),
    key: 'rct',
    fetchApi: 'ta_rct_grade/range',
    query: { range: 'rct', split: 10, rangeSize: 800, source: true },
  },
  {
    name: i18n.t('page loading completed'),
    key: 'pct',
    fetchApi: 'ta_pct_grade/range',
    query: { range: 'pct', split: 10, rangeSize: 600, source: true },
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

class Position extends React.Component<IProps, IState> {
  state = { tabKey: sortTabList[0].key };

  changeSortTab = (tabKey: string) => {
    this.setState({ tabKey });
  };

  render() {
    const { tabKey } = this.state;
    const curSortObj = find(sortTabList, { key: tabKey }) as ITab;
    return (
      <div>
        <div className="flex justify-end mb-3">
          <TimeSelectWithStore />
        </div>
        <div className="position-bars">
          <SortTab tabList={sortTabList} onChange={this.changeSortTab} />
          {tabKey === 'comparative' ? (
            <ComparativePanel />
          ) : tabKey === 'apdex' ? (
            <PositionMap.apdex />
          ) : (
            <PositionMap.timing fetchApi={curSortObj.fetchApi} query={curSortObj.query} />
          )}
        </div>
        <Row gutter={[20, 20]}>
          <Col span={12}>
            {
              <PositionMap.dimension
                titleText={i18n.t('msp:operating system')}
                fetchApi="ta_percent_os"
                query={{ group: 'os' }}
                chartName="os"
              />
            }
          </Col>
          <Col span={12}>
            {
              <PositionMap.dimension
                titleText={i18n.t('msp:device')}
                fetchApi="ta_percent_device"
                query={{ group: 'device' }}
                chartName="device"
              />
            }
          </Col>
          <Col span={12}>
            {
              <PositionMap.dimension
                titleText={i18n.t('msp:browser')}
                fetchApi="ta_percent_browser"
                query={{ group: 'browser' }}
                chartName="browser"
              />
            }
          </Col>
          <Col span={12}>
            {
              <PositionMap.dimension
                titleText={i18n.t('msp:domain name')}
                fetchApi="ta_percent_host"
                query={{ group: 'host' }}
                chartName="domain"
              />
            }
          </Col>
          <Col span={12}>
            {
              <PositionMap.dimension
                titleText={i18n.t('msp:page')}
                fetchApi="ta_percent_page"
                query={{ group: 'doc_path' }}
                chartName="page"
              />
            }
          </Col>
        </Row>
      </div>
    );
  }
}

export default Position;
