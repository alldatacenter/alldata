// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { map, compact } from 'lodash';
import { orgPerm } from './_perm-org';
import { projectPerm } from './_perm-project';
import { appPerm } from './_perm-app';
import { mspPerm } from './_perm-msp';
import { sysPerm } from './_perm-sys';
import yaml from 'js-yaml';

export const permPrefix = 'UI';
export const permState = {
  org: orgPerm,
  project: projectPerm,
  app: appPerm,
  sys: sysPerm,
  msp: mspPerm,
};
export type OrgPermType = typeof orgPerm;
export type ProjectPermType = typeof projectPerm;
export type AppPermType = typeof appPerm;
export type MspPermType = typeof mspPerm;

const scopeNames = Object.keys(permState);

// 将permState转换为后端所需的格式
/**
 * {scope:'org',resource:'UI',action:'repo.closeMr',role:'',resource_role:''}
 * * */
export const changePerm2Yml = (permData: any, scope: string, ROLES: Obj) => {
  // const postData = [] as any[];
  const setPostData = (obj: any, postData = [] as any[], extra = {} as any) => {
    if (!obj) return postData;
    const { name, role, ...rest } = obj;
    if (!role) {
      map(rest, (item, key) => {
        const reExtra = { ...extra, nameArr: [...(extra.nameArr || []), name] } as any;
        if (scopeNames.includes(key)) {
          reExtra.scope = key;
        } else {
          reExtra.resource = `${reExtra.resource || permPrefix}.${key}`;
        }
        return setPostData(item, postData, reExtra);
      });
    } else {
      const customRole = [] as string[];
      const regularRole = [] as string[];
      map(role, (rItem) => {
        if (ROLES[rItem] && !ROLES[rItem].isCustomRole) {
          regularRole.push(rItem);
        } else {
          customRole.push(rItem);
        }
      });
      const dataItem = {
        scope: extra.scope,
        name: (extra.nameArr || []).concat(name).join(' > '),
        resource: '',
        action: '',
        role: regularRole.join(','),
      } as any;
      const reResource = extra.resource;
      const resourceArr = reResource.split('.');
      dataItem.action = resourceArr.pop();
      dataItem.resource = resourceArr.join('.');
      if (customRole.length) {
        // 含自定义角色resource_role
        dataItem.resource_role = customRole.join(',');
        postData.push(dataItem);
      } else {
        postData.push(dataItem);
      }
    }
    return postData;
  };

  const ymlStr = yaml.dump(setPostData(permData, [], { scope }));
  const ymlStrArr = compact(ymlStr.split('\n'));
  // 去除yml缩进多余
  return map(ymlStrArr, (yItem: string) => {
    return yItem.startsWith('  ') ? yItem.replace('  ', '') : yItem;
  }).join('\n');
};
