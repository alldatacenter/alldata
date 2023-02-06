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

// import { CubeState } from "cube-state/dist/typings";

interface IRoute {
  routeQuery: {
    [prop: string]: any;
  };
  exact: boolean;
  key: string;
  path: string;
  relativePath: string;
  breadcrumbName?: string | Function;
  _parent: IRoute;
  mark?: string;
  layout: {
    [k: string]: any;
  };
  encode?: boolean;
  eternal?: string;
  disabled?: boolean;
  pageName?: string;
  changePath?: (path: string) => string;
  component: () => any;
  getComp: (cb: Function) => any;
}

interface IRouteInfo {
  routes: IRoute[];
  params: {
    [k: string]: string;
  };
  query: {
    [k: string]: any;
  };
  currentRoute: IRoute;
  routeMarks: string[];
  prevRouteInfo: IRouteInfo;
  isIn: (mark: string) => boolean;
  isMatch: (pattern?: string | RegExp) => boolean;
  isEntering: (mark: string) => boolean;
  isLeaving: (mark: string) => boolean;
}

type IListenRouteCb = (routeInfo: IRouteInfo) => any;

type RouterGetComp = (loadingMod: Promise<{ readonly default?: any; [k: string]: any }>, key?: string) => any;

interface IStoreSubs {
  store: any;
  listenRoute: (listener: IListenRouteCb) => void;
  registerWSHandler: (key: string, handler: (data: SocketMsg) => void) => void;
}
