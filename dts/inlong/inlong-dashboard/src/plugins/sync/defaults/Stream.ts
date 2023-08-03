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
import { SyncInfo } from '../common/SyncInfo';
import i18n from '@/i18n';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class Stream extends SyncInfo implements DataWithBackend, RenderRow, RenderList {
  @FieldDecorator({
    type: 'input',
    props: values => ({
      maxLength: 100,
      disable: Boolean(values?.id),
    }),
    rules: [
      { required: true },
      {
        pattern: /^[a-z_0-9]+$/,
        message: i18n.t('meta.Group.InlongGroupIdRules'),
      },
    ],
  })
  @I18n('StreamID')
  inlongStreamId: string;

  @FieldDecorator({
    type: 'input',
  })
  @I18n('meta.Stream.StreamName')
  name: string;
}
