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
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { getLoggedInUserPermissions } from 'app/pages/MainPage/slice/thunks';
import { RootState } from 'types';
import { request2 } from 'utils/request';
import {
  AddSourceParams,
  DeleteSourceParams,
  EditSourceParams,
  Source,
  UnarchiveSourceParams,
} from './types';

export const getSources = createAsyncThunk<Source[], string>(
  'source/getSources',
  async orgId => {
    const { data } = await request2<Source[]>({
      url: '/sources',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const getArchivedSources = createAsyncThunk<Source[], string>(
  'source/getArchivedSources',
  async orgId => {
    const { data } = await request2<Source[]>({
      url: '/sources/archived',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const getSource = createAsyncThunk<Source, string>(
  'source/getSource',
  async id => {
    const { data } = await request2<Source>(`/sources/${id}`);
    return data;
  },
);

export const addSource = createAsyncThunk<
  Source,
  AddSourceParams,
  { state: RootState }
>('source/addSource', async ({ source, resolve }, { getState, dispatch }) => {
  const { data } = await request2<Source>({
    url: '/sources',
    method: 'POST',
    data: source,
  });

  // FIXME 拥有Read权限等级的扁平结构资源新增后需要更新权限字典；后续如改造为目录结构则删除该逻辑
  const orgId = selectOrgId(getState());
  await dispatch(getLoggedInUserPermissions(orgId));

  resolve(data.id);
  return data;
});

export const editSource = createAsyncThunk<Source, EditSourceParams>(
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
  async ({ id, resolve }) => {
    await request2<boolean>({
      url: `/sources/unarchive/${id}`,
      method: 'PUT',
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
