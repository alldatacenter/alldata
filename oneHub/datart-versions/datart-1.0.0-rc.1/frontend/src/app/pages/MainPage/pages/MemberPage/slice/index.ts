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

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import {
  addRole,
  deleteMember,
  deleteRole,
  editRole,
  getMemberRoles,
  getMembers,
  getRoleMembers,
  getRoles,
  grantOwner,
  inviteMember,
  removeMember,
  revokeOwner,
  saveMember,
} from './thunks';
import { MemberState } from './types';

export const initialState: MemberState = {
  members: [],
  roles: [],
  editingMember: null,
  editingRole: null,
  memberListLoading: false,
  roleListLoading: false,
  inviteMemberLoading: false,
  getMemberRolesLoading: false,
  saveMemberLoading: false,
  removeMemberLoading: false,
  roleDetailLoading: false,
  saveRoleLoading: false,
  deleteRoleLoading: false,
  getRoleMembersLoading: false,
  grantOwnerLoading: false,
};

const slice = createSlice({
  name: 'member',
  initialState,
  reducers: {
    initEditingMember(state, action: PayloadAction<string>) {
      const user = state.members.find(m => m.id === action.payload);
      if (user) {
        state.editingMember = { info: user, roles: [] };
      }
    },
    clearEditingMember(state) {
      state.editingMember = null;
    },
    initEditingRole(state, action: PayloadAction<string>) {
      const role = state.roles.find(r => r.id === action.payload);
      if (role) {
        state.editingRole = { info: role, members: [] };
      }
    },
    clearEditingRole(state) {
      state.editingRole = null;
    },
  },
  extraReducers: builder => {
    // getMembers
    builder.addCase(getMembers.pending, state => {
      state.memberListLoading = true;
    });
    builder.addCase(getMembers.fulfilled, (state, action) => {
      state.memberListLoading = false;
      state.members = action.payload;
    });
    builder.addCase(getMembers.rejected, state => {
      state.memberListLoading = false;
    });

    // inviteMember
    builder.addCase(inviteMember.pending, state => {
      state.inviteMemberLoading = true;
    });
    builder.addCase(inviteMember.fulfilled, (state, action) => {
      state.inviteMemberLoading = false;
    });
    builder.addCase(inviteMember.rejected, state => {
      state.inviteMemberLoading = false;
    });

    // getMemberRoles
    builder.addCase(getMemberRoles.pending, state => {
      state.getMemberRolesLoading = true;
    });
    builder.addCase(getMemberRoles.fulfilled, (state, action) => {
      state.getMemberRolesLoading = false;
      if (state.editingMember) {
        state.editingMember.roles = action.payload;
      }
    });
    builder.addCase(getMemberRoles.rejected, state => {
      state.getMemberRolesLoading = false;
    });

    // saveMember
    builder.addCase(saveMember.pending, state => {
      state.saveMemberLoading = true;
    });
    builder.addCase(saveMember.fulfilled, (state, action) => {
      state.saveMemberLoading = false;
      if (action.payload) {
        const index = state.members.findIndex(m => m.id === action.payload!.id);
        if (index >= 0) {
          state.members[index] = action.payload;
        } else {
          state.members.push(action.payload);
        }
      }
    });
    builder.addCase(saveMember.rejected, state => {
      state.saveMemberLoading = false;
    });

    // removeMember
    builder.addCase(removeMember.pending, state => {
      state.removeMemberLoading = true;
    });
    builder.addCase(removeMember.fulfilled, (state, action) => {
      state.removeMemberLoading = false;
      state.members = state.members.filter(m => m.id !== action.meta.arg.id);
    });
    builder.addCase(removeMember.rejected, state => {
      state.removeMemberLoading = false;
    });

    // deleteMember
    builder.addCase(deleteMember.pending, state => {
      state.removeMemberLoading = true;
    });
    builder.addCase(deleteMember.fulfilled, (state, action) => {
      state.removeMemberLoading = false;
      state.members = state.members.filter(m => m.id !== action.meta.arg.id);
    });
    builder.addCase(deleteMember.rejected, state => {
      state.removeMemberLoading = false;
    });

    // getRoles
    builder.addCase(getRoles.pending, state => {
      state.roleListLoading = true;
    });
    builder.addCase(getRoles.fulfilled, (state, action) => {
      state.roleListLoading = false;
      state.roles = action.payload;
    });
    builder.addCase(getRoles.rejected, state => {
      state.roleListLoading = false;
    });

    // addRole
    builder.addCase(addRole.pending, state => {
      state.saveRoleLoading = true;
    });
    builder.addCase(addRole.fulfilled, (state, action) => {
      state.saveRoleLoading = false;
      state.roles.push(action.payload);
    });
    builder.addCase(addRole.rejected, state => {
      state.saveRoleLoading = false;
    });

    // editRole
    builder.addCase(editRole.pending, state => {
      state.saveRoleLoading = true;
    });
    builder.addCase(editRole.fulfilled, (state, action) => {
      state.saveRoleLoading = false;
      if (action.payload) {
        state.roles = state.roles.map(r =>
          r.id === action.payload!.id ? action.payload! : r,
        );
      }
    });
    builder.addCase(editRole.rejected, state => {
      state.saveRoleLoading = false;
    });

    // deleteRole
    builder.addCase(deleteRole.pending, state => {
      state.deleteRoleLoading = true;
    });
    builder.addCase(deleteRole.fulfilled, (state, action) => {
      state.deleteRoleLoading = false;
      state.roles = state.roles.filter(r => r.id !== action.meta.arg.id);
    });
    builder.addCase(deleteRole.rejected, state => {
      state.deleteRoleLoading = false;
    });

    // getRoleMembers
    builder.addCase(getRoleMembers.pending, state => {
      state.getRoleMembersLoading = true;
    });
    builder.addCase(getRoleMembers.fulfilled, (state, action) => {
      state.getRoleMembersLoading = false;
      if (state.editingRole) {
        state.editingRole.members = action.payload;
      }
    });
    builder.addCase(getRoleMembers.rejected, state => {
      state.getRoleMembersLoading = false;
    });

    // grantOwner
    builder.addCase(grantOwner.pending, state => {
      state.grantOwnerLoading = true;
    });
    builder.addCase(grantOwner.fulfilled, (state, action) => {
      state.grantOwnerLoading = false;
      const member = state.members.find(
        ({ id }) => id === action.meta.arg.userId,
      );
      if (member) {
        member.orgOwner = true;
      }
      if (state.editingMember?.info) {
        state.editingMember.info.orgOwner = true;
      }
    });
    builder.addCase(grantOwner.rejected, state => {
      state.grantOwnerLoading = false;
    });

    // revokeOwner
    builder.addCase(revokeOwner.pending, state => {
      state.grantOwnerLoading = true;
    });
    builder.addCase(revokeOwner.fulfilled, (state, action) => {
      state.grantOwnerLoading = false;
      const member = state.members.find(
        ({ id }) => id === action.meta.arg.userId,
      );
      if (member) {
        member.orgOwner = false;
      }
      if (state.editingMember?.info) {
        state.editingMember.info.orgOwner = false;
      }
    });
    builder.addCase(revokeOwner.rejected, state => {
      state.grantOwnerLoading = false;
    });
  },
});

export const { actions: memberActions, reducer } = slice;

export const useMemberSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
