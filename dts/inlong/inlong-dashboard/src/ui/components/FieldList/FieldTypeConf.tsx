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

import { ColumnsType } from 'antd/es/table';

export interface Props {
  inlongGroupId: string;
  inlongStreamId?: string;
  isSource: boolean;
  columns: ColumnsType;
  //   readonly?: string;
}

const clickhouseFieldType = [
  'String',
  'Int8',
  'Int16',
  'Int32',
  'Int64',
  'Float32',
  'Float64',
  'DateTime',
  'Date',
].map(item => ({
  label: item,
  value: item,
}));

const dorisFieldTypes = [
  'NULL_TYPE',
  'BOOLEAN',
  'TINYINT',
  'SMALLINT',
  'INT',
  'BIGINT',
  'FLOAT',
  'DOUBLE',
  'DATE',
  'DATETIME',
  'DECIMAL',
  'CHAR',
  'LARGEINT',
  'VARCHAR',
  'DECIMALV2',
  'TIME',
  'HLL',
].map(item => ({
  label: item,
  value: item,
}));

const esFieldTypes = [
  'text',
  'keyword',
  'date',
  'boolean',
  'long',
  'integer',
  'short',
  'byte',
  'double',
  'float',
  'half_float',
  'scaled_float',
].map(item => ({
  label: item,
  value: item,
}));

const hbaseFieldTypes = [
  'int',
  'short',
  'long',
  'float',
  'double',
  'byte',
  'bytes',
  'string',
  'boolean',
].map(item => ({
  label: item,
  value: item,
}));

const hiveFieldTypes = [
  'string',
  'varchar',
  'char',
  'tinyint',
  'smallint',
  'int',
  'bigint',
  'float',
  'double',
  'decimal',
  'numeric',
  'boolean',
  'binary',
  'timestamp',
  'date',
  // 'interval',
].map(item => ({
  label: item,
  value: item,
}));

const hudiFieldTypes = [
  'int',
  'long',
  'string',
  'float',
  'double',
  'date',
  'timestamp',
  'time',
  'boolean',
  'decimal',
  'timestamptz',
  'binary',
  'fixed',
  'uuid',
].map(item => ({
  label: item,
  value: item,
}));

const icebergFieldTypes = [
  'string',
  'boolean',
  'int',
  'long',
  'float',
  'double',
  'decimal',
  'date',
  'time',
  'timestamp',
  'timestamptz',
  'binary',
  'fixed',
  'uuid',
].map(item => ({
  label: item,
  value: item,
}));

const kuduFieldTypes = [
  'int',
  'long',
  'string',
  'float',
  'double',
  'date',
  'timestamp',
  'time',
  'boolean',
  'decimal',
  'timestamptz',
  'binary',
  'fixed',
  'uuid',
].map(item => ({
  label: item,
  value: item,
}));

const redisFieldTypes = [
  'int',
  'long',
  'string',
  'float',
  'double',
  'date',
  'timestamp',
  'time',
  'boolean',
  'timestamptz',
  'binary',
].map(item => ({
  label: item,
  value: item,
}));

const tdsqlPgFieldTypes = [
  'SMALLINT',
  'SMALLSERIAL',
  'INT2',
  'SERIAL2',
  'INTEGER',
  'SERIAL',
  'BIGINT',
  'BIGSERIAL',
  'REAL',
  'FLOAT4',
  'FLOAT8',
  'DOUBLE',
  'NUMERIC',
  'DECIMAL',
  'BOOLEAN',
  'DATE',
  'TIME',
  'TIMESTAMP',
  'CHAR',
  'CHARACTER',
  'VARCHAR',
  'TEXT',
  'BYTEA',
].map(item => ({
  label: item,
  value: item,
}));

const greenplumTypesConf = {
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  INT2: () => '',
  SMALLSERIAL: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  SERIAL: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  SERIAL2: () => '',
  INTEGER: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  BIGSERIAL: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  REAL: () => '',
  FLOAT4: () => '',
  FLOAT8: () => '',
  DOUBLE: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  NUMERIC: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  DECIMAL: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  BOOLEAN: () => '',
  DATE: () => '',
  TIME: () => '',
  TIMESTAMP: () => '',
  CHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1 <= M <= 255'),
  CHARACTER: (m, d) => (1 <= m && m <= 255 ? '' : '1 <= M <= 255'),
  VARCHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1 <= M <= 255'),
  TEXT: () => '',
  BYTEA: () => '',
};

const mysqlTypesConf = {
  TINYINT: (m, d) => (1 <= m && m <= 4 ? '' : '1<=M<=4'),
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1<=M<=6'),
  MEDIUMINT: (m, d) => (1 <= m && m <= 9 ? '' : '1<=M<=9'),
  INT: (m, d) => (1 <= m && m <= 11 ? '' : '1<=M<=11'),
  FLOAT: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1<=M<=20'),
  DOUBLE: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  NUMERIC: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  DECIMAL: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  BOOLEAN: () => '',
  DATE: () => '',
  TIME: () => '',
  DATETIME: () => '',
  CHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  VARCHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  TEXT: () => '',
  BINARY: (m, d) => (1 <= m && m <= 64 ? '' : '1<=M<=64'),
  VARBINARY: (m, d) => (1 <= m && m <= 64 ? '' : '1<=M<=64'),
  BLOB: () => '',
};

