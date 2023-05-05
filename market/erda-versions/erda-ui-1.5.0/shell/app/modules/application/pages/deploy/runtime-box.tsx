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
import { DeleteConfirm, Avatar, IF, MenuPopover, ErdaIcon } from 'common';
import { cutStr, goTo, fromNow } from 'common/utils';
import { Spin, Tooltip, Alert } from 'antd';
import HealthPoint from 'project/common/components/health-point';
import routeInfoStore from 'core/stores/route';
import appStore from 'application/stores/application';
import i18n, { isZh } from 'i18n';
import { find, get } from 'lodash';
import { WithAuth, usePerm } from 'app/user/common';
import { approvalStatusMap } from 'application/pages/deploy-list/deploy-list';
import './runtime-box.scss';

const envMap = {
  DEV: i18n.t('develop'),
  TEST: i18n.t('test'),
  STAGING: i18n.t('staging'),
  PROD: i18n.t('prod'),
};

interface IProps {
  id: number;
  name: string;
  env: string;
  releaseId: string;
  status: string;
  addonState: string;
  extra: { fakeRuntime: boolean };
  deleteStatus: string;
  lastOperatorName: string;
  lastOperatorAvatar: string;
  lastOperateTime: string;
  deployStatus: string;
  canDeploy: boolean;
  onDelete: (runtimeId: string) => void;
  onRestart: (runtimeId: string) => void;
  onUpdate: () => void;
}

const RuntimeBox = (props: IProps) => {
  const permMap = usePerm((s) => s.app.runtime);
  const branchInfo = appStore.getState((s) => s.branchInfo);
  const params = routeInfoStore.getState((s) => s.params);
  const branchAuthObj = usePerm((s) => s.app.pipeline);
  const popoverComp =
    (isDeploying: boolean, id: number, onDelete: Function, onRestart: Function, env: string, branch: string) =>
    (setVisible: Function) => {
      const updateAuth = get(find(branchInfo, { name: branch }), 'isProtect')
        ? branchAuthObj.executeProtected.pass
        : branchAuthObj.executeNormal.pass;
      return (
        <div>
          <WithAuth pass={updateAuth}>
            <span
              className="popover-item"
              onClick={(e) => {
                e.stopPropagation();
                props.onUpdate();
                setVisible(false);
              }}
            >
              {i18n.t('update')}
            </span>
          </WithAuth>
          <DeleteConfirm
            onConfirm={() => {
              onDelete(id.toString());
              setVisible(false);
            }}
            onShow={() => {
              setVisible(false);
            }}
            countDown={3}
            secondTitle={
              <span>
                {i18n.t('dop:confirm to delete Runtime')}:{' '}
                <b>{isZh() ? `${envMap[env.toUpperCase()]}环境的 【${branch}】` : `【${branch}】 in ${env}`}</b>
              </span>
            }
          >
            <WithAuth pass={permMap[`${env}Delete`]} disableMode={false}>
              <span className="popover-item">{i18n.t('delete')}</span>
            </WithAuth>
          </DeleteConfirm>
          <WithAuth pass={permMap[`${env}DeployOperation`]} disableMode={false}>
            <span
              className={isDeploying ? 'popover-item disabled' : 'popover-item'}
              onClick={(e) => {
                e.stopPropagation();
                if (!isDeploying) {
                  onRestart(id.toString());
                }
                setVisible(false);
              }}
            >
              {i18n.t('dop:restart')}
            </span>
          </WithAuth>
        </div>
      );
    };

  const gotoRelease = (_releaseId: string, e: any) => {
    e.stopPropagation();
    goTo(goTo.pages.release, { ...params, q: _releaseId });
  };

  const gotoRuntime = (runtimeId: number, e: any) => {
    e.stopPropagation();
    goTo(goTo.pages.runtimeDetail, { ...params, runtimeId });
  };

  const {
    id,
    name,
    releaseId,
    status,
    deleteStatus,
    lastOperatorName,
    addonState,
    lastOperatorAvatar,
    lastOperateTime,
    deployStatus,
    onDelete,
    onRestart,
    extra,
  } = props;

  const fakeRuntime = extra?.fakeRuntime;

  const isDeploying = ['DEPLOYING'].includes(deployStatus);
  const env = props.env.toLowerCase();
  const isWaitApprove = deployStatus.toLowerCase() === approvalStatusMap.WaitApprove.value.toLowerCase();

  const addonStateMap = {
    sqlAcctountChanging: i18n.t('dop:mysql addon account switching, please restart to take effect'),
  };

  if (fakeRuntime) {
    return (
      <div className="flex justify-between items-center runtime-box">
        <div className="flex justify-between items-center runtime-box-header">
          <div className="branch disabled">
            <ErdaIcon fill="black-800" width="20" height="21" type="slbb" />
            <Tooltip title={name}>
              <span className="font-bold nowrap">{name}</span>
            </Tooltip>
          </div>
        </div>
        <Alert message={i18n.t('initializing, please wait')} type="warning" showIcon />
      </div>
    );
  }

  return (
    <Spin spinning={deleteStatus === 'DELETING'} tip={i18n.t('dop:deleting')}>
      <div
        className={`flex justify-between items-center runtime-box ${isWaitApprove ? 'large' : ''}`}
        onClick={(e) => gotoRuntime(id, e)}
      >
        <div className="flex justify-between items-center runtime-box-header">
          <div className="branch">
            <ErdaIcon className="mr-1 mt-0.5" color="black-800" fill="black-800" width="20" height="21" type="slbb" />
            <Tooltip title={name}>
              <span className="font-bold nowrap">{name}</span>
            </Tooltip>
          </div>
          <div>
            <IF check={addonStateMap[addonState]}>
              <Tooltip title={addonStateMap[addonState]}>
                <ErdaIcon className="mr-1" type="caution" color="orange" size={16} />
              </Tooltip>
            </IF>
            <IF
              check={
                props.canDeploy &&
                deleteStatus !== 'DELETING' &&
                (permMap[`${env}Delete`].pass || permMap[`${env}DeployOperation`].pass)
              }
            >
              <MenuPopover content={popoverComp(isDeploying, id, onDelete, onRestart, env, name)} />
            </IF>
          </div>
        </div>

        {releaseId ? (
          <div className="transform-box">
            <Tooltip title={i18n.t('dop:view version information')}>
              <span className="text-link release-link" onClick={(e) => gotoRelease(releaseId, e)}>
                <ErdaIcon className="mr-1 transform-icon" fill="primary-800" width="20" height="21" type="bb" />
                <span>{cutStr(releaseId, 6, { suffix: '' })}</span>
              </span>
            </Tooltip>
          </div>
        ) : null}
        <div className="flex justify-between items-center runtime-box-body">
          <div className="flex justify-between items-center">
            <Avatar name={lastOperatorName} url={lastOperatorAvatar} className="mr-1" size={20} />
            {lastOperatorName || ''}
            <span className="deploy-time">{lastOperateTime ? fromNow(lastOperateTime) : ''}</span>
          </div>

          {['Healthy', 'OK'].includes(status) ? null : <HealthPoint type="runtime" status={status} />}
        </div>
        {isWaitApprove ? <Alert message={i18n.t('dop:project admin confirming')} type="info" showIcon /> : null}
      </div>
    </Spin>
  );
};

export default RuntimeBox;
