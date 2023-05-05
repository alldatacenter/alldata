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

import { enhanceMock, mockData } from './data-rank.mock';

export const useMock = (payload: Obj) => {
  if (process.env.NODE_ENV === 'production') {
    return Promise.resolve();
  } else {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(enhanceMock(mockData, payload));
      }, 200);
    });
  }
};
