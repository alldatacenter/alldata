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
import { ColumnsType } from 'antd/es/table';
import StaffSelect from '@/components/StaffSelect';
import rulesPattern from '@/utils/pattern';

export const getCreateFormContent = (defaultValues = {}) => {
  const array = [
    {
      type: 'input',
      label: i18n.t('pages.Datasources.DbConfig.ServerName'),
      name: 'connectionName',
      rules: [{ required: true }],
      props: {
        maxLength: 128,
      },
    },
    {
      type: 'select',
      label: i18n.t('pages.Datasources.DbConfig.Type'),
      name: 'dbType',
      rules: [{ required: true }],
      props: {
        options: [
          {
            label: 'MySQL',
            value: 'MySQL',
          },
          {
            label: 'PG',
            value: 'PG',
          },
        ],
      },
    },
    {
      type: 'input',
      label: 'DB IP',
      name: 'dbServerIp',
      rules: [
        { required: true },
        {
          pattern: rulesPattern.ip,
          message: i18n.t('pages.Datasources.DbConfig.IpPattern'),
        },
      ],
    },
    {
      type: 'inputnumber',
      label: i18n.t('pages.Datasources.DbConfig.Port'),
      name: 'port',
      rules: [
        { required: true },
        {
          pattern: rulesPattern.port,
          message: i18n.t('pages.Datasources.DbConfig.PortPattern'),
        },
      ],
    },
    {
      type: 'input',
      label: i18n.t('pages.Datasources.DbConfig.BackupDbIp'),
      name: 'backupDbServerIp',
      rules: [
        {
          pattern: rulesPattern.ip,
          message: i18n.t('pages.Datasources.DbConfig.IpPattern'),
        },
      ],
    },
    {
      type: 'inputnumber',
      label: i18n.t('pages.Datasources.DbConfig.BackupDbPort'),
      name: 'backupDbPort',
      rules: [
        {
          pattern: rulesPattern.port,
          message: i18n.t('pages.Datasources.DbConfig.PortPattern'),
        },
      ],
    },
    {
      type: 'input',
      label: i18n.t('pages.Datasources.DbConfig.DbName'),
      name: 'dbName',
    },
    {
      type: 'input',
      label: i18n.t('pages.Datasources.DbConfig.Username'),
      name: 'username',
      rules: [{ required: true }],
    },
    {
      type: 'input',
      label: i18n.t('pages.Datasources.DbConfig.Password'),
      name: 'password',
      rules: [{ required: true }],
    },
    {
      type: <StaffSelect mode="multiple" currentUserClosable={false} />,
      label: i18n.t('pages.Datasources.DbConfig.InCharges'),
      name: 'inCharges',
      rules: [{ required: true }],
    },
    {
      type: 'input',
      label: i18n.t('pages.Datasources.DbConfig.DbDescription'),
      name: 'dbDescription',
      props: {
        maxLength: 256,
      },
    },
  ];

  return array;
};

export const tableColumns: ColumnsType = [
  {
    title: i18n.t('pages.Datasources.DbConfig.ServerName'),
    dataIndex: 'connectionName',
  },
  {
    title: i18n.t('pages.Datasources.DbConfig.Type'),
    dataIndex: 'dbType',
  },
  {
    title: 'DB IP',
    dataIndex: 'dbServerIp',
  },
  {
    title: i18n.t('pages.Datasources.DbConfig.Port'),
    dataIndex: 'port',
  },
  {
    title: i18n.t('pages.Datasources.DbConfig.DbName'),
    dataIndex: 'dbName',
  },
  {
    title: i18n.t('pages.Datasources.DbConfig.DbDescription'),
    dataIndex: 'dbDescription',
  },
];
