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

import { fastDeleteArrayElement, getDiffParams } from 'utils/utils';
import {
  PermissionLevels,
  ResourceTypes,
  RESOURCE_TYPE_PERMISSION_MAPPING,
  SubjectTypes,
  Viewpoints,
  VizResourceSubTypes,
} from '../../constants';
import {
  DataSourceTreeNode,
  GrantPermissionParams,
  Privilege,
} from '../../slice/types';
import { getDefaultPermissionArray, getInverseViewpoints } from '../../utils';

export function getTreeNodeWithPermission(
  nodes: DataSourceTreeNode[],
  getPermissionFunc: (
    node: Omit<DataSourceTreeNode, 'children'>,
    parentPermissionArray: PermissionLevels[],
  ) => PermissionLevels[],
  parentPermissionArray: PermissionLevels[],
): DataSourceTreeNode[] {
  return nodes.map(({ children, ...rest }) => {
    const permissionArray = getPermissionFunc(rest, parentPermissionArray);
    return {
      ...rest,
      permissionArray,
      ...(children && {
        children: getTreeNodeWithPermission(
          children,
          getPermissionFunc,
          permissionArray,
        ),
      }),
    };
  });
}

export function setTreeDataWithPrivilege(
  treeData: DataSourceTreeNode[],
  privileges: Privilege[],
  viewpoint: Viewpoints,
  viewpointType: SubjectTypes | ResourceTypes,
  dataSourceType: SubjectTypes | ResourceTypes,
  vizSubTypes?: VizResourceSubTypes,
): DataSourceTreeNode[] {
  return getTreeNodeWithPermission(
    treeData,
    (node, parentPermissionArray) => {
      let permissionArray = parentPermissionArray;

      for (let i = 0; i < privileges.length; i += 1) {
        if (viewpoint === Viewpoints.Subject) {
          if (
            isRootId(node.id, node.type as ResourceTypes) &&
            isRootId(privileges[i].resourceId, privileges[i].resourceType) &&
            node.id === privileges[i].resourceId
          ) {
            permissionArray = parsePermission(
              privileges[i].permission,
              privileges[i].resourceType,
              vizSubTypes,
            );
            fastDeleteArrayElement(privileges, i);
            break;
          }
        }

        if (node.id === privileges[i][`${getInverseViewpoints(viewpoint)}Id`]) {
          permissionArray = parsePermission(
            privileges[i].permission,
            getPrivilegeSettingType(viewpoint, viewpointType, dataSourceType)!,
            vizSubTypes,
          );
          fastDeleteArrayElement(privileges, i);
          break;
        }
      }

      return permissionArray;
    },
    getDefaultPermissionArray(),
  );
}

export function isRootId(id: string, type: ResourceTypes) {
  return type === ResourceTypes.Viz
    ? Object.values(VizResourceSubTypes).includes(id as VizResourceSubTypes)
    : id === type;
}

export function calcPermission(permissionViewModel: PermissionLevels[]) {
  return permissionViewModel.reduce((s, p) => s | p, PermissionLevels.Disable);
}

export function parsePermission(
  permission: number,
  resourceType: ResourceTypes,
  vizSubTypes?: VizResourceSubTypes,
) {
  let permissionViewModel: PermissionLevels[] = [];
  RESOURCE_TYPE_PERMISSION_MAPPING[resourceType + (vizSubTypes || '')].forEach(
    p => {
      if ((permission & (p as PermissionLevels)) === p) {
        permissionViewModel.push(p);
      } else {
        permissionViewModel.push(PermissionLevels.Disable);
      }
    },
  );
  return permissionViewModel;
}

/**
 * 计算变更后的单条资源权限
 * 由于权限等级中有包含关系，在取消权限时，需要计算其他权限是否包含被操作权限，
 * 如果包含需要一并取消
 * 授权时不用，在授权完成后通过 parsePermission 自动分解
 */
export function getChangedPermission(
  canceled: boolean,
  origin: PermissionLevels[],
  index,
  changedValue,
) {
  return canceled
    ? origin.map(p =>
        (p & changedValue) === changedValue ? PermissionLevels.Disable : p,
      )
    : origin.map((p, i) => (index === i ? changedValue : p));
}

