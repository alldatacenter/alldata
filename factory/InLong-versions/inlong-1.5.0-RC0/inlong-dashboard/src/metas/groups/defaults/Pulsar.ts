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

import { DataWithBackend } from '@/metas/DataWithBackend';
import { RenderRow } from '@/metas/RenderRow';
import { RenderList } from '@/metas/RenderList';
import i18n from '@/i18n';
import { GroupInfo } from '../common/GroupInfo';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class PulsarGroup
  extends GroupInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'radio',
    initialValue: 'SERIAL',
    rules: [{ required: true }],
    props: {
      options: [
        {
          label: i18n.t('meta.Group.Pulsar.Parallel'),
          value: 'PARALLEL',
        },
        {
          label: i18n.t('meta.Group.Pulsar.Serial'),
          value: 'SERIAL',
        },
      ],
    },
  })
  @I18n('meta.Group.Pulsar.QueueModule')
  queueModule: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 3,
    rules: [{ required: true }],
    extra: i18n.t('meta.Group.Pulsar.PartitionExtra'),
    props: {
      min: 1,
      max: 100,
      precision: 0,
    },
    visible: values => values.queueModule === 'PARALLEL',
  })
  @I18n('meta.Group.Pulsar.PartitionNum')
  partitionNum: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 3,
    suffix: i18n.t('meta.Group.Pulsar.EnsembleSuffix'),
    extra: i18n.t('meta.Group.Pulsar.EnsembleExtra'),
    rules: [
      ({ getFieldValue }) => ({
        validator(_, val) {
          if (val) {
            const writeQuorum = getFieldValue(['writeQuorum']) || 0;
            const ackQuorum = getFieldValue(['ackQuorum']) || 0;
            return ackQuorum <= writeQuorum && writeQuorum <= val
              ? Promise.resolve()
              : Promise.reject(new Error('Max match: ensemble ≥ write quorum ≥ ack quorum'));
          }
          return Promise.resolve();
        },
      }),
    ],
    props: {
      min: 1,
      max: 10,
      precision: 0,
    },
  })
  @I18n('ensemble')
  ensemble: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 1,
    suffix: i18n.t('meta.Group.Pulsar.WriteQuorumSuffix'),
    extra: i18n.t('meta.Group.Pulsar.WriteQuorumExtra'),
    props: {
      min: 1,
      max: 10,
      precision: 0,
    },
  })
  @I18n('Write Quorum')
  writeQuorum: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 1,
    suffix: i18n.t('meta.Group.Pulsar.AckQuorumSuffix'),
    extra: i18n.t('meta.Group.Pulsar.AckQuorumExtra'),
    props: {
      min: 1,
      max: 10,
      precision: 0,
    },
  })
  @I18n('ACK Quorum')
  ackQuorum: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 24,
    rules: [
      ({ getFieldValue }) => ({
        validator(_, val) {
          if (val) {
            const unit = getFieldValue(['ttlUnit']);
            const value = unit === 'hours' ? Math.ceil(val / 24) : val;
            return value <= 14 ? Promise.resolve() : Promise.reject(new Error('Max: 14 Days'));
          }
          return Promise.resolve();
        },
      }),
    ],
    suffix: {
      type: 'select',
      name: 'ttlUnit',
      initialValue: 'hours',
      props: {
        options: [
          {
            label: 'D',
            value: 'days',
          },
          {
            label: 'H',
            value: 'hours',
          },
        ],
      },
    },
    extra: i18n.t('meta.Group.Pulsar.TtlExtra'),
    props: {
      min: 1,
      precision: 0,
    },
  })
  @I18n('Time To Live')
  ttl: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 72,
    rules: [
      ({ getFieldValue }) => ({
        validator(_, val) {
          const retentionSize = getFieldValue(['retentionSize']);
          if ((val === 0 && retentionSize > 0) || (val > 0 && retentionSize === 0)) {
            return Promise.reject(
              new Error(
                'Can not: retentionTime=0, retentionSize>0 | retentionTime>0, retentionSize=0',
              ),
            );
          }
          if (val) {
            const unit = getFieldValue(['retentionTimeUnit']);
            const value = unit === 'hours' ? Math.ceil(val / 24) : val;
            return value <= 14 ? Promise.resolve() : Promise.reject(new Error('Max: 14 Days'));
          }
          return Promise.resolve();
        },
      }),
    ],
    suffix: {
      type: 'select',
      name: 'retentionTimeUnit',
      initialValue: 'hours',
      props: {
        options: [
          {
            label: 'D',
            value: 'days',
          },
          {
            label: 'H',
            value: 'hours',
          },
        ],
      },
    },
    extra: i18n.t('meta.Group.Pulsar.RetentionTimeExtra'),
    props: {
      min: -1,
      precision: 0,
    },
  })
  @I18n('Retention Time')
  retentionTime: number;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: -1,
    suffix: {
      type: 'select',
      name: 'retentionSizeUnit',
      initialValue: 'MB',
      props: {
        options: [
          {
            label: 'MB',
            value: 'MB',
          },
          {
            label: 'GB',
            value: 'GB',
          },
          {
            label: 'TB',
            value: 'TB',
          },
        ],
      },
    },
    extra: i18n.t('meta.Group.Pulsar.RetentionSizeExtra'),
    props: {
      min: -1,
      precision: 0,
    },
  })
  @I18n('Retention Size')
  retentionSize: number;
}
