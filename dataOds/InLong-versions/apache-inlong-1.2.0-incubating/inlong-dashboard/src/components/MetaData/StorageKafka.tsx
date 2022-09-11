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

import { getColsFromFields, GetStorageFormFieldsType } from '@/utils/metaData';
import i18n from '@/i18n';
import { ColumnsType } from 'antd/es/table';
import { excludeObject } from '@/utils';

const getForm: GetStorageFormFieldsType = (
  type: 'form' | 'col' = 'form',
  { currentValues, isEdit } = {} as any,
) => {
  const fileds = [
    {
      name: 'bootstrapServers',
      type: 'input',
      label: i18n.t('components.AccessHelper.StorageMetaData.Kafka.Server'),
      rules: [{ required: true }],
      initialValue: '127.0.0.1:9092',
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'topicName',
      type: 'input',
      label: 'Topic',
      rules: [{ required: true }],
      props: {
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'serializationType',
      type: 'radio',
      label: i18n.t('components.AccessHelper.StorageMetaData.Kafka.SerializationType'),
      initialValue: 'JSON',
      rules: [{ required: true }],
      props: {
        options: [
          {
            label: 'JSON',
            value: 'JSON',
          },
          {
            label: 'CANAL',
            value: 'CANAL',
          },
          {
            label: 'AVRO',
            value: 'AVRO',
          },
        ],
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      _inTable: true,
    },
    {
      name: 'partitionNum',
      type: 'inputnumber',
      label: i18n.t('components.AccessHelper.StorageMetaData.Kafka.PartitionNum'),
      initialValue: 3,
      props: {
        min: 1,
        max: 30,
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
      rules: [{ required: true }],
    },
    {
      name: 'autoOffsetReset',
      type: 'radio',
      label: i18n.t('components.AccessHelper.StorageMetaData.Kafka.AutoOffsetReset'),
      initialValue: 'earliest',
      rules: [{ required: true }],
      props: {
        options: [
          {
            label: 'earliest',
            value: 'earliest',
          },
          {
            label: 'latest',
            value: 'latest',
          },
          {
            label: 'none',
            value: 'none',
          },
        ],
        disabled: isEdit && [110, 130].includes(currentValues?.status),
      },
    },
  ];

  return type === 'col'
    ? getColsFromFields(fileds)
    : fileds.map(item => excludeObject(['_inTable'], item));
};

const tableColumns = getForm('col') as ColumnsType;

export const StorageKafka = {
  getForm,
  tableColumns,
};
