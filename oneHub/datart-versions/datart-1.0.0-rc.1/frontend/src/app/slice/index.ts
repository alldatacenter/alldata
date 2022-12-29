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
  getOauth2Clients,
  getSystemInfo,
  getUserInfoByToken,
  login,
  logout,
  modifyAccountPassword,
  register,
  saveProfile,
  setLoggedInUser,
  setup,
} from './thunks';
import { AppState, User } from './types';

export const initialState: AppState = {
  loggedInUser: null,
  systemInfo: null,
  setupLoading: false,
  loginLoading: false,
  registerLoading: false,
  saveProfileLoading: false,
  modifyPasswordLoading: false,
  oauth2Clients: [],
};

const slice = createSlice({
  name: 'app',
  initialState,
  reducers: {
    updateUser(state, { payload }: PayloadAction<User>) {
      state.loggedInUser = payload;
    },
  },
  extraReducers: builder => {
    // setup
    builder.addCase(setup.pending, state => {
      state.setupLoading = true;
    });
    builder.addCase(setup.fulfilled, (state, action) => {
      state.setupLoading = false;
      state.systemInfo!.initialized = true;
    });
    builder.addCase(setup.rejected, state => {
      state.setupLoading = false;
    });

    // login
    builder.addCase(login.pending, state => {
      state.loginLoading = true;
    });
    builder.addCase(login.fulfilled, (state, action) => {
      state.loginLoading = false;
      state.loggedInUser = action.payload;
    });
    builder.addCase(login.rejected, state => {
      state.loginLoading = false;
    });

    // getUserInfoByToken
    builder.addCase(getUserInfoByToken.fulfilled, (state, action) => {
      if (action.payload) {
        state.loggedInUser = action.payload;
      }
    });

    // register
    builder.addCase(register.pending, state => {
      state.registerLoading = true;
    });
    builder.addCase(register.fulfilled, (state, action) => {
      state.registerLoading = false;
    });
    builder.addCase(register.rejected, state => {
      state.registerLoading = false;
    });

    // setLoggedInUser
    builder.addCase(
      setLoggedInUser.fulfilled,
      (state, action: PayloadAction<User>) => {
        state.loggedInUser = action.payload;
      },
    );

    // logout
    builder.addCase(logout.fulfilled, state => {
      state.loggedInUser = null;
    });

    // saveProfile
    builder.addCase(saveProfile.pending, state => {
      state.saveProfileLoading = true;
    });
    builder.addCase(saveProfile.fulfilled, (state, action) => {
      state.saveProfileLoading = false;
      state.loggedInUser = action.payload;
    });
    builder.addCase(saveProfile.rejected, state => {
      state.saveProfileLoading = false;
    });

    // modifyAccountPassword
    builder.addCase(modifyAccountPassword.pending, state => {
      state.modifyPasswordLoading = true;
    });
    builder.addCase(modifyAccountPassword.fulfilled, state => {
      state.modifyPasswordLoading = false;
    });
    builder.addCase(modifyAccountPassword.rejected, state => {
      state.modifyPasswordLoading = false;
    });

    // getSystemInfo
    builder.addCase(getSystemInfo.fulfilled, (state, action) => {
      state.systemInfo = action.payload;
    });

    builder.addCase(getOauth2Clients.fulfilled, (state, action) => {
      state.oauth2Clients = action.payload.map(x => ({
        name: Object.keys(x)[0],
        value: x[Object.keys(x)[0]],
      }));
    });
  },
});

export const { actions: appActions, reducer } = slice;

export const useAppSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
