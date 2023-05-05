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

import { map } from 'lodash';
import i18n from 'i18n';
import { multipleDataHandler, groupHandler } from 'common/utils/chart-utils';

export const rdsAccountType = [
  { name: i18n.t('cmp:read and write'), value: 'ReadWrite' },
  { name: i18n.t('cmp:read only'), value: 'ReadOnly' },
  { name: i18n.t('cmp:DDL only'), value: 'DDLOnly' },
  { name: i18n.t('cmp:DML only'), value: 'DMLOnly' },
];

export const supportLBRegion = [
  `${i18n.t('cmp:east china')} 1`,
  `${i18n.t('cmp:north china')} 3`,
  `${i18n.t('cmp:north china')} 5`,
  `${i18n.t('cmp:north china')} 1`,
  `${i18n.t('cmp:north china')} 2`,
  `${i18n.t('cmp:east china')} 2`,
  `${i18n.t('cmp:Southern China')} 1`,
  `${i18n.t('cmp:Southeast Asia Pacific')} 1 (${i18n.t('cmp:singapore')})`,
  `${i18n.t('cmp:Southeast Asia Pacific')} 3 (${i18n.t('cmp:kuala lumpur')})`,
  `${i18n.t('cmp:Southeast Asia Pacific')} 5 (${i18n.t('cmp:jakarta')})`,
  `${i18n.t('cmp:south asia pacific')} 1 (${i18n.t('cmp:mumbai')})`,
  `${i18n.t('cmp:Western United States')} 1 (${i18n.t('cmp:silicon valley')})`,
  `${i18n.t('cmp:eastern united states')} 1 (${i18n.t('cmp:virginia')})`,
  `${i18n.t('cmp:hong kong')}`,
];

export const CHARGE_TYPE_i18n = {
  Prepaid: i18n.t('cmp:Subscription'),
  Postpaid: i18n.t('cmp:Pay-As-You-Go'),
};

export const lbConfig = [
  {
    key: 'slb.s1.small',
    maxConn: '5000',
    CPS: '3000',
    QPS: '1000',
  },
  {
    key: 'slb.s2.small',
    maxConn: '50,000',
    CPS: '5,000',
    QPS: '5,000',
  },
  {
    key: 'slb.s2.medium',
    maxConn: '100,000',
    CPS: '10,000',
    QPS: '10,000',
  },
  {
    key: 'slb.s3.small',
    maxConn: '200,000',
    CPS: '20,000',
    QPS: '20,000',
  },
  {
    key: 'slb.s3.medium',
    maxConn: '500,000',
    CPS: '50,000',
    QPS: '30,000',
  },
  {
    key: 'slb.s3.large',
    maxConn: '1,000,000',
    CPS: '100,000',
    QPS: '50,000',
  },
];

const zhucong = i18n.t('cmp:master-slave');
const gaopei = i18n.t('cmp:high-configured');
const danji = i18n.t('cmp:single machine');

