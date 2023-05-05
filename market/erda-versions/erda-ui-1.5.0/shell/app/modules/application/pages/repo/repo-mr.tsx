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

import { Button, Alert } from 'antd';
import React from 'react';
import { goTo } from 'common/utils';
import { RepoMrTable } from './components/repo-mr-table';
import i18n from 'i18n';
import repoStore from 'application/stores/repo';
import routeInfoStore from 'core/stores/route';
import { WithAuth, usePerm } from 'user/common';
import { IF } from 'common';

export const mrTabs = () => {
  const info = repoStore.useStore((s) => s.info);
  return [
    {
      key: 'all',
      name: i18n.t('all'),
    },
    {
      key: 'open',
      name: (
        <span>
          {i18n.t('dop:committed')}
          <span className="dice-badge">{info ? info.mergeRequestCount : 0}</span>
        </span>
      ),
    },
    {
      key: 'merged',
      name: i18n.t('dop:have merged'),
    },
    {
      key: 'closed',
      name: i18n.t('closed'),
    },
  ];
};

const PureRepoMR = () => {
  const info = repoStore.useStore((s) => s.info);
  const params = routeInfoStore.useStore((s) => s.params);
  const permObj = usePerm((s) => s.app.repo.mr);
  const { mrType = 'open' } = params;

  return (
    <div>
      <div className="top-button-group">
        <WithAuth pass={permObj.create} tipProps={{ placement: 'bottom' }}>
          <Button
            disabled={info.empty || info.isLocked}
            type="primary"
            onClick={() => goTo('./createMR', { forbidRepeat: true })}
          >
            {i18n.t('dop:new merge request')}
          </Button>
        </WithAuth>
      </div>
      <IF check={info.isLocked}>
        <Alert message={i18n.t('lock-repository-tip')} type="error" />
      </IF>
      <RepoMrTable key={mrType} type={mrType as REPOSITORY.MrType} />
    </div>
  );
};

export default PureRepoMR;
