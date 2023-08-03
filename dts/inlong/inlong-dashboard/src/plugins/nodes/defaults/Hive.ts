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

import { DataWithBackend } from '@/plugins/DataWithBackend';
import { RenderRow } from '@/plugins/RenderRow';
import { RenderList } from '@/plugins/RenderList';
import i18n from '@/i18n';
import { NodeInfo } from '../common/NodeInfo';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class HiveNode extends NodeInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
  })
  @I18n('meta.Nodes.Hive.Username')
  username: string;

  @FieldDecorator({
    type: 'password',
    rules: [{ required: true }],
  })
  @I18n('meta.Nodes.Hive.Password')
  token: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: {
      placeholder: 'jdbc:hive2://127.0.0.1:10000',
    },
  })
  @I18n('JDBC URL')
  url: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    tooltip: i18n.t('meta.Nodes.Hive.DataPathHelp'),
    props: {
      placeholder: 'hdfs://127.0.0.1:9000/user/hive/warehouse/default',
    },
  })
  @I18n('meta.Nodes.Hive.DataPath')
  dataPath: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    tooltip: i18n.t('meta.Nodes.Hive.ConfDirHelp'),
    props: {
      placeholder: '/usr/hive/conf',
    },
  })
  @I18n('meta.Nodes.Hive.ConfDir')
  hiveConfDir: string;
}
