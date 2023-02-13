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

import { FormModal, MemberSelector, LoadMoreSelector } from 'common';
import i18n from 'i18n';
import React from 'react';
import { map, isEmpty } from 'lodash';
import { MemberScope } from 'common/stores/member-scope';
import projectMemberStore from 'common/stores/project-member';
import orgMemberStore from 'common/stores/org-member';
import appMemberStore from 'common/stores/application-member';
import sysMemberStore from 'common/stores/sys-member';
import { insertWhen } from '../../utils';
import { getApps } from 'common/services';
import { useMount } from 'react-use';
import { Alert, message } from 'antd';
import mspProjectMember from 'common/stores/msp-project-member';

interface IProps {
  visible: boolean;
  roleMap: object;
  memberLabels: Array<{ name: string; label: string }>;
  scope: MEMBER.MemberScope;
  queryParams: Obj;
  hasConfigAppAuth?: boolean;
  toggleModal: () => void;
  updateMember?: (values: MEMBER.UpdateMemberBody) => void;
}

const storeMap = {
  [MemberScope.PROJECT]: projectMemberStore,
  [MemberScope.ORG]: orgMemberStore,
  [MemberScope.APP]: appMemberStore,
  [MemberScope.SYS]: sysMemberStore,
  [MemberScope.MSP]: mspProjectMember,
};

export const AddMemberModal = ({
  scope,
  roleMap,
  visible,
  memberLabels,
  hasConfigAppAuth,
  toggleModal,
  queryParams,
}: IProps) => {
  const memberStore = storeMap[scope.type];
  const { addMembers, updateMembers } = memberStore.effects;
  const handleSubmit = (values: MEMBER.UpdateMemberBody) => {
    const { app_roles, applications, ...rest } = values as any;
    addMembers({ ...rest, scope }, { queryParams }).then(() => {
      if (scope.type === MemberScope.PROJECT && !isEmpty(applications)) {
        // 添加到项目后才给应用授权
        updateMembers(
          {
            scope,
            userIds: rest.userIds,
            roles: app_roles,
            targetScopeType: MemberScope.APP,
            targetScopeIds: applications,
          },
          {
            successMsg: false,
          },
        );
      }
    });
    toggleModal();
  };

  const checkData = (values: any) => {
    const rolesIsEmpty = isEmpty(values.app_roles);
    const appsIsEmpty = isEmpty(values.applications);
    if ((rolesIsEmpty && !appsIsEmpty) || (!rolesIsEmpty && appsIsEmpty)) {
      message.warn(i18n.t('common:Application and application roles need to be set at the same time'));
      return null;
    }
    return values;
  };

  const appRoleMap = appMemberStore.useStore((s) => s.roleMap);

  // 在项目级添加成员后，还需要给应用级授权角色
  useMount(
    () => scope.type === MemberScope.PROJECT && appMemberStore.effects.getRoleMap({ scopeType: MemberScope.APP }),
  );

  const _getApps = (q: any) => {
    return getApps({ ...q }).then((res: any) => res.data);
  };

  const fieldList = [
    {
      label: i18n.t('member'),
      name: 'userIds',
      required: true,
      getComp: () => {
        return <MemberSelector.Add mode="multiple" scopeType={scope.type} />;
      },
    },
    {
      label: i18n.t('role'),
      name: 'roles',
      type: 'select',
      itemProps: {
        mode: 'multiple',
        placeholder: i18n.t('please select role for member'),
      },
      options: map(roleMap, (v: string, k: string) => ({ name: v, value: k })),
    },
    ...insertWhen(!isEmpty(memberLabels) && scope.type === MemberScope.ORG, [
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
    ...insertWhen(!!hasConfigAppAuth && scope.type === MemberScope.PROJECT, [
      {
        getComp: () => (
          <Alert showIcon type="info" message={i18n.t('common:You can set application-level roles at the same time')} />
        ),
      },
      {
        label: i18n.t('application'),
        name: 'applications',
        type: 'custom',
        required: false,
        getComp: () => (
          <LoadMoreSelector
            getData={_getApps}
            mode="multiple"
            placeholder={i18n.t('dop:please select application')}
            extraQuery={{ projectId: scope.id }}
            dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
              total,
              list: map(list, (application) => {
                const { name, id } = application;
                return {
                  ...application,
                  label: name,
                  value: id,
                };
              }),
            })}
          />
        ),
      },
      {
        label: i18n.t('application role'),
        name: 'app_roles',
        type: 'select',
        required: false,
        itemProps: {
          mode: 'multiple',
          placeholder: i18n.t('dop:please set'),
        },
        options: [...map(appRoleMap, (v: string, k: string) => ({ name: v, value: k }))],
      },
    ]),
  ];

  return (
    <FormModal
      title={i18n.t('add member')}
      fieldsList={fieldList}
      visible={visible}
      beforeSubmit={checkData}
      keepValue
      onOk={handleSubmit}
      onCancel={() => toggleModal()}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    />
  );
};
