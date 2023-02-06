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
import { Drawer, Input } from 'antd';
import { Icon } from 'common';
import i18n from 'i18n';

import './index.scss';

interface IProps {
  node: React.ReactNode;
  dataSource: Array<{ app: LAYOUT.IApp; key: string }>;
  linkRender: (item: { app: LAYOUT.IApp }) => any;
  title: React.ReactNode;
  visible: boolean;
  onVisible: (visible: boolean) => void;
}

const AppCenter = ({ node, dataSource, linkRender, title, visible, onVisible }: IProps) => {
  const [value, setValue] = React.useState<string>('');

  const onFilterChange = React.useCallback(
    (e) => {
      setValue(e.target.value);
    },
    [setValue],
  );

  const list = value
    ? dataSource.filter(({ app }: { app: LAYOUT.IApp }) => app.name?.toLowerCase().includes(value.toLowerCase()))
    : dataSource;

  return (
    <>
      <span onClick={() => onVisible(true)}>{node}</span>
      <Drawer
        width={332}
        placement="left"
        closable={false}
        bodyStyle={{ height: '100%' }}
        onClose={() => onVisible(false)}
        visible={visible}
        destroyOnClose
        className="erda-app-center-drawer"
      >
        <div className="flex items-center justify-start">
          <span className="mr-4">
            <Icon type="gb" className="text-2xl font-bold cursor-pointer" onClick={() => onVisible(false)} />
          </span>
          <span>{title}</span>
        </div>
        <Input
          className="mt-4"
          placeholder={i18n.t('Enter the keyword query')}
          prefix={<Icon type="search" />}
          value={value}
          onChange={onFilterChange}
        />
        <ul className="mt-4">
          {list.map((item) => (
            <li key={item.app.key} className="app-center-list-item mb-2 cursor-pointer">
              {linkRender(item)}
            </li>
          ))}
        </ul>
      </Drawer>
    </>
  );
};

export default AppCenter;
