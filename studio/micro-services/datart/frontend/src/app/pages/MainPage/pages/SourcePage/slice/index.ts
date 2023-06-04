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

import { createSlice } from '@reduxjs/toolkit';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import {
  addSource,
  deleteSource,
  editSource,
  getArchivedSources,
  getSource,
  getSources,
  syncSourceSchema,
  unarchiveSource,
  updateSourceBase,
} from './thunks';
import { SourceState } from './types';

export const initialState: SourceState = {
  sources: [],
  archived: [],
  editingSource: '',
  sourceListLoading: false,
  archivedListLoading: false,
  sourceDetailLoading: false,
  saveSourceLoading: false,
  unarchiveSourceLoading: false,
  deleteSourceLoading: false,
  syncSourceSchemaLoading: false,
  updateLoading: false,
};

const slice = createSlice({
  name: 'source',
  initialState,
  reducers: {
    clearEditingSource(state) {
      state.editingSource = '';
    },
  },
  extraReducers: builder => {
    // getSources
    builder.addCase(getSources.pending, state => {
      state.sourceListLoading = true;
    });
    builder.addCase(getSources.fulfilled, (state, action) => {
      state.sourceListLoading = false;
      state.sources = action.payload.map(v => ({ ...v, deleteLoading: false }));
    });
    builder.addCase(getSources.rejected, state => {
      state.sourceListLoading = false;
    });

    // getArchivedSources
    builder.addCase(getArchivedSources.pending, state => {
      state.archivedListLoading = true;
    });
    builder.addCase(getArchivedSources.fulfilled, (state, action) => {
      state.archivedListLoading = false;
      state.archived = action.payload.map(v => ({
        ...v,
        deleteLoading: false,
      }));
    });
    builder.addCase(getArchivedSources.rejected, state => {
      state.archivedListLoading = false;
    });

    // getSource
    builder.addCase(getSource.pending, state => {
      state.sourceDetailLoading = true;
    });
    builder.addCase(getSource.fulfilled, (state, action) => {
      state.sourceDetailLoading = false;
      state.sources = state.sources.map(s =>
        s.id === action.payload.id
          ? { ...action.payload, deleteLoading: false }
          : s,
      );
      state.editingSource = action.payload.id;
    });
    builder.addCase(getSource.rejected, state => {
      state.sourceDetailLoading = false;
    });

    // addSource
    builder.addCase(addSource.pending, state => {
      state.saveSourceLoading = true;
    });
    builder.addCase(addSource.fulfilled, (state, action) => {
      state.saveSourceLoading = false;
      state.sources.unshift({ ...action.payload, deleteLoading: false });
    });
    builder.addCase(addSource.rejected, state => {
      state.saveSourceLoading = false;
    });

    // editSource
    builder.addCase(editSource.pending, state => {
      state.saveSourceLoading = true;
    });
    builder.addCase(editSource.fulfilled, (state, action) => {
      state.saveSourceLoading = false;
      state.sources = state.sources.map(s =>
        s.id === action.payload.id
          ? { ...action.payload, deleteLoading: false }
          : s,
      );
    });
    builder.addCase(editSource.rejected, state => {
      state.saveSourceLoading = false;
    });

    // unarchiveSource
    builder.addCase(unarchiveSource.pending, state => {
      state.unarchiveSourceLoading = true;
    });
    builder.addCase(unarchiveSource.fulfilled, (state, action) => {
      state.unarchiveSourceLoading = false;
      state.archived = state.archived.filter(
        s => s.id !== action.meta.arg.source.id,
      );
    });
    builder.addCase(unarchiveSource.rejected, state => {
      state.unarchiveSourceLoading = false;
    });

    // deleteSource
    builder.addCase(deleteSource.pending, state => {
      state.deleteSourceLoading = true;
    });
    builder.addCase(deleteSource.fulfilled, (state, action) => {
      state.deleteSourceLoading = false;
      state.sources = state.sources.filter(s => s.id !== action.meta.arg.id);
      state.archived = state.archived.filter(s => s.id !== action.meta.arg.id);
    });
    builder.addCase(deleteSource.rejected, state => {
      state.deleteSourceLoading = false;
    });

    // syncSourceSchema
    builder.addCase(syncSourceSchema.pending, state => {
      state.syncSourceSchemaLoading = true;
    });
    builder.addCase(syncSourceSchema.fulfilled, (state, action) => {
      state.syncSourceSchemaLoading = false;
    });
    builder.addCase(syncSourceSchema.rejected, state => {
      state.syncSourceSchemaLoading = false;
    });

    // updateSourceBase
    builder.addCase(updateSourceBase.pending, state => {
      state.updateLoading = true;
    });
    builder.addCase(updateSourceBase.fulfilled, (state, action) => {
      state.updateLoading = false;
      state.sources = state.sources.map(v =>
        v.id === action.payload.id
          ? {
              ...v,
              ...action.payload,
              parentId: action.payload.parentId || null,
              deleteLoading: false,
            }
          : v,
      );
    });
    builder.addCase(updateSourceBase.rejected, state => {
      state.updateLoading = false;
    });
  },
});

export const { actions: sourceActions, reducer } = slice;

export const useSourceSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
