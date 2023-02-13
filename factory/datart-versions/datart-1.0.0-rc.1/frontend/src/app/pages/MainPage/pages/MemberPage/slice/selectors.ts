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

import { createSelector } from '@reduxjs/toolkit';
import { RootState } from 'types';
import { initialState } from '.';

const selectDomain = (state: RootState) => state.member || initialState;

export const selectMembers = createSelector(
  [selectDomain],
  memberState => memberState.members,
);

export const selectRoles = createSelector(
  [selectDomain],
  memberState => memberState.roles,
);

export const selectEditingMember = createSelector(
  [selectDomain],
  memberState => memberState.editingMember,
);

export const selectEditingRole = createSelector(
  [selectDomain],
  memberState => memberState.editingRole,
);

export const selectMemberListLoading = createSelector(
  [selectDomain],
  memberState => memberState.memberListLoading,
);

export const selectInviteMemberLoading = createSelector(
  [selectDomain],
  memberState => memberState.inviteMemberLoading,
);

export const selectGetMemberRolesLoading = createSelector(
  [selectDomain],
  memberState => memberState.getMemberRolesLoading,
);

export const selectSaveMemberLoading = createSelector(
  [selectDomain],
  memberState => memberState.saveMemberLoading,
);

export const selectRemoveMemberLoading = createSelector(
  [selectDomain],
  memberState => memberState.removeMemberLoading,
);

export const selectRoleListLoading = createSelector(
  [selectDomain],
  memberState => memberState.roleListLoading,
);

export const selectRoleDetailLoading = createSelector(
  [selectDomain],
  memberState => memberState.roleDetailLoading,
);

export const selectSaveRoleLoading = createSelector(
  [selectDomain],
  memberState => memberState.saveRoleLoading,
);

export const selectDeleteRoleLoading = createSelector(
  [selectDomain],
  memberState => memberState.deleteRoleLoading,
);

export const selectGetRoleMembersLoading = createSelector(
  [selectDomain],
  memberState => memberState.getRoleMembersLoading,
);

export const selectGrantOwnerLoading = createSelector(
  [selectDomain],
  memberState => memberState.grantOwnerLoading,
);
