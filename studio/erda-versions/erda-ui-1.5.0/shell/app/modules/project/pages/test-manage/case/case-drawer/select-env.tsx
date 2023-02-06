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

import { isEmpty, map } from 'lodash';
import { Dropdown, Menu } from 'antd';
import React from 'react';

interface ISelectEnvProps {
  children: JSX.Element;
  noEnv?: TEST_ENV.Item[];
  envList?: readonly TEST_ENV.Item[];
  onClick: (data: TEST_ENV.Item) => void;
}

const SelectEnv = ({ children, onClick, noEnv = [], envList }: ISelectEnvProps) => {
  const envs = [...(envList || []), ...noEnv];
  if (isEmpty(envs)) {
    return children;
  }
  const menu = (
    <Menu
      onClick={({ key, domEvent }: any) => {
        domEvent.stopPropagation();
        const target = envs.find((a: any) => +a.id === +key) as TEST_ENV.Item;
        onClick(target);
      }}
    >
      {map(envs, (env) => (
        <Menu.Item key={env.id}>
          {env.name} {env.domain ? `(${env.domain})` : null}
        </Menu.Item>
      ))}
    </Menu>
  );
  const child = React.Children.count(children) === 1 ? children : <span>{children}</span>;
  return (
    <Dropdown overlay={menu} placement="bottomRight">
      {child}
    </Dropdown>
  );
};

export default SelectEnv;
