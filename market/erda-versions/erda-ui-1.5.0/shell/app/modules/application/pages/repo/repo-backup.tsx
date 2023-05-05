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
import { get } from 'lodash';
import { Table, Button, Popconfirm, Select, Tooltip } from 'antd';
import { WithAuth, usePerm } from 'user/common';
import { fromNow, setApiWithOrg } from 'common/utils';
import { Copy, FormModal } from 'common';
import { useMount } from 'react-use';
import i18n from 'i18n';
import repoStore from 'application/stores/repo';

const { Option } = Select;

export default function BackupManagement() {
  const [visible, setVisible] = React.useState(false);
  const [backupList, backupPaging, info] = repoStore.useStore((s) => [s.backupList, s.backupPaging, s.info]);
  const { addBackup, getBackupList, deleteBackup, getLatestCommit } = repoStore.effects;
  const [newBackupAuth, deleteBackupAuth] = usePerm((s) => [
    s.app.repo.backup.backupRepo.pass,
    s.app.repo.backup.deleteBackup.pass,
  ]);
  const download = (uuid: string) => window.open(setApiWithOrg(`/api/backup/${uuid}`));
  const { branches = [] } = info;
  const columns: any[] = [
    {
      title: i18n.t('dop:Git reference'),
      dataIndex: 'name',
      width: 180,
      render: (name: string) => name.slice(0, name.indexOf('.')),
    },
    {
      title: i18n.t('dop:commit ID'),
      width: 150,
      dataIndex: 'commitId',
      render: (commitId: string) => (
        <span className="cursor-copy" data-clipboard-text={commitId}>
          {commitId}
        </span>
      ),
    },
    {
      title: i18n.t('dop:submit information'),
      dataIndex: 'remark',
    },
    {
      title: i18n.t('backup person'),
      dataIndex: 'creator',
      width: 120,
    },
    {
      title: i18n.t('backup time'),
      dataIndex: 'createdAt',
      width: 120,
      render: (text: string) => fromNow(text),
    },
    {
      title: i18n.t('operation'),
      key: 'operation',
      align: 'center',
      width: 160,
      render: (_: any, record: REPOSITORY.BackupQuery) => {
        return (
          <div className="table-operations">
            <span className="table-operations-btn" onClick={() => download(record.uuid)}>
              {i18n.t('download')}
            </span>
            <WithAuth pass={deleteBackupAuth}>
              <Popconfirm
                title={`${i18n.t('common:confirm to delete')}?`}
                disabled={info.isLocked}
                onConfirm={() => {
                  deleteBackup({ uuid: record.uuid }).then(() => reloadBackupList({ pageNo: 1 }));
                }}
              >
                <Tooltip title={info.isLocked ? i18n.t('dop:lock-operation-tip') : i18n.t('delete')}>
                  <span className={info.isLocked ? '' : 'table-operations-btn'}>{i18n.t('delete')}</span>
                </Tooltip>
              </Popconfirm>
            </WithAuth>
          </div>
        );
      },
    },
  ];

  const fieldsList = [
    {
      label: i18n.t('dop:choose branch'),
      name: 'branchRef',
      type: 'custom',
      getComp: ({ form }: any) => (
        <Select
          showSearch
          optionFilterProp="children"
          onSelect={(value) => {
            getLatestCommit({ branchRef: value as string }).then((res: any) => {
              form.setFieldsValue({ commitId: get(res, 'backupLatestCommit.id') });
            });
          }}
          filterOption={(input, option: any) => option.props.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
        >
          {branches.map((option: string) => (
            <Option key={option} value={option}>
              {option}
            </Option>
          ))}
        </Select>
      ),
    },
    // 暂无法备份历史 commit 代码，只能备份最新 commit 代码，故隐藏此字段
    {
      label: i18n.t('dop:commit ID'),
      name: 'commitId',
      required: true,
      type: 'input',
      itemProps: {
        type: 'hidden',
      },
      rules: [
        {
          pattern: /^[a-z0-9]{40}$/,
          message: i18n.t('dop:commitID-input-tip'),
        },
      ],
    },
    {
      label: i18n.t('dop:submit information'),
      name: 'remark',
      required: false,
      type: 'textArea',
      itemProps: {
        autoComplete: 'off',
        maxLength: 1024,
      },
    },
  ];

  const backupPagination = {
    current: backupPaging.pageNo,
    pageSize: backupPaging.pageSize,
    total: backupPaging.total,
    showSizeChanger: true,
    onChange: (current: number, size: number) => reloadBackupList({ pageNo: current, pageSize: size }),
  };

  function reloadBackupList(query = {}) {
    return getBackupList({
      pageSize: backupPaging.pageSize,
      pageNo: backupPaging.pageNo,
      ...query,
    });
  }

  useMount(() => {
    reloadBackupList();
  });

  const onAddBackup = (backupInfo: REPOSITORY.IBackupAppendBody) => {
    addBackup(backupInfo).then(() => reloadBackupList({ pageNo: 1 }));
    setVisible(false);
  };

  return (
    <div>
      <Copy selector=".cursor-copy" />
      <div className="top-button-group">
        <WithAuth pass={newBackupAuth}>
          <Tooltip title={info.isLocked ? i18n.t('dop:lock-operation-tip') : i18n.t('dop:new backup')} placement="left">
            <Button type="primary" disabled={info.isLocked} onClick={() => setVisible(true)}>
              {i18n.t('dop:new backup')}
            </Button>
          </Tooltip>
        </WithAuth>
        <FormModal
          visible={visible}
          name={i18n.t('dop:backup')}
          fieldsList={fieldsList}
          onOk={onAddBackup}
          onCancel={() => setVisible(false)}
        />
      </div>
      <Table
        columns={columns}
        rowKey="id"
        dataSource={backupList}
        pagination={backupPagination}
        scroll={{ x: '100%' }}
      />
    </div>
  );
}
