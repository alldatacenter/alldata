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

import { map, get } from 'lodash';
import React from 'react';
import { Row, Col } from 'antd';
import moment from 'moment';
import i18n from 'i18n';
import { CardContainer, ErdaIcon } from 'common';

const scriptDetail = ({ data }: { data: object }) => {
  const errorDetail = get(data, 'list') || [];
  if (!errorDetail) {
    return (
      <div className="no-data-list">
        <div className="no-data-content">
          <ErdaIcon type="attention" size="16px" />
          {i18n.t('msp:no data')}
        </div>
      </div>
    );
  }

  return (
    <div>
      {map(errorDetail, (value, index) => {
        return (
          <CardContainer.ChartContainer
            className="error-detail"
            title={
              <div className="title">
                {i18n.t('msp:access path')}
                <a
                  href={`http://${value.host}${value.url}`}
                  target="_blank"
                  rel="noopener noreferrer"
                >{`${value.host}${value.url}`}</a>
              </div>
            }
            key={index}
          >
            <Row gutter={36}>
              <Col span={8}>
                <span className="title-secondly">{i18n.t('msp:time of occurrence')}</span>
                {moment(value.time).format('YYYY-MM-DD HH:mm:ss')}
              </Col>
              <Col span={10}>
                <span className="title-secondly">{i18n.t('msp:device type')}</span>
                {value.device}
              </Col>
              <Col span={6}>
                <span className="title-secondly">{i18n.t('msp:operating system')}</span>
                {value.os}
              </Col>
            </Row>
            <Row gutter={36}>
              <Col span={8}>
                <span className="title-secondly">{i18n.t('msp:browser')}</span>
                {value.browser}
              </Col>
              <Col span={8}>
                <span className="title-secondly">{i18n.t('version')}</span>
                {value.browser_version}
              </Col>
            </Row>
            <Row gutter={36}>
              <Col>
                <div className="title">{i18n.t('msp:error message')}</div>
                {value.error}
                <div>
                  <span className="title-secondly">{i18n.t('msp:error source')}</span>
                  {value.source}
                </div>
              </Col>
            </Row>
            <Row gutter={24}>
              <Col>
                <div className="title">{i18n.t('msp:stack information')}</div>
                {value.stack_trace ? <pre>{value.stack_trace}</pre> : i18n.t('none')}
              </Col>
            </Row>
          </CardContainer.ChartContainer>
        );
      })}
    </div>
  );
};

export default scriptDetail;
