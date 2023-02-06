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

import { MemberScope } from 'common/stores/member-scope';
import { usePerm } from 'app/user/common';
import userStore from 'app/user/stores';
import appMemberStore from 'common/stores/application-member';
import { AddMemberModal, Copy, FilterGroup, FormModal, IF, ErdaIcon } from 'common';
import { useLoading } from 'core/stores/loading';
import AuthorizeMemberModal from '../authorize-member-modal';
import i18n from 'i18n';
import { debounce, map, isEmpty, find, isArray, filter, get } from 'lodash';
import { Button, Modal, Select, Spin, Tooltip, message, Avatar } from 'antd';
import Table from 'common/components/table';
import { IActions, ColumnProps } from 'common/components/table/interface';
import orgMemberStore from 'common/stores/org-member';
import projectMemberStore from 'common/stores/project-member';
import sysMemberStore from 'common/stores/sys-member';
import React from 'react';
import { useEffectOnce } from 'react-use';
import UrlInviteModal from './url-invite-modal';
import BatchAuthorizeMemberModal from '../batch-authorize-member-modal';
import { insertWhen, goTo, getAvatarChars } from 'common/utils';
import { useUpdate } from '../../use-hooks';
import routeInfoStore from 'core/stores/route';
import memberLabelStore from 'common/stores/member-label';
import orgStore from 'app/org-home/stores/org';
import { FULL_ROOT_DOMAIN } from '../../constants';
import mspProjectMember from 'common/stores/msp-project-member';
import './index.scss';

const storeMap = {
  [MemberScope.ORG]: orgMemberStore,
  [MemberScope.PROJECT]: projectMemberStore,
  [MemberScope.APP]: appMemberStore,
  [MemberScope.MSP]: mspProjectMember,
  [MemberScope.SYS]: sysMemberStore,
};

enum batchOptionType {
  edit = 'edit',
  remove = 'remove',
  authorize = 'authorize',
}

const { Option } = Select;

interface IProps {
  scopeKey: MemberScope;
  readOnly?: boolean;
  showAuthorize?: boolean;
  hideBatchOps?: boolean;
  hideRowSelect?: boolean;
  hasConfigAppAuth?: boolean;
  buttonInCard?: boolean;
  topContent?: React.ReactNode;
  overwriteAuth?: {
    add?: boolean;
    edit?: boolean;
    delete?: boolean;
    showAuthorize?: boolean;
  };
  roleFilter?(data: Obj): Obj;
}

