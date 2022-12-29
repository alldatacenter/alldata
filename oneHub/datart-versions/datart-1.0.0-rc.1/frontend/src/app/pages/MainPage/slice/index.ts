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
import { ResourceTypes } from '../pages/PermissionPage/constants';
import {
  addOrganization,
  deleteOrganization,
  editOrganization,
  getDataProviderConfigTemplate,
  getDataProviderDatabases,
  getDataProviders,
  getLoggedInUserPermissions,
  getOrganizations,
  getUserSettings,
} from './thunks';
import {
  DownloadManagementStatus,
  DownloadTask,
  DownloadTaskState,
  MainState,
  UserPermissionMap,
  UserSetting,
} from './types';

export const initialState: MainState = {
  userSettings: void 0,
  organizations: [],
  orgId: '',
  dataProviders: {},
  isOwner: false,
  permissionMap: {},
  downloadManagement: {
    status: DownloadManagementStatus.INIT,
    tasks: [],
  },
  userSettingLoading: false,
  organizationListLoading: false,
  dataProviderListLoading: false,
  dataProviderConfigTemplateLoading: false,
  dataProviderDatabaseListLoading: false,
  saveOrganizationLoading: false,
  deleteOrganizationLoading: false,
  initializationError: false,
  downloadPolling: false,
};

