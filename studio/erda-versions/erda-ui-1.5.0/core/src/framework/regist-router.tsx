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

import React, { ComponentType } from 'react';
import { renderRoutes, RouteConfig } from 'react-router-config';
import { map, set, compact } from 'lodash';
import { produce } from 'immer';

const EmptyContainer = ({ route }: { route: RouteConfig }) => renderRoutes(route.routes);

// in react-router v4, there is no `getComponent` to pass a callback to load comp, instead introduce this asyncComponent to implement it.
// this is also solution for dva/dynamic
const asyncComponent = (getComponent: () => Promise<ComponentType>) => {
  return class AsyncComponent extends React.Component {
    state = { Component: null as unknown as any };

    componentDidMount() {
      if (!this.state.Component) {
        getComponent().then((Component) => {
          this.setState({ Component });
        });
      }
    }

    render() {
      const { Component } = this.state;
      if (!Component) {
        return null;
      }
      return <Component {...this.props} />;
    }
  };
};

const parseRoutes = (rootRoute: SHELL.ParsedRoute) => {
  const routeMap = {} as unknown as SHELL.ParsedRoute;
  const { NotFound, ...rootCopy } = rootRoute;
  const notFoundRoute: SHELL.ParsedRoute = {
    path: '(.*)', // path-to-regexp doesn't support wildcard * `No wildcard asterisk (*) - use parameters instead ((.*) or :splat*)`
    component: NotFound,
  };
  const walk = (routes: SHELL.ParsedRoute[], _parent: SHELL.ParsedRoute, _deep: number | string) => {
    return routes.map((item, i) => {
      const route = { ...item };
      route._parent = _parent;
      route.key = route.key ? route.key : `${_deep}-${i + 1}`;
      const relativePath = route.relativePath || route.path || '';
      route.path =
        _parent.path !== '/'
          ? relativePath === ''
            ? _parent.path
            : `${_parent.path}/${relativePath}`
          : `/${relativePath}`;
      route.relativePath = relativePath;

      const { wrapper } = route;
      // replace getComp to component
      if (route.getComp) {
        route.component = asyncComponent(() =>
          route.getComp((importPromise: Promise<any>, key = 'default') => {
            return importPromise.then((mod) => (wrapper ? wrapper(mod[key]) : mod[key]));
          }),
        );
        route.exact = route.exact || true;
      } else if (!route.component && route.routes) {
        route.component = EmptyContainer;
      }

      routeMap[route.path] = {
        key: route.key,
        route,
      };

      if (route.routes) {
        route.routes = walk(route.routes.concat(notFoundRoute), route, route.key);
      }
      return route;
    });
  };
  return [
    {
      ...rootCopy,
      key: '0',
      routes: walk(rootCopy.routes.concat(notFoundRoute), rootCopy, 1),
    },
    routeMap,
  ];
};

const sortRoutes = (r: SHELL.ParsedRoute) => {
  const keys = Object.keys(r);
  const newRoutes = keys.sort((a, b) => {
    const aLev = a.split('/').length;
    const bLev = b.split('/').length;
    if (aLev !== bLev) {
      // 层级不同的，深层级往前放
      return bLev - aLev;
    } else {
      // 层级一样的，* 匹配的往后放
      const aMathAll = a.split('*').length;
      const bMathAll = b.split('*').length;
      if (aMathAll !== bMathAll) {
        return aMathAll - bMathAll;
      } else {
        //  层级一样的，且无*匹配的，:匹配多的的往后放
        const aMaths = a.split(':').length;
        const bMaths = b.split(':').length;
        return aMaths - bMaths;
      }
    }
  });
  return newRoutes;
};

let moduleRouteMap: Obj = {};
let NewestRoot = ({ route }: { route: RouteConfig }) => renderRoutes(route.routes);
let NewestNotFound = () => 'Page not found';

export type IGetRouter = () => SHELL.Route[] | SHELL.Route[];
export interface CompMap {
  Root?: ComponentType<any>;
  NotFound?: ComponentType<any>;
}

const resetRouter = (routers: Obj<SHELL.Route[]>) => {
  return produce(routers, (draft) => {
    const routerMarkObj: Obj = {};
    const toMarkObj: Obj = {};
    const getRouterMarks = (_r: SHELL.Route[], _path: string) => {
      _r.forEach((rItem: SHELL.Route, idx: number) => {
        const { mark, routes: _rs, toMark } = rItem;
        if (mark && !routerMarkObj[mark]) {
          routerMarkObj[mark] = rItem;
        }
        if (toMark) {
          toMarkObj[toMark] = (toMarkObj[toMark] || []).concat({ router: rItem, key: `${_path}.[${idx}]` });
        }

        if (_rs) {
          getRouterMarks(_rs, `${_path}.[${idx}].routes`);
        }
      });
    };

    map(draft, (rItem, key) => {
      getRouterMarks(rItem, key);
    });

    map(toMarkObj, (_toObjArr, k) => {
      map(_toObjArr, (_toObj) => {
        const { key, router: _toRouter } = _toObj;
        if (_toRouter && routerMarkObj[k]) {
          _toRouter.toMark = undefined;
          routerMarkObj[k].routes = (routerMarkObj[k].routes || []).concat(_toRouter);
          set(draft, key, undefined);
        }
      });
    });
  });
};

export const registRouters = (key: string, routers: IGetRouter, { Root, NotFound }: CompMap = {}) => {
  const rs = typeof routers === 'function' ? routers() : routers || [];
  NewestRoot = Root || (NewestRoot as any);
  NewestNotFound = NotFound || (NewestNotFound as any);
  if (rs.length) {
    moduleRouteMap = produce(moduleRouteMap, (draft) => {
      draft[key] = rs;
    });
    const reRoutes = resetRouter(moduleRouteMap);
    const [parsed, routeMap] = parseRoutes({
      path: '/',
      component: NewestRoot,
      NotFound: NewestNotFound,
      routes: compact(Object.values(reRoutes).flat()),
    });
    const routePatterns = sortRoutes(routeMap);
    return { routeMap, routePatterns, parsed };
  }
  return {};
};
