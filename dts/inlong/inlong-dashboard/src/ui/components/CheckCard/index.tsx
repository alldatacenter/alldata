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

import React, { useState, useEffect } from 'react';
import { Card, Col, Row, theme } from 'antd';
import { DoubleRightOutlined, DatabaseOutlined } from '@ant-design/icons';
import styles from './index.module.less';

export interface CheckCardOption {
  label: string;
  value: string | number;
}

export interface CheckCardProps {
  value?: string | number;
  onChange?: (value: boolean) => void;
  options?: CheckCardOption[];
  disabled: boolean;
  span?: number;
}

const { useToken } = theme;

const CheckCard: React.FC<CheckCardProps> = ({ options, value, onChange, disabled, span = 6 }) => {
  const [currentValue, setCurrentValue] = useState(value);

  const [isExpand, setExpandStatus] = useState(!Boolean(currentValue));

  const { token } = useToken();

  useEffect(() => {
    if (value !== currentValue) {
      setCurrentValue(value);
      setExpandStatus(false);
    }
    // eslint-disable-next-line
  }, [value]);

  const handleCardClick = newValue => {
    setExpandStatus(false);
    if (newValue !== currentValue) {
      setCurrentValue(newValue);
      if (onChange) {
        onChange(newValue);
      }
    }
  };

  return (
    <Row gutter={10} className={styles.cardRow}>
      {!isExpand ? (
        <>
          <Col span={span} className={styles.cardCol}>
            <Card
              size="small"
              bodyStyle={{ textAlign: 'center' }}
              className={disabled ? styles.cardDisabled : ''}
            >
              <DatabaseOutlined style={{ fontSize: 20 }} />
              <div>{options.find(item => item.value === currentValue)?.label}</div>
            </Card>
          </Col>
          {!disabled && (
            <Col style={{ display: 'flex', alignItems: 'center' }}>
              <DoubleRightOutlined
                className={styles.editIcon}
                onClick={() => setExpandStatus(true)}
              />
            </Col>
          )}
        </>
      ) : (
        options.map(item => (
          <Col span={span} key={item.value} className={styles.cardCol} title={item.label}>
            <Card
              hoverable
              size="small"
              onClick={() => handleCardClick(item.value)}
              style={item.value === currentValue ? { borderColor: token.colorPrimary } : {}}
            >
              {item.label}
            </Card>
          </Col>
        ))
      )}
    </Row>
  );
};

export default CheckCard;
