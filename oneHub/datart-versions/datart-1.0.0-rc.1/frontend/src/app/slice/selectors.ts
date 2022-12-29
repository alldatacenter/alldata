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

const selectDomain = (state: RootState) => state.app || initialState;

export const selectLoggedInUser = createSelector(
  [selectDomain],
  appState => appState.loggedInUser,
);

export const selectSystemInfo = createSelector(
  [selectDomain],
  appState => appState.systemInfo,
);

export const selectSetupLoading = createSelector(
  [selectDomain],
  appState => appState.setupLoading,
);

export const selectLoginLoading = createSelector(
  [selectDomain],
  appState => appState.loginLoading,
);

export const selectRegisterLoading = createSelector(
  [selectDomain],
  appState => appState.registerLoading,
);

export const selectSaveProfileLoading = createSelector(
  [selectDomain],
  appState => appState.saveProfileLoading,
);

export const selectModifyPasswordLoading = createSelector(
  [selectDomain],
  appState => appState.modifyPasswordLoading,
);

export const selectOauth2Clients = createSelector(
  [selectDomain],
  appState => appState.oauth2Clients,
);
