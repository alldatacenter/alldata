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
import { map, get, set, filter, uniq } from 'lodash';
import { Tabs, Button } from 'antd';
import { Prompt } from 'react-router-dom';
import { DebounceSearch } from 'common';
import { useUpdate } from 'common/use-hooks';
import routeInfoStore from 'core/stores/route';
import { orgPerm, orgRoleMap } from 'user/stores/_perm-org';
import { appPerm, appRoleMap } from 'user/stores/_perm-app';
import { projectPerm, projectRoleMap } from 'user/stores/_perm-project';
import { mspPerm, mspRoleMap } from 'user/stores/_perm-msp';
import i18n from 'i18n';
import PermExport from './perm-export';
import PermRoleEditor, { IRoleData } from './role-editor';
import { produce } from 'immer';
import { IRoleChange, PermTable } from './perm-table';
import AddScope from './add-scope';
import './perm-editor.scss';

const { TabPane } = Tabs;

interface IAction {
  name: string;
  role: string[];
}

interface IPermItem {
  name: string;
  children?: IPerm;
  actions?: IAction[];
}

interface IPerm {
  [key: string]: IPermItem;
}

export const roleMaps = {
  org: orgRoleMap,
  app: appRoleMap,
  project: projectRoleMap,
};

const permDatas = {
  org: orgPerm,
  project: projectPerm,
  app: appPerm,
};

const defaultRole = {
  Manager: { name: i18n.t('administrator'), value: 'Manager' },
};

const getRoleMap = (_roleMap: Obj, isEdit: boolean) => {
  if (isEdit) return _roleMap;
  const reRoleMap = {};
  map(_roleMap, (item, key) => {
    const subRoleMap = {};
    map(item, (subItem, subKey) => {
      if (!subItem.isBuildIn) subRoleMap[subKey] = { ...subItem };
    });
    reRoleMap[key] = subRoleMap;
  });
  return reRoleMap;
};

interface IProps {
  data: Obj;
  roleMap: Obj;
  scope?: string;
}

export const PermEditor = (props: IProps) => {
  const [{ scope = 'org', mode }, { projectId }] = routeInfoStore.getState((s) => [s.query, s.params]);
  const isMsp = props.scope === 'msp';
  const permData = isMsp ? props.data : permDatas;
  const originRoleMap = isMsp ? props.roleMap : roleMaps;
  const defaultScope = isMsp ? props.scope : scope;
  const [{ data, tabKey, searchKey, roleMap, reloadKey }, updater, update] = useUpdate({
    data: permData,
    tabKey: defaultScope,
    searchKey: '',
    roleMap: originRoleMap,
    reloadKey: 1,
  });

  // 默认在项目下，即为编辑状态： project/:id/perm
  // 在根路由下，即为查看状态:/perm
  const isEdit = isMsp ? mode === 'edit' : !!projectId;
  const onChangeRole = (_data: IRoleChange) => {
    const { key, role, checked } = _data;
    const newData = produce(data, (draft) => {
      if (role) {
        const curDataRole = get(draft, `${key}.role`);
        set(draft, `${key}.role`, checked ? uniq([...curDataRole, role]) : filter(curDataRole, (r) => r !== role));
      }
    });
    updater.data(newData);
  };

  const deleteData = (_dataKey: string) => {
    const newData = produce(data, (draft) => {
      set(draft, _dataKey, undefined);
    });
    updater.data(newData);
  };

  const editData = (val: Obj) => {
    const { key, name, keyPath, preKey, subData } = val;
    const newData = produce(data, (draft) => {
      const curKeyPath = keyPath ? `${keyPath}.` : '';
      const curData = get(draft, `${curKeyPath}${preKey}`);
      if (subData) {
        set(draft, `${curKeyPath}${preKey}`, { ...curData, ...subData });
      } else {
        set(draft, `${curKeyPath}${preKey}`, undefined);
        set(draft, `${curKeyPath}${key}`, { ...curData, name });
      }
    });
    updater.data(newData);
  };

  const reset = () => {
    update({
      data: permData,
      tabKey: defaultScope,
      searchKey: '',
      roleMap: originRoleMap,
      reloadKey: reloadKey + 1,
    });
  };

  const addScope = (newScope: Obj) => {
    const { key, name } = newScope;
    update({
      data: { ...data, [key]: { name, test: { pass: false, name: '测试权限', role: ['Manager'] } } },
      roleMap: { ...originRoleMap, [key]: defaultRole },
    });
  };

  const updateRole = (roleData: Obj<IRoleData>) => {
    update({
      roleMap: {
        ...roleMap,
        [tabKey]: roleData,
      },
      reloadKey: reloadKey + 1,
    });
  };
  return (
    <div className="dice-perm-editor h-full">
      {isEdit ? (
        <div className="top-button-group">
          <AddScope onSubmit={addScope} currentData={data} />
          <Button type="primary" ghost onClick={reset}>
            {i18n.t('reset')}
          </Button>
        </div>
      ) : null}
      <Prompt when={isEdit} message={`${i18n.t('Are you sure to leave?')}?`} />
      <Tabs
        activeKey={tabKey}
        tabBarExtraContent={
          <div className="flex justify-between items-center mt-2">
            <DebounceSearch size="small" value={searchKey} className="mr-2" onChange={updater.searchKey} />
            {isEdit ? (
              <>
                <PermExport
                  activeScope={tabKey}
                  roleMap={roleMap}
                  data={data}
                  projectId={projectId}
                  onSubmit={updater.data}
                  isEdit
                />
                <PermRoleEditor data={roleMap[tabKey]} updateRole={updateRole} />
              </>
            ) : null}
          </div>
        }
        renderTabBar={(p: any, DefaultTabBar) => <DefaultTabBar {...p} onKeyDown={(e: any) => e} />}
        onChange={(curKey: string) => update({ searchKey: '', tabKey: curKey })}
      >
        {map(data, (item: IPermItem, key: string) => {
          if (!item) return null;
          const { name } = item;
          return (
            <TabPane tab={name} key={key}>
              <PermTable
                data={item}
                scope={key}
                key={reloadKey}
                filterKey={searchKey}
                isEdit={isEdit}
                roleMap={getRoleMap(roleMap, isEdit)}
                onChangeRole={onChangeRole}
                deleteData={deleteData}
                editData={editData}
                originData={permData}
                originRoleMap={originRoleMap}
                currentData={data}
              />
            </TabPane>
          );
        })}
      </Tabs>
    </div>
  );
};

export const MspPermEditor = () => {
  return <PermEditor data={{ msp: mspPerm }} roleMap={{ msp: mspRoleMap }} scope="msp" />;
};
