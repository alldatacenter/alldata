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

import { Icon as CustomIcon, IF } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Dropdown, Input } from 'antd';
import React from 'react';
import { isEqual, isEmpty, debounce } from 'lodash';
import routeInfoStore from 'core/stores/route';
import monitorCommonStore from 'common/stores/monitorCommon';
import { useEffectOnce } from 'react-use';
import './appsSelector.scss';
import i18n from 'i18n';

interface IApp {
  [pro: string]: any;
  id: string;
  name: string;
}

const AppsSelector = () => {
  const { getProjectApps } = monitorCommonStore.effects;
  const { changeChosenApp } = monitorCommonStore.reducers;
  const [projectApps, appPaging, chosenApp] = monitorCommonStore.useStore((s) => [
    s.projectApps,
    s.projectAppsPaging,
    s.chosenApp,
  ]);
  const query = routeInfoStore.useStore((s) => s.query);

  const [{ searchKey }, updater] = useUpdate({
    searchKey: '',
  });

  useEffectOnce(() => {
    if (appPaging.pageNo === 1 && isEmpty(projectApps)) {
      // 只有当无数据的时候才请求
      getApp();
    }
  });

  React.useEffect(() => {
    if (isEmpty(chosenApp) && !isEmpty(projectApps)) {
      changeChosenApp({ chosenApp: projectApps[0] });
    }
  }, [changeChosenApp, chosenApp, projectApps]);

  React.useEffect(() => {
    const { appId, appName }: any = query || {};
    if (appId !== undefined && appName) {
      // 优先取路由上选中
      changeChosenApp({ chosenApp: { id: appId, name: appName } });
    }
  }, [changeChosenApp, query]);

  const getApp = (q?: object) => {
    getProjectApps({ loadMore: true, ...q });
  };

  const debounceSearch = debounce((_searchKey) => {
    getApp({
      q: _searchKey || undefined,
      pageNo: 1,
    });
  }, 1000);

  const filterList = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    updater.searchKey(e.target.value);
    debounceSearch(value);
  };

  const loadMore = (e: any) => {
    e.stopPropagation();
    const increasedPaging = { pageNo: appPaging.pageNo + 1, q: searchKey };
    getApp(increasedPaging);
  };

  const handleAppChange = (_chosenApp: IApp) => {
    onChangeChosenApp(_chosenApp);
    onCloseSwitch(false);
  };

  const onChangeChosenApp = (_chosenApp: IApp) => {
    if (!isEqual(chosenApp, _chosenApp)) {
      changeChosenApp({ chosenApp: _chosenApp });
    }
  };

  const onCloseSwitch = (visible: boolean) => {
    if (!visible && searchKey) {
      updater.searchKey('');
      getApp({ pageNo: 1 });
    }
  };

  const { name } = chosenApp as IApp;
  const nameStyle = `nowrap ${!name ? 'p-holder' : ''}`;
  const switchList = (
    <div className="app-selector-switcher w-full">
      <div className="input-wrap" onClick={(e) => e.stopPropagation()}>
        <Input placeholder={i18n.t('msp:search for')} onChange={filterList} value={searchKey} />
      </div>
      <ul>
        {projectApps &&
          projectApps.map((item) => {
            return (
              <li onClick={() => handleAppChange(item)} className="nowrap" key={item.id}>
                {item.name}
              </li>
            );
          })}
        <IF check={appPaging.hasMore}>
          <li onClick={loadMore}>
            <CustomIcon type="Loading" />
            {i18n.t('load more')}
          </li>
        </IF>
      </ul>
    </div>
  );
  return (
    <Dropdown
      overlayClassName="app-selector-dropdown"
      trigger={['click']}
      overlay={switchList}
      onVisibleChange={onCloseSwitch}
    >
      <div className="flex justify-between items-center app-selector-name">
        <span className={nameStyle}>{`${
          name ? `${i18n.t('msp:application')}: ${name}` : i18n.t('msp:switch application')
        }`}</span>
        <CustomIcon className="caret" type="caret-down" />
      </div>
    </Dropdown>
  );
};

export default AppsSelector;
