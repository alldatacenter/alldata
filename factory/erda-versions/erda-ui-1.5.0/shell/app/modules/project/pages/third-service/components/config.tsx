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

import { FormInstance } from 'core/common/interface';
import { insertWhen } from 'common/utils/index';
import { Select } from 'antd';
import i18n from 'i18n';
import { produce } from 'immer';
import { getOptions, groupOptions } from 'app/modules/cmp/pages/cluster-manage/config';
import React from 'react';
import { useUpdate } from 'common/use-hooks';

const { Option } = Select;

export const AddonType = {
  AliCloudRds: 'alicloud-rds',
  AliCloudRedis: 'alicloud-redis',
  AliCloudOns: 'alicloud-ons',
  AliCloudOss: 'alicloud-oss',
  APIGateway: 'api-gateway',
};

export const useFields = (_fields: any[]) => {
  const [fields, setFields] = React.useState(_fields);

  const updateFields = (cb: any) => {
    const newFields = produce(fields, cb);
    setFields(newFields);
  };

  const removeFields = (fieldNames: string[]) => {
    setFields((prev) => prev.filter((f) => fieldNames.includes(f.name)));
  };

  return [fields, { updateFields, removeFields }];
};

export const MysqlFieldsConfig = {
  basicTypes: [
    {
      name: i18n.t('resource:1Core 2GB'),
      value: 'mysql.n2.small.1',
      specName: `${i18n.t('resource:Basic')} ${i18n.t('resource:1Core 2GB')}`,
    },
    {
      name: i18n.t('resource:4Core 8GB'),
      value: 'mysql.n2.large.1',
      specName: `${i18n.t('resource:Basic')} ${i18n.t('resource:4Core 8GB')}`,
    },
    {
      name: i18n.t('resource:8Core 32GB'),
      value: 'mysql.n4.xlarge.1',
      specName: `${i18n.t('resource:Basic')} ${i18n.t('resource:8Core 32GB')}`,
    },
  ],
  highTypes: [
    {
      name: i18n.t('resource:1Core 2GB'),
      value: 'rds.mysql.s1.small',
      specName: `${i18n.t('resource:High-availability')} ${i18n.t('resource:1Core 2GB')}`,
    },
    {
      name: i18n.t('resource:4Core 8GB'),
      value: 'rds.mysql.s3.large',
      specName: `${i18n.t('resource:High-availability')} ${i18n.t('resource:4Core 8GB')}`,
    },
    {
      name: i18n.t('resource:8Core 32GB'),
      value: 'rds.mysql.c1.xlarge',
      specName: `${i18n.t('resource:High-availability')} ${i18n.t('resource:8Core 32GB')}`,
    },
  ],
  getFields: (
    form: FormInstance,
    { chargeType, onChangeChargeType }: { chargeType: string; onChangeChargeType: (val: string) => void },
  ) => {
    const basicSpecs = MysqlFieldsConfig.basicTypes.map((a) => a.value);
    const highSpecs = MysqlFieldsConfig.highTypes.map((a) => a.value);
    return [
      {
        label: i18n.t('instance name'),
        name: 'instanceName',
        rules: [
          {
            pattern: /^[a-zA-Z\u4e00-\u9fa5][a-zA-Z\u4e00-\u9fa50-9-_]{1,254}$/,
            message: i18n.t(
              'cmp:2-255 characters, starts with English or Chinese characters, which can contain numbers, underscores and hyphens.',
            ),
          },
        ],
      },
      {
        label: i18n.t('resource:specifications'),
        name: 'spec',
        type: 'select',
        options: () =>
          groupOptions(
            [
              {
                name: i18n.t('resource:Basic'),
                children: MysqlFieldsConfig.basicTypes,
              },
              {
                name: i18n.t('resource:High-availability'),
                children: MysqlFieldsConfig.highTypes,
              },
            ],
            (item) => {
              const { name, value } = item;
              return (
                <Option key={value} value={value}>
                  {name}
                </Option>
              );
            },
          ),
        itemProps: {
          onChange: (v: string) => {
            if (basicSpecs.includes(v)) {
              form.setFieldsValue({ specType: 'Basic' });
            } else if (highSpecs.includes(v)) {
              form.setFieldsValue({ specType: 'High-availability' });
            }
          },
        },
      },
      {
        name: 'specType',
        initialValue: 'Basic',
        itemProps: {
          type: 'hidden',
        },
      },
      {
        label: `${i18n.t('resource:storage space')}(G)`,
        name: 'storageSize',
        type: 'inputNumber',
        initialValue: 200,
        itemProps: {
          min: 20,
          max: 600,
        },
      },
      {
        label: i18n.t('resource:charge type'),
        name: 'chargeType',
        type: 'radioGroup',
        options: getOptions('chargeType'),
        initialValue: chargeType,
        itemProps: {
          onChange: (e: any) => {
            onChangeChargeType(e.target.value);
          },
        },
      },
      ...insertWhen(chargeType === 'PrePaid', [
        {
          label: i18n.t('resource:purchased time'),
          name: 'chargePeriod',
          type: 'select',
          options: getOptions('chargePeriod'),
          itemProps: {
            className: 'w-full',
          },
        },
        {
          label: i18n.t('resource:whether to renew automatically'),
          name: 'autoRenew',
          type: 'switch',
          initialValue: true,
        },
      ]),
    ];
  },
};

