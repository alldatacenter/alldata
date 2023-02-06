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
import { renderRoutes } from 'react-router-config';
import { ErrorBoundary } from 'common';
import { useUpdate } from 'common/use-hooks';
import classnames from 'classnames';
import SideBar from 'layout/pages/page-container/components/sidebar';
import SubSideBar from 'layout/pages/page-container/components/sub-sidebar';
import Header from 'layout/pages/page-container/components/header';
import { NoAuth, NotFound } from 'app/layout/common/error-page';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { useEffectOnce } from 'react-use';
import userStore from 'app/user/stores';
import agent from 'agent';
import { MessageCenter } from '../message/message';
import layoutStore from 'app/layout/stores/layout';
import { checkVersion } from 'app/layout/common/check-version';
import routeInfoStore from 'core/stores/route';
import { LSObserver } from 'common/utils';
import { Card, Carousel } from 'antd';
import Shell from './components/shell';
import { ErrorLayout } from './error-layout';
import { eventHub } from 'common/utils/event-hub';
import orgStore from 'app/org-home/stores/org';
import './page-container.scss';

const layoutMap = {
  error: ErrorLayout,
};

interface IProps {
  route: any;
}
const PageContainer = ({ route }: IProps) => {
  const [noAuth, notFound] = userStore.useStore((s: any) => [s.noAuth, s.notFound]);
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const [showMessage, customMain, announcementList] = layoutStore.useStore((s) => [
    s.showMessage,
    s.customMain,
    s.announcementList,
  ]);
  const [currentRoute, prevRouteInfo, isIn] = routeInfoStore.useStore((s) => [s.currentRoute, s.prevRouteInfo, s.isIn]);
  const [state, updater] = useUpdate({
    startInit: false,
  });

  const mainEle = React.useRef<HTMLDivElement>(null);

  useEffectOnce(() => {
    const skeleton = document.querySelector('#erda-skeleton');
    const content = document.querySelector('#erda-content');
    if (skeleton && content) {
      skeleton.className += ' fade';
      content.classList.remove('hidden');
      setTimeout(() => {
        skeleton.remove();
        const scriptDom = document.querySelector('#init-script');
        if (scriptDom) {
          scriptDom.remove();
        }
      }, 500);
    }
    eventHub.emit('layout/mount');

    if (process.env.NODE_ENV === 'production') {
      checkVersion();
    }
    const checkLoginStatus = () => {
      agent.get('/api/users/me').catch((error: any) => {
        const { statusCode } = error.response;
        if ([401].includes(statusCode)) {
          userStore.effects.login();
        }
      });
    };

    // 注册并监听登录状态的变化
    LSObserver.watch('diceLoginState', (val: any) => {
      if (val.toString() === 'false' && currentOrg.id) {
        setTimeout(() => {
          checkLoginStatus();
        }, 0);
      }
    });

    // wait for layout complete
    setTimeout(() => {
      updater.startInit(true);
    }, 0);
  });

  React.useEffect(() => {
    if (prevRouteInfo?.currentRoute.path !== currentRoute.path) {
      if (mainEle && mainEle.current) {
        mainEle.current.scrollTop = 0;
      }
    }
  }, [currentRoute, prevRouteInfo, mainEle]);

  const { layout } = currentRoute;
  const hideHeader = showMessage || layout?.hideHeader;
  const layoutCls = ['dice-layout'];
  const noticeWrap = ['notice-wrap'];
  if (announcementList.length) {
    layoutCls.push('has-notice');
    if (announcementList.length === 1) {
      layoutCls.push('has-notice-only-one');
      noticeWrap.push('only-one');
    }
  }
  let showSubSidebar = !noAuth && !notFound;
  let CustomLayout;
  let noWrapper = false;
  if (typeof layout === 'object') {
    const { className, use, showSubSidebar: layoutShowSubSiderBar } = layout;
    if (showSubSidebar && layoutShowSubSiderBar === false) showSubSidebar = false;
    className && layoutCls.push(className);
    layoutMap[use] && (CustomLayout = layoutMap[use]);
    noWrapper = layout.noWrapper;
  }
  const layoutClass = classnames(layoutCls);
  const noticeClass = classnames(noticeWrap);
  if (CustomLayout) {
    return <CustomLayout layoutClass={layoutClass}>{renderRoutes(route.routes)}</CustomLayout>;
  }
  let MainContent = null;
  if (noAuth) {
    MainContent = <NoAuth />;
  } else if (notFound) {
    MainContent = <NotFound />;
  } else if (state.startInit) {
    MainContent = (
      <ErrorBoundary>
        <DndProvider backend={HTML5Backend}>
          {noWrapper ? (
            <>
              {typeof customMain === 'function' ? customMain() : customMain}
              {renderRoutes(route.routes)}
            </>
          ) : (
            <Card className={layout && layout.fullHeight ? 'h-full overflow-auto' : ''}>
              {typeof customMain === 'function' ? customMain() : customMain}
              {renderRoutes(route.routes)}
            </Card>
          )}
        </DndProvider>
      </ErrorBoundary>
    );
  }

  return (
    <>
      {announcementList.length ? (
        <div className={noticeClass}>
          <Carousel arrows autoplay autoplaySpeed={5000}>
            {announcementList.map((announcement) => {
              return <div key={announcement.id}>{announcement.content}</div>;
            })}
          </Carousel>
        </div>
      ) : null}
      <div className={layoutClass}>
        <Shell
          layout="vertical"
          fixed
          className="dice-main-shell"
          theme="BL"
          globalNavigation={<SideBar />}
          sideNavigation={showSubSidebar ? <SubSideBar /> : undefined}
          pageHeader={!hideHeader ? <Header /> : undefined}
        >
          <div
            id="main"
            ref={mainEle}
            style={{ opacity: showMessage ? 0 : undefined }}
            className={hideHeader ? 'p-0' : ''}
          >
            {MainContent}
          </div>
          <MessageCenter show={showMessage} />
        </Shell>
      </div>
    </>
  );
};

export default PageContainer;
