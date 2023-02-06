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
import { Tab } from 'layout/pages/tab/tab';
import layoutStore from 'layout/stores/layout';
import { goTo } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import breadcrumbStore from 'layout/stores/breadcrumb';
import { isEmpty, isFunction } from 'lodash';
import { matchPath } from 'react-router-dom';
import { ErdaIcon } from 'common';
import { Route } from 'core/common/interface';
import { Breadcrumb, Tooltip } from 'antd';
import './header.scss';

const getPath = (path: string, params: Obj<string>) => {
  path = (path || '').replace(/^\//, '');
  Object.keys(params).forEach((key) => {
    path = path.replace(`:${key}`, params[key]);
  });
  return path;
};

const BreadcrumbItem = ({
  route,
  paths,
  params,
  title,
}: {
  route: IRoute;
  params: Obj<string>;
  paths: string[];
  title?: string;
}) => {
  const { path, eternal, changePath, pageName } = route;
  const [link, setLink] = React.useState('');

  React.useEffect(() => {
    const currentPath = paths[paths.length - 1];
    const lastPath = paths.length > 1 ? paths[paths.length - 2] : '';

    let finalPath = route.encode
      ? `${lastPath}/${encodeURIComponent(params[route.relativePath.slice(1)])}`
      : currentPath;
    if (changePath) {
      finalPath = changePath(finalPath);
    }

    setLink(`/${finalPath}`);
  }, [changePath, params, paths, route.encode, route.relativePath]);

  const displayTitle = title || pageName;

  return displayTitle ? (
    <span
      className={`breadcrumb-name ${route.disabled ? 'breadcrumb-disabled' : ''}`}
      title={displayTitle}
      key={eternal || path}
      onClick={() => !route.disabled && goTo(link)}
    >
      {displayTitle}
    </span>
  ) : null;
};

const Header = () => {
  const [currentApp] = layoutStore.useStore((s) => [s.currentApp]);
  const routes: IRoute[] = routeInfoStore.useStore((s) => s.routes);
  const [pageName, setPageName] = React.useState<string>();
  const infoMap = breadcrumbStore.useStore((s) => s.infoMap);
  const [query] = routeInfoStore.useStore((s) => [s.query]);

  const [allRoutes, setAllRoutes] = React.useState<Route[]>([]);
  const [params, setParams] = React.useState<Obj<string>>({});
  const [pageNameInfo, setPageNameInfo] = React.useState<Function>();
  const checkHasTemplate = React.useCallback(
    (breadcrumbName: string) => {
      const replacePattern = /\{([\w.])+\}/g;
      let _breadcrumbName = breadcrumbName || '';
      const matches = _breadcrumbName.match(replacePattern);
      if (!matches) {
        return _breadcrumbName;
      }
      matches.forEach((match: string) => {
        const [type, key] = match.slice(1, -1).split('.'); // match: {params.id}
        let value;
        if (type === 'params') {
          value = params[key];
        } else if (type === 'query') {
          value = decodeURIComponent(query[key]);
        } else {
          value = infoMap[type];
        }
        _breadcrumbName = _breadcrumbName.replace(match, value);
      });
      if (_breadcrumbName === 'undefined') {
        _breadcrumbName = '';
      }
      return _breadcrumbName;
    },
    [infoMap, params, query],
  );

  const getBreadcrumbTitle = React.useCallback(
    (route: Route, isBreadcrumb = false) => {
      const { breadcrumbName, pageName } = route;
      let _title = '';
      if (!isBreadcrumb && pageName) {
        _title = isFunction(pageName)
          ? breadcrumbName({ infoMap, route, params, query })
          : checkHasTemplate(pageName as string);
      } else {
        _title = isFunction(breadcrumbName)
          ? breadcrumbName({ infoMap, route, params, query })
          : checkHasTemplate(breadcrumbName as string);
      }
      return _title;
    },
    [checkHasTemplate, infoMap, params, query],
  );

  React.useEffect(() => {
    if (allRoutes.length) {
      const lastRoute = allRoutes[allRoutes.length - 1];
      const _title = getBreadcrumbTitle(lastRoute);
      setPageNameInfo(() => lastRoute?.pageNameInfo);
      setPageName(_title);
    }
  }, [allRoutes, getBreadcrumbTitle]);

  React.useEffect(() => {
    document.title = pageName ? `${pageName} · Erda` : 'Erda';
  }, [pageName]);

  React.useEffect(() => {
    let _params: Obj<string> = {};
    // get params from path
    if (routes.length > 0) {
      const match = matchPath(window.location.pathname, {
        path: routes[0].path,
        exact: true,
        strict: false,
      });
      if (match) {
        _params = match.params;
        setParams(_params);
      }
    }
    const filteredRoutes = routes.filter((route) => {
      return route.path && (route.breadcrumbName || route.pageName || typeof route.breadcrumbName === 'function');
    });
    if (!isEmpty(currentApp)) {
      const eternalApp = {
        key: currentApp.key,
        eternal: currentApp.href,
        breadcrumbName: currentApp.breadcrumbName,
        path: typeof currentApp.path === 'function' ? currentApp.path(_params || {}, routes) : currentApp.href,
      };
      filteredRoutes.reverse().splice(1, 0, eternalApp as IRoute);
      setAllRoutes(filteredRoutes);
    }
  }, [currentApp, routes]);

  const itemRender = (route: Route, _params: Obj<string>, _routes: Route[], paths: string[]) => {
    if (!route.breadcrumbName || (allRoutes.length && allRoutes[allRoutes.length - 1] === route)) {
      return null;
    }
    const _title = getBreadcrumbTitle(route, true);
    return _title && <BreadcrumbItem paths={[...paths]} route={route as IRoute} params={_params} title={_title} />;
  };

  const displayPageName = () => {
    if (typeof pageNameInfo === 'function') {
      const Comp = pageNameInfo;
      return <Comp />;
    }
    return (
      <div className="erda-header-title truncate">
        <Tooltip title={pageName}>{pageName}</Tooltip>
      </div>
    );
  };

  const paths: string[] = [];

  return (
    <div className="erda-header">
      <div className="erda-header-breadcrumb">
        <Breadcrumb separator={<ErdaIcon className="align-middle mr-1 mb-0.5" type="right" size="14px" />}>
          {allRoutes.map((item) => {
            paths.push(getPath(item.path, params));
            return <Breadcrumb.Item key={item.key}>{itemRender(item, params, allRoutes, paths)}</Breadcrumb.Item>;
          })}
        </Breadcrumb>
      </div>

      <div className={'erda-header-top'}>
        <div className="erda-header-title-con">{pageName && displayPageName()}</div>
        <Tab />
      </div>
    </div>
  );
};

export default Header;
