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

export default class TubeMqGroup
  extends GroupInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: true }],
    initialValue: 1,
    suffix: i18n.t('meta.Group.TubeMq.TenThousand/Day'),
    props: {
      min: 1,
      precision: 0,
    },
  })
  @I18n('meta.Group.TubeMq.NumberOfAccess')
  dailyRecords: number;

  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: true }],
    initialValue: 10,
    suffix: i18n.t('meta.Group.TubeMq.GB/Day'),
    props: {
      min: 1,
      precision: 0,
    },
  })
  @I18n('meta.Group.TubeMq.AccessSize')
  dailyStorage: number;

  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: true }],
    initialValue: 100,
    suffix: i18n.t('meta.Group.TubeMq.Stripe/Second'),
    props: {
      min: 1,
      precision: 0,
    },
  })
  @I18n('meta.Group.TubeMq.AccessPeakPerSecond')
  peakRecords: number;

  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: true }],
    initialValue: 1024,
    suffix: 'Byte',
    props: {
      min: 1,
      precision: 0,
    },
  })
  @I18n('meta.Group.TubeMq.SingleStripMaximumLength')
  maxLength: number;
}
