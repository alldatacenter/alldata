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
import { Tabs, Button, Tooltip } from 'antd';
import PipelineConfigDetail from './config-detail';
import PipelineRunDetail from './run-detail';
import routeInfoStore from 'core/stores/route';
import buildStore from 'application/stores/build';
import fileTreeStore from 'common/stores/file-tree';
import yaml from 'js-yaml';
import { useUpdate } from 'common/use-hooks';
import { updateSearch } from 'common/utils';
import orgStore from 'app/org-home/stores/org';
import { useMount } from 'react-use';
import { get, isEmpty, find } from 'lodash';
import { WithAuth, usePerm } from 'user/common';
import appStore from 'application/stores/application';
import { useLoading } from 'core/stores/loading';
import commonStore from 'common/stores/common';

import { getBranchPath } from './config';
import i18n from 'i18n';

interface IProps {
  nodeId?: string;
  addDrawerProps?: Obj;
  scopeParams: { scope: string; scopeID: string };
  scope: string;
}

const envBlockKeyMap = {
  DEV: 'blockDev',
  TEST: 'blockTest',
  PROD: 'blockProd',
  STAGE: 'blockStage',
};

const PipelineDetail = (props: IProps) => {
  const { nodeId: propsNodeId, addDrawerProps = {}, scope, ...rest } = props || {};
  const [caseDetail] = fileTreeStore.useStore((s) => [s.curNodeDetail]);
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const orgBlockoutConfig = orgStore.useStore((s) => s.currentOrg.blockoutConfig);
  const nodeId = propsNodeId || query.nodeId;
  // mobile_init is a special node, only allowed to run pipeline
  const isMobileInit = nodeId === 'mobile_init';
  const { branch, path, env } = getBranchPath(caseDetail, params.appId);
  const { addPipeline } = buildStore.effects;
  const { clearExecuteRecords } = buildStore.reducers;
  const [deployPerm, branchAuthObj] = usePerm((s) => [s.app.runtime, s.app.repo.branch]);
  const [branchInfo, appBlockStatus] = appStore.useStore((s) => [s.branchInfo, s.detail?.blockStatus]);
  const [loading] = useLoading(commonStore, ['getRenderPageLayout']);
  const envBlocked = get(orgBlockoutConfig, envBlockKeyMap[env], false);

  const [{ activeKey, runKey, canRunTest }, updater, update] = useUpdate({
    activeKey: isMobileInit ? 'runDetail' : 'configDetail',
    runKey: 1,
    canRunTest: true,
  });

  useMount(() => {
    query.pipelineID && updater.activeKey('runDetail');
  });

  const getDeployAuth = () => {
    // depoloy auth, same to deploy center
    if (envBlocked && appBlockStatus !== 'unblocked') {
      // network blocked
      return {
        hasAuth: false,
        authTip: i18n.t('dop:Function unavailable in network block period.'),
      };
    }
    if (!deployPerm[`${(env || 'dev').toLowerCase()}DeployOperation`]) {
      // no auth
      return { hasAuth: false };
    } else if (isMobileInit) {
      return { hasAuth: true };
    }

    const ymlStr = get(caseDetail, 'meta.pipelineYml') || '';
    let ymlObj = {} as any;
    if (ymlStr) {
      try {
        ymlObj = yaml.load(ymlStr);
      } catch (e) {
        // eslint-disable-next-line no-console
        console.error(e);
      }
    }
    const hasUseableYml = ymlObj?.stages && !isEmpty(ymlObj?.stages);
    if (!hasUseableYml) {
      return {
        hasAuth: false,
        authTip: i18n.t('dop:please add valid tasks to the pipeline below before operating'),
      };
    }
    return { hasAuth: true };
  };

  const getEditAuth = () => {
    // edit auth, same to repo
    const isProtectBranch = get(find(branchInfo, { name: branch }), 'isProtect');
    const branchAuth = isProtectBranch ? branchAuthObj.writeProtected.pass : branchAuthObj.writeNormal.pass;
    const authTip = isProtectBranch ? i18n.t('dop:branch is protected, you have no permission yet') : undefined;
    return { hasAuth: branchAuth, authTip };
  };

  // deploy auth not same as edit auth
  const deployAuthObj = getDeployAuth();
  const editAuthObj = getEditAuth();

  const onCaseChange = (bool: boolean) => {
    updater.canRunTest(bool);
  };

  const addNewPipeline = () => {
    const postData = {
      appID: +params.appId,
      branch,
      pipelineYmlName: path,
      pipelineYmlSource: 'gittar',
      source: 'dice',
    };
    addPipeline(postData).then(() => {
      clearExecuteRecords();
      if (activeKey !== 'runDetail') updater.activeKey('runDetail');
      updateSearch({ pipelineID: undefined });
      updater.runKey((pre: number) => pre + 1);
    });
  };

  return (
    <>
      <Tabs
        tabBarExtraContent={
          isMobileInit ? null : canRunTest ? (
            <WithAuth pass={deployAuthObj.hasAuth} noAuthTip={deployAuthObj.authTip}>
              <Button type="primary" onClick={addNewPipeline}>
                {i18n.t('dop:add pipeline')}
              </Button>
            </WithAuth>
          ) : (
            <Tooltip title={i18n.t('dop:pipeline-run-tip')}>
              <Button type="primary" disabled>
                {i18n.t('dop:add pipeline')}
              </Button>
            </Tooltip>
          )
        }
        onChange={(aKey: string) => updater.activeKey(aKey)}
        activeKey={activeKey}
        renderTabBar={(p: any, DefaultTabBar) => <DefaultTabBar {...p} onKeyDown={(e: any) => e} />}
      >
        {!isMobileInit ? (
          <Tabs.TabPane tab={i18n.t('configuration information')} key={'configDetail'}>
            <PipelineConfigDetail
              {...rest}
              onCaseChange={onCaseChange}
              scope={scope}
              nodeId={nodeId}
              addDrawerProps={addDrawerProps}
              editAuth={editAuthObj}
            />
          </Tabs.TabPane>
        ) : null}
        <Tabs.TabPane tab={i18n.t('execute detail')} key={'runDetail'} disabled={loading}>
          <PipelineRunDetail key={runKey} deployAuth={deployAuthObj} isMobileInit={isMobileInit} />
        </Tabs.TabPane>
      </Tabs>
    </>
  );
};

export default PipelineDetail;
