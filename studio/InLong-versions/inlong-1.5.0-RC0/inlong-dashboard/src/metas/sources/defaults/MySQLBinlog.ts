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
import { SourceInfo } from '../common/SourceInfo';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;
const { ColumnDecorator } = RenderList;

export default class TubeMqSource
  extends SourceInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sources.Db.Server')
  hostname: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 3306,
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
      min: 0,
      max: 65535,
    }),
  })
  @ColumnDecorator()
  @I18n('meta.Sources.Db.Port')
  port: number;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Db.User')
  user: string;

  @FieldDecorator({
    type: 'password',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Db.Password')
  password: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    initialValue: '/data/inlong-agent/.history',
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Db.HistoryFilename')
  historyFilename: string;

  @FieldDecorator({
    type: 'input',
    tooltip: 'UTC, UTC+8, Asia/Shanghai, ...',
    initialValue: 'UTC',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @I18n('meta.Sources.Db.ServerTimezone')
  serverTimezone: string;

  @FieldDecorator({
    type: 'inputnumber',
    initialValue: 1000,
    rules: [{ required: true }],
    suffix: 'ms',
    props: values => ({
      disabled: values?.status === 101,
      min: 1000,
      max: 3600000,
    }),
  })
  @I18n('meta.Sources.Db.IntervalMs')
  intervalMs: number;

  @FieldDecorator({
    type: 'radio',
    rules: [{ required: true }],
    initialValue: false,
    props: values => ({
      disabled: values?.status === 101,
      options: [
        {
          label: i18n.t('basic.Yes'),
          value: true,
        },
        {
          label: i18n.t('basic.No'),
          value: false,
        },
      ],
    }),
  })
  @I18n('meta.Sources.Db.AllMigration')
  allMigration: boolean;

  @FieldDecorator({
    type: 'input',
    tooltip: i18n.t('meta.Sources.Db.TableWhiteListHelp'),
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
    visible: values => !values?.allMigration,
  })
  @I18n('meta.Sources.Db.TableWhiteList')
  tableWhiteList: boolean;
}
