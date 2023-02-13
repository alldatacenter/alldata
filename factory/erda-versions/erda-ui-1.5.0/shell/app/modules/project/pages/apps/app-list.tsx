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

import { goTo } from 'common/utils';
import React from 'react';
import { Button } from 'antd';
import { PureAppList } from 'application/common/app-list-protocol';
import { WithAuth, usePerm } from 'app/user/common';
import i18n from 'i18n';
import projectStore from 'project/stores/project';
import { useLoading } from 'core/stores/loading';

export const ProjectAppList = () => {
  const [loading] = useLoading(projectStore, ['getProjectApps']);
  const { getProjectApps } = projectStore.effects;
  const { clearProjectAppList } = projectStore.reducers;

  const permMap = usePerm((s) => s.project);

  return (
    <React.Fragment>
      <div className="top-button-group">
        <WithAuth pass={permMap.addApp} disableMode={false} tipProps={{ placement: 'bottom' }}>
          <Button type="primary" onClick={() => goTo('./createApp')}>
            {i18n.t('add application')}
          </Button>
        </WithAuth>
      </div>
      <PureAppList getList={getProjectApps} clearList={clearProjectAppList} isFetching={loading} isInProject />
    </React.Fragment>
  );
};
