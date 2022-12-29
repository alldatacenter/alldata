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
import { getDataProviderDatabases } from 'app/pages/MainPage/slice/thunks';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import { ViewViewModelStages } from '../constants';
import {
  diffMergeHierarchyModel,
  transformQueryResultToModelAndDataSource,
} from '../utils';
import {
  deleteView,
  getArchivedViews,
  getSchemaBySourceId,
  getViewDetail,
  getViews,
  runSql,
  saveFolder,
  saveView,
  unarchiveView,
  updateViewBase,
} from './thunks';
import { Schema, ViewState, ViewViewModel } from './types';

export const initialState: ViewState = {
  views: void 0,
  archived: void 0,
  viewListLoading: false,
  archivedListLoading: false,
  editingViews: [],
  currentEditingView: '',
  sourceDatabases: {},
  sourceDatabaseSchema: {},
  saveViewLoading: false,
  unarchiveLoading: false,
  databaseSchemaLoading: false,
};

const slice = createSlice({
  name: 'view',
  initialState,
  reducers: {
    addEditingView(state, action: PayloadAction<ViewViewModel>) {
      state.editingViews.push(action.payload);
      state.currentEditingView = action.payload.id;
    },
    removeEditingView(state, action: PayloadAction<string>) {
      state.editingViews = state.editingViews.filter(
        v => v.id !== action.payload,
      );
      if (state.currentEditingView === action.payload) {
        if (state.editingViews.length > 0) {
          state.currentEditingView =
            state.editingViews[state.editingViews.length - 1].id;
        } else {
          state.currentEditingView = '';
        }
      }
    },
    closeOtherEditingViews(state, action: PayloadAction<string>) {
      state.editingViews = state.editingViews.filter(
        v => v.id === action.payload,
      );
      state.currentEditingView =
        state.editingViews[state.editingViews.length - 1].id || '';
    },
    closeAllEditingViews(state) {
      state.editingViews = [];
      state.currentEditingView = '';
    },
    switchCurrentEditingView(state, action: PayloadAction<string>) {
      state.currentEditingView = action.payload;
    },
    changeCurrentEditingView(
      state,
      action: PayloadAction<Partial<ViewViewModel>>,
    ) {
      const currentEditingView = state.editingViews.find(
        v => v.id === state.currentEditingView,
      );

      if (currentEditingView) {
        const entries = Object.entries(action.payload);
        entries.forEach(([key, value]) => {
          currentEditingView[key] = value;
        });
        if (
          !(
            entries.length === 1 && ['fragment', 'size'].includes(entries[0][0])
          )
        ) {
          currentEditingView.touched = true;
          if (
            ['model', 'variables', 'columnPermissions'].includes(entries[0][0])
          ) {
            currentEditingView.stage = ViewViewModelStages.Saveable;
          } else {
            if (currentEditingView.stage > ViewViewModelStages.Fresh) {
              currentEditingView.stage = ViewViewModelStages.Initialized;
            }
          }
        }
      }
    },
    initCurrentEditingStructViewScript(
      state,
      action: PayloadAction<Partial<ViewViewModel>>,
    ) {
      const currentEditingView = state.editingViews.find(
        v => v.id === state.currentEditingView,
      );

      if (currentEditingView) {
        const entries = Object.entries(action.payload);
        entries.forEach(([key, value]) => {
          currentEditingView[key] = value;
        });
      }
    },
    addTables(
      state,
      action: PayloadAction<{
        sourceId: string;
        databaseName: string;
        tables: string[];
      }>,
    ) {
      const { sourceId, databaseName, tables } = action.payload;
      const database = state.sourceDatabases[sourceId].find(
        ({ key }) => key === databaseName,
      );
      if (database) {
        database.children = tables.map(name => ({
          key: [databaseName, name].join(String.fromCharCode(0)),
          title: name,
          value: [databaseName, name],
        }));
      }
    },
    addSchema(
      state,
      action: PayloadAction<{
        sourceId: string;
        databaseName: string;
        tableName: string;
        schema: Schema[];
      }>,
    ) {
      const { sourceId, databaseName, tableName, schema } = action.payload;
      const database = state.sourceDatabases[sourceId].find(
        ({ key }) => key === databaseName,
      );
      if (database && database.children) {
        const table = database.children.find(
          ({ key }) =>
            (key as string).split(String.fromCharCode(0))[1] === tableName,
        );
        if (table) {
          table.children = schema.map(column => ({
            key: [databaseName, tableName, column.name].join(
              String.fromCharCode(0),
            ),
            title: column.name,
            value: column,
            isLeaf: true,
          }));
        }
      }
    },
    clear(state) {
      Object.entries(initialState).forEach(([key, value]) => {
        state[key] = value;
      });
    },
  },
  extraReducers: builder => {
    // getViews
    builder.addCase(getViews.pending, state => {
      state.viewListLoading = true;
    });
    builder.addCase(getViews.fulfilled, (state, action) => {
      state.viewListLoading = false;
      state.views = action.payload.map(v => ({ ...v, deleteLoading: false }));
    });
    builder.addCase(getViews.rejected, state => {
      state.viewListLoading = false;
    });

    // getArchivedViews
    builder.addCase(getArchivedViews.pending, state => {
      state.archivedListLoading = true;
    });
    builder.addCase(getArchivedViews.fulfilled, (state, action) => {
      state.archivedListLoading = false;
      state.archived = action.payload.map(v => ({
        ...v,
        deleteLoading: false,
      }));
    });
    builder.addCase(getArchivedViews.rejected, state => {
      state.archivedListLoading = false;
    });

    // getViewDetail
    builder.addCase(getViewDetail.fulfilled, (state, action) => {
      const index = state.editingViews.findIndex(
        v => v.id === action.meta.arg.viewId,
      );
      const loadedEditingView: ViewViewModel = {
        ...action.payload,
        stage:
          action.payload.stage === ViewViewModelStages.Loading
            ? ViewViewModelStages.Fresh
            : action.payload.stage,
      };
      state.editingViews.splice(index, 1, loadedEditingView);
    });
    builder.addCase(getViewDetail.rejected, (state, action) => {
      const view = state.editingViews.find(
        v => v.id === action.meta.arg.viewId,
      ) as ViewViewModel;
      view.stage = ViewViewModelStages.NotLoaded;
      view.error = action.payload as string;
    });

    // runSql
    builder.addCase(runSql.pending, (state, action) => {
      const currentEditingView = state.editingViews.find(
        v => v.id === action.meta.arg.id,
      );
      if (currentEditingView) {
        currentEditingView.stage = ViewViewModelStages.Running;
        currentEditingView.error = '';
      }
    });
    builder.addCase(runSql.fulfilled, (state, action) => {
      const currentEditingView = state.editingViews.find(
        v => v.id === action.meta.arg.id,
      );

      if (currentEditingView && action.payload && action.payload.rows) {
        const { model, dataSource } = transformQueryResultToModelAndDataSource(
          action.payload,
          currentEditingView.model,
          currentEditingView.type,
        );
        currentEditingView.model = diffMergeHierarchyModel(
          model,
          currentEditingView.type!,
        );
        currentEditingView.previewResults = dataSource;
        if (!action.meta.arg.isFragment) {
          currentEditingView.stage = ViewViewModelStages.Saveable;
        } else {
          currentEditingView.stage = ViewViewModelStages.Initialized;
        }

        if (action.payload.warnings) {
          currentEditingView.warnings = action.payload.warnings;
        }
      }
    });

    // saveView
    builder.addCase(saveView.pending, (state, action) => {
      const currentEditingView = state.editingViews.find(
        v => v.id === state.currentEditingView,
      );
      const isSaveAs = action.meta.arg.isSaveAs;
      if (currentEditingView && !isSaveAs) {
        currentEditingView.stage = ViewViewModelStages.Saving;
      }
    });
    builder.addCase(saveView.fulfilled, (state, action) => {
      if (!action.payload.isSaveAs) {
        const editingIndex = state.editingViews.findIndex(
          v => v.id === state.currentEditingView,
        );
        state.editingViews.splice(editingIndex, 1, {
          ...action.payload,
          touched: false,
          stage: ViewViewModelStages.Saved,
          originVariables: [...action.payload.variables],
          originColumnPermissions: [...action.payload.columnPermissions],
        });
        state.currentEditingView = action.payload.id;
      }

      if (state.views) {
        const treeIndex = state.views.findIndex(
          n => n.id === action.payload.id,
        );
        const {
          description = '',
          id,
          index = 0,
          isFolder = false,
          name,
          parentId = '',
          sourceId = '',
        } = action.payload;
        if (treeIndex >= 0) {
          state.views.splice(treeIndex, 1, {
            ...state.views[treeIndex],
            name,
            parentId,
          });
        } else {
          state.views.unshift({
            description,
            id,
            index,
            isFolder,
            name,
            parentId,
            sourceId,
            deleteLoading: false,
          });
        }
      }
    });
    builder.addCase(saveView.rejected, state => {
      const currentEditingView = state.editingViews.find(
        v => v.id === state.currentEditingView,
      );
      if (currentEditingView) {
        currentEditingView.stage = ViewViewModelStages.Saveable;
      }
    });

    // saveFolder
    builder.addCase(saveFolder.pending, state => {
      state.saveViewLoading = true;
    });
    builder.addCase(saveFolder.fulfilled, (state, action) => {
      state.saveViewLoading = false;
      state.views?.unshift({ ...action.payload, deleteLoading: false });
    });
    builder.addCase(saveFolder.rejected, state => {
      state.saveViewLoading = false;
    });

    // updateViewBase
    builder.addCase(updateViewBase.pending, state => {
      state.saveViewLoading = true;
    });
    builder.addCase(updateViewBase.fulfilled, (state, action) => {
      state.saveViewLoading = false;
      state.views = state.views?.map(v =>
        v.id === action.payload.id ? { ...v, ...action.payload } : v,
      );
      const editing = state.editingViews.find(
        ({ id }) => id === action.payload.id,
      );
      if (editing) {
        Object.entries(action.payload).forEach(([key, value]) => {
          editing[key] = value;
        });
      }
    });
    builder.addCase(updateViewBase.rejected, state => {
      state.saveViewLoading = false;
    });

    // unarchiveView
    builder.addCase(unarchiveView.pending, state => {
      state.unarchiveLoading = true;
    });
    builder.addCase(unarchiveView.fulfilled, (state, action) => {
      state.unarchiveLoading = false;
      state.archived = state.archived?.filter(
        ({ id }) => id !== action.meta.arg.view.id,
      );
    });
    builder.addCase(unarchiveView.rejected, state => {
      state.unarchiveLoading = false;
    });

    // deleteView
    builder.addCase(deleteView.pending, (state, action) => {
      const view =
        state.views?.find(({ id }) => id === action.meta.arg.id) ||
        state.archived?.find(({ id }) => id === action.meta.arg.id);
      if (view) {
        view.deleteLoading = true;
      }
    });
    builder.addCase(deleteView.fulfilled, (state, action) => {
      state.views = state.views?.filter(v => v.id !== action.meta.arg.id);
      state.archived = state.archived?.filter(v => v.id !== action.meta.arg.id);
    });
    builder.addCase(deleteView.rejected, (state, action) => {
      const view =
        state.views?.find(({ id }) => id === action.meta.arg.id) ||
        state.archived?.find(({ id }) => id === action.meta.arg.id);
      if (view) {
        view.deleteLoading = false;
      }
    });

    // getDataProviderDatabases
    builder.addCase(getDataProviderDatabases.fulfilled, (state, action) => {
      state.sourceDatabases[action.meta.arg] = action.payload.map(name => ({
        key: name,
        title: name,
        value: [name],
      }));
    });

    // getSchemaBySourceId
    builder.addCase(getSchemaBySourceId.pending, state => {
      state.databaseSchemaLoading = true;
    });
    builder.addCase(getSchemaBySourceId.rejected, state => {
      state.databaseSchemaLoading = false;
    });
    builder.addCase(getSchemaBySourceId.fulfilled, (state, action) => {
      state.databaseSchemaLoading = false;
      if (!action.payload?.data?.schemaItems) {
        return;
      }
      state.sourceDatabaseSchema[action.payload?.sourceId] =
        action.payload.data.schemaItems;
    });
  },
});

export const { actions: viewActions, reducer } = slice;

export const useViewSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
