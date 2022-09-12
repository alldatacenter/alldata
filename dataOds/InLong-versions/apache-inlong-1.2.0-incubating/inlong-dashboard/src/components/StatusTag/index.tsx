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
import { Tag } from 'antd';
import { CheckCircleFilled, ClockCircleFilled, CloseCircleFilled } from '@/components/Icons';

export interface StatusTagProps {
  type: 'default' | 'primary' | 'success' | 'error' | 'warning';
  title: string;
  icon?: React.ReactNode;
}

const list = [
  {
    type: 'default',
    color: '#999999',
    bg: 'rgba(153,153,153,0.10)',
    icon: <ClockCircleFilled />,
  },
  {
    type: 'primary',
    color: '#0052D9',
    bg: 'rgba(0,82,217,0.10)',
    icon: <CheckCircleFilled />,
  },
  {
    type: 'success',
    color: '#00A870',
    bg: 'rgba(0,168,112,0.10)',
    icon: <CheckCircleFilled />,
  },
  {
    type: 'error',
    color: '#E34D59',
    bg: 'rgba(227,77,89,0.10)',
    icon: <CloseCircleFilled />,
  },
  {
    type: 'warning',
    color: '#ED7B2F',
    bg: 'rgba(237,123,47,0.10)',
    icon: <ClockCircleFilled />,
  },
];

const map = new Map(list.map(item => [item.type, item]));

const Comp: React.FC<StatusTagProps> = ({ type, title, icon }) => {
  const config = map.get(type);

  return (
    <Tag
      icon={icon || config.icon}
      color={config.bg}
      style={{ borderRadius: 20, color: config.color, fontSize: '14px' }}
    >
      {title}
    </Tag>
  );
};

export default Comp;
