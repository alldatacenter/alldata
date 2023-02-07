/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import { Col, Row } from 'antd';
import { DoubleRightOutlined } from '@ant-design/icons';
import DataSources from '../DataSources';
import DataStorage from '../DataStorage';

export interface Props {
  inlongGroupId: string;
  inlongStreamId: string;
}

const Comp: React.FC<Props> = ({ inlongGroupId, inlongStreamId }) => {
  return (
    <>
      <Row gutter={60}>
        <Col span={12}>
          <DataSources inlongGroupId={inlongGroupId} inlongStreamId={inlongStreamId} />
        </Col>
        <Col span={12}>
          <DataStorage inlongGroupId={inlongGroupId} inlongStreamId={inlongStreamId} />
        </Col>
        <DoubleRightOutlined
          style={{ position: 'absolute', top: '50%', left: 'calc(50% - 7px)' }}
        />
      </Row>
    </>
  );
};

export default Comp;