const oracleTypesConf = {
  BINARY_FLOAT: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  BINARY_DOUBLE: (m, d) => (1 <= m && m <= 10 ? '' : '1 <= M <= 10'),
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  FLOAT: (m, d) => (1 <= m && m <= 126 ? '' : '1 <= M <= 126'),
  FLOAT4: () => '',
  FLOAT8: () => '',
  DOUBLE: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  REAL: () => '',
  NUMBER: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  NUMERIC: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  DATE: () => '',
  DECIMAL: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  BOOLEAN: () => '',
  TIMESTAMP: () => '',
  CHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  VARCHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  CLOB: () => '',
  RAW: (m, d) => (1 <= m && m <= 2000 ? '' : ' 1 <= M <= 2000'),
  BLOB: () => '',
};

const pgTypesConf = {
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  INT2: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  SMALLSERIAL: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  SERIAL2: () => '',
  INTEGER: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  SERIAL: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  BIGSERIAL: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  REAL: (m, d) => (1 <= m && m <= 24 ? '' : '1 <= M <= 24'),
  FLOAT4: (m, d) => (1 <= m && m <= 24 ? '' : '1 <= M <= 24'),
  FLOAT8: (m, d) => (24 < m && m <= 53 ? '' : '24 < M <= 53'),
  DOUBLE: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  NUMERIC: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  DECIMAL: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  BOOLEAN: () => '',
  DATE: () => '',
  TIME: () => '',
  TIMESTAMP: () => '',
  CHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  CHARACTER: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  VARCHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  TEXT: () => '',
  BYTEA: () => '',
};

const sqlServerTypesConf = {
  CHAR: (m, d) => (1 <= m && m <= 8000 ? '' : '1 <= M <= 8000'),
  VARCHAR: (m, d) => (1 <= m && m <= 8000 ? '' : '1 <= M<= 8000'),
  NCHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  NVARCHAR: (m, d) => (1 <= m && m <= 4000 ? '' : '1 <= M <= 4000'),
  TEXT: () => '',
  NTEXT: () => '',
  XML: () => '',
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  BIGSERIAL: (m, d) => (1 <= m && m <= 20 ? '' : '1 <= M <= 20'),
  DECIMAL: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D < M'),
  MONEY: (m, d) => (1 <= m && m <= 15 && 1 <= d && d <= 4 ? '' : '1 <= M <= 15, 1 <= D <= 4'),
  SMALLMONEY: (m, d) => (1 <= m && m <= 7 && 1 <= d && d <= 4 ? '' : '1 <= M <= 7, 1 <= D <= 4'),
  NUMERIC: (m, d) => (1 <= m && m <= 38 && 0 <= d && d < m ? '' : '1 <= M <= 38, 0 <= D <= M'),
  FLOAT: (m, d) => (1 <= m && m <= 24 ? '' : '1 <= M <= 24'),
  REAL: (m, d) => (1 <= m && m <= 24 ? '' : '1 <= M <= 24'),
  BIT: (m, d) => (1 <= m && m <= 64 ? '' : '1 <= M <= 64'),
  INT: (m, d) => (1 <= m && m <= 11 ? '' : '1 <= M <= 11'),
  TINYINT: (m, d) => (1 <= m && m <= 4 ? '' : '1 <= M <= 4'),
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1 <= M <= 6'),
  TIME: () => '',
  DATETIME: () => '',
  DATETIME2: () => '',
  SMALLDATETIME: () => '',
  DATETIMEOFFSET: () => '',
};

const starRocksTypesConf = {
  CHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  VARCHAR: (m, d) => (1 <= m && m <= 255 ? '' : '1<=M<=255'),
  DATE: () => '',
  TINYINT: (m, d) => (1 <= m && m <= 4 ? '' : '1<=M<=4'),
  SMALLINT: (m, d) => (1 <= m && m <= 6 ? '' : '1<=M<=6'),
  INT: (m, d) => (1 <= m && m <= 11 ? '' : '1<=M<=11'),
  BIGINT: (m, d) => (1 <= m && m <= 20 ? '' : '1<=M<=20'),
  LARGEINT: () => '',
  STRING: () => '',
  DATETIME: () => '',
  FLOAT: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  DOUBLE: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  DECIMAL: (m, d) =>
    1 <= m && m <= 255 && 1 <= d && d <= 30 && d <= m - 2 ? '' : '1<=M<=255,1<=D<=30,D<=M-2',
  BOOLEAN: () => '',
};

const getFieldTypes = fieldTypesConf => {
  return Object.keys(fieldTypesConf).reduce(
    (acc, key) =>
      acc.concat({
        label: key,
        value: key,
      }),
    [],
  );
};

export const fieldAllTypes = {
  CLICKHOUSE: clickhouseFieldType,
  DORIS: dorisFieldTypes,
  ELASTICSEARCH: esFieldTypes,
  GREENPLUM: getFieldTypes(greenplumTypesConf),
  HIVE: hiveFieldTypes,
  HBASE: hbaseFieldTypes,
  ICEBERG: icebergFieldTypes,
  HUDI: hudiFieldTypes,
  MYSQL: getFieldTypes(mysqlTypesConf),
  ORACLE: getFieldTypes(oracleTypesConf),
  POSTGRESQL: getFieldTypes(pgTypesConf),
  SQLSERVER: getFieldTypes(sqlServerTypesConf),
  STARROCKS: getFieldTypes(starRocksTypesConf),
  TDSQLPOSTGRESQL: tdsqlPgFieldTypes,
  REDIS: redisFieldTypes,
  KUDU: kuduFieldTypes,
};
