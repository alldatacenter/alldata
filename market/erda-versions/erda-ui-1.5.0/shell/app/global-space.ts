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

import { set, get } from 'lodash';

export const globalSpace = {
  initRouteData: {
    routes: [],
    params: {},
    currentRoute: {},
    routeMarks: [],
    isIn: () => false,
    isMatch: () => false,
    isEntering: () => false,
    isLeaving: () => false,
  },
  history: null,
  erdaInfo: {
    currentOrgId: 0,
    isSysAdmin: false,
  },
};

export const getGlobal = (key: string) => get(globalSpace, key);
export const setGlobal = (key: string, value: any) => {
  set(globalSpace, key, value);
};
