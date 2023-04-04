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
import { RootState } from 'types';
import { request2 } from 'utils/request';
import {
  DeleteSourceParams,
  EditSourceParams,
  SourceBase,
  SourceParamsResolve,
  SourceSimple,
  UnarchiveSourceParams,
  UpdateSourceBaseParams,
} from './types';

export const getSources = createAsyncThunk<SourceSimple[], string>(
  'source/getSources',
  async orgId => {
    const { data } = await request2<SourceSimple[]>({
      url: '/sources',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const getArchivedSources = createAsyncThunk<SourceSimple[], string>(
  'source/getArchivedSources',
  async orgId => {
    const { data } = await request2<SourceSimple[]>({
      url: '/sources/archived',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const getSource = createAsyncThunk<SourceSimple, string>(
  'source/getSource',
  async id => {
    const { data } = await request2<SourceSimple>(`/sources/${id}`);
    return data;
  },
);

export const addSource = createAsyncThunk<
  SourceSimple,
  SourceParamsResolve,
  { state: RootState }
>('source/addSource', async ({ source, resolve }, { getState, dispatch }) => {
  const { data } = await request2<SourceSimple>({
    url: '/sources',
    method: 'POST',
    data: source,
  });

  resolve(data.id);
  return data;
});

export const editSource = createAsyncThunk<SourceSimple, EditSourceParams>(
  'source/editSource',
  async ({ source, resolve, reject }) => {
    await request2<boolean>(
      {
        url: `/sources/${source.id}`,
        method: 'PUT',
        data: source,
      },
      undefined,
      {
        onRejected(error) {
          reject && reject();
        },
      },
    );
    resolve();
    return source;
  },
);

export const unarchiveSource = createAsyncThunk<null, UnarchiveSourceParams>(
  'source/unarchiveSource',
  async ({ source, resolve }) => {
    await request2<boolean>({
      url: `/sources/unarchive/${source.id}`,
      method: 'PUT',
      params: source,
    });
    resolve();
    return null;
  },
);

export const deleteSource = createAsyncThunk<null, DeleteSourceParams>(
  'source/deleteSource',
  async ({ id, archive, resolve }) => {
    await request2<boolean>({
      url: `/sources/${id}`,
      method: 'DELETE',
      params: { archive },
    });
    resolve();
    return null;
  },
);

export const syncSourceSchema = createAsyncThunk<null, { sourceId: string }>(
  'source/syncSourceSchema',
  async ({ sourceId }) => {
    const { data } = await request2<any>({
      url: `/sources/sync/schemas/${sourceId}`,
      method: 'GET',
    });
    return data;
  },
);

export const updateSourceBase = createAsyncThunk<
  SourceBase,
  UpdateSourceBaseParams
>('source/updateSourceBase', async ({ source, resolve }) => {
  await request2<SourceBase>({
    url: `/sources/${source.id}/base`,
    method: 'PUT',
    data: source,
  });
  resolve();
  return source;
});
