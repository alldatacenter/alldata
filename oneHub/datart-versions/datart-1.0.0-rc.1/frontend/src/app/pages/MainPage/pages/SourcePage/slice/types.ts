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

export interface SourceState {
  sources: Source[];
  archived: Source[];
  editingSource: string;
  sourceListLoading: boolean;
  archivedListLoading: boolean;
  sourceDetailLoading: boolean;
  saveSourceLoading: boolean;
  unarchiveSourceLoading: boolean;
  deleteSourceLoading: boolean;
  syncSourceSchemaLoading: boolean;
}

export interface Source {
  config: string;
  createBy: string;
  createTime: string;
  id: string;
  name: string;
  orgId: string;
  status: number;
  type: string;
  updateBy: string;
  updateTime: string;
  schemaUpdateDate: string;
}

export interface SourceFormModel extends Pick<Source, 'name' | 'type'> {
  id?: string;
  config: object;
}

export interface AddSourceParams {
  source: Pick<Source, 'name' | 'type' | 'orgId' | 'config'>;
  resolve: (redirectId: string) => void;
}

export interface EditSourceParams {
  source: Source;
  resolve: () => void;
  reject?: () => void;
}

export interface UnarchiveSourceParams {
  id: string;
  resolve: () => void;
}

export interface DeleteSourceParams {
  id: string;
  archive?: boolean;
  resolve: () => void;
}
