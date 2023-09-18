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

const selectDomain = (state: RootState) => state.main || initialState;

export const selectOrganizations = createSelector(
  [selectDomain],
  mainState => mainState.organizations,
);

export const selectUserSettings = createSelector(
  [selectDomain],
  mainState => mainState.userSettings,
);

export const selectOrgId = createSelector(
  [selectDomain],
  mainState => mainState.orgId,
);

export const selectCurrentOrganization = createSelector(
  [selectOrganizations, selectOrgId],
  (organizations, orgId) => organizations.find(({ id }) => id === orgId),
);

export const selectIsOrgOwner = createSelector(
  [selectDomain],
  mainState => mainState.isOwner,
);

export const selectPermissionMap = createSelector(
  [selectDomain],
  mainState => mainState.permissionMap,
);

export const selectUserSettingLoading = createSelector(
  [selectDomain],
  mainState => mainState.userSettingLoading,
);

export const selectDataProviders = createSelector(
  [selectDomain],
  mainState => mainState.dataProviders,
);

export const selectOrganizationListLoading = createSelector(
  [selectDomain],
  mainState => mainState.organizationListLoading,
);

export const selectDataProviderListLoading = createSelector(
  [selectDomain],
  mainState => mainState.dataProviderListLoading,
);

export const selectDataProviderConfigTemplateLoading = createSelector(
  [selectDomain],
  mainState => mainState.dataProviderConfigTemplateLoading,
);

export const selectDataProviderDatabaseListLoading = createSelector(
  [selectDomain],
  mainState => mainState.dataProviderDatabaseListLoading,
);

export const selectSaveOrganizationLoading = createSelector(
  [selectDomain],
  mainState => mainState.saveOrganizationLoading,
);

export const selectDeleteOrganizationLoading = createSelector(
  [selectDomain],
  mainState => mainState.deleteOrganizationLoading,
);

export const selectDownloadManagement = createSelector(
  [selectDomain],
  mainState => mainState.downloadManagement,
);

export const selectInitializationError = createSelector(
  [selectDomain],
  mainState => mainState.initializationError,
);

export const selectDownloadPolling = createSelector(
  [selectDomain],
  mainState => mainState.downloadPolling,
);
