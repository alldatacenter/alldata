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

// import request from '@/utils/request';
import { getColsFromFields } from '@/utils/metaData';
import { ColumnsType } from 'antd/es/table';
import i18n from '@/i18n';

export const getDataSourcesBinLogFields = (
  type: 'form' | 'col' = 'form',
  { currentValues } = {} as any,
) => {
  const fileds = [
    {
      name: 'hostname',
      type: 'input',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.Server'),
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
      },
      _inTable: true,
    },
    {
      name: 'port',
      type: 'inputnumber',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.Port'),
      initialValue: 3306,
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
        min: 0,
        max: 65535,
      },
      _inTable: true,
    },
    {
      name: 'user',
      type: 'input',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.User'),
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
      },
    },
    {
      name: 'password',
      type: 'password',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.Password'),
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
      },
    },
    {
      name: 'historyFilename',
      type: 'input',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.HistoryFilename'),
      rules: [{ required: true }],
      initialValue: '/data/inlong-agent/.history',
      props: {
        disabled: currentValues?.status === 101,
      },
      _inTable: true,
    },
    {
      name: 'serverTimezone',
      type: 'input',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.ServerTimezone'),
      tooltip: 'UTC, UTC+8, Asia/Shanghai, ...',
      initialValue: 'UTC',
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
      },
    },
    {
      name: 'intervalMs',
      type: 'inputnumber',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.IntervalMs'),
      initialValue: 1000,
      rules: [{ required: true }],
      suffix: 'ms',
      props: {
        min: 1000,
        max: 3600000,
        disabled: currentValues?.status === 101,
      },
    },
    {
      name: 'allMigration',
      type: 'radio',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.AllMigration'),
      rules: [{ required: true }],
      initialValue: false,
      props: {
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
        disabled: currentValues?.status === 101,
      },
    },
    {
      name: 'databaseWhiteList',
      type: 'input',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.DatabaseWhiteList'),
      tooltip: i18n.t('components.AccessHelper.DataSourceMetaData.Db.WhiteListHelp'),
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
      },
      visible: values => !values?.allMigration,
    },
    {
      name: 'tableWhiteList',
      type: 'input',
      label: i18n.t('components.AccessHelper.DataSourceMetaData.Db.TableWhiteList'),
      tooltip: i18n.t('components.AccessHelper.DataSourceMetaData.Db.WhiteListHelp'),
      rules: [{ required: true }],
      props: {
        disabled: currentValues?.status === 101,
      },
      visible: values => !values?.allMigration,
    },
  ];

  return type === 'col' ? getColsFromFields(fileds) : fileds;
};

export const toFormValues = data => {
  return {
    ...data,
    _startDumpPosition: data.startDumpPosition ? 1 : 0,
  };
};

export const toSubmitValues = data => {
  const output = { ...data };
  delete output._startDumpPosition;
  return {
    ...output,
    startDumpPosition: data._startDumpPosition ? output.startDumpPosition : null,
  };
};

export const dataSourcesBinLogColumns = getDataSourcesBinLogFields('col') as ColumnsType;
