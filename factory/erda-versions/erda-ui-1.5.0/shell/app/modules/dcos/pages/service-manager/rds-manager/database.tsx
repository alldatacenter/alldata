// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { Table, Button, Select, Tooltip } from 'antd';
import { map } from 'lodash';
import i18n from 'i18n';
import cloudServiceStore from 'dcos/stores/cloud-service';
import { getCloudResourceStatusCol, getRemarkCol } from 'cmp/common/components/table-col';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import { insertWhen, goTo } from 'common/utils';
import { characterSetLists } from 'dcos/common/config';
import { rdsAccountType } from 'dcos/common/config.js';
import { useEffectOnce } from 'react-use';

const { Option } = Select;
const DataBase = () => {
  const [RDSDatabaseList, RDSAccountList] = cloudServiceStore.useStore((s) => [s.RDSDatabaseList, s.RDSAccountList]);
  const [rdsID, query] = routeInfoStore.useStore((s) => [s.params.rdsID, s.query]);
  const [isFetching] = useLoading(cloudServiceStore, ['getRDSDatabaseList']);
  const { addRDSDatabase, getRDSDatabaseList, getRDSAccountList } = cloudServiceStore.effects;
  const { clearRDSDatabaseList, clearRDSAccountList } = cloudServiceStore.reducers;
  const [{ formVisible, ifSelectAccType, accountVal }, updater, update] = useUpdate({
    formVisible: false,
    ifSelectAccType: false,
    accountVal: undefined as undefined | string,
  });

  useEffectOnce(() => {
    getList();
    getRDSAccountList({
      id: rdsID,
      query,
    });
    return () => {
      clearRDSDatabaseList();
      clearRDSAccountList();
    };
  });

  const getList = () => {
    getRDSDatabaseList({
      id: rdsID,
      query,
    });
  };

  const columns = [
    {
      title: i18n.t('cmp:database name'),
      dataIndex: 'dBName',
      ellipsis: {
        showTitle: false,
      },
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    getCloudResourceStatusCol('rds', i18n.t('cmp:database status'), 'dBStatus'),
    {
      title: i18n.t('character set'),
      dataIndex: 'characterSetName',
    },
    {
      title: i18n.t('bind account'),
      dataIndex: 'accounts',
      ellipsis: true,
      render: (_v: Array<{ Account: string }>) => {
        return map(_v, (item) => <div key={item.Account}>{item.Account}</div>);
      },
    },
    getRemarkCol('dBDescription'),
  ];

  const allDBName = map(RDSDatabaseList, 'dBName');
  const fieldsList = [
    {
      label: i18n.t('cmp:database (DB) name'),
      name: 'dbName',
      rules: [
        {
          validator: (_: any, value: string, callback: Function) => {
            if (!value) return callback();
            if (allDBName.includes(value)) {
              return callback(i18n.t('{name} already exists', { name: i18n.t('cmp:database name') }));
            }
            if (value.length < 2 || value.length > 64 || !/^[a-z][a-z0-9_-]*[a-z0-9]$/.test(value)) {
              return callback(i18n.t('cmp:rds-db-name-format'));
            }
            callback();
          },
        },
      ],
      itemProps: {
        placeholder: i18n.t('cmp:rds-db-name-format'),
      },
    },
    {
      label: i18n.t('cmp:support character set'),
      name: 'characterSetName',
      type: 'select',
      options: characterSetLists.map((a) => ({ name: a, value: a })),
      initialValue: characterSetLists[3],
      itemProps: {
        style: {
          width: '50%',
        },
      },
    },
    {
      label: i18n.t('cmp:authorized account'),
      name: 'account',
      required: false,
      getComp: () => {
        return (
          <>
            <Select
              className="mr-5"
              allowClear
              placeholder={i18n.t('cmp:unauthorized account (default)')}
              style={{ width: '50%' }}
              value={accountVal}
              onChange={(value: any) => {
                update({
                  ifSelectAccType: !!value,
                  accountVal: value,
                });
              }}
            >
              {RDSAccountList.map((user) => (
                <Option key={user.accountName} value={user.accountName}>
                  {user.accountName}
                </Option>
              ))}
            </Select>
            <span
              className="fake-link nowrap"
              onClick={() => {
                goTo(`../account${location.search}`, { jumpOut: true });
              }}
            >
              {i18n.t('create new account')}
            </span>
          </>
        );
      },
    },
    ...insertWhen(ifSelectAccType, [
      {
        label: i18n.t('account type'),
        name: 'accountPrivilege',
        type: 'radioGroup',
        initialValue: rdsAccountType[0].value,
        options: rdsAccountType,
      },
    ]),
    {
      label: i18n.t('dop:remark'),
      name: 'description',
      type: 'textArea',
      required: false,
      itemProps: {
        maxLength: 256,
        rows: 4,
      },
    },
  ];

  const onCancel = () => {
    update({
      accountVal: undefined,
      formVisible: false,
      ifSelectAccType: false,
    });
  };

  const handleDBSubmit = (formRes: any) => {
    const form = {
      region: query.region,
      vendor: 'alicloud',
      instanceID: rdsID,
      source: 'resource',
      databases: [formRes],
    };
    return addRDSDatabase(form).then(() => {
      getList();
      onCancel();
    });
  };

  return (
    <div>
      <div className="text-right mb-3">
        <Button type="primary" onClick={() => updater.formVisible(true)}>
          {i18n.t('cmp:create database')}
        </Button>
      </div>
      <Table
        loading={isFetching}
        columns={columns}
        dataSource={RDSDatabaseList}
        rowKey="dBName"
        scroll={{ x: '100%' }}
      />
      <FormModal
        title={i18n.t('cmp:create database')}
        visible={formVisible}
        fieldsList={fieldsList}
        onCancel={onCancel}
        onOk={(formRes: any) => {
          const newFormRes = {
            ...formRes,
            account: accountVal,
          };
          handleDBSubmit(newFormRes);
        }}
      />
    </div>
  );
};

export default DataBase;
