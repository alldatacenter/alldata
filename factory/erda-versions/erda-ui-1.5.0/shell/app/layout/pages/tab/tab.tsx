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

import { last, find } from 'lodash';
import React from 'react';
import { Menu } from 'common';
import routeInfoStore from 'core/stores/route';

/**
 * render page tabs
 *
 */

/*
  Tab组件不可以写成纯函数组件.
  因为props修改后导致MenuComponent中的TabRightComp被重新渲染
  如TimeSelecotr，会导致时间重置，引起一些因timeSpan改变后的查询
 */
interface IProps {
  routes: IRoute[];
  path: string;
}

export class PureTab extends React.PureComponent<IProps> {
  render() {
    const { routes, path } = this.props;
    const lastRouteWithPath = find(routes, 'relativePath');
    if (lastRouteWithPath) {
      const lastPathPart = last(path.split('/')) || '';
      const {
        relativePath,
        tabs,
        ignoreTabQuery,
        keepTabQuery,
        tabKey = lastPathPart,
        alwaysShowTabKey,
        TabRightComp,
      } = lastRouteWithPath;
      // 如果最后一级路径有:号，则不用严格匹配
      if (alwaysShowTabKey || ((relativePath === lastPathPart || relativePath.includes(':')) && tabs)) {
        return (
          <>
            <Menu
              activeKey={alwaysShowTabKey || tabKey}
              menus={tabs}
              TabRightComp={TabRightComp}
              ignoreTabQuery={ignoreTabQuery}
              keepTabQuery={keepTabQuery}
            />
          </>
        );
      }
    }
    return null;
  }
}

export const Tab = () => {
  const routes = routeInfoStore.useStore((s) => s.routes);
  // 路由变化时routes和currentRoute可能不变化，比如最后一段是/:type，所以传一个path触发变化
  return <PureTab routes={routes} path={window.location.pathname} />;
};
