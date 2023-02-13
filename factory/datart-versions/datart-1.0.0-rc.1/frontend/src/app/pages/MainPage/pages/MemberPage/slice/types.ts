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

import { User } from 'app/slice/types';

export interface MemberState {
  members: User[];
  roles: Role[];
  editingMember: null | {
    info: User;
    roles: Role[];
  };
  editingRole: null | {
    info: Role;
    members: User[];
  };
  memberListLoading: boolean;
  roleListLoading: boolean;
  inviteMemberLoading: boolean;
  getMemberRolesLoading: boolean;
  saveMemberLoading: boolean;
  removeMemberLoading: boolean;
  roleDetailLoading: boolean;
  saveRoleLoading: boolean;
  deleteRoleLoading: boolean;
  getRoleMembersLoading: boolean;
  grantOwnerLoading: boolean;
}

export interface InviteMemberParams {
  params: {
    orgId: string;
    emails: string[];
    sendMail: boolean;
  };
  resolve: ({ success, fail }: { success: string[]; fail: string[] }) => void;
}

export interface SaveMemberParams {
  type: 'add' | 'edit' | 'update';
  orgId: string;
  username: string;
  email: string;
  password: string;
  name: string;
  description: string;
  roleIds: string[];
  resolve: () => void;
}

export interface RemoveMemberParams {
  id: string;
  orgId: string;
  resolve: () => void;
}

export interface Role {
  avatar: string;
  description: string;
  id: string;
  name: string;
  orgId: string;
}

export interface RoleViewModel extends Role {
  members: User[];
}

export interface AddRoleParams {
  role: Omit<Role, 'id'>;
  resolve: () => void;
}

export interface EditRoleParams {
  role: Partial<Role>;
  members: User[];
  resolve: () => void;
}

export interface DeleteRoleParams {
  id: string;
  resolve: () => void;
}

export interface GrantOwnerParams {
  orgId: string;
  userId: string;
  resolve: () => void;
}
