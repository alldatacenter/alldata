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

import i18n from 'i18next';
import {
  PermissionLevels,
  ResourceTypes,
  Viewpoints,
  VizResourceSubTypes,
} from './constants';
import { DataSourceViewModel } from './slice/types';

export function getDefaultPermissionArray(vizSubTypes?: VizResourceSubTypes) {
  return vizSubTypes !== VizResourceSubTypes.Storyboard
    ? [
        PermissionLevels.Disable,
        PermissionLevels.Disable,
        PermissionLevels.Disable,
        PermissionLevels.Disable,
      ]
    : [
        PermissionLevels.Disable,
        PermissionLevels.Disable,
        PermissionLevels.Disable,
      ];
}

export function generateRootNode(
  type: ResourceTypes,
  vizId?: VizResourceSubTypes,
): DataSourceViewModel {
  return {
    id: type === ResourceTypes.Viz ? (vizId as string) : (type as string),
    name: i18n.t('permission.allResources'),
    type,
    parentId: null,
    index: null,
    isFolder: true,
    permissionArray: getDefaultPermissionArray(vizId),
  };
}

export function getInverseViewpoints(viewpoint: Viewpoints) {
  return viewpoint === Viewpoints.Resource
    ? Viewpoints.Subject
    : Viewpoints.Resource;
}
