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
import { createBrowserHistory, UnregisterCallback, LocationState, LocationListener } from 'history';

const history = createBrowserHistory();

const { listen } = history;

// because of qiankun > single-spa rewrite pushState, lead to history.listen execute twice
history.listen = (listener: LocationListener<LocationState>): UnregisterCallback => {
  let lastPathname = '';
  function enhancerCb(...args: any) {
    const { pathname, search, hash } = window.location;
    const curUrl = `${pathname}${search}${hash}`;
    if (curUrl === lastPathname) return;
    lastPathname = curUrl;
    // @ts-ignore pass args directly
    return listener(...args);
  }
  return listen(enhancerCb);
};

export default history;
