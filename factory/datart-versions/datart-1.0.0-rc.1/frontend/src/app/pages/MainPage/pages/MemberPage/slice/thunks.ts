/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createAsyncThunk } from '@reduxjs/toolkit';
import { User } from 'app/slice/types';
import omit from 'lodash/omit';
import { RootState } from 'types';
import { request2 } from 'utils/request';
import { selectEditingMember, selectEditingRole } from './selectors';
import {
  AddRoleParams,
  DeleteRoleParams,
  EditRoleParams,
  GrantOwnerParams,
  InviteMemberParams,
  RemoveMemberParams,
  Role,
  SaveMemberParams,
} from './types';

export const getMembers = createAsyncThunk<User[], string>(
  'member/getMembers',
  async orgId => {
    const result = await request2<User[]>(`/orgs/${orgId}/members`);
    return result.data;
  },
);

export const inviteMember = createAsyncThunk<null, InviteMemberParams>(
  'member/inviteMember',
  async ({ params: { orgId, emails, sendMail }, resolve }) => {
    const { data } = await request2<{ success: string[]; fail: string[] }>({
      url: `/orgs/${orgId}/invite`,
      method: 'POST',
      data: emails,
      params: { sendMail },
    });
    resolve(data);
    return null;
  },
);

export const getMemberRoles = createAsyncThunk<
  Role[],
  { orgId: string; memberId: string }
>('member/getMemberRoles', async ({ orgId, memberId }) => {
  const { data } = await request2<Role[]>({
    url: `/orgs/${orgId}/members/${memberId}/roles`,
  });
  return data;
});

export const saveMember = createAsyncThunk<
  User | null,
  SaveMemberParams,
  { state: RootState }
>(
  'member/editMember',
  async ({ type, orgId, roleIds, resolve, ...memberInfo }, { getState }) => {
    const editingMember = selectEditingMember(getState());
    if (type === 'add') {
      const { data } = await request2<User>({
        url: `/users/${orgId}/addUser`,
        method: 'POST',
        data: { roleIds, ...memberInfo },
      });
      resolve();
      return data;
    } else if (type === 'edit') {
      if (editingMember) {
        await request2<User>({
          url: `/users/${orgId}/updateUser`,
          method: 'PUT',
          data: { roleIds, ...memberInfo },
        });
        resolve();
        return { ...editingMember.info, ...memberInfo };
      }
    } else {
      if (editingMember) {
        const { info, roles: originRoles } = editingMember;
        const originRoleIds = originRoles.map(({ id }) => id);
        await Promise.all([
          // TODO user attributes update
          originRoleIds.sort().join() !== roleIds.sort().join()
            ? request2<boolean>({
                url: `/roles/${info.id}/roles`,
                method: 'PUT',
                params: { orgId },
                data: roleIds,
              })
            : null,
        ]);
        resolve();
      }
    }
    return null;
  },
);

export const removeMember = createAsyncThunk<null, RemoveMemberParams>(
  'member/removeMember',
  async ({ id, orgId, resolve }) => {
    await request2<boolean>({
      url: `/orgs/${orgId}/members/${id}`,
      method: 'DELETE',
    });
    resolve();
    return null;
  },
);

export const deleteMember = createAsyncThunk<null, RemoveMemberParams>(
  'member/deleteMember',
  async ({ id, orgId, resolve }) => {
    await request2<boolean>({
      url: `/users/${orgId}/deleteUser`,
      method: 'DELETE',
      params: { userId: id },
    });
    resolve();
    return null;
  },
);

export const getRoles = createAsyncThunk<Role[], string>(
  'member/getRoles',
  async orgId => {
    const { data } = await request2<Role[]>(`/orgs/${orgId}/roles`);
    return data;
  },
);

export const addRole = createAsyncThunk<Role, AddRoleParams>(
  'member/addRole',
  async ({ role, resolve }) => {
    const { data } = await request2<Role>({
      url: '/roles',
      method: 'POST',
      data: role,
    });
    resolve();
    return data;
  },
);

export const editRole = createAsyncThunk<
  Role | null,
  EditRoleParams,
  { state: RootState }
>('member/editRole', async ({ role, members, resolve }, { getState }) => {
  const editingRole = selectEditingRole(getState());
  if (editingRole) {
    const { info, members: originMembers } = editingRole;
    const mergedRole = { ...info, ...role };
    const originMemberIds = originMembers.map(({ id }) => id);
    const memberIds = members.map(({ id }) => id);

    await Promise.all([
      request2<boolean>({
        url: `/roles/${info.id}`,
        method: 'PUT',
        data: omit(mergedRole, 'orgId'),
      }),
      originMemberIds.sort().join() !== memberIds.sort().join()
        ? request2<boolean>({
            url: `/roles/${info.id}/users`,
            method: 'PUT',
            data: memberIds,
          })
        : null,
    ]);

    resolve();
    return mergedRole;
  }
  return null;
});

export const deleteRole = createAsyncThunk<null, DeleteRoleParams>(
  'member/deleteRole',
  async ({ id, resolve }) => {
    await request2<boolean>({
      url: `/roles/${id}`,
      method: 'DELETE',
    });
    resolve();
    return null;
  },
);

export const getRoleMembers = createAsyncThunk<User[], string>(
  'member/getRoleMembers',
  async roleId => {
    const { data } = await request2<User[]>({
      url: `/roles/${roleId}/users`,
    });
    return data;
  },
);

export const grantOwner = createAsyncThunk<null, GrantOwnerParams>(
  'member/grantOwner',
  async ({ orgId, userId, resolve }) => {
    await request2<boolean>({
      url: '/roles/permission/grant/org_owner',
      method: 'POST',
      params: { orgId, userId },
    });
    resolve();
    return null;
  },
);

export const revokeOwner = createAsyncThunk<null, GrantOwnerParams>(
  'member/revokeOwner',
  async ({ orgId, userId, resolve }) => {
    await request2<boolean>({
      url: '/roles/permission/revoke/org_owner',
      method: 'DELETE',
      params: { orgId, userId },
    });
    resolve();
    return null;
  },
);
