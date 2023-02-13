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

import layoutStore from 'layout/stores/layout';
import React from 'react';
import { get } from 'lodash';
import { useDebounce } from 'react-use';

export { SplitPage } from './split-page';

export const useWatchTableWidth = (selector: string) => {
  const client = layoutStore.useStore((s) => s.client);
  const [tableWidth, setTableWidth] = React.useState(0);
  React.useLayoutEffect(() => {
    setTableWidth(get(document.querySelector(selector), ['clientWidth'], 0));
  }, [selector]);
  useDebounce(
    () => {
      setTableWidth(get(document.querySelector(selector), ['clientWidth'], 0));
    },
    50,
    [client],
  );
  return [tableWidth];
};
