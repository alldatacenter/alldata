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
import { Tabs } from 'antd';
import { EmptyHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { get, find } from 'lodash';
import './tabs.scss';

const CP_TABS = (props: CP_TABS.Props) => {
  const {
    children,
    tabBarExtraContent,
    props: configProps,
    state: propsState,
    operations,
    customOp,
    execOperation,
  } = props || {};
  const { tabMenu, visible = true } = configProps || {};
  const customOpRef = React.useRef(customOp);

  const [state, updater, update] = useUpdate({
    activeKey: propsState?.activeKey || get(tabMenu, '[0].key'),
  });

  React.useEffect(() => {
    update((prev) => ({ ...prev, ...propsState }));
  }, [propsState, update]);

  React.useEffect(() => {
    if (customOpRef.current?.onStateChange) {
      customOpRef.current.onStateChange(state);
    }
  }, [state]);

  const changeTab = (ak: string) => {
    updater.activeKey(ak);
    if (operations?.onChange) {
      execOperation(operations?.onChange, { ...state, activeKey: ak });
    }
    const curTabOperation = find(tabMenu, { key: ak }) as CP_TABS.ITabMenu;
    if (curTabOperation?.operations?.click) {
      execOperation(curTabOperation.operations.click);
    }
  };

  if (!visible) return null;
  return (
    <Tabs
      activeKey={state.activeKey}
      tabBarExtraContent={<div className="dice-cp-tabs-extra">{tabBarExtraContent}</div>}
      onChange={changeTab}
      renderTabBar={(p: any, DefaultTabBar) => <DefaultTabBar {...p} onKeyDown={(e: any) => e} />}
    >
      {tabMenu.map((item, idx) => {
        const TabComp = (children && children[idx]) || <EmptyHolder relative />;
        return (
          <Tabs.TabPane key={item.key} tab={item.name} disabled={item.disable}>
            {TabComp}
          </Tabs.TabPane>
        );
      })}
    </Tabs>
  );
};
export default CP_TABS;
