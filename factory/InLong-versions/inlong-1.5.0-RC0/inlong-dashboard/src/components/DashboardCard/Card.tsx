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
import { Avatar, Card } from 'antd';
import { AvatarProps } from 'antd/lib/avatar';
import styles from './index.module.less';

export interface CardProps {
  title: string | number;
  desc?: string;
  icon?: AvatarProps['icon'];
}

const Comp: React.FC<CardProps> = ({ title, desc, icon }) => {
  return (
    <Card>
      <Card.Meta
        avatar={
          <Avatar
            shape="square"
            icon={icon}
            size={64}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'rgba(81,146,251,0.10)',
            }}
          />
        }
        title={<div className={styles.dashCardNums}>{title}</div>}
        description={desc}
      />
    </Card>
  );
};

export default Comp;
