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

import i18n from '@/i18n';
import { ColumnsItemProps } from '@/components/EditableTable';

export const fieldTypes = ['int', 'long', 'float', 'double', 'string', 'date', 'timestamp'].map(
  item => ({
    label: item,
    value: item,
  }),
);

export const sourceDataFields: ColumnsItemProps[] = [
  {
    title: i18n.t('components.AccessHelper.StorageMetaData.SourceFieldName'),
    dataIndex: 'sourceFieldName',
    initialValue: '',
    rules: [
      { required: true },
      {
        pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
        message: i18n.t('components.AccessHelper.StorageMetaData.SourceFieldNameRule'),
      },
    ],
    props: (text, record, idx, isNew) => ({
      disabled: text && !isNew,
    }),
  },
  {
    title: i18n.t('components.AccessHelper.StorageMetaData.SourceFieldType'),
    dataIndex: 'sourceFieldType',
    initialValue: fieldTypes[0].value,
    type: 'select',
    rules: [{ required: true }],
    props: (text, record, idx, isNew) => ({
      disabled: text && !isNew,
      options: fieldTypes,
    }),
  },
];
