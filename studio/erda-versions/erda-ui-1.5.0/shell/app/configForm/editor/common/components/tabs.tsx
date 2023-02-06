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
import { map, get, findIndex } from 'lodash';
import './tabs.scss';

interface IProps {
  defaultActiveKey?: string;
  onChange?: (key: string) => void;
  children?: any;
  activeKey?: string;
  tabs: Array<{ key: string; name: string; content: React.ReactChild }>;
}

export const Tabs = (props: IProps) => {
  const { defaultActiveKey, activeKey: actKey, onChange, tabs } = props;
  const [activeKey, setActiveKey] = React.useState(defaultActiveKey || get(tabs, '[0].key'));

  React.useEffect(() => {
    actKey && setActiveKey(actKey);
  }, [actKey]);

  const activeIndex = findIndex(tabs, { key: activeKey });

  React.useEffect(() => {
    onChange && onChange(activeKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeKey]);

  return (
    <div className="dice-form-tabs">
      <div className="tabs-menu">
        {map(tabs, ({ key, name }) => {
          return (
            <div
              className={`tabs-menu-item ${activeKey === key ? 'is-active' : ''}`}
              key={key}
              onClick={() => setActiveKey(key)}
            >
              {name}
            </div>
          );
        })}
      </div>
      <div
        className="tabs-content"
        style={{
          marginLeft: `${-activeIndex * 100}%`,
        }}
      >
        {map(tabs, ({ key, content }, index: number) => {
          const pos = index > activeIndex ? 'right' : index === activeIndex ? 'center' : 'left';
          return (
            <div className={`tabs-content-item ${pos}`} key={key}>
              {content}
            </div>
          );
        })}
      </div>
    </div>
  );
};
