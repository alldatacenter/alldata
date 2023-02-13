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
import OverviewMap from './config/chartMap';
import './overview.scss';
import TopTabRight from 'application-insight/common/components/tab-right';
import monitorCommonStore from 'common/stores/monitorCommon';
import i18n from 'i18n';

const Overview = () => {
  const chosenApp = monitorCommonStore.useStore((s) => s.chosenApp);
  const filter_target_application_id = chosenApp && chosenApp.id;
  const shouldLoad = filter_target_application_id !== undefined;
  return (
    <React.Fragment>
      <TopTabRight />
      <Row className="ai-overview" gutter={20}>
        <Col span={8}>
          <div className="ai-overview-sort-list">
            <div className="list-title font-medium">{i18n.t('msp:web transaction')} Top10</div>
            <OverviewMap.sortList shouldLoad={shouldLoad} query={{ filter_target_application_id }} />
          </div>
        </Col>
        <Col span={16}>
          <Row>
            <OverviewMap.overviewWeb shouldLoad={shouldLoad} query={{ filter_target_application_id }} />
          </Row>
          <Row>
            <OverviewMap.overviewCpm shouldLoad={shouldLoad} query={{ filter_target_application_id }} />
          </Row>
        </Col>
      </Row>
    </React.Fragment>
  );
};

export default Overview;
