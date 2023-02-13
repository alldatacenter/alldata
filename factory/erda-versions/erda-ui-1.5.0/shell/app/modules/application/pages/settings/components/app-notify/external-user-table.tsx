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
import { cloneDeep, find, findIndex, fill, uniqueId, filter, map, omit, every, some, isEmpty } from 'lodash';
import { Button, Table, Input, message } from 'antd';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';

export default ({ value: targets, onChange }: { value?: any[]; onChange: (value: string[]) => void }) => {
  const [{ editingExternalUsers }, updater] = useUpdate({
    editingExternalUsers: [],
  });

  React.useEffect(() => {
    updater.editingExternalUsers(
      map(targets, (item) => ({
        uniKey: uniqueId(),
        ...JSON.parse(item),
      })),
    );
  }, [updater, targets]);

  const updateEditingExternalUsers = (users: COMMON_NOTIFY.ExternalUserInfo[]) => {
    onChange([]);
    if (every(users, (user) => isEmpty(omit(user, 'uniKey')))) {
      message.warning(i18n.t('dop:add at least one'));
      return;
    }
    if (some(users, (user) => !user.username.trim())) {
      message.warning(i18n.t('dop:external username cannot be empty'));
      return;
    }
    onChange(map(users, (user) => JSON.stringify(omit(user, 'uniKey'))));
  };

  const handleEditExternalUser = (uniKey: string, key: string, value: any) => {
    const users = cloneDeep(editingExternalUsers);
    const user = find(users, { uniKey });
    const index = findIndex(users, { uniKey });

    fill(
      users,
      {
        uniKey,
        ...user,
        [key]: value,
      },
      index,
      index + 1,
    );

    updateEditingExternalUsers(users);
  };

  const handleAddExternalUser = () => {
    updater.editingExternalUsers([
      {
        uniKey: uniqueId(),
        username: undefined,
        email: undefined,
        mobile: undefined,
      },
      ...editingExternalUsers,
    ]);
  };

  const handleRemoveExternalUser = (uniKey: string) => {
    updateEditingExternalUsers(filter(editingExternalUsers, (item) => item.uniKey !== uniKey));
  };

  const columns = [
    {
      title: i18n.t('user name'),
      dataIndex: 'username',
      render: (value: string, { uniKey }: COMMON_NOTIFY.ExternalUserInfo) => (
        <Input
          defaultValue={value}
          onBlur={(e: any) => {
            handleEditExternalUser(uniKey, 'username', e.target.value);
          }}
        />
      ),
    },
    {
      title: i18n.t('dop:email'),
      dataIndex: 'email',
      render: (value: string, { uniKey }: COMMON_NOTIFY.ExternalUserInfo) => (
        <Input
          defaultValue={value}
          onBlur={(e: any) => {
            handleEditExternalUser(uniKey, 'email', e.target.value);
          }}
        />
      ),
    },
    {
      title: i18n.t('dop:mobile'),
      dataIndex: 'mobile',
      render: (value: string, { uniKey }: COMMON_NOTIFY.ExternalUserInfo) => (
        <Input
          defaultValue={value}
          onBlur={(e: any) => {
            handleEditExternalUser(uniKey, 'mobile', e.target.value);
          }}
        />
      ),
    },
    {
      title: i18n.t('operate'),
      width: 65,
      dataIndex: 'uniKey',
      render: (uniKey: string) => {
        return (
          <div className="table-operations">
            <span
              className="table-operations-btn"
              onClick={() => {
                handleRemoveExternalUser(uniKey);
              }}
            >
              {i18n.t('delete')}
            </span>
          </div>
        );
      },
    },
  ];

  return (
    <>
      <Button className="mb-2" type="primary" ghost onClick={handleAddExternalUser}>
        {i18n.t('cmp:add external user')}
      </Button>
      <Table bordered rowKey="uniKey" dataSource={editingExternalUsers} columns={columns} scroll={{ x: '100%' }} />
    </>
  );
};
