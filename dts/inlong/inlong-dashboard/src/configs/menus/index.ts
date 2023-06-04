/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { treeToArray } from '@/core/utils';
import menusTreeConf from './conf';

export interface MenuItemType {
  name: string;
  key?: string; // auto generate
  deepKey?: string; // auto generate
  children?: MenuItemType[];
  path?: string;
  isAdmin?: boolean;
  icon?: React.ReactNode;
}

const genMenuKey = (array: Omit<MenuItemType, 'key'>[], parentKey = ''): MenuItemType[] => {
  return array.map((item, index) => {
    let obj = { ...item };
    const num = index + 1 < 10 ? `0${index + 1}` : (index + 1).toString();
    const currentKey = `${parentKey}${num}`;
    if (obj.children) {
      obj.children = genMenuKey(obj.children, currentKey);
    }
    return {
      ...obj,
      key: currentKey,
    };
  });
};

const menusTree: MenuItemType[] = genMenuKey(menusTreeConf);

export const menuArrays: Omit<MenuItemType, 'children'>[] = treeToArray(menusTree, 'key', 'pKey');

export default menusTree;
