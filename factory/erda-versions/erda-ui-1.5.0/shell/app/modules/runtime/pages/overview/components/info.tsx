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
import { Popconfirm, Button, Dropdown, Menu } from 'antd';
import { IF, NoAuthTip, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo } from 'common/utils';
import RollbackList from './rollback-list';
import DeployStatus from './deploy-status';
import { usePerm } from 'app/user/common';
import orgStore from 'app/org-home/stores/org';
import routeInfoStore from 'core/stores/route';
import runtimeStore from 'runtime/stores/runtime';
import { get } from 'lodash';
import i18n from 'i18n';
import { confirmRedeploy } from '../containers';
import appStore from 'application/stores/application';

import './info.scss';

const { ELSE } = IF;
const cancelableDeployStatus = ['INIT', 'WAITING', 'DEPLOYING', 'CANCELING', 'WAITAPPROVE'];
const MenuItem = Menu.Item;

const evnBlockMap: { [key in APPLICATION.Workspace]: string } = {
  DEV: 'blockDev',
  TEST: 'blockTest',
  STAGING: 'blockStage',
  PROD: 'blockProd',
};

const DeployInfo = () => {
  const params = routeInfoStore.useStore((s) => s.params);
  const { blockStatus } = appStore.useStore((s) => s.detail);
  const appBlocked = blockStatus !== 'unblocked';
  const currentOrg = orgStore.useStore((s) => s.currentOrg);

  const { blockoutConfig } = currentOrg;

  const permMap = usePerm((s) => s.app);
  const [runtimeDetail, cancelDeploying] = runtimeStore.useStore((s) => [s.runtimeDetail, s.cancelDeploying]);

  const [state, updater] = useUpdate({
    visible: false,
  });

  const showSlidePanel = () => {
    runtimeStore.getRollbackList({ pageNo: 1, pageSize: 10 });
    updater.visible(true);
  };

  const onClose = () => {
    updater.visible(false);
  };

  const { deployStatus } = runtimeDetail;

  let showCancelBtn = false;
  let cancelable = false;
  const showDeployStatus = deployStatus !== 'OK' || !cancelable; // TODO: 是ok还是OK ？
  let cancelOperation = (force: boolean) => {};
  if (cancelableDeployStatus.includes(deployStatus)) {
    cancelOperation = (force: boolean) => runtimeStore.cancelDeployment({ force });
    showCancelBtn = true;
    cancelable = cancelDeploying || deployStatus === 'CANCELING';
  }

  const menu = (
    <Menu>
      <MenuItem>
        <div onClick={confirmRedeploy}>{i18n.t('runtime:restart')}</div>
      </MenuItem>
      <MenuItem>
        <div onClick={showSlidePanel}>{i18n.t('runtime:rollback')}</div>
      </MenuItem>
    </Menu>
  );

  const env = get(runtimeDetail, 'extra.workspace');
  const envBlocked = get(blockoutConfig, evnBlockMap[env], false);
  const isBlocked = envBlocked && appBlocked;
  const hasAuth = (permMap.runtime[`${(env || '').toLowerCase()}DeployOperation`] || {}).pass;
  return (
    <React.Fragment>
      <div className="runtime-operation-content top-button-group">
        <div className="deploy-status">{showDeployStatus && <DeployStatus deployStatus={deployStatus} />}</div>
        <div className="operation">
          <IF check={showCancelBtn}>
            <div>
              <IF check={cancelable}>
                <IF check={deployStatus === 'CANCELING'}>
                  <IF check={hasAuth}>
                    <Popconfirm
                      title={i18n.t('runtime:confirm force cancel?')}
                      onConfirm={() => cancelOperation(true)}
                      placement="bottomRight"
                    >
                      <Button className="runtime-operate cancel">
                        <ErdaIcon type="loading" />
                        <span>{i18n.t('runtime:force cancel')}</span>
                      </Button>
                    </Popconfirm>
                    <ELSE />
                    <NoAuthTip>
                      <Button className="runtime-operate cancel">
                        <ErdaIcon type="loading" />
                        <span>{i18n.t('runtime:force cancel')}</span>
                      </Button>
                    </NoAuthTip>
                  </IF>
                </IF>

                <ELSE />

                <IF check={hasAuth}>
                  <Popconfirm
                    title={i18n.t('runtime:confirm cancel deploy?')}
                    onConfirm={() => cancelOperation(false)}
                    placement="bottomRight"
                  >
                    <Button className="runtime-operate cancel">{i18n.t('runtime:cancel deployment')}</Button>
                  </Popconfirm>
                  <IF.ELSE />
                  <Button disabled className="runtime-operate cancel">
                    {i18n.t('runtime:cancel deployment')}
                  </Button>
                </IF>
              </IF>
            </div>
          </IF>
          <Button type="primary" ghost onClick={() => goTo(goTo.pages.appSetting_config, params)}>
            {i18n.t('runtime:deployment config')}
          </Button>
          <IF check={hasAuth}>
            <Dropdown overlay={menu} trigger={['click']} disabled={showCancelBtn || isBlocked}>
              <Button type="primary" disabled={showCancelBtn || isBlocked}>
                {i18n.t('runtime:deployment operation')}
                <ErdaIcon type="down" />
              </Button>
            </Dropdown>
            <ELSE />
            <NoAuthTip>
              <Button type="primary">
                {i18n.t('runtime:deployment operation')}
                <ErdaIcon type="down" />
              </Button>
            </NoAuthTip>
          </IF>
        </div>
      </div>
      <RollbackList visible={state.visible} onClose={onClose} />
    </React.Fragment>
  );
};

export default DeployInfo;