export function getRecalculatedPrivileges(
  nodes: DataSourceTreeNode[],
  viewpoint: Viewpoints,
  viewpointType: ResourceTypes | SubjectTypes,
  viewpointId: string,
  orgId: string,
  parentPermissionArray?: PermissionLevels[],
) {
  let privileges: Privilege[] = [];

  nodes.forEach(({ id, type, permissionArray, children }) => {
    const permission = calcPermission(permissionArray);
    if (
      !parentPermissionArray ||
      calcPermission(parentPermissionArray) !== permission
    ) {
      privileges.push(
        viewpoint === Viewpoints.Resource
          ? {
              resourceId: viewpointId,
              resourceType: viewpointType as ResourceTypes,
              subjectId: id,
              subjectType: type as SubjectTypes,
              orgId,
              permission,
            }
          : {
              resourceId: id,
              resourceType: type as ResourceTypes,
              subjectId: viewpointId,
              subjectType: viewpointType as SubjectTypes,
              orgId,
              permission,
            },
      );
    }

    if (children) {
      privileges = privileges.concat(
        getRecalculatedPrivileges(
          children,
          viewpoint,
          viewpointType,
          viewpointId,
          orgId,
          permissionArray,
        ),
      );
    }
  });
  return privileges;
}

export function getPrivilegeResult(
  origin: Privilege[],
  recalculated: Privilege[],
) {
  return getDiffParams(
    origin,
    recalculated,
    (oe, ce) =>
      oe.resourceId === ce.resourceId &&
      oe.resourceType === ce.resourceType &&
      oe.subjectId === ce.subjectId &&
      oe.subjectType === ce.subjectType,
    (oe, ce) => oe.permission !== ce.permission,
    oe => oe.resourceId === '*',
  );
}

export function getIndependentPermissionChangeParams(
  resourceId: string,
  value: PermissionLevels,
  privileges: Privilege[],
  orgId: string,
  subjectId: string,
  subjectType: SubjectTypes,
  resourceType: ResourceTypes,
): GrantPermissionParams['params'] {
  const params: GrantPermissionParams['params'] = {
    permissionToCreate: [],
    permissionToDelete: [],
    permissionToUpdate: [],
  };

  if (!value) {
    const independentPermission = privileges.find(
      p => p.resourceId === resourceId && p.resourceType === resourceType,
    )!;
    params.permissionToDelete.push({
      ...independentPermission,
      permission: value,
    });
  } else {
    params.permissionToCreate.push({
      orgId,
      resourceId,
      resourceType,
      subjectId,
      subjectType,
      permission: value,
    });
  }

  return params;
}

export function getPrivilegeSettingType(
  viewpoint: Viewpoints,
  viewpointType: SubjectTypes | ResourceTypes,
  dataSourceType: SubjectTypes | ResourceTypes,
) {
  return Object.values(ResourceTypes).find(
    t =>
      (viewpoint === Viewpoints.Subject && dataSourceType === t) ||
      (viewpoint === Viewpoints.Resource && viewpointType === t),
  );
}

export function getPrivilegeSettingWidth(
  viewpoint: Viewpoints,
  viewpointType: SubjectTypes | ResourceTypes,
  dataSourceType: SubjectTypes | ResourceTypes,
  vizSubTypes?: VizResourceSubTypes,
) {
  switch (getPrivilegeSettingType(viewpoint, viewpointType, dataSourceType)) {
    case ResourceTypes.Viz:
      if (vizSubTypes === VizResourceSubTypes.Storyboard) {
        return 3 * 80 + 40;
      }
      return 4 * 80 + 40;
    case ResourceTypes.Schedule:
      return 1 * 80 + 40;
    default:
      return 2 * 80 + 40;
  }
}

export function getBasePrivilege(value) {
  if ((value & PermissionLevels.Grant) === PermissionLevels.Grant) {
    return PermissionLevels.Grant;
  } else if ((value & PermissionLevels.Manage) === PermissionLevels.Manage) {
    return PermissionLevels.Manage;
  } else if ((value & PermissionLevels.Read) === PermissionLevels.Read) {
    return PermissionLevels.Read;
  } else {
    return PermissionLevels.Disable;
  }
}

export function getExtraPrivilege(value) {
  let extraValue: PermissionLevels[] = [];

  if ((value & PermissionLevels.Download) === PermissionLevels.Download) {
    extraValue.push(PermissionLevels.Download);
  }
  if ((value & PermissionLevels.Share) === PermissionLevels.Share) {
    extraValue.push(PermissionLevels.Share);
  }

  return extraValue;
}
