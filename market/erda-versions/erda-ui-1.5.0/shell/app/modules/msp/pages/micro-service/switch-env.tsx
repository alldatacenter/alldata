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
import routeInfoStore from 'core/stores/route';
import { Dropdown, Menu } from 'antd';
import mspStore from 'msp/stores/micro-service';
import { ErdaIcon } from 'common';
import { goTo } from 'common/utils';

const SwitchEnv = () => {
  const [{ relationship, id }] = mspStore.useStore((s) => [s.currentProject]);
  const [{ env }] = routeInfoStore.useStore((s) => [s.params]);
  const [currentEnv, setEnv] = React.useState(env);
  React.useEffect(() => {
    setEnv(env);
  }, [env]);
  const [menu, envName] = React.useMemo(() => {
    const handleChangeEnv = ({ key }: { key: string }) => {
      const selectEnv = relationship.find((item) => item.workspace === key);
      if (selectEnv && key !== currentEnv) {
        goTo(goTo.pages.mspOverview, { tenantGroup: selectEnv.tenantId, projectId: id, env: key });
      }
    };
    const { displayWorkspace, workspace } = relationship?.find((t) => t.workspace === currentEnv) ?? {};
    return [
      <Menu onClick={handleChangeEnv}>
        {relationship?.map((item) => {
          return (
            <Menu.Item
              className={`${workspace === item.workspace ? 'bg-light-primary text-primary' : ''}`}
              key={item.workspace}
            >
              {item.displayWorkspace}
            </Menu.Item>
          );
        })}
      </Menu>,
      displayWorkspace,
    ];
  }, [relationship, currentEnv]);
  return (
    <div className="px-3 mt-2">
      <Dropdown overlay={menu} trigger={['click']}>
        <div className="font-bold text-base h-8 rounded border border-solid border-transparent flex justify-center cursor-pointer hover:border-primary">
          <span className="self-center">{envName}</span>
          <ErdaIcon className="self-center" type="caret-down" size="16" />
        </div>
      </Dropdown>
    </div>
  );
};

export default SwitchEnv;
