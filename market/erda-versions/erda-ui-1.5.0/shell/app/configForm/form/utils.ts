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

import React from 'react';
import { get, map } from 'lodash';

export const isPromise = (obj: any) => {
  return !!obj && (typeof obj === 'object' || typeof obj === 'function') && typeof obj.then === 'function';
};

export const useMount = (fn) => {
  React.useEffect(() => {
    fn();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
};

export const getData = ({ dataPath, valueKey, nameKey }) => {
  const mockData = {
    data: {
      list: [
        { name1: 'opt1', value1: 1 },
        { name1: 'opt2', value1: 2 },
        { name1: 'opt3', value1: 3 },
        { name1: 'opt4', value1: 4 },
      ],
    },
  };
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(
        map(get(mockData, `${dataPath}`), (item) => {
          return {
            ...item,
            name: get(item, `${nameKey}`),
            value: get(item, `${valueKey}`),
          };
        }),
      );
    }, 2000);
  });
};
