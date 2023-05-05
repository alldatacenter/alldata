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
import { Modal } from 'antd';
import cloudAccountStore from 'app/modules/cmp/stores/cloud-account';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import { CRUDTable } from 'common';
import { getAccountsFieldsList } from 'cmp/common/cloud-common';
import i18n from 'i18n';

const { confirm } = Modal;

const CloudAccounts = () => {
  const { deleteItem, getList } = cloudAccountStore.effects;

  const showDeleteConfirm = (record: CLOUD_ACCOUNTS.Account) => {
    confirm({
      title: i18n.t('cmp:confirm deletion of cloud account?'),
      content: i18n.t('cmp:cloud account cannot be restored after deletion, confirm execution?'),
      onOk() {
        const { vendor, accessKeyID } = record;
        deleteItem({ vendor, accessKeyID }).then(() => {
          getList({ pageNo: 1, pageSize: 10 });
        });
      },
    });
  };

  const getColumns = () => {
    return [
      {
        title: 'Access Key ID',
        dataIndex: 'accessKeyID',
      },
      {
        title: i18n.t('vender'),
        dataIndex: 'vendor',
      },
      {
        title: i18n.t('description'),
        dataIndex: 'description',
      },
      {
        title: i18n.t('operation'),
        key: 'operation',
        width: 100,
        render: (_text: string, record: CLOUD_ACCOUNTS.Account) => {
          return (
            <div className="table-operations">
              <span className="table-operations-btn" onClick={() => showDeleteConfirm(record)}>
                {i18n.t('delete')}
              </span>
            </div>
          );
        },
      },
    ];
  };

  const getFieldsList = () => {
    return getAccountsFieldsList([{ name: i18n.t('Alibaba Cloud'), value: 'aliyun' }]);
  };

  const handleFormSubmit = (data: CLOUD_ACCOUNTS.Account, { addItem }: { addItem: (arg: any) => Promise<any> }) => {
    return addItem(data).then(() => cloudCommonStore.checkCloudAccount());
  };

  return (
    <>
      <CRUDTable.StoreTable<CLOUD_ACCOUNTS.Account>
        name={i18n.t('account')}
        getColumns={getColumns}
        store={cloudAccountStore}
        showTopAdd
        getFieldsList={getFieldsList}
        handleFormSubmit={handleFormSubmit}
      />
    </>
  );
};

export default CloudAccounts;
