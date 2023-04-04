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
import { StorageKeys } from 'globalConstants';
import { removeToken, setToken, setTokenExpiration } from 'utils/auth';
import { request2 } from 'utils/request';
import { errorHandle } from 'utils/utils';
import { appActions } from '.';
import {
  LoginParams,
  LogoutParams,
  ModifyPasswordParams,
  RegisterParams,
  SaveProfileParams,
  SetupParams,
  SystemInfo,
  User,
  UserInfoByTokenParams,
} from './types';

export const setup = createAsyncThunk<boolean, SetupParams>(
  'app/setup',
  async ({ params, resolve }) => {
    const { data } = await request2<boolean>({
      url: '/sys/setup',
      method: 'POST',
      data: { user: params },
    });
    resolve();
    return data;
  },
);

export const login = createAsyncThunk<User, LoginParams>(
  'app/login',
  async ({ params, resolve }) => {
    const { data } = await request2<User>({
      url: '/users/login',
      method: 'POST',
      data: params,
    });
    localStorage.setItem(StorageKeys.LoggedInUser, JSON.stringify(data));
    resolve();
    return data;
  },
);

export const getUserInfoByToken = createAsyncThunk<
  User | undefined,
  UserInfoByTokenParams
>('app/getUserInfoByToken', async ({ token, resolve, reject }) => {
  setToken(token);
  const { data } = await request2<User>(
    {
      url: '/users',
      method: 'GET',
    },
    undefined,
    {
      onRejected(error) {
        reject();
        removeToken();
      },
    },
  );
  localStorage.setItem(StorageKeys.LoggedInUser, JSON.stringify(data));
  resolve();
  return data;
});

export const register = createAsyncThunk<null, RegisterParams>(
  'app/register',
  async ({ data, resolve }) => {
    await request2<User>({
      url: '/users/register',
      method: 'POST',
      data,
    });
    resolve();
    return null;
  },
);

export const setLoggedInUser = createAsyncThunk<User>(
  'app/setLoggedInUser',
  async () => {
    try {
      const loggedInUser = localStorage.getItem(StorageKeys.LoggedInUser);
      return JSON.parse(loggedInUser as string);
    } catch (error) {
      errorHandle(error);
      throw error;
    }
  },
);

export const logout = createAsyncThunk<undefined, LogoutParams>(
  'app/logout',
  async resolve => {
    try {
      removeToken();
      localStorage.removeItem(StorageKeys.LoggedInUser);
      resolve && resolve();
      return void 0;
    } catch (error) {
      errorHandle(error);
      throw error;
    }
  },
);

export const updateUser = createAsyncThunk<null, User>(
  'app/updateUser',
  async (user, { dispatch }) => {
    try {
      localStorage.setItem(StorageKeys.LoggedInUser, JSON.stringify(user));
      dispatch(appActions.updateUser(user));
      return null;
    } catch (error) {
      errorHandle(error);
      throw error;
    }
  },
);

export const saveProfile = createAsyncThunk<User, SaveProfileParams>(
  'app/saveProfile',
  async ({ user, resolve }) => {
    const loggedInUser = localStorage.getItem(StorageKeys.LoggedInUser) || '{}';
    const merged = { ...JSON.parse(loggedInUser), ...user };
    await request2({
      url: '/users',
      method: 'PUT',
      data: merged,
    });
    resolve();
    localStorage.setItem(StorageKeys.LoggedInUser, JSON.stringify(merged));
    return merged;
  },
);

export const modifyAccountPassword = createAsyncThunk<
  void,
  ModifyPasswordParams
>('app/modifyAccountPassword', async ({ params, resolve }) => {
  await request2({
    url: '/users/change/password',
    method: 'PUT',
    data: params,
  });
  resolve();
});

export const getSystemInfo = createAsyncThunk<SystemInfo>(
  'app/getSystemInfo',
  async () => {
    const { data } = await request2<SystemInfo>('/sys/info');
    // minute -> millisecond
    const tokenTimeout = Number(data.tokenTimeout) * 60 * 1000;
    setTokenExpiration(tokenTimeout);
    return data;
  },
);

export const getOauth2Clients = createAsyncThunk<[]>(
  'app/getOauth2Clients',
  async () => {
    const { data } = await request2<[]>({
      url: '/tpa/getOauth2Clients',
      method: 'GET',
    });
    return data;
  },
);
