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
import type { SinkMetaType } from '../types';

export const allDefaultSinks: MetaExportWithBackendList<SinkMetaType> = [
  {
    label: 'ALL',
    value: '',
    LoadEntity: () => import('../common/SinkInfo').then(r => ({ default: r.SinkInfo })),
  },
  {
    label: 'ClickHouse',
    value: 'CLICKHOUSE',
    LoadEntity: () => import('./ClickHouse'),
  },
  {
    label: 'Doris',
    value: 'DORIS',
    LoadEntity: () => import('./Doris'),
  },
  {
    label: 'Elasticsearch',
    value: 'ELASTICSEARCH',
    LoadEntity: () => import('./Elasticsearch'),
  },
  {
    label: 'Greenplum',
    value: 'GREENPLUM',
    LoadEntity: () => import('./Greenplum'),
  },
  {
    label: 'Hive',
    value: 'HIVE',
    LoadEntity: () => import('./Hive'),
  },
  {
    label: 'HBase',
    value: 'HBASE',
    LoadEntity: () => import('./HBase'),
  },
  {
    label: 'Iceberg',
    value: 'ICEBERG',
    LoadEntity: () => import('./Iceberg'),
  },
  {
    label: 'Hudi',
    value: 'HUDI',
    LoadEntity: () => import('./Hudi'),
  },
  {
    label: 'Kafka',
    value: 'KAFKA',
    LoadEntity: () => import('./Kafka'),
  },
  {
    label: 'MySQL',
    value: 'MYSQL',
    LoadEntity: () => import('./MySQL'),
  },
  {
    label: 'Oracle',
    value: 'ORACLE',
    LoadEntity: () => import('./Oracle'),
  },
  {
    label: 'PostgreSQL',
    value: 'POSTGRESQL',
    LoadEntity: () => import('./PostgreSQL'),
  },
  {
    label: 'SQLServer',
    value: 'SQLSERVER',
    LoadEntity: () => import('./SQLServer'),
  },
  {
    label: 'StarRocks',
    value: 'STARROCKS',
    LoadEntity: () => import('./StarRocks'),
  },
  {
    label: 'TDSQLPostgreSQL',
    value: 'TDSQLPOSTGRESQL',
    LoadEntity: () => import('./TDSQLPostgreSQL'),
  },
];
