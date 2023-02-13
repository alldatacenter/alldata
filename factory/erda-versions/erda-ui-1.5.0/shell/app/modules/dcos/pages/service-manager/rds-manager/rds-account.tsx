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
import { Table, Button, Tooltip } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { map } from 'lodash';
import i18n from 'i18n';
import { getCloudResourceStatusCol, getRemarkCol } from 'cmp/common/components/table-col';
import cloudServiceStore from 'dcos/stores/cloud-service';
import ResetPasswordForm, { IFormRes as ResetFormRes } from './account-form/reset-password-form';
import ChangePermissionForm from './account-form/change-permission-form';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import { useEffectOnce } from 'react-use';
import AddAccountForm from './account-form/add-account-form';

const RdsAccount = () => {
  const [rdsID, query] = routeInfoStore.useStore((s) => [s.params.rdsID, s.query]);
  const { getRDSAccountList, addRDSAccount, resetRDSAccountPWD, revokeRDSAccountPriv } = cloudServiceStore.effects;
  const { clearRDSAccountList } = cloudServiceStore.reducers;
  const RDSAccountList = cloudServiceStore.useStore((s) => s.RDSAccountList);
  const [isFetching] = useLoading(cloudServiceStore, ['getRDSAccountList']);

  const [{ formVisible, resetFormVisible, permissionFormVisible, activeRow }, updater, update] = useUpdate({
    formVisible: false,
    resetFormVisible: false,
    permissionFormVisible: false,
    activeRow: {},
  });

  useEffectOnce(() => {
    getList();
    return () => clearRDSAccountList();
  });

  const getList = () => {
    getRDSAccountList({
      id: rdsID,
      query,
    });
  };

  const columns = [
    {
      title: i18n.t('account'),
      dataIndex: 'accountName',
      ellipsis: {
        showTitle: false,
      },
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: i18n.t('type'),
      dataIndex: 'accountType',
      ellipsis: {
        showTitle: false,
      },
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
      width: 100,
    },
    getCloudResourceStatusCol('account', i18n.t('status'), 'accountStatus'),
    {
      title: i18n.t('cmp:owned database'),
      dataIndex: 'databasePrivileges',
      ellipsis: true,
      render: (_v: Array<{ dBName: string; accountPrivilege: string }>) => {
        let str = null as any;
        map(_v, (item) => {
          str = (
            <>
              {str}
              {item.dBName} （{item.accountPrivilege}）<br />
            </>
          );
        });
        return str;
      },
    },
    getRemarkCol('accountDescription'),
    {
      title: i18n.t('operation'),
      dataIndex: 'op',
      width: 200,
      render: (_v: any, record: CLOUD_SERVICE.IRDSAccountResp) => {
        return (
          <div className="table-operations">
            <span
              className="table-operations-btn"
              onClick={() => {
                update({
                  activeRow: record,
                  resetFormVisible: true,
                });
              }}
            >
              {i18n.t('reset Password')}
            </span>
            <span
              className="table-operations-btn"
              onClick={() => {
                update({
                  activeRow: record,
                  permissionFormVisible: true,
                });
              }}
            >
              {i18n.t('modify permissions')}
            </span>
          </div>
        );
      },
    },
  ];

  const handleResetSubmit = (formRes: ResetFormRes) => {
    const { password } = formRes;
    const form = {
      account: activeRow.accountName,
      password,
      instanceID: rdsID,
      region: query.region,
      vendor: 'alicloud',
    };
    resetRDSAccountPWD(form);
    updater.resetFormVisible(false);
  };

  const handlePermissionSubmit = (permissionChoose: Record<string, string>) => {
    const form = {
      account: activeRow.accountName,
      instanceID: rdsID,
      vendor: 'alicloud',
      region: query.region,
      oldAccountPrivileges: activeRow.databasePrivileges,
      accountPrivileges: map(permissionChoose, (accountPrivilege, dBName) => {
        return {
          dBName,
          accountPrivilege,
        };
      }),
    };
    return revokeRDSAccountPriv(form).then(() => {
      getList();
      updater.permissionFormVisible(false);
    });
  };

  const handleAccountSubmit = (formRes: any) => {
    const { account, password, description } = formRes;
    const form = {
      account,
      password,
      description,
      instanceID: rdsID,
      region: query.region,
      vendor: 'alicloud',
    };
    return addRDSAccount(form).then(() => {
      getList();
      updater.formVisible(false);
    });
  };

  return (
    <div>
      <div className="text-right mb-3">
        <Button type="primary" onClick={() => updater.formVisible(true)}>
          {i18n.t('create an account')}
        </Button>
      </div>
      <Table
        loading={isFetching}
        columns={columns}
        dataSource={RDSAccountList}
        rowKey="accountName"
        scroll={{ x: '100%' }}
      />
      <ResetPasswordForm
        visible={resetFormVisible}
        onClose={() => updater.resetFormVisible(false)}
        handleSubmit={handleResetSubmit}
      />
      <ChangePermissionForm
        visible={permissionFormVisible}
        onClose={() => updater.permissionFormVisible(false)}
        handleSubmit={handlePermissionSubmit}
        data={activeRow as CLOUD_SERVICE.IRDSAccountResp | null}
      />
      <AddAccountForm
        allAccountName={map(RDSAccountList, 'accountName')}
        visible={formVisible}
        onClose={() => updater.formVisible(false)}
        handleSubmit={handleAccountSubmit}
      />
    </div>
  );
};

export default RdsAccount;
