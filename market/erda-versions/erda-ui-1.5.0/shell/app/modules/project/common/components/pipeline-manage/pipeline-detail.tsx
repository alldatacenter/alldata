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
import { Tabs } from 'antd';
import PipelineConfigDetail from './config-detail';
import PipelineRunDetail from './run-detail';
import ConfigEnvSelector from './common/config-env-selector';
import routeInfoStore from 'core/stores/route';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';

interface IProps {
  caseId?: string;
  addDrawerProps?: Obj;
  scope: string;
}

const PipelineDetail = (props: IProps) => {
  const { caseId: propsCaseId, addDrawerProps = {}, scope } = props || {};
  const caseId = propsCaseId || routeInfoStore.useStore((s) => s.query.caseId);
  const [{ activeKey, runKey, canRunTest }, updater] = useUpdate({
    activeKey: 'configDetail',
    runKey: 1,
    canRunTest: true,
  });

  const onTest = (val: any) => {
    updater.runKey(runKey + 1);
    if (val.id && activeKey === 'configDetail') {
      updater.activeKey('runDetail');
    }
  };

  const onCaseChange = (bool: boolean) => {
    updater.canRunTest(bool);
  };

  return (
    <>
      <Tabs
        tabBarExtraContent={<ConfigEnvSelector canRunTest={canRunTest} scope={scope} onTest={onTest} />}
        onChange={(aKey: string) => updater.activeKey(aKey)}
        activeKey={activeKey}
        renderTabBar={(p: any, DefaultTabBar) => <DefaultTabBar {...p} onKeyDown={(e: any) => e} />}
      >
        <Tabs.TabPane tab={i18n.t('configuration information')} key={'configDetail'}>
          <PipelineConfigDetail
            onCaseChange={onCaseChange}
            scope={scope}
            caseId={caseId}
            addDrawerProps={addDrawerProps}
          />
        </Tabs.TabPane>
        <Tabs.TabPane tab={i18n.t('execute detail')} key={'runDetail'}>
          <PipelineRunDetail scope={scope} key={caseId} runKey={runKey} />
        </Tabs.TabPane>
      </Tabs>
    </>
  );
};

export default PipelineDetail;
