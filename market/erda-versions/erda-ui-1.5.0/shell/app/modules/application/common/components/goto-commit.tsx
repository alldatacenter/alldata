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
import { ErdaIcon } from 'common';
import { goTo } from 'common/utils';

import './goto-commit.scss';
import routeInfoStore from 'core/stores/route';

export const getCommitPath = (commitId: string) => {
  const path = `/repo/commit/${commitId}`;
  return window.location.pathname.replace(/(\/projects\/\d+\/apps\/\d+)(.+)/, `$1${path}`);
};

const GotoCommit = ({ projectId, appId, commitId = '', length = 6, className = '', gotoParams = {} }: any) => {
  const params = routeInfoStore.useStore((s) => s.params);
  return (
    <span
      className={`goto-commit-link items-center inline-flex text-link ${className}`}
      onClick={() => {
        goTo(goTo.pages.commit, {
          projectId: projectId || params.projectId,
          appId: appId || params.appId,
          commitId,
          ...gotoParams,
        });
      }}
    >
      <ErdaIcon className="mr-1" fill="primary-800" size="16" type="commit" />
      <span>{commitId.slice(0, length)}</span>
    </span>
  );
};

export default GotoCommit;
