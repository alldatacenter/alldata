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

declare namespace SHELL {
  interface Route {
    routeQuery?: {
      [prop: string]: any;
    };
    query?: any;
    path: string;
    mark?: string;
    toMark?: string;
    tabKey?: string;
    exact?: boolean;
    tabs?: Array<{
      key: string;
      name: string;
    }>;
    routes?: Route[];
    relativePath?: string;
    ignoreTabQuery?: boolean;
    keepTabQuery?: string[];
    alwaysShowTabKey?: string;
    TabRightComp?: React.ComponentType;
    wrapper?(Comp: React.ComponentType): React.ComponentType;
    getComp?(cb: Function): Promise<any>;
    connectToTab?(a: object[] | Function): React.ComponentType;
  }

  interface ParsedRoute extends Route {
    key?: string;
    _parent?: Route;
    relativePath?: string;
    NotFound?: any;
    component?: any;
  }
}
