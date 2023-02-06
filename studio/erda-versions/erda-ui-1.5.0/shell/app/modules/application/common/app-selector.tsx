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
import { LoadMoreSelector, ErdaIcon } from 'common';
import { goTo } from 'common/utils';
import { map, isArray, filter, isEmpty, find, get } from 'lodash';
import { Tooltip } from 'antd';
import i18n from 'i18n';
import { getApps } from 'common/services';
import routeInfoStore from 'core/stores/route';
import appStore from 'application/stores/application';
import { getAppDetail } from 'application/services/application';
import './app-selector.scss';

interface IProps {
  [pro: string]: any;
  value: string | number;
  projectId?: string;
  onClickItem?: (arg: IApplication) => void;
  onChange?: (arg: number) => void;
}

const AppItem = (app: IApplication) => {
  return (
    <Tooltip key={app.id} title={app.name}>
      {app.displayName || app.name}
    </Tooltip>
  );
};

interface IChosenItem {
  value: string;
  label?: string;
}
export const chosenItemConvert = (values: IChosenItem[] | IChosenItem) => {
  const curApp = appStore.getState((s) => s.detail);
  const curValues = isArray(values) ? values : values && [values];
  const existApp = {};
  if (curApp.id) existApp[curApp.id] = curApp;
  const reValue = map(curValues, (item) => {
    const appItem = existApp[item.value] || {};
    return item.label ? item : { ...appItem, ...item, label: appItem.displayName || appItem.name };
  });

  const lackLabelItems = filter(reValue, (item) => !item.label);
  if (isEmpty(lackLabelItems)) {
    return reValue;
  }

  // if there are some lack label items, then get label from servicel
  return Promise.all(map(lackLabelItems, (item) => getAppDetail(item.value))).then((res: IApplication[]) => {
    const newValue = map(reValue, (val) => {
      const appItem = find(map(res, 'data'), (resItem) => `${resItem.id}` === `${val.value}`);
      return appItem ? { ...appItem, ...val, label: appItem.displayName || appItem.name } : { ...val };
    });
    return newValue;
  });
};

export const AppSelector = (props: IProps) => {
  const { projectId: _projectId, ...rest } = props;
  const pId = routeInfoStore.useStore((s) => s.params.projectId);
  const projectId = _projectId || pId;
  const getData = (_q: Obj = {}) => {
    if (!projectId) return;
    return getApps({ projectId, ..._q } as any).then((res: any) => res.data);
  };

  return (
    <LoadMoreSelector
      getData={getData}
      placeholder={i18n.t('common:search by {name}', { name: i18n.t('application') })}
      dataFormatter={({ list, total }) => ({
        total,
        list: map(list, (item) => ({ ...item, label: item.displayName || item.name, value: item.id })),
      })}
      optionRender={AppItem}
      chosenItemConvert={(v: IChosenItem[] | IChosenItem) => chosenItemConvert(v)}
      {...rest}
    />
  );
};

const headAppRender = (val: any = {}) => {
  const curApp = appStore.getState((s) => s.detail);
  const name = val.displayName || val.name || curApp.displayName || curApp.name || '';
  return (
    <div className="head-app-name">
      <span className="nowrap text-base font-bold" title={name}>
        {name}
      </span>
      <ErdaIcon type="caret-down" size="16" className="align-middle caret ml-1" />
    </div>
  );
};

export const HeadAppSelector = () => {
  const { appId, projectId } = routeInfoStore.useStore((s) => s.params);
  return (
    <div className="head-app-selector mt-2">
      <AppSelector
        valueItemRender={headAppRender}
        value={appId}
        onClickItem={(app: IApplication) => {
          goTo(goTo.pages.app, { projectId, appId: app.id }); // 切换app
        }}
      />
    </div>
  );
};