const MembersTable = ({
  scopeKey,
  readOnly = false,
  showAuthorize = false,
  hideBatchOps = false,
  hideRowSelect = false,
  overwriteAuth = {},
  hasConfigAppAuth = false,
  buttonInCard = false,
  topContent = null,
  roleFilter,
}: IProps) => {
  const memberLabels = memberLabelStore.useStore((s) => s.memberLabels);
  const { getMemberLabels } = memberLabelStore.effects;
  const loginUser = userStore.useStore((s) => s.loginUser);
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const { id: orgId, name: orgName, displayName: orgDisplayName } = currentOrg;

  const [projectMemberPerm, appMemberPerm, mspProjectMemberPerm] = usePerm((s) => [
    s.project.member,
    s.app.member,
    s.project.microService.member,
  ]);
  const { id: currentUserId } = loginUser;
  const { params } = routeInfoStore.getState((s) => s);
  const isAdminManager = scopeKey === MemberScope.SYS && loginUser.adminRoles.includes('Manager');

  const memberStore = storeMap[scopeKey];
  const [list, paging, allRoleMap] = memberStore.useStore((s) => [s.list, s.paging, s.roleMap]);
  const { cleanMembers } = memberStore.reducers;
  const { getMemberList, updateMembers, removeMember, getRoleMap, genOrgInviteCode } = memberStore.effects;
  const roleMap = roleFilter ? roleFilter(allRoleMap) : allRoleMap;

  const [getLoading, removeLoading, addLoading, updateLoading, getInviteLoading] = useLoading(memberStore, [
    'getMemberList',
    'removeMember',
    'addMembers',
    'updateMembers',
    'genOrgInviteCode',
  ]);

  const [state, updater] = useUpdate({
    addModalVisible: false,
    batchEditVisible: false,
    inviteModalVisible: false,
    batchAuthorizeVisible: false,
    verifyCode: undefined,
    selectedKeys: [],
    authorizeMember: null,
    editMember: null,
    queryParams: {},
  });

  const scopeIdMap = {
    [MemberScope.ORG]: String(orgId),
    [MemberScope.PROJECT]: `${params.projectId || ''}`,
    [MemberScope.APP]: `${params.appId || ''}`,
    [MemberScope.MSP]: `${params.terminusKey || ''}`,
  };

  const scopeId = scopeIdMap[scopeKey];
  const scope = React.useMemo(
    () => ({ id: scopeKey === MemberScope.SYS ? '0' : scopeId, type: scopeKey }),
    [scopeId, scopeKey],
  );

  const memberAuthMap = {
    [MemberScope.PROJECT]: {
      add: projectMemberPerm.addProjectMember.pass,
      edit: projectMemberPerm.editProjectMember.pass,
      delete: projectMemberPerm.removeProjectMember.pass,
      showAuthorize: projectMemberPerm.showAuthorize.pass,
      invite: false,
    },
    [MemberScope.ORG]: { add: true, edit: true, delete: true, showAuthorize: false, batch: true, invite: true },
    [MemberScope.APP]: {
      add: appMemberPerm.addAppMember.pass,
      edit: appMemberPerm.editAppMember.pass,
      delete: appMemberPerm.deleteAppMember.pass,
      showAuthorize: false, // 应用级别无授权
      invite: false,
    },
    [MemberScope.MSP]: {
      add: mspProjectMemberPerm.addProjectMember.pass,
      edit: mspProjectMemberPerm.editProjectMember.pass,
      delete: mspProjectMemberPerm.removeProjectMember.pass,
      showAuthorize: false,
      invite: false,
    },
  };
  const memberAuth = { ...memberAuthMap[scopeKey], ...overwriteAuth };

  const batchOptions = [
    {
      key: batchOptionType.edit,
      name: (
        <span className="flex">
          <ErdaIcon type="edit" className="mr-1" />
          {i18n.t('edit')}
        </span>
      ),
      disabled: !memberAuth.edit,
      onClick: () => onBatchClick({ key: batchOptionType.edit }),
    },
    ...insertWhen(showAuthorize && memberAuth.showAuthorize, [
      {
        key: batchOptionType.authorize,
        name: i18n.t('authorize'),
        onClick: () => onBatchClick({ key: batchOptionType.authorize }),
      },
    ]),
    {
      key: batchOptionType.remove,
      name: (
        <span className="flex">
          <ErdaIcon type="delete" className="mr-1" />
          {i18n.t('remove')}
        </span>
      ),
      disabled: !memberAuth.delete || isEmpty(filter(state.selectedKeys, (item) => item !== currentUserId)),
      onClick: () => onBatchClick({ key: batchOptionType.remove }),
    },
  ];

  useEffectOnce(() => {
    getRoleMap({ scopeType: scopeKey, scopeId: scopeKey === MemberScope.SYS ? 0 : scopeId });
    if (scope.type === MemberScope.ORG) {
      getMemberLabels();
    }
    return () => {
      cleanMembers();
    };
  });

  React.useEffect(() => {
    if (scope.id || scope.type === MemberScope.SYS) {
      getMemberList({ scope, ...state.queryParams } as MEMBER.GetListQuery);
    }
  }, [getMemberList, scope, state.queryParams]);

  const onTableSelectChange = React.useCallback(
    (keys: Array<number | string>) => {
      updater.selectedKeys(keys);
    },
    [updater],
  );

  const onBatchClick = ({ key }: any) => {
    switch (key) {
      case batchOptionType.edit:
        updater.batchEditVisible(true);
        break;
      case batchOptionType.remove:
        confirmDelete(filter(state.selectedKeys, (item) => item !== currentUserId));
        break;
      case batchOptionType.authorize:
        updater.batchAuthorizeVisible(true);
        break;
      default:
        break;
    }
  };

  const handleGenOrgInviteCode = () => {
    genOrgInviteCode().then((result) => {
      const verifyCode = get(result, 'verifyCode');
      if (verifyCode) {
        updater.verifyCode(verifyCode as any);
        updater.inviteModalVisible(true);
      } else {
        message.error(i18n.t('cmp:cannot generate invitation code temporarily'));
      }
    });
  };

  const onSearchMembers = React.useCallback(
    ({ query, queryRole, label }: { query: string; queryRole: string; label: string[] }) => {
      updater.queryParams({ ...state.queryParams, q: query, roles: [queryRole], pageNo: 1, label });
    },
    [state.queryParams, updater],
  );

  const handleCloseEditModal = React.useCallback(() => {
    updater.editMember(null);
    updater.batchEditVisible(false);
  }, [updater]);

  const updateRole = React.useCallback(
    (data: { roles: string[] }) => {
      let userIds = [];
      if (state.batchEditVisible) {
        userIds = state.selectedKeys;
      } else {
        const { userId } = state.editMember as IMember;
        userIds = [userId];
      }
      updateMembers(
        { userIds, scope, ...data },
        { isSelf: userIds.includes(currentUserId), queryParams: state.queryParams },
      );
      handleCloseEditModal();
    },
    [
      handleCloseEditModal,
      currentUserId,
      scope,
      state.batchEditVisible,
      state.editMember,
      state.queryParams,
      state.selectedKeys,
      updateMembers,
    ],
  );

  const handleBatchAuthorize = ({ applications, roles }: { applications: number[]; roles: string[] }) => {
    updateMembers({
      scope: { id: params.projectId, type: MemberScope.PROJECT },
      userIds: state.selectedKeys,
      roles,
      targetScopeType: MemberScope.APP,
      targetScopeIds: applications,
    });
    updater.batchAuthorizeVisible(false);
  };

  const fieldsList = React.useMemo(() => {
    return [
      {
        label: i18n.t('role'),
        name: 'roles',
        type: 'select',
        itemProps: {
          mode: 'multiple',
        },
        options: () => {
          return map(roleMap, (name, value) => (
            <Option key={value} value={value}>
              {name}
            </Option>
          ));
        },
      },
      ...insertWhen(scope.type === MemberScope.ORG && !isEmpty(memberLabels), [
        {
          label: i18n.t('member label'),
          name: 'labels',
          type: 'select',
          required: false,
          itemProps: {
            mode: 'multiple',
            placeholder: i18n.t('select member label'),
          },
          options: map(memberLabels, (item) => ({ name: item.name, value: item.label })),
        },
      ]),
    ];
  }, [memberLabels, roleMap, scope.type]);

  const confirmDelete = React.useCallback(
    (user: IMember | string[], isSelf?: boolean) => {
      const { projectId, orgName: currentOrgName } = params;
      let title = '' as any;
      let userIds = [] as string[];
      if (isArray(user)) {
        title = i18n.t('common:confirm to remove user in batches');
        userIds = user;
      } else {
        title = isSelf
          ? `{name}, ${i18n.t('common:Are you sure to quit?')}?`
          : `${i18n.t('common:confirm to remove user')}{name}?`;
        const [prev, after] = title.split('{name}');
        title = (
          <span>
            {prev} <b>{`${user.nick} (${user.name || i18n.t('common:none')})`}</b> {after}
          </span>
        );
        userIds = [user.userId];
      }
      Modal.confirm({
        title,
        onOk: () => {
          removeMember({ userIds, scope }, {
            ...state.queryParams,
            pageNo: paging.pageNo,
            pageSize: paging.pageSize,
          } as Omit<MEMBER.GetListQuery, 'scope'>).then(() => {
            isArray(user) && updater.selectedKeys([]);
            // if remove other users, still stay current page
            if (!isSelf) {
              return;
            }
            if (scope?.type === 'msp') {
              goTo(goTo.resolve.mspProjects());
            }
            if (scope?.type === 'org') {
              location.href = goTo.resolve.orgRoot({ orgName: '-' });
            }
            if (scope?.type === 'project') {
              goTo(goTo.resolve.dopRoot({ orgName: currentOrgName }), { replace: true });
            }
            if (scope?.type === 'app') {
              goTo(goTo.resolve.projectApps({ projectId, orgName: currentOrgName }), { replace: true });
            }
          });
        },
      });
    },
    [paging.pageNo, paging.pageSize, params, removeMember, scope, state.queryParams, updater],
  );

  const columns = React.useMemo(
    () =>
      [
        {
          title: i18n.t('nickname'),
          dataIndex: 'nick',
          render: (nick: string, record: IMember) => {
            const { userId, removed } = record;
            return (
              <div
                className="member-username nowrap"
                title={`${nick} ${(currentUserId === userId && i18n.t('current user')) || ''} ${
                  (removed && i18n.t('exit the organization')) || ''
                }`}
              >
                <span>
                  <Avatar src={record.avatar} size="small" className="mr-1">
                    {getAvatarChars(nick || i18n.t('none'))}
                  </Avatar>
                  {nick || i18n.t('common:none')}
                </span>
                <IF check={currentUserId === userId}>
                  <span className="member-username-info"> [{i18n.t('current user')}]</span>
                </IF>
                <IF check={removed}>
                  <span className="member-username-info"> [{i18n.t('exit the organization')}]</span>
                </IF>
              </div>
            );
          },
        },
        {
          title: i18n.t('user name'),
          dataIndex: 'name',
          render: (name: string) => {
            return (
              <div className="member-username nowrap" title={name || i18n.t('common:none')}>
                <span>{name || i18n.t('common:none')}</span>
              </div>
            );
          },
        },
        {
          title: 'Email',
          dataIndex: 'email',
          hidden: true,
          render: (value: string) => (
            <Tooltip title={value}>
              <span className="cursor-copy" data-clipboard-tip="Email" data-clipboard-text={value}>
                {value || i18n.t('common:none')}
              </span>
            </Tooltip>
          ),
        },
        {
          title: i18n.t('cellphone'),
          dataIndex: 'mobile',
          render: (value: string | number) => (
            <span className="cursor-copy" data-clipboard-tip={i18n.t('cellphone')} data-clipboard-text={value}>
              {value || '-'}
            </span>
          ),
        },
        {
          title: i18n.t('role'),
          dataIndex: 'roles',
          render: (roles: string[]) => {
            const rolesStr = map(roles, (role) => roleMap[role] || i18n.t('common:other')).join(',');
            return rolesStr ? (
              <div className="members-list-role-operate nowrap">
                <Tooltip title={rolesStr}>
                  <span className="role-tag">{rolesStr}</span>
                </Tooltip>
              </div>
            ) : (
              '-'
            );
          },
        },
        ...insertWhen(scope.type === MemberScope.ORG, [
          {
            title: i18n.t('member label'),
            dataIndex: 'labels',
            hidden: true,
            render: (val: string[]) => {
              const curLabels = map(val, (item) => {
                const labelObj = find(memberLabels, { label: item }) || { name: item, label: item };
                return (
                  <div className="members-list-label-item" key={labelObj.label}>
                    {labelObj.name}
                  </div>
                );
              });
              return (
                <div className="members-list-label nowrap">
                  <Tooltip
                    title={curLabels}
                    getPopupContainer={(triggerNode: unknown) => get(triggerNode, 'parentElement')}
                  >
                    {curLabels}
                  </Tooltip>
                </div>
              );
            },
          },
        ]),
      ] as Array<ColumnProps<IMember>>,
    [currentUserId, memberLabels, roleMap, scope.type],
  );

  const tableActions: IActions<IMember> = {
    render: (record: IMember) => {
      const { userId, removed, labels } = record;
      const isCurrentUser = currentUserId === userId;

      const ops = [];

      if (!!updateMembers && !removed) {
        ops.push({
          title: i18n.t('edit'),
          onClick: () => updater.editMember({ ...record, labels: labels || [] }),
        });
      }

      if (showAuthorize && memberAuth.showAuthorize) {
        ops.push({
          title: i18n.t('authorize'),
          onClick: () => updater.authorizeMember(record),
        });
      }

      if (isCurrentUser || memberAuth.delete || isAdminManager) {
        ops.push({
          title: isCurrentUser ? i18n.t('exit') : i18n.t('remove'),
          onClick: () => confirmDelete(record, isCurrentUser),
        });
      }

      return ops;
    },
  };

  const filterList = React.useMemo(
    () => [
      {
        name: 'query',
        placeholder: i18n.t('search by nickname, username, email or mobile number'),
        style: { width: '260px' },
      },
      {
        type: 'select',
        name: 'queryRole',
        placeholder: i18n.t('select role'),
        allowClear: true,
        options: map(roleMap, (name, value) => ({ name, value })),
      },
      ...insertWhen(scope.type === MemberScope.ORG && !isEmpty(memberLabels), [
        {
          name: 'label',
          type: 'select',
          placeholder: i18n.t('select member label'),
          allowClear: true,
          mode: 'multiple',
          options: map(memberLabels, (item) => ({ name: item.name, value: item.label })),
        },
      ]),
    ],
    [memberLabels, roleMap, scope.type],
  );

  const memoTable = () => {
    const onChangePage = (no: number, size: number) => {
      updater.queryParams({ ...state.queryParams, pageNo: no, pageSize: size });
    };
    return (
      <Table
        slot={<FilterGroup list={filterList} onChange={debounce(onSearchMembers, 1000)} />}
        rowKey={'userId'}
        rowSelection={
          hideRowSelect
            ? undefined
            : {
                selectedRowKeys: state.selectedKeys,
                onChange: onTableSelectChange,
                actions: hideBatchOps ? null : batchOptions,
              }
        }
        rowClassName={(record: IMember) => (record.removed ? 'not-allowed' : '')}
        pagination={{ ...paging, current: paging.pageNo, onChange: onChangePage }}
        columns={columns}
        dataSource={list}
        actions={!readOnly ? tableActions : null}
      />
    );
  };

  return (
    <div className="member-table-manage">
      <div className={buttonInCard ? 'flex justify-between mb-2 mr-2' : 'top-button-group'}>
        {topContent}
        <div>
          {(memberAuth.add || isAdminManager) && !readOnly ? (
            <Button type="primary" onClick={() => updater.addModalVisible(true)}>
              {i18n.t('add member')}
            </Button>
          ) : null}
          {memberAuth.invite && !readOnly ? (
            <Button loading={getInviteLoading} onClick={handleGenOrgInviteCode}>
              {i18n.t('invite')}
            </Button>
          ) : null}
        </div>
      </div>
      <Spin spinning={getLoading || removeLoading || addLoading || updateLoading}>
        <div className="member-table-manage-list">
          <AddMemberModal
            scope={scope}
            visible={state.addModalVisible}
            roleMap={roleMap}
            hasConfigAppAuth={hasConfigAppAuth}
            memberLabels={memberLabels}
            queryParams={state.queryParams}
            toggleModal={() => updater.addModalVisible(false)}
          />
          <FormModal
            title={
              state.batchEditVisible
                ? i18n.t('common:batch set the role of member')
                : i18n.t('common:set the role of member {user}', { user: state.editMember && state.editMember.nick })
            }
            alertProps={
              state.batchEditVisible
                ? {
                    message: i18n.t(
                      "common:The user's original permissions will be overwritten after batch processing. Please be cautious.",
                    ),
                    type: 'warning',
                    showIcon: true,
                    className: 'mb-2',
                  }
                : undefined
            }
            formData={state.editMember}
            fieldsList={fieldsList}
            visible={!!state.editMember || state.batchEditVisible}
            onOk={updateRole}
            onCancel={handleCloseEditModal}
          />
          <IF check={scopeKey !== MemberScope.SYS}>
            <BatchAuthorizeMemberModal
              projectId={params.projectId}
              visible={state.batchAuthorizeVisible}
              onOk={handleBatchAuthorize}
              onCancel={() => updater.batchAuthorizeVisible(false)}
              alertProps={{
                message: i18n.t(
                  "common:The user's original permissions will be overwritten after batch processing. Please be cautious.",
                ),
                type: 'warning',
                showIcon: true,
                className: 'mb-2',
              }}
            />
            <AuthorizeMemberModal
              key={state.authorizeMember ? 'show' : 'hidden'} // 关闭后销毁
              type={scope.type}
              member={state.authorizeMember as IMember | null}
              closeModal={() => updater.authorizeMember(null)}
            />
            <UrlInviteModal
              visible={state.inviteModalVisible}
              url={`${
                window.location.origin.endsWith('erda.cloud') ? FULL_ROOT_DOMAIN : window.location.origin
              }${goTo.resolve.inviteToOrg()}`}
              linkPrefixTip={`${i18n.t('cmp:visit the link to join the organization')} [${orgDisplayName || orgName}]`}
              code={state.verifyCode}
              tip={i18n.t(
                'cmp:You can share the link to QQ, WeChat, DingTalk and other work groups, and colleagues can join the organization through this link.',
              )}
              onCancel={() => updater.inviteModalVisible(false)}
              modalProps={{ width: 600 }}
            />
          </IF>
          <div className="members-list">
            {memoTable()}
            <Copy selector=".cursor-copy" />
          </div>
        </div>
      </Spin>
    </div>
  );
};

export default MembersTable;
