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
import { listToTree } from 'utils/utils';
import { initialState } from '.';
import { ResourceTypes } from '../../PermissionPage/constants';
import { SelectSourceFolderTreeProps, SelectSourceTreeProps } from './types';

const selectDomain = (state: RootState) => state.source || initialState;

export const selectSources = createSelector(
  [selectDomain],
  sourceState => sourceState.sources,
);

export const makeSelectSourceTree = () =>
  createSelector(
    [
      selectSources,
      (_, props: SelectSourceTreeProps) => props.getIcon,
      (_, props: SelectSourceTreeProps) => props.getDisabled,
    ],
    (sources, getIcon, getDisabled) =>
      listToTree(sources, null, [ResourceTypes.Source], {
        getIcon,
        getDisabled,
      }),
  );

export const makeSelectSourceFolderTree = () =>
  createSelector(
    [
      selectSources,
      (_, props: SelectSourceFolderTreeProps) => props.id,
      (_, props: SelectSourceFolderTreeProps) => props.getDisabled,
    ],
    (sources, id, getDisabled) =>
      listToTree(
        sources && sources.filter(v => v.isFolder && v.id !== id),
        null,
        [ResourceTypes.Source],
        { getDisabled },
      ),
  );

export const selectArchived = createSelector(
  [selectDomain],
  sourceState => sourceState.archived,
);

export const selectEditingSourceKey = createSelector(
  [selectDomain],
  sourceState => sourceState.editingSource,
);

export const selectEditingSource = createSelector([selectDomain], sourceState =>
  sourceState.sources
    .concat(sourceState.archived)
    .find(({ id }) => sourceState.editingSource === id),
);

export const selectSourceListLoading = createSelector(
  [selectDomain],
  sourceState => sourceState.sourceListLoading,
);

export const selectArchivedListLoading = createSelector(
  [selectDomain],
  sourceState => sourceState.archivedListLoading,
);

export const selectSaveSourceLoading = createSelector(
  [selectDomain],
  sourceState => sourceState.saveSourceLoading,
);

export const selectUnarchiveSourceLoading = createSelector(
  [selectDomain],
  sourceState => sourceState.unarchiveSourceLoading,
);

export const selectDeleteSourceLoading = createSelector(
  [selectDomain],
  sourceState => sourceState.deleteSourceLoading,
);

export const selectSyncSourceSchemaLoading = createSelector(
  [selectDomain],
  sourceState => sourceState.syncSourceSchemaLoading,
);
