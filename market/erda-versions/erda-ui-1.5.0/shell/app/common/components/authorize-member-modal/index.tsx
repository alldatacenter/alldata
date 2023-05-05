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

import routeInfoStore from 'core/stores/route';
import { MemberScope } from 'common/stores/member-scope';
import { getAppList } from 'common/services';
import appMemberStore from 'common/stores/application-member';
import i18n from 'i18n';
import { map, compact, isEmpty } from 'lodash';
import { Modal, Select, Table, Button, Input } from 'antd';
import orgMemberStore from 'common/stores/org-member';
import projectMemberStore from 'common/stores/project-member';
import sysMemberStore from 'common/stores/sys-member';
import React from 'react';
import { useEffectOnce } from 'react-use';
import mspProjectMember from 'common/stores/msp-project-member';
import { usePaging } from 'core/service';

const { Option } = Select;

interface IProps {
  member: IMember | null;
  type: MemberScope;
  closeModal: () => void;
}

const storeMap = {
  [MemberScope.PROJECT]: projectMemberStore,
  [MemberScope.ORG]: orgMemberStore,
  [MemberScope.APP]: appMemberStore,
  [MemberScope.MSP]: mspProjectMember,
  [MemberScope.SYS]: sysMemberStore,
};

const AuthorizeMemberModal = ({ type, member, closeModal }: IProps) => {
  const memberStore = storeMap[type];
  const { updateMembers, removeMember } = memberStore.effects;
  const { getRoleMap } = appMemberStore.effects; // 应用授权，只查询项目的角色
  const roleMap = appMemberStore.useStore((s) => s.roleMap);
  const { params } = routeInfoStore.getState((s) => s);
  const load = usePaging({
    service: getAppList.fetch,
    required: {
      memberID: member?.userId,
      projectId: params.projectId,
    },
  });

  const [data, loading] = getAppList.useState();

  useEffectOnce(() => {
    getRoleMap({ scopeType: MemberScope.APP });
    load({ pageNo: 1 });
  });

  if (!data) {
    return null;
  }
  const { list, paging } = data;

  const pagination = {
    total: paging.total,
    current: paging.pageNo,
    pageSize: paging.pageSize,
    onChange: (no: number) => load({ pageNo: no }),
  };
  const columns = [
    {
      title: i18n.t('application'),
      dataIndex: 'name',
    },
    {
      title: i18n.t('role'),
      dataIndex: 'memberRoles',
      render: (text: string, record: IApplication) => {
        return (
          <Select
            getPopupContainer={() => document.body}
            style={{ width: 180 }}
            value={text || undefined}
            mode="multiple"
            onChange={(v: string[]) => {
              if (member) {
                const payload = { scope: { id: String(record.id), type: MemberScope.APP }, userIds: [member.userId] };
                if (!isEmpty(compact(v))) {
                  updateMembers({ ...payload, roles: v }, { forbidReload: true }).then(() => {
                    load({ pageNo: paging.pageNo });
                  });
                } else {
                  removeMember(payload).then(() => {
                    load({ pageNo: paging.pageNo });
                  });
                }
              }
            }}
            placeholder={`${i18n.t('dop:please set ')}`}
          >
            {map(roleMap, (v: string, k: string) => (
              <Option key={k} value={k}>
                {v}
              </Option>
            ))}
          </Select>
        );
      },
    },
  ];

  return (
    <Modal
      title={i18n.t('authorize user {user}', { user: member ? member.nick || member.name : '' })}
      visible={!!member}
      onOk={closeModal}
      onCancel={closeModal}
      destroyOnClose
      maskClosable={false}
      footer={[
        <Button type="primary" key="back" onClick={closeModal}>
          {i18n.t('close')}
        </Button>,
      ]}
      width={600}
    >
      <Input.Search
        onSearch={(q) => load({ q, pageNo: 1 })}
        className="mb-3"
        allowClear
        placeholder={i18n.t('dop:search by application name')}
      />
      <Table loading={loading} rowKey={'id'} pagination={pagination} columns={columns} dataSource={list} />
    </Modal>
  );
};

export default AuthorizeMemberModal;