export const useMysqlFields = (form: FormInstance) => {
  const [{ chargeType }, updater] = useUpdate({
    chargeType: 'PostPaid',
  });

  return MysqlFieldsConfig.getFields(form, {
    chargeType,
    onChangeChargeType: (val) => {
      updater.chargeType(val);
    },
  });
};

export const useOnsFields = () => {
  return [
    {
      label: i18n.t('resource:instance name'),
      name: 'name',
      itemProps: {
        maxLength: 64,
      },
      rules: [
        {
          pattern: /^[a-z\u4e00-\u9fa5A-Z0-9_-]{3,64}$/,
          message: `${i18n.t('length is {min}~{max}', { min: 3, max: 64 })},${i18n.t(
            'could include Chinese, English, numbers, hyphens and underscores',
          )}`,
        },
      ],
    },
    {
      label: i18n.t('description'),
      name: 'remark',
      required: false,
    },
  ];
};

export const RedisFieldConfig = {
  spec: {
    standard: [
      { name: '1G', value: 'redis.master.small.default', specName: `${i18n.t('resource:master-slave edition')} 1G` },
      { name: '4G', value: 'redis.master.stand.default', specName: `${i18n.t('resource:master-slave edition')} 4G` },
      { name: '8G', value: 'redis.master.large.default', specName: `${i18n.t('resource:master-slave edition')} 8G` },
    ],
    cluster: [
      {
        name: '4G',
        value: 'redis.logic.sharding.2g.2db.0rodb.4proxy.default',
        specName: `${i18n.t('resource:cluster edition')} 4G`,
      },
      {
        name: '8G',
        value: 'redis.logic.sharding.4g.2db.0rodb.4proxy.default',
        specName: `${i18n.t('resource:cluster edition')} 8G`,
      },
      {
        name: '16G',
        value: 'redis.logic.sharding.8g.2db.0rodb.4proxy.default',
        specName: `${i18n.t('resource:cluster edition')} 16G`,
      },
    ],
  },
  getFields: ({
    chargeType,
    onChangeChargeType,
  }: {
    chargeType: string;
    onChangeChargeType: (val: string) => void;
  }) => {
    return [
      {
        label: i18n.t('resource:version'),
        name: 'version',
        type: 'select',
        options: [
          { name: '2.8', value: '2.8' },
          { name: '4.0', value: '4.0' },
          { name: '5.0', value: '5.0' },
        ],
      },
      {
        label: i18n.t('resource:specifications'),
        name: 'spec',
        type: 'select',
        options: () =>
          groupOptions(
            [
              {
                name: i18n.t('resource:master-slave edition'), // General-purpose
                children: RedisFieldConfig.spec.standard,
              },
              {
                name: i18n.t('resource:cluster edition'),
                children: RedisFieldConfig.spec.cluster,
              },
            ],
            (item) => {
              const { name, value } = item;
              return (
                <Option key={value} value={value}>
                  {name}
                </Option>
              );
            },
          ),
      },
      {
        label: i18n.t('resource:instance name'),
        name: 'instanceName',
        itemProps: {
          maxLength: 64,
        },
        rules: [
          {
            pattern: /^[a-zA-Z\u4e00-\u9fa5](?!.*[@/=\s":<>{}[\]]).{1,127}$/,
            message: `${i18n.t('length is {min}~{max}', { min: 2, max: 128 })},${i18n.t('cmp:redis-name-tip')}`,
          },
        ],
      },
      {
        label: i18n.t('resource:charge type'),
        name: 'chargeType',
        type: 'radioGroup',
        options: getOptions('chargeType'),
        initialValue: 'PostPaid',
        itemProps: {
          onChange: (e: any) => {
            onChangeChargeType(e.target.value);
          },
        },
      },
      ...insertWhen(chargeType === 'PrePaid', [
        {
          label: i18n.t('resource:purchased time'),
          name: 'chargePeriod',
          type: 'select',
          options: [
            { name: i18n.t('{num} month(s)', { num: 1 }), value: 1 },
            { name: i18n.t('{num} month(s)', { num: 2 }), value: 2 },
            { name: i18n.t('{num} month(s)', { num: 3 }), value: 3 },
            { name: i18n.t('{num} month(s)', { num: 4 }), value: 4 },
            { name: i18n.t('{num} month(s)', { num: 5 }), value: 5 },
            { name: i18n.t('{num} month(s)', { num: 6 }), value: 6 },
            { name: i18n.t('{num} month(s)', { num: 7 }), value: 7 },
            { name: i18n.t('{num} month(s)', { num: 8 }), value: 8 },
            { name: i18n.t('{num} month(s)', { num: 9 }), value: 9 },
            { name: i18n.t('{num} year(s)', { num: 1 }), value: 12 },
            { name: i18n.t('{num} year(s)', { num: 2 }), value: 24 },
            { name: i18n.t('{num} year(s)', { num: 3 }), value: 36 },
          ],
        },
        {
          label: i18n.t('resource:whether to renew automatically'),
          name: 'autoRenew',
          type: 'switch',
          initialValue: true,
        },
      ]),
    ];
  },
};

export const useRedisFields = () => {
  const [{ chargeType }, updater] = useUpdate({
    chargeType: 'PostPaid',
  });
  return RedisFieldConfig.getFields({
    chargeType,
    onChangeChargeType: (val) => updater.chargeType(val),
  });
};

export const useDBFields = () => [
  {
    label: i18n.t('database'),
    name: ['databases.0', 'dbName'],
    itemProps: {
      maxLength: 64,
    },
    rules: [
      {
        pattern: /^[a-zA-Z0-9_-]{2,64}$/,
        message: `${i18n.t('length is {min}~{max}', { min: 2, max: 64 })},${i18n.t(
          'dop:can only contain characters, numbers, underscores and hyphens',
        )}`,
      },
    ],
  },
];

export const useTopicFields = () => [
  {
    label: i18n.t('resource:Topic'),
    name: ['topics', 0, 'topicName'],
    itemProps: {
      maxLength: 64,
    },
    rules: [
      {
        pattern: /^[a-zA-Z0-9_-]{5,64}$/,
        message: `${i18n.t('length is {min}~{max}', { min: 5, max: 64 })},${i18n.t(
          'dop:can only contain characters, numbers, underscores and hyphens',
        )}`,
      },
      {
        validator: (_rule: any, value: string, callback: Function) => {
          const prefixWrong = value.startsWith('CID') || value.startsWith('GID');
          if (prefixWrong) {
            callback(i18n.t('resource:Topic name cannot start with `CID` or `GID`'));
          } else {
            callback();
          }
        },
      },
    ],
  },
  {
    label: i18n.t('resource:message type'),
    name: ['topics', 0, 'messageType'],
    type: 'select',
    options: [
      { name: i18n.t('resource:general message'), value: 0 }, // Ordinary message
      { name: i18n.t('resource:partition order message'), value: 1 }, // Partition order message
      { name: i18n.t('resource:global order message'), value: 2 }, // Global order message
      { name: i18n.t('resource:transaction message'), value: 4 }, // Transaction message
      { name: i18n.t('resource:timing/delay message'), value: 5 }, // Timing/delay message
    ],
  },
  {
    label: 'producerID',
    name: ['topics', 0, 'groupID'],
    itemProps: {
      maxLength: 64,
      placeholder: `${i18n.t('start with {name}', { name: 'GID-、GID_' })},${i18n.t('length is {min}~{max}', {
        min: 5,
        max: 64,
      })},${i18n.t('dop:can only contain characters, numbers, underscores and hyphens')}`,
    },
    rules: [
      {
        pattern: /^GID[-|_][a-zA-Z0-9_-]{1,60}$/,
        message: `${i18n.t('start with {name}', { name: 'GID-、GID_' })},${i18n.t('length is {min}~{max}', {
          min: 5,
          max: 64,
        })},${i18n.t('dop:can only contain characters, numbers, underscores and hyphens')}`,
      },
    ],
  },
];

export const useBucketField = () => {
  return [
    {
      label: i18n.t('resource:Bucket name'),
      name: ['buckets', 0, 'name'],
      itemProps: {
        maxLength: 64,
      },
      rules: [
        {
          pattern: /^[a-zA-Z0-9_-]{3,64}$/,
          message: `${i18n.t('length is {min}~{max}', { min: 3, max: 64 })},${i18n.t(
            'dop:can only contain characters, numbers, underscores and hyphens',
          )}`,
        },
      ],
    },
    {
      label: i18n.t('resource:read and write permissions'),
      name: ['buckets', 0, 'acl'],
      type: 'select',
      options: [
        { name: i18n.t('private'), value: 'private' },
        { name: i18n.t('resource:public reading'), value: 'public-read' },
        { name: i18n.t('resource:public reading and writing'), value: 'public-read-write' },
      ],
    },
  ];
};

export const ChargeType = (chargeTypeName: string, chargePeriod: string, autoRenew: string, required = true) => {
  const [chargeType, setChargeType] = React.useState('PostPaid');
  return [
    {
      label: i18n.t('resource:charge type'),
      name: chargeTypeName,
      required,
      type: 'radioGroup',
      options: getOptions('chargeType'),
      initialValue: chargeType,
      itemProps: {
        onChange: (e: any) => {
          setChargeType(e.target.value);
        },
      },
    },
    ...insertWhen(chargeType === 'PrePaid', [
      {
        label: i18n.t('resource:purchased time'),
        name: chargePeriod,
        required,
        type: 'select',
        options: getOptions('chargePeriod'),
        itemProps: {
          className: 'w-full',
        },
      },
      {
        label: i18n.t('resource:whether to renew automatically'),
        name: autoRenew,
        required,
        type: 'switch',
        initialValue: true,
      },
    ]),
  ];
};

export const SlbFields = (data: CUSTOM_ADDON.GatewayInstance[], form: FormInstance) => {
  const [source, setSource] = React.useState('');
  const newInstance = ChargeType('slb.chargeType', 'slb.chargePeriod', 'slb.autoRenew');
  return [
    {
      getComp: () => {
        return <p className="secondary-title">{i18n.t('resource:private network SLB')}</p>;
      },
    },
    {
      label: i18n.t('instance source'),
      name: ['slb', 'instanceID'],
      type: 'select',
      options: data.map(({ instanceID, name }) => ({ name, value: instanceID })),
      itemProps: {
        onChange: (v: string) => {
          setSource(v);
          let name;
          if (v !== '-1') {
            name = (data.find((t) => t.instanceID === v) || {}).name;
          }
          form.setFieldsValue({ 'slb.name': name });
        },
      },
    },
    ...(source === '-1'
      ? [
          {
            label: i18n.t('instance name'),
            name: ['slb', 'name'],
            itemProps: {
              maxLength: 30,
            },
            rules: [
              {
                pattern: /^[a-zA-Z\u4e00-\u9fa5][a-zA-Z\u4e00-\u9fa50-9_]{3,39}$/,
                message: i18n.t(
                  'resource:4-40 characters, start with English letters or Chinese characters, and can contain numbers and underscores',
                ),
              },
            ],
          },
          {
            label: i18n.t('resource:specifications'),
            name: ['slb', 'spec'],
            type: 'select',
            options: [
              { name: i18n.t('resource:performance shared instance'), value: '-1' },
              { name: `${i18n.t('resource:simple')}I(slb.s1.small)`, value: 'slb.s1.small' },
              { name: `${i18n.t('resource:standard')}I(slb.s2.small)`, value: 'slb.s2.small' },
              { name: `${i18n.t('resource:standard')}II(slb.s2.medium)`, value: 'slb.s2.medium' },
              { name: `${i18n.t('resource:high level')}Ⅰ(slb.s3.small)`, value: 'slb.s3.small' },
              { name: `${i18n.t('resource:high level')}II(slb.s3.medium)`, value: 'slb.s3.medium' },
              { name: `${i18n.t('resource:super advanced')}Ⅰ(slb.s3.large)`, value: 'slb.s3.large' },
            ],
          },
          ...newInstance,
        ]
      : [
          {
            name: ['slb', 'name'],
            itemProps: {
              type: 'hidden',
            },
          },
        ]),
  ];
};

export const ApiFields = (data: CUSTOM_ADDON.GatewayInstance[], form: FormInstance) => {
  const [source, setSource] = React.useState('');
  const newInstance = ChargeType('chargeType', 'chargePeriod', 'autoRenew');
  return [
    {
      getComp: () => {
        return <p className="secondary-title">{i18n.t('resource:API gateway')}</p>;
      },
    },
    {
      label: i18n.t('instance source'),
      name: 'instanceID',
      type: 'select',
      options: data.map(({ instanceID, name }) => ({ name, value: instanceID })),
      itemProps: {
        onChange: (v: string) => {
          setSource(v);
          let name;
          if (v !== '-1') {
            name = (data.find((t) => t.instanceID === v) || {}).name;
          }
          form.setFieldsValue({ name });
        },
      },
    },
    ...(source === '-1'
      ? [
          {
            label: i18n.t('instance name'),
            name: 'name',
            itemProps: {
              maxLength: 30,
            },
            rules: [
              {
                pattern: /^[a-zA-Z\u4e00-\u9fa5][a-zA-Z\u4e00-\u9fa50-9_]{3,39}$/,
                message: i18n.t(
                  'resource:4-40 characters, start with English letters or Chinese characters, and can contain numbers and underscores',
                ),
              },
            ],
          },
          {
            label: i18n.t('resource:specifications'),
            name: 'spec',
            type: 'select',
            options: [
              { name: 'api.s1.small', value: 'api.s1.small', maxQPS: 2500, MaxConnectCount: 50000, SLA: '99.95%' },
              { name: 'api.s1.medium', value: 'api.s1.medium', maxQPS: 5000, MaxConnectCount: 100000, SLA: '99.95%' },
              { name: 'api.s2.large', value: 'api.s2.large', maxQPS: 10000, MaxConnectCount: 200000, SLA: '99.95%' },
            ],
          },
          {
            label: i18n.t('msp:security strategy'),
            type: 'select',
            name: 'httpsPolicy',
            options: [
              { name: 'HTTPS1_1_TLS1_0', value: 'HTTPS1_1_TLS1_0' },
              { name: 'HTTPS2_TLS1_0', value: 'HTTPS2_TLS1_0' },
              { name: 'HTTPS2_TLS1_2', value: 'HTTPS2_TLS1_2' },
            ],
          },
          ...newInstance,
          {
            name: 'source',
            initialValue: 'addon',
            itemProps: {
              type: 'hidden',
            },
          },
        ]
      : [
          {
            name: 'name',
            itemProps: {
              type: 'hidden',
            },
          },
        ]),
  ];
};

export const useGatewayFields = (
  slb: CUSTOM_ADDON.GatewayInstance[],
  gateway: CUSTOM_ADDON.GatewayInstance[],
  form: FormInstance,
) => {
  return [...SlbFields(slb, form), ...ApiFields(gateway, form)];
};