const slice = createSlice({
  name: 'main',
  initialState,
  reducers: {
    setOrgId(state, { payload }: PayloadAction<string>) {
      state.orgId = payload;
    },
    setUserSettings(state, { payload }: PayloadAction<UserSetting[]>) {
      state.userSettings = payload;
    },
    setCurrentOrganizationAvatar(state, { payload }: PayloadAction<string>) {
      const currentOrganization = state.organizations.find(
        ({ id }) => id === state.orgId,
      );
      if (currentOrganization) {
        currentOrganization.avatar = payload;
      }
    },
    clear(state) {
      Object.entries(initialState).forEach(([key, value]) => {
        state[key] = value;
      });
    },
    setDownloadManagement(
      state,
      {
        payload,
      }: PayloadAction<{
        newStatus?: DownloadManagementStatus;
        newTasks?: DownloadTask[];
      }>,
    ) {
      let newDownloadStatus = payload?.newStatus;
      const _isNotPendingDownload = status =>
        status !== DownloadTaskState.CREATED;
      if (!newDownloadStatus) {
        const originTasks = state.downloadManagement?.tasks || [];
        const newTasks = payload?.newTasks || [];
        if (!originTasks?.length) {
          newDownloadStatus = DownloadManagementStatus.INIT;
        } else if (originTasks.length !== newTasks.length) {
          newDownloadStatus = DownloadManagementStatus.NEW;
        } else {
          const isAllFinish = originTasks.every(
            ot =>
              newTasks.find(nt => nt.id === ot.id)?.status === ot.status &&
              _isNotPendingDownload(ot.status),
          );
          newDownloadStatus = isAllFinish
            ? DownloadManagementStatus.FINISH
            : DownloadManagementStatus.NEW;
        }
      }
      state.downloadManagement = {
        status: newDownloadStatus,
        tasks: payload?.newTasks || state.downloadManagement?.tasks,
      };
    },
    setDownloadPolling(state, { payload }: PayloadAction<boolean>) {
      state.downloadPolling = payload;
    },
  },
  extraReducers: builder => {
    // getUserSettings
    builder.addCase(getUserSettings.pending, state => {
      state.userSettingLoading = true;
    });
    builder.addCase(getUserSettings.fulfilled, (state, action) => {
      const { organizations, userSettings, orgId } = action.payload;
      state.userSettingLoading = false;
      state.userSettings = userSettings;
      state.organizations = organizations;
      if (orgId) {
        state.orgId = orgId;
      }
    });
    builder.addCase(getUserSettings.rejected, state => {
      state.userSettingLoading = false;
      state.initializationError = true;
    });

    // getOrganizations
    builder.addCase(getOrganizations.pending, state => {
      state.organizationListLoading = true;
    });
    builder.addCase(getOrganizations.fulfilled, (state, action) => {
      state.organizationListLoading = false;
      state.organizations = action.payload;
    });
    builder.addCase(getOrganizations.rejected, state => {
      state.organizationListLoading = false;
    });

    // getLoggedInUserPermissions
    builder.addCase(getLoggedInUserPermissions.fulfilled, (state, action) => {
      state.isOwner = action.payload.orgOwner || false;
      state.permissionMap = Object.values(
        ResourceTypes,
      ).reduce<UserPermissionMap>((map, type) => {
        map[type] = action.payload.permissionInfos
          .filter(({ resourceType }) => resourceType === type)
          .reduce((privilegeObject, { resourceId, permission }) => {
            if (!privilegeObject[resourceId]) {
              privilegeObject[resourceId] = permission;
            }
            privilegeObject[resourceId] =
              privilegeObject[resourceId] | permission;
            return privilegeObject;
          }, {});
        return map;
      }, {});
    });

    // getDataProviders
    builder.addCase(getDataProviders.pending, state => {
      state.dataProviderListLoading = true;
    });
    builder.addCase(getDataProviders.fulfilled, (state, action) => {
      state.dataProviderListLoading = false;
      state.dataProviders = action.payload;
    });
    builder.addCase(getDataProviders.rejected, state => {
      state.dataProviderListLoading = false;
    });

    // getDataProviderConfigTemplate
    builder.addCase(getDataProviderConfigTemplate.pending, state => {
      state.dataProviderConfigTemplateLoading = true;
    });
    builder.addCase(
      getDataProviderConfigTemplate.fulfilled,
      (state, action) => {
        state.dataProviderConfigTemplateLoading = false;
        state.dataProviders[action.meta.arg].config = action.payload;
      },
    );
    builder.addCase(getDataProviderConfigTemplate.rejected, state => {
      state.dataProviderConfigTemplateLoading = false;
    });

    // getDataProviderDatabases
    builder.addCase(getDataProviderDatabases.pending, state => {
      state.dataProviderDatabaseListLoading = true;
    });
    builder.addCase(getDataProviderDatabases.fulfilled, (state, action) => {
      state.dataProviderDatabaseListLoading = false;
    });
    builder.addCase(getDataProviderDatabases.rejected, state => {
      state.dataProviderDatabaseListLoading = false;
    });

    // addOrganization
    builder.addCase(addOrganization.pending, state => {
      state.saveOrganizationLoading = true;
    });
    builder.addCase(addOrganization.fulfilled, (state, action) => {
      const { organization, userSettings } = action.payload;
      state.saveOrganizationLoading = false;
      state.organizations = state.organizations.concat(organization);
      state.userSettings = userSettings;
      state.orgId = organization.id;
    });
    builder.addCase(addOrganization.rejected, state => {
      state.saveOrganizationLoading = false;
    });

    // editOrganization
    builder.addCase(editOrganization.pending, state => {
      state.saveOrganizationLoading = true;
    });
    builder.addCase(editOrganization.fulfilled, (state, action) => {
      state.saveOrganizationLoading = false;
      state.organizations = state.organizations.map(o =>
        o.id === action.payload.id ? { ...o, ...action.payload } : o,
      );
    });
    builder.addCase(editOrganization.rejected, state => {
      state.saveOrganizationLoading = false;
    });

    // deleteOrganization
    builder.addCase(deleteOrganization.pending, state => {
      state.deleteOrganizationLoading = true;
    });
    builder.addCase(deleteOrganization.fulfilled, (state, action) => {
      const { delOrgId, nextOrgId, userSettings } = action.payload;
      state.deleteOrganizationLoading = false;
      state.organizations = state.organizations.filter(
        ({ id }) => id !== delOrgId,
      );
      state.userSettings = userSettings;
      state.orgId = nextOrgId;
    });
    builder.addCase(deleteOrganization.rejected, state => {
      state.deleteOrganizationLoading = false;
    });
  },
});

export const { actions: mainActions, reducer } = slice;

export const useMainSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
