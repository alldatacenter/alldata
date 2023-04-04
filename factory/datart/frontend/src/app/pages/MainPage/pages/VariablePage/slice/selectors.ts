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

const selectDomain = (state: RootState) => state.variable || initialState;

export const selectVariables = createSelector(
  [selectDomain],
  variableState => variableState.variables,
);

export const selectVariableListLoading = createSelector(
  [selectDomain],
  variableState => variableState.variableListLoading,
);

export const selectSaveVariableLoading = createSelector(
  [selectDomain],
  variableState => variableState.saveVariableLoading,
);

export const selectDeleteVariablesLoading = createSelector(
  [selectDomain],
  variableState => variableState.deleteVariablesLoading,
);