export const redisConfig = {
  double: [
    {
      text: `256 MB ${zhucong}`,
      key: 'redis.master.micro.default',
      maxConn: '10000',
      maxThrp: '10 MB',
    },
    {
      text: `1 GB ${zhucong}`,
      key: 'redis.master.small.default',
      maxConn: '10000',
      maxThrp: '10 MB',
    },
    {
      text: `2 GB ${zhucong}`,
      key: 'redis.master.mid.default',
      maxConn: '10000',
      maxThrp: '16 MB',
    },
    {
      text: `4 GB ${zhucong}`,
      key: 'redis.master.stand.default',
      maxConn: '10000',
      maxThrp: '24 MB',
    },
    {
      text: `8 GB ${zhucong}`,
      key: 'redis.master.large.default',
      maxConn: '10000',
      maxThrp: '24 MB',
    },
    {
      text: `16 GB ${zhucong}`,
      key: 'redis.master.2xlarge.default',
      maxConn: '10000',
      maxThrp: '32 MB',
    },
    {
      text: `32 GB ${zhucong}`,
      key: 'redis.master.4xlarge.default',
      maxConn: '10000',
      maxThrp: '32 MB',
    },
    {
      text: `1 GB ${zhucong}${gaopei}`,
      key: 'redis.master.small.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `2 GB ${zhucong}${gaopei}`,
      key: 'redis.master.mid.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `4 GB ${zhucong}${gaopei}`,
      key: 'redis.master.stand.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `8 GB ${zhucong}${gaopei}`,
      key: 'redis.master.large.special1x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `16 GB ${zhucong}${gaopei}`,
      key: 'redis.master.2xlarge.special1x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `32 GB ${zhucong}${gaopei}`,
      key: 'redis.master.4xlarge.special1x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
  ],
  single: [
    {
      text: `1 GB ${danji}`,
      key: 'redis.basic.small.default',
      maxConn: '10000',
      maxThrp: '10 MB',
    },
    {
      text: `2 GB ${danji}`,
      key: 'redis.basic.mid.default',
      maxConn: '10000',
      maxThrp: '16 MB',
    },
    {
      text: `4 GB ${danji}`,
      key: 'redis.basic.stand.default',
      maxConn: '10000',
      maxThrp: '24 MB',
    },
    {
      text: `8 GB ${danji}`,
      key: 'redis.basic.large.default',
      maxConn: '10000',
      maxThrp: '24 MB',
    },
    {
      text: `16 GB ${danji}`,
      key: 'redis.basic.2xlarge.default',
      maxConn: '10000',
      maxThrp: '32 MB',
    },
    {
      text: `32 GB ${danji}`,
      key: 'redis.basic.4xlarge.default',
      maxConn: '10000',
      maxThrp: '32 MB',
    },
    {
      text: `1 GB ${danji}${gaopei}`,
      key: 'redis.basic.small.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `2 GB ${danji}${gaopei}`,
      key: 'redis.basic.mid.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `4 GB ${danji}${gaopei}`,
      key: 'redis.basic.stand.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `8 GB ${danji}${gaopei}`,
      key: 'redis.basic.large.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `16 GB ${danji}${gaopei}`,
      key: 'redis.basic.2xlarge.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
    {
      text: `32 GB ${danji}${gaopei}`,
      key: 'redis.basic.4xlarge.special2x',
      maxConn: '20000',
      maxThrp: '48 MB',
    },
  ],
};

export const characterSetLists = ['utf8', 'gbk', 'latin1', 'utf8mb4'];

export const rdsForbiddenWords = [
  'admin',
  'aurora',
  'replicator',
  'xtrabak',
  'root',
  'mysql',
  'test',
  'eagleye',
  'information_schema',
  'guest',
  'add',
  'analyze',
  'asc',
  'between',
  'blob',
  'call',
  'change',
  'check',
  'condition',
  'continue',
  'cross',
  'current_timestamp',
  'database',
  'day_microsecond',
  'dec',
  'default',
  'desc',
  'distinct',
  'double',
  'each',
  'enclosed',
  'exit',
  'fetch',
  'float8',
  'foreign',
  'goto',
  'having',
  'hour_minute',
  'ignore',
  'infile',
  'insensitive',
  'int1',
  'int4',
  'interval',
  'iterate',
  'keys',
  'leading',
  'like',
  'lines',
  'localtimestamp',
  'longblob',
  'low_priority',
  'mediumint',
  'minute_microsecond',
  'modifies',
  'no_write_to_binlog',
  'on',
  'optionally',
  'out',
  'precision',
  'purge',
  'read',
  'references',
  'rename',
  'require',
  'revoke',
  'schema',
  'select',
  'set',
  'spatial',
  'sqlexception',
  'sql_big_result',
  'ssl',
  'table',
  'tinyblob',
  'to',
  'true',
  'unique',
  'update',
  'using',
  'utc_timestamp',
  'varchar',
  'when',
  'with',
  'xor',
  'all',
  'and',
  'asensitive',
  'bigint',
  'both',
  'cascade',
  'char',
  'collate',
  'connection',
  'convert',
  'current_date',
  'current_user',
  'databases',
  'day_minute',
  'decimal',
  'delayed',
  'describe',
  'distinctrow',
  'drop',
  'else',
  'escaped',
  'explain',
  'float',
  'for',
  'from',
  'grant',
  'high_priority',
  'hour_second',
  'in',
  'inner',
  'insert',
  'int2',
  'int8',
  'into',
  'join',
  'kill',
  'leave',
  'limit',
  'load',
  'lock',
  'longtext',
  'match',
  'mediumtext',
  'minute_second',
  'natural',
  'null',
  'optimize',
  'or',
  'outer',
  'primary',
  'raid0',
  'reads',
  'regexp',
  'repeat',
  'restrict',
  'right',
  'schemas',
  'sensitive',
  'show',
  'specific',
  'sqlstate',
  'sql_calc_found_rows',
  'starting',
  'terminated',
  'tinyint',
  'trailing',
  'undo',
  'unlock',
  'usage',
  'utc_date',
  'values',
  'varcharacter',
  'where',
  'write',
  'year_month',
  'alter',
  'as',
  'before',
  'binary',
  'by',
  'case',
  'character',
  'column',
  'constraint',
  'create',
  'current_time',
  'cursor',
  'day_hour',
  'day_second',
  'declare',
  'delete',
  'deterministic',
  'div',
  'dual',
  'elseif',
  'exists',
  'false',
  'float4',
  'force',
  'fulltext',
  'group',
  'hour_microsecond',
  'if',
  'index',
  'inout',
  'int',
  'int3',
  'integer',
  'is',
  'key',
  'label',
  'left',
  'linear',
  'localtime',
  'long',
  'loop',
  'mediumblob',
  'middleint',
  'mod',
  'not',
  'numeric',
  'option',
  'order',
  'outfile',
  'procedure',
  'range',
  'real',
  'release',
  'replace',
  'return',
  'rlike',
  'second_microsecond',
  'separator',
  'smallint',
  'sql',
  'sqlwarning',
  'sql_small_result',
  'straight_join',
  'then',
  'tinytext',
  'trigger',
  'union',
  'unsigned',
  'use',
  'utc_time',
  'varbinary',
  'varying',
  'while',
  'x509',
  'zerofill',
  'galaxy',
];

const core = i18n.t('cmp:cores');
export const rdsConfig = [
  {
    key: 'rds.mysql.t1.small',
    cpu: `1${core}`,
    mem: '1GB',
    maxConn: '300',
    maxIOPS: '600',
  },
  {
    key: 'rds.mysql.s1.small',
    cpu: `1${core}`,
    mem: '2GB',
    maxConn: '600',
    maxIOPS: '1000',
  },
  {
    key: 'rds.mysql.s2.large',
    cpu: `2${core}`,
    mem: '4GB',
    maxConn: '1200',
    maxIOPS: '2000',
  },
  {
    key: 'rds.mysql.s2.xlarge',
    cpu: `2${core}`,
    mem: '8GB',
    maxConn: '2000',
    maxIOPS: '4000',
  },
  {
    key: 'rds.mysql.s3.large',
    cpu: `4${core}`,
    mem: '8GB',
    maxConn: '2000',
    maxIOPS: '5000',
  },
  {
    key: 'rds.mysql.m1.medium',
    cpu: `4${core}`,
    mem: '16GB',
    maxConn: '4000',
    maxIOPS: '7000',
  },
  {
    key: 'rds.mysql.c1.large',
    cpu: `8${core}`,
    mem: '16GB',
    maxConn: '4000',
    maxIOPS: '8000',
  },
  {
    key: 'rds.mysql.c1.xlarge',
    cpu: `8${core}`,
    mem: '32GB',
    maxConn: '8000',
    maxIOPS: '12000',
  },
];

export const fieldsTranslationMap = {
  resourceType: i18n.t('cmp:resource type'),
  clusterName: i18n.t('cluster name'),
  // vpcCidr: '专有网络cidr',
  accessKeyId: 'AccessKeyId',
  accessKeySecret: 'AccessKeySecret',
  PrePaid: i18n.t('cmp:Subscription'),
  PostPaid: i18n.t('cmp:Pay-As-You-Go'),
  regionId: i18n.t('region'),
  zoneId: i18n.t('cmp:availability zone'),
  periodUnit: i18n.t('time unit'),
  period: i18n.t('cmp:duration period'),
  Week: i18n.t('week'),
  Month: i18n.t('month'),
  password: i18n.t('password'),
  port: i18n.t('cmp:port'),
  type: i18n.t('type'),
  username: i18n.t('username'),
  tag: i18n.t('label'),
  connectionstring: i18n.t('cmp:connection address'),

  ecsSettings: i18n.t('cmp:ecs configuration'),
  nodeType: i18n.t('cmp:node type'),
  amount: i18n.t('cmp:purchased instances'),
  instanceType: i18n.t('cmp:instance specification'),
  instanceChargeType: i18n.t('cmp:billing method'),
  systemDiskSize: `${i18n.t('cmp:system disk')}(${i18n.t('cmp:cloud')}SSD)${i18n.t('cmp:capacity')}(GiB)`,

  nasSetting: i18n.t('cmp:file storage configuration'),
  nasStorageType: i18n.t('storage type'),
  Capacity: i18n.t('cmp:performance type'),
  Performance: i18n.t('capacity type'),

  rdsSettings: i18n.t('cmp:cloud database configuration'),
  dbInstanceClass: i18n.t('cmp:specifications'),
  payType: i18n.t('cmp:billing method'),
  Postpaid: i18n.t('cmp:Pay-As-You-Go'),
  Prepaid: i18n.t('cmp:Subscription'),
  engineVersion: i18n.t('version'),
  dbInstanceStorage: i18n.t('storage disk capacity'),
  accountName: i18n.t('account name'),
  dbName: i18n.t('cmp:database name'),
  Password: i18n.t('password'),
  character_set_server: i18n.t('cmp:database encoding'),

  redisSettings: `${i18n.t('cmp:cloud')}redis${i18n.t('cmp:configuration')}`,
  instanceClass: i18n.t('cmp:specifications'),
  chargeType: i18n.t('cmp:billing method'),

  loadBalancerSetting: i18n.t('cmp:load balancing configuration'),
  loadBalancerSpec: i18n.t('cmp:specifications'),
  loadBalancePayType: i18n.t('cmp:billing method'),
  PayOnDemand: i18n.t('cmp:pay as you go'),
  loadBalancerInternetChargeType: i18n.t('by flow'),
  paybybandwidth: i18n.t('cmp:network billing method'),
  paybytraffic: i18n.t('by fixed bandwidth'),
};

const lowerCase = {};
map(fieldsTranslationMap, (v, k) => {
  lowerCase[k.toLowerCase()] = v;
});

export const translate = (source, target) => {
  map(source, (value, key) => {
    // if (Array.isArray(value) && typeof value[0] === 'object') {
    //   return value.map((v, i) => translate(v, target[i]));
    // }
    let k = String(key).toLowerCase();
    if (lowerCase[k]) {
      k = lowerCase[k];
      delete target[key];
    }
    const v = lowerCase[value] || value;
    target[k] = v;
    if (typeof value === 'object') {
      translate(value, target[k]);
    }
  });
};

export const preOrPostPaid = {
  type: 'radioGroup',
  options: [
    {
      value: 'PostPaid',
      name: i18n.t('cmp:Pay-As-You-Go'),
    },
    {
      value: 'PrePaid',
      name: i18n.t('cmp:Subscription'),
    },
  ],
  initialValue: 'PostPaid',
};

export const checkForbiddenWord = (rule, value, callback) => {
  if (rdsForbiddenWords.includes(value)) {
    callback(`${i18n.t('cmp:cannot contain restricted keywords')}： ${value}`);
  }
  callback();
};

export const checkRdsAccountName = (rule, value, callback) => {
  if (rdsForbiddenWords.includes(value)) {
    callback(`${i18n.t('cmp:cannot contain restricted keywords')}： ${value}`);
  }
  if (!/^[a-z][a-z0-9_]*$/.test(value)) {
    callback(
      i18n.t(
        'cmp:Composed of lowercase letters, numbers, underscores and hyphens, started with letter and ended with letter or number, 2~64 characters',
      ),
    );
  }
  callback();
};

export const checkPassword = (_rule, value, callback) => {
  if (value === '') {
    return callback();
  }
  if (!/^.{8,32}$/.test(value)) {
    return callback(i18n.t('{min} ~ {max} digits', { min: 8, max: 32 }));
  }
  if (/[!<>()[\]{},`~.\-_@#$%^&*]/.test(value)) {
    return callback(i18n.t('cannot contain special characters'));
  }
  if (!/[a-z]/.test(value)) {
    return callback(i18n.t('lowercase letter'));
  }
  if (!/[A-Z]/.test(value)) {
    return callback(i18n.t('uppercase letter'));
  }
  if (!/\d+/.test(value)) {
    return callback(i18n.t('number'));
  }
  callback();
};

export const CLUSTER_INFOS = [
  {
    key: 'cluster',
    name: i18n.t('cmp:number of clusters'),
  },
];

export const CLUSTER_RESOURCES_PROPORTION_MAP = {
  host: {
    yAxisName: i18n.t('cmp:assignment'),
    totalValue: 'totalHosts',
    totalName: i18n.t('cmp:total number of hosts'),
    valueName: i18n.t('cmp:number of assigned hosts'),
    value: 'hostsNum',
    // schedulableName: '可调度',
    // schedulableKey: 'schedulerHost',
    // schedulableUnit: '台',
    unit: '台',
  },
  cpu: {
    yAxisName: i18n.t('cmp:assignment'),
    totalValue: 'totalCpu',
    totalName: `${i18n.t('total')} CPU`,
    valueName: `${i18n.t('cmp:assignment')} CPU`,
    value: 'usedCpu',
    schedulableName: i18n.t('cmp:schedulable'),
    schedulableKey: 'schedulerCPU',
    schedulableUnit: '%',
    unit: i18n.t('core'),
  },
  mem: {
    yAxisName: i18n.t('cmp:assignment'),
    totalValue: 'totalMemory',
    totalName: i18n.t('cmp:total memory'),
    valueName: i18n.t('cmp:allocate memory'),
    value: 'usedMemory',
    schedulableName: i18n.t('cmp:schedulable'),
    schedulableKey: 'schedulerMemory',
    schedulableUnit: '%',
    unit: 'G',
  },
};

// 着色分组
export const COLOUR_MAP = {
  load: i18n.t('cmp:system load'),
  cpu: i18n.t('cmp:CPU usage'),
  mem: i18n.t('cmp:Memory usage'),
  disk: i18n.t('cmp:Disk usage'),
  scheduledCPU: i18n.t('CPU allocation'),
  scheduledMEM: i18n.t('cmp:Memory allocation'),
};

export const ALARM_REPORT_CHART_MAP = {
  load: {
    cnName: i18n.t('cmp:system load'),
    chartType: 'system',
    extraQuery: { avg: ['load1', 'load5', 'load15'] },
    handler: multipleDataHandler(['avg.load1', 'avg.load5', 'avg.load15']),
  },
  cpu: {
    cnName: i18n.t('cmp:CPU usage rate'),
    chartType: 'cpu',
    extraQuery: { avg: 'cpu_usage_active' },
    handler: multipleDataHandler(['avg.cpu_usage_active']),
  },
  diskio: {
    cnName: 'Disk IO Util',
    chartType: 'disk',
    extraQuery: { max: 'pct_util' },
    handler: multipleDataHandler(['max.pct_util']),
  },
  disk: {
    cnName: i18n.t('cmp:Disk usage rate'),
    chartType: 'disk',
    extraQuery: { max: 'used_percent', group: 'device' },
    handler: groupHandler('max.used_percent'),
  },
  crash: {
    cnName: i18n.t('cmp:system load'),
    chartType: 'system',
    extraQuery: { avg: ['load1', 'load5', 'load15'] },
    handler: multipleDataHandler(['avg.load1', 'avg.load5', 'avg.load15']),
  },
};

export const customTagColor = {
  // any: 'green',
  // locked: 'red',
  // pack: 'cyan',
  // job: 'blue',
  // 'service-stateless': 'purple',
};
