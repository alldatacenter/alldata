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
import GeographyMap from 'browser-insight/pages/geography-china/config/chartMap';
import PositionMap from 'browser-insight/pages/position/config/chartMap';
import OverviewMap, { commonAttr } from './config/chartMap';
import { TimeSelectWithStore } from 'msp/components/time-select';
import i18n, { isZh } from 'i18n';

const Overview = () => {
  return (
    <div>
      <div className="flex justify-end mb-3">
        <TimeSelectWithStore />
      </div>
      <Row gutter={[20, 20]}>
        <Col span={16}>
          <OverviewMap.performanceInterval />
        </Col>
        <Col span={8}>
          <PositionMap.apdex titleText={i18n.t('msp:user experience')} groupId={commonAttr.groupId} />
        </Col>
        <Col span={12}>
          <OverviewMap.scriptError />
        </Col>
        <Col span={12}>
          <OverviewMap.pagePerformanceTrends />
        </Col>
        {isZh() && (
          <Col span={12}>
            <GeographyMap.regionalLoadingTime />
          </Col>
        )}
        <Col span={12}>
          <OverviewMap.ajaxPerformanceTrends />
        </Col>
      </Row>
    </div>
  );
};

export default Overview;
