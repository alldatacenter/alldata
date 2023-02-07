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

import type { MetaExportWithBackendList } from '@/metas/types';
import type { GroupMetaType } from '../types';

export const allDefaultGroups: MetaExportWithBackendList<GroupMetaType> = [
  {
    label: 'ALL',
    value: '',
    LoadEntity: () => import('../common/GroupInfo').then(r => ({ default: r.GroupInfo })),
  },
  {
    label: 'Kafka',
    value: 'KAFKA',
    LoadEntity: () => import('./Kafka'),
  },
  {
    label: 'Pulsar',
    value: 'PULSAR',
    LoadEntity: () => import('./Pulsar'),
  },
  {
    label: 'TubeMQ',
    value: 'TUBEMQ',
    LoadEntity: () => import('./TubeMq'),
  },
];
