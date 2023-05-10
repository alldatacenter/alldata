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

import { updateSearch } from 'common/utils';
import { map } from 'lodash';
import React from 'react';
import { useMount } from 'react-use';
import routeInfoStore from 'core/stores/route';
import './index.scss';

interface IProps {
  className?: string;
  dataSource: ITabItem[] | ITabItemGroup[];
  [prop: string]: any;
}

interface ITabItem {
  tabTitle: string;
  tabKey: string;
  tabIcon?: JSX.Element;
  content: JSX.Element;
}
interface ITabItemGroup {
  tabGroup: ITabItem[];
  groupTitle: string;
  groupKey?: string;
}

const SettingTabs = ({ dataSource, className = '' }: IProps) => {
  const query = routeInfoStore.useStore((s) => s.query);
  const [activeKey, updateActive] = React.useState('');
  const [content, updateContent] = React.useState(null);

  useMount(() => {
    setTimeout(() => {
      // only need scroll into view once, setTimeout wait menu render completely
      const activeTabDom = document.querySelector('.group-tabs li.active');
      activeTabDom && activeTabDom.scrollIntoView();
    }, 0);
  });

  React.useEffect(() => {
    const tabMap = {};
    let activeTab: any = {};
    if (dataSource.length) {
      const first = dataSource[0];
      if ((first as ITabItemGroup).groupTitle) {
        activeTab = (first as ITabItemGroup).tabGroup[0];
        map(dataSource as any, (group: ITabItemGroup) =>
          map(group.tabGroup, (tab: ITabItem) => {
            tabMap[tab.tabKey] = tab;
          }),
        );
      } else {
        activeTab = first;
        map(dataSource as any, (tab: ITabItem) => {
          tabMap[tab.tabKey] = tab;
        });
      }
    }
    activeTab = tabMap[query.tabKey] || activeTab;
    updateActive(activeTab.tabKey);
    updateContent(activeTab.content);
  }, [dataSource, query.tabKey]);

  return (
    <div className={`settings-main ${className}`}>
      <ul className="settings-menu">
        {map(dataSource as any, ({ groupTitle, tabGroup, ...rest }: any, index: number) => {
          const renderTabItem = (tab: ITabItem) => (
            <li
              key={tab.tabKey}
              className={activeKey === tab.tabKey ? 'active' : ''}
              onClick={() => updateSearch({ tabKey: tab.tabKey })}
            >
              {tab.tabIcon || null}
              <span className="tab-title">{tab.tabTitle}</span>
            </li>
          );
          if (groupTitle && tabGroup) {
            return (
              <ul className="group-tabs" key={index}>
                <div className="group-title">{groupTitle}</div>
                {map(tabGroup, renderTabItem)}
              </ul>
            );
          }
          return renderTabItem(rest);
        })}
      </ul>
      <div className="settings-content" key={activeKey}>
        {content}
      </div>
    </div>
  );
};

export default SettingTabs;
