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

import React from 'react';
import i18n from '@/i18n';
import UserSelect from '@/components/UserSelect';
import type { FieldItemType } from '@/metas/common';
import { genFields, genForm, genTable } from '@/metas/common';
import { hive } from './hive';

const allNodes = [
  {
    label: 'Hive',
    value: 'HIVE',
    fields: hive,
  },
];

const defaultCommonFields: FieldItemType[] = [
  {
    type: 'input',
    label: i18n.t('meta.Nodes.Name'),
    name: 'name',
    rules: [{ required: true }],
    props: {
      maxLength: 128,
    },
    _renderTable: true,
  },
  {
    type: 'select',
    label: i18n.t('meta.Nodes.Type'),
    name: 'type',
    initialValue: allNodes[0].value,
    rules: [{ required: true }],
    props: {
      options: allNodes.map(item => ({
        label: item.label,
        value: item.value,
      })),
    },
    _renderTable: true,
  },
  {
    type: <UserSelect mode="multiple" currentUserClosable={false} />,
    label: i18n.t('meta.Nodes.Owners'),
    name: 'inCharges',
    rules: [{ required: true }],
    _renderTable: true,
  },
  {
    type: 'textarea',
    label: i18n.t('meta.Nodes.Description'),
    name: 'description',
    props: {
      maxLength: 256,
    },
  },
];

export const nodes = allNodes.map(item => {
  const itemFields = defaultCommonFields.concat(item.fields);
  const fields = genFields(itemFields);

  return {
    ...item,
    fields,
    form: genForm(fields),
    table: genTable(fields),
  };
});
