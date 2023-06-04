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

import { createStore } from 'redux';
import { pathToRegexp } from 'path-to-regexp';
import { menuArrays, MenuItemType } from '@/configs/menus';
import { getPathnameExist } from '@/configs/routes';
import { getCurrentLocale } from '@/configs/locales';

export interface State {
  locale: string;
  userName: string;
  userId: number;
  roles: string[];
  currentMenu: null | Omit<MenuItemType, 'children'>;
}

const state: State = {
  locale: getCurrentLocale(),
  userName: '',
  userId: 0,
  roles: [],
  currentMenu: null,
};

const reducers = {
  setUserInfo: (state: State, payload) => {
    return {
      ...state,
      userName: payload.userName,
      userId: payload.userId,
      roles: payload.roles,
    };
  },

  setCurrentLocale(state, payload) {
    return {
      ...state,
      locale: payload.locale,
    };
  },

  setCurrentMenu(state, payload) {
    const pathname = payload && payload.pathname;
    if (!pathname) return state;

    // Find the selected route
    let currentMenu = null;
    for (const item of menuArrays) {
      if (
        item.path &&
        // The route in the menu || is not in the menu, but belongs to a sub-route under a menu
        (pathToRegexp(item.path).test(pathname) ||
          (getPathnameExist(pathname) && new RegExp(`^${item.path}/[\\w|/]+`, 'i').test(pathname)))
      ) {
        currentMenu = item;
        break;
      }
    }

    return {
      ...state,
      currentMenu,
    };
  },
};

const model = (stateData = state, action) => {
  const { type, payload } = action;

  if (typeof reducers[type] === 'function') {
    return reducers[type](stateData, payload);
  }

  return stateData;
};

export default createStore(model);
