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
import { SplitPage } from 'layout/common';
import { EmptyHolder } from 'common';
import PipelineDetail from './pipeline-detail';
import routeInfoStore from 'core/stores/route';
import fileTreeStore from 'common/stores/file-tree';
import { scopeConfig } from './scope-config';
import { updateSearch } from 'common/utils';
import { getINodeByPipelineId } from 'application/services/build';
import DiceConfigPage from 'app/config-page';
import { ActionType } from 'yml-chart/common/pipeline-node-drawer';
import appStore from 'application/stores/application';

interface IProps {
  scope: string;
}

const PipelineManage = (props: IProps) => {
  const appDetail = appStore.useStore((s) => s.detail);
  const curScope = appDetail.isProjectLevel ? ActionType.projectLevelAppPipeline : ActionType.appPipeline;
  const { scope = curScope } = props;
  const scopeConfigData = scopeConfig[scope];
  const { clearTreeNodeDetail } = fileTreeStore;
  const [{ projectId, appId }, { nodeId, pipelineID }] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const scopeParams = React.useMemo(
    () => ({ scopeID: projectId, scope: scopeConfigData.scope }),
    [projectId, scopeConfigData.scope],
  );

  const nodeIdRef = React.useRef(null as any);

  React.useEffect(() => {
    nodeIdRef.current = nodeId;
  }, [nodeId]);

  if (pipelineID && !nodeId) {
    getINodeByPipelineId({ pipelineId: pipelineID }).then((res: any) => {
      const inode = res?.data?.inode;
      inode && updateSearch({ nodeId: inode });
    });
  }

  const inParams = {
    projectId,
    appId,
    ...scopeParams,
    selectedKeys: nodeId,
  };
  return (
    <SplitPage>
      <SplitPage.Left className="pipeline-manage-left">
        {pipelineID && !nodeId ? (
          <EmptyHolder relative />
        ) : (
          <DiceConfigPage
            scenarioType=""
            scenarioKey={'app-pipeline-tree'}
            inParams={inParams}
            showLoading
            forceUpdateKey={['inParams']}
            customProps={{
              fileTree: {
                op: {
                  onClickNode: (_inode: string) => {
                    if (nodeIdRef.current !== _inode) {
                      clearTreeNodeDetail();
                      setTimeout(() => {
                        updateSearch({ nodeId: _inode, pipelineID: undefined });
                      }, 0);
                    }
                  },
                },
              },
            }}
          />
        )}
      </SplitPage.Left>
      <SplitPage.Right>
        {nodeId ? <PipelineDetail scopeParams={scopeParams} key={nodeId} scope={scope} /> : <EmptyHolder relative />}
      </SplitPage.Right>
    </SplitPage>
  );
};

export default PipelineManage;
