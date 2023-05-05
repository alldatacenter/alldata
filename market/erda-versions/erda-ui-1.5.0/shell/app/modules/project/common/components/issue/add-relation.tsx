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

import { MemberSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Button, Select } from 'antd';
import React from 'react';
import i18n from 'i18n';
import { getJoinedApps } from 'app/user/services/user';
import routeInfoStore from 'core/stores/route';
import projectStore from 'project/stores/project';
import { useMount } from 'react-use';
import { getAppMR } from 'application/services/repo';
import { WithAuth } from 'user/common';

import './add-relation.scss';

interface IProps {
  editAuth: boolean;
  onSave: (v: ISSUE.IssueStreamBody) => void;
}

const initState = {
  visible: false,
  appList: [],
  selectApp: null,
  mrList: [],
  selectMr: null,
  selectedCreator: undefined,
};
export const AddRelation = ({ onSave, editAuth }: IProps) => {
  const { projectId } = routeInfoStore.getState((s) => s.params);
  const { name: projectName } = projectStore.getState((s) => s.info);

  const [{ visible, appList, selectApp, selectedCreator, mrList, selectMr }, updater, update] = useUpdate(initState);

  const getMyProjectApps = (extra = {}) => {
    update({
      mrList: [],
      selectApp: null,
      selectMr: null,
    });
    getJoinedApps({ projectID: +projectId, pageSize: 50, pageNo: 1, ...extra }).then((res: any) => {
      if (res.success) {
        res.data.list && updater.appList(res.data.list);
      }
    });
  };

  useMount(() => {
    getMyProjectApps();
  });

  const getAppMr = React.useCallback(
    (query?: string) => {
      if (selectApp) {
        update({
          mrList: [],
          selectMr: null,
        });
        getAppMR({ projectName, appName: selectApp.name, query, authorId: selectedCreator }).then((res: any) => {
          if (res.success) {
            res.data.list && updater.mrList(res.data.list);
          } else {
            updater.mrList([]);
          }
        });
      }
    },
    [projectName, selectApp, selectedCreator, update, updater],
  );

  React.useEffect(() => {
    getAppMr();
  }, [getAppMr, selectApp, selectedCreator]);

  const onClose = () => {
    update({ ...initState, appList });
  };

  if (!visible) {
    return (
      <WithAuth pass={editAuth}>
        <Button className="ml-3" onClick={() => updater.visible(true)}>
          {i18n.t('dop:relate to mr')}
        </Button>
      </WithAuth>
    );
  }

  return (
    <div className="issue-comment-box flex justify-between items-center mt-3">
      <div className="flex justify-between items-center flex-1">
        <Select
          className="filter-select"
          onSearch={(q) => getMyProjectApps({ q })}
          onSelect={(v) => updater.selectApp(appList.find((a) => a.id === v))}
          showSearch
          value={selectApp ? selectApp.id : undefined}
          filterOption={false}
          placeholder={i18n.t('dop:search by application name')}
        >
          {appList.map(({ id, name }) => (
            <Select.Option key={id} value={id}>
              {name}
            </Select.Option>
          ))}
        </Select>
        <MemberSelector
          className="filter-select"
          scopeType="app"
          scopeId={selectApp && selectApp.id}
          onChange={(val: any) => updater.selectedCreator(val)}
          value={selectedCreator}
          placeholder={i18n.t('dop:filter by creator')}
          extraQuery={{ scopeId: selectApp && selectApp.id }}
        />
        <Select
          className="filter-select"
          onSearch={(q) => getAppMr(q)}
          onSelect={(v) => updater.selectMr(mrList.find((a) => a.id === v))}
          showSearch
          value={selectMr ? selectMr.id : undefined}
          filterOption={false}
          placeholder={i18n.t('dop:search by id or title')}
        >
          {mrList.map(({ id, title }) => (
            <Select.Option key={id} value={id}>
              {title}
            </Select.Option>
          ))}
        </Select>
      </div>
      <Button
        type="primary"
        className="ml-3"
        disabled={!(selectApp && selectMr)}
        onClick={() => {
          if (selectApp && selectMr) {
            onSave({
              content: '',
              type: 'RelateMR',
              mrInfo: {
                appID: selectApp.id,
                mrID: selectMr.mergeId,
                mrTitle: selectMr.title,
              },
            });
            onClose();
          }
        }}
      >
        {i18n.t('ok')}
      </Button>
      <Button type="link" onClick={onClose}>
        {i18n.t('cancel')}
      </Button>
    </div>
  );
};
