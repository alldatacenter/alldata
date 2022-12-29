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
import { request2 } from 'utils/request';
import {
  AddVariableParams,
  DeleteVariableParams,
  EditVariableParams,
  Variable,
} from './types';

export const getVariables = createAsyncThunk<Variable[], string>(
  'variable/getVariables',
  async orgId => {
    const { data } = await request2<Variable[]>({
      url: '/variables/org',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const addVariable = createAsyncThunk<Variable, AddVariableParams>(
  'variable/addVariable',
  async ({ variable, resolve }) => {
    const { data } = await request2<Variable>({
      url: '/variables',
      method: 'POST',
      data: variable,
    });
    resolve();
    return data;
  },
);

export const editVariable = createAsyncThunk<Variable, EditVariableParams>(
  'variable/editVariable',
  async ({ variable, resolve }) => {
    await request2<boolean>({
      url: '/variables',
      method: 'PUT',
      data: [variable],
    });
    resolve();
    return variable;
  },
);

export const deleteVariable = createAsyncThunk<null, DeleteVariableParams>(
  'variable/deleteVariable',
  async ({ ids, resolve }) => {
    await request2<Variable>({
      url: '/variables',
      method: 'DELETE',
      params: { variables: ids.join(',') },
    });
    resolve();
    return null;
  },
);
