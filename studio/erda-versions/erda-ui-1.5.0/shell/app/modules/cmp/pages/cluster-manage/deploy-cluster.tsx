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
import { useMount, useUnmount } from 'react-use';
import i18n from 'i18n';
import { Button, Drawer, message } from 'antd';
import { isEmpty } from 'lodash';
import DeployClusterLog from './deploy-cluster-log';
import DeployClusterForm from './deploy-cluster-form';
import clusterStore from '../../stores/cluster';
import orgStore from 'app/org-home/stores/org';
import './deploy-cluster.scss';
/**
 * 进入页面后检查当前是否有正在部署集群，按照约定，一个org同一个时刻只会有一个部署
 * 若有，则直接展示部署参数，
 * 若无，则展示添加部署集群表单
 * */
let firstVisit = true;
const DeployCluster = () => {
  const deployingCluster = clusterStore.useStore((s) => s.deployingCluster);
  const { killDeployCluster, deployCluster, getCurDeployCluster } = clusterStore.effects;
  const { clearDeployCluster } = clusterStore.reducers;
  const currentOrg = orgStore.useStore((s) => s.currentOrg);

  const [logVisible, setLogVisible] = React.useState(false);

  useMount(() => {
    getCurDeployCluster();
  });

  useUnmount(() => {
    clearDeployCluster();
    firstVisit = true;
  });

  React.useEffect(() => {
    if (!isEmpty(deployingCluster) && firstVisit) {
      message.info(
        i18n.t(
          'cmp:The last deployment information has been initialized. Please click the reset button at the bottom if unnecessary.',
        ),
      );
      firstVisit = false;
    }
  }, [deployingCluster]);

  const startDeployCluster = (postData: any) => {
    deployCluster(postData).then((res: any) => {
      if (res) {
        setLogVisible(true);
      }
    });
  };
  return (
    <div className="deploy-cluster">
      <div className="deploy-info font-medium">
        {i18n.t('organization')} {currentOrg.name} {i18n.t('cmp:new cluster deployment')}
        <div className="deploy-operator">
          <Button onClick={() => setLogVisible(true)}>{i18n.t('check log')}</Button>
          <Button onClick={() => killDeployCluster()}>{i18n.t('stop deploying')}</Button>
        </div>
      </div>
      <div className="deploy-content">
        <DeployClusterForm
          data={deployingCluster}
          orgId={currentOrg.id}
          orgName={currentOrg.name}
          onSubmit={startDeployCluster}
        />
      </div>

      <Drawer
        destroyOnClose
        title={i18n.t('deployment log')}
        width="80%"
        visible={logVisible}
        onClose={() => setLogVisible(false)}
      >
        <DeployClusterLog />
      </Drawer>
    </div>
  );
};

export default DeployCluster;
