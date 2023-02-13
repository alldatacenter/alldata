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
import { Icon as CustomIcon } from 'common';
import './deploy-status.scss';
import i18n from 'i18n';

const runtimeDeployStatus = {
  INIT: {
    color: 'gray',
    text: i18n.t('runtime:initializing'),
    animation: 'running',
  },
  WAITING: {
    color: 'blue',
    text: i18n.t('runtime:waiting for deployment'),
    animation: 'running',
  },
  DEPLOYING: {
    color: 'blue',
    text: i18n.t('deploying'),
    animation: 'running',
  },
  CANCELING: {
    color: 'yellow',
    text: i18n.t('runtime:cancel deploying'),
    animation: 'running',
  },
  OK: null,
  CANCELED: {
    color: 'yellow',
    text: i18n.t('runtime:deployment canceled'),
    animation: '',
  },
  FAILED: {
    color: 'red',
    text: i18n.t('runtime:deployment failed'),
    animation: '',
  },
};

interface IStatus {
  deployStatus: RUNTIME.DeployStatus;
}
const DeployStatus = ({ deployStatus }: IStatus) => {
  const state = runtimeDeployStatus[deployStatus];
  if (!state) {
    return null;
  }

  return (
    <span className="deploy-status">
      {state.animation && <span className={`light-circle ${state.color}`} />}
      <CustomIcon type="shandian" className={`${state.color} ${state.animation}`} />
      <span className="deploy-status-text">{state.text}</span>
    </span>
  );
};

export default DeployStatus;
