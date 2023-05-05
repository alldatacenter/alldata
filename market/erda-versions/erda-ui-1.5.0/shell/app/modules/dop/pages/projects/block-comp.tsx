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
import i18n from 'i18n';
import { Alert, Tooltip } from 'antd';
import { Icon as CustomIcon } from 'common';
import { map } from 'lodash';
import moment from 'moment';
import orgStore from 'app/org-home/stores/org';

const envMap = {
  blockDev: i18n.t('default:dev environment'),
  blockTest: i18n.t('default:test environment'),
  blockStage: i18n.t('default:staging environment'),
  blockProd: i18n.t('default:prod environment'),
};

export const BlockNetworkTips = () => {
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const { show, message } = React.useMemo(() => {
    const { blockoutConfig } = currentOrg;
    const envs: string[] = [];
    map(blockoutConfig, (value, key) => {
      if (value) {
        envs.push(envMap[key]);
      }
    });
    return {
      show: !!envs.length,
      message: envs.join(','),
    };
  }, [currentOrg]);
  return show ? (
    <Alert
      className="mb-4"
      showIcon
      type="error"
      message={i18n.t('default:Deployment not allowed in {env} in network block period.', { env: message })}
    />
  ) : null;
};

interface IProps {
  scope: 'project' | 'app';
  unBlockEnd?: string;
  unBlockStart?: string;
  canOperate?: boolean;
  status: PROJECT.BlockStatus;
  onClick?: (key: string) => void;
}

const BlockNetworkStatus = ({ status, canOperate = false, onClick, scope, unBlockStart, unBlockEnd }: IProps) => {
  const handleClick = (e: React.MouseEvent<HTMLSpanElement>) => {
    e.stopPropagation();
    onClick && onClick(status);
  };
  if (!status) {
    return null;
  }
  const period =
    unBlockEnd && unBlockStart && scope === 'app' ? (
      <span className="text-desc ml-3">
        {i18n.t('dop:time period')}: {moment(unBlockStart).format('YYYY-MM-DD HH:mm')}~
        {moment(unBlockEnd).format('YYYY-MM-DD HH:mm')}
      </span>
    ) : null;
  let unBlock = null;
  if (scope === 'project') {
    unBlock = canOperate ? (
      <span className="text-primary ml-3 unblock-btn" onClick={handleClick}>
        {i18n.t('dop:apply to deploy')}
      </span>
    ) : (
      <Tooltip title={i18n.t('You do not have enough permissions')}>
        <span className="not-allowed ml-3 unblock-btn">{i18n.t('dop:apply to deploy')}</span>
      </Tooltip>
    );
  }
  const statusMap: { [key in PROJECT.BlockStatus]: React.ReactNode } = {
    blocked: (
      <>
        {unBlock}
        {period}
      </>
    ),
    unblocking: (
      <>
        <span className="inline-flex justify-between items-center text-yellow">
          <CustomIcon type="lock1" className="text-yellow" />
          {i18n.t('default:unblocking, please wait')}
        </span>
        {period}
        {unBlock}
      </>
    ),
    unblocked: (
      <>
        <span className="inline-flex justify-between items-center text-green">
          <CustomIcon type="unlock1" className="text-green" />
          {i18n.t('default:unblocked')}
        </span>
        {period}
        {unBlock}
      </>
    ),
  };
  return <>{statusMap[status]}</>;
};

export default BlockNetworkStatus;
