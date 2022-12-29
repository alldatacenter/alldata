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
import { initialState } from '.';
import { RootState } from '../../../../types';

const selectDomain = (state: RootState) => state.share || initialState;

export const selectShareVizType = createSelector(
  [selectDomain],
  shareState => shareState.vizType,
);

export const selectChartPreview = createSelector(
  [selectDomain],
  shareState => shareState.chartPreview,
);

export const selectHeadlessBrowserRenderSign = createSelector(
  [selectDomain],
  shareState => shareState.headlessBrowserRenderSign,
);
export const selectPageWidthHeight = createSelector(
  [selectDomain],
  shareState => shareState.pageWidthHeight,
);

export const selectShareDownloadPolling = createSelector(
  [selectDomain],
  shareState => shareState.shareDownloadPolling,
);

export const selectShareExecuteToken = createSelector(
  [selectDomain],
  shareState => shareState.executeToken,
);

export const selectShareExecuteTokenMap = createSelector(
  [selectDomain],
  shareState => shareState.executeTokenMap,
);

export const selectSubVizTokenMap = createSelector(
  [selectDomain],
  shareState => shareState.subVizTokenMap,
);

export const selectNeedVerify = createSelector(
  [selectDomain],
  shareState => shareState.needVerify,
);

export const selectSharePassword = createSelector(
  [selectDomain],
  shareState => shareState.sharePassword,
);

export const selectLoginLoading = createSelector(
  [selectDomain],
  shareState => shareState.loginLoading,
);

export const selectOauth2Clients = createSelector(
  [selectDomain],
  appState => appState.oauth2Clients,
);

export const selectAvailableSourceFunctions = createSelector(
  [selectDomain],
  shareState => shareState.availableSourceFunctions,
);

export const selectSelectedItems = createSelector(
  [selectDomain],
  shareState => shareState.selectedItems,
);

export const selectPageTitle = createSelector(
  [selectDomain],
  shareState => shareState.title,
);
