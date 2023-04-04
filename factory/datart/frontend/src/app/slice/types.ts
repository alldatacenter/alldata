import { TenantManagementMode } from 'app/constants';

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
export interface AppState {
  loggedInUser: null | User;
  systemInfo: null | SystemInfo;
  setupLoading: boolean;
  loginLoading: boolean;
  registerLoading: boolean;
  saveProfileLoading: boolean;
  modifyPasswordLoading: boolean;
  oauth2Clients: Array<{ name: string; value: string }>;
}

export interface User {
  id: string;
  username: string;
  email: string;
  avatar: string;
  name: string | null;
  description: string;
  orgOwner?: boolean;
  timezone?: string;
}

export interface SystemInfo {
  initialized: boolean;
  mailEnable: boolean;
  registerEnable: boolean;
  tenantManagementMode: TenantManagementMode;
  tokenTimeout: string;
  version: string;
}

export interface ModifyUserPassword {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface SetupParams {
  params: {
    email: string;
    username: string;
    password: string;
    name?: string;
  };
  resolve: () => void;
}

export interface LoginParams {
  params: {
    username: string;
    password: string;
  };
  resolve: () => void;
}

export interface UserInfoByTokenParams {
  token: string;
  resolve: () => void;
  reject: () => void;
}

export interface RegisterParams {
  data: {
    email: string;
    username: string;
    password: string;
  };
  resolve: () => void;
}

export type LogoutParams = undefined | (() => void);

export interface SaveProfileParams {
  user: User;
  resolve: () => void;
}

export interface ModifyPasswordParams {
  params: Omit<ModifyUserPassword, 'confirmPassword'>;
  resolve: () => void;
}
