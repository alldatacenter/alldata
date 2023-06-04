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

import { TreeNodeProps } from 'antd';
import { ReactElement } from 'react';

export interface SourceState {
  sources: SourceSimpleViewModel[];
  archived: SourceSimpleViewModel[];
  editingSource: string;
  sourceListLoading: boolean;
  archivedListLoading: boolean;
  sourceDetailLoading: boolean;
  saveSourceLoading: boolean;
  unarchiveSourceLoading: boolean;
  deleteSourceLoading: boolean;
  syncSourceSchemaLoading: boolean;
  updateLoading: boolean;
}

export interface Source {
  config: string;
  createBy?: string;
  createTime?: string;
  id: string;
  name: string;
  orgId: string;
  status?: number;
  type: string;
  index: number | null;
  updateBy?: string;
  updateTime?: string;
  permission?: number;
  schemaUpdateDate?: string;
}

export interface SelectSourceTreeProps {
  getIcon: (
    o: SourceSimpleViewModel,
  ) => ReactElement | ((props: TreeNodeProps) => ReactElement);
  getDisabled: (o: SourceSimpleViewModel) => boolean;
}

export interface SelectSourceFolderTreeProps {
  id?: string;
  getDisabled: (o: SourceSimpleViewModel, path: string[]) => boolean;
}

export interface UpdateSourceBaseParams {
  source: SourceBase;
  resolve: () => void;
}

export interface SourceBase {
  id: string;
  name: string;
  parentId: string | null;
  index: number | null;
}

export interface SourceSimple extends Source {
  isFolder: boolean;
  parentId: string | null;
}

export interface SourceSimpleViewModel extends SourceSimple {
  deleteLoading: boolean;
}

export interface SourceFormModel
  extends Pick<
    SourceSimple,
    'isFolder' | 'name' | 'type' | 'parentId' | 'orgId' | 'index'
  > {
  config: object;
}

export interface AddSourceParams {
  config: string;
  parentId: string | null;
  index: number | null;
  orgId: string;
  isFolder: boolean;
  id?: string | undefined;
  name: string;
  type?: string;
}

export interface SourceParamsResolve {
  source: AddSourceParams;
  resolve: (redirectId: string) => void;
}

export interface EditSourceParams {
  source: SourceSimple;
  resolve: () => void;
  reject?: () => void;
}

export interface UnarchiveSourceParams {
  source: SourceBase;
  resolve: () => void;
}

export interface DeleteSourceParams {
  id: string;
  archive?: boolean;
  resolve: () => void;
}
