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
import { Form, Row, Col, Radio } from 'antd';
import { JsonChecker } from 'common';
import i18n from 'i18n';

const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;
interface IProps {
  [pro: string]: any;
  trace?: any;
  expandSpan?: any;
}
const TraceDetailFilter = (props: IProps) => {
  const [form] = Form.useForm();
  const { trace, expandSpan } = props;
  const { spans, duration, services, depth, totalSpans } = trace;
  const jsonStr = JSON.stringify(trace, null, 2);
  const root = spans ? spans[0] : {};
  return (
    <Form form={form} className="ant-advanced-search-form">
      <div className="form-filter-container">
        <Row className="filter-top">
          <Col span={24}>
            <ul className="trace-nav clearfix">
              <li className="float-left">
                <strong>Duration:</strong> <span className="badge">{duration || 0}</span>
              </li>
              <li className="float-left">
                <strong>Services:</strong> <span className="badge">{services || 0}</span>
              </li>
              <li className="float-left">
                <strong>Depth:</strong> <span className="badge">{depth || 0}</span>
              </li>
              <li className="float-left">
                <strong>Total Spans:</strong> <span className="badge">{totalSpans || 0}</span>
              </li>
              <li className="float-right">
                <JsonChecker jsonString={jsonStr} />
              </li>
            </ul>
          </Col>
        </Row>
      </div>
      <Row className="trace-detail-filter-tab">
        <Col span={6}>
          <RadioGroup
            size="small"
            onChange={(e) => {
              const { value } = e.target;
              const isExpand = value === 'collapseAll';
              expandSpan({ spanId: root.spanId, children: root.children, isExpand });
            }}
            defaultValue="expandAll"
          >
            <RadioButton value="expandAll">{i18n.t('msp:expand all')}</RadioButton>
            <RadioButton value="collapseAll">{i18n.t('msp:fold all')}</RadioButton>
          </RadioGroup>
        </Col>
      </Row>
    </Form>
  );
};

export default TraceDetailFilter;
