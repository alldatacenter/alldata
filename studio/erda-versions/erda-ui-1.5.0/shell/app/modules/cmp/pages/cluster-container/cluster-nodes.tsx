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
import DiceConfigPage from 'app/config-page';
import routeInfoStore from 'core/stores/route';
import { getUrlQuery } from 'config-page/utils';
import { Drawer } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { PureClusterNodeDetail } from './cluster-nodes-detail';
import { updateSearch } from 'common/utils';
import { K8sClusterTerminalButton } from './cluster-terminal';
import { ClusterContainer } from './index';

interface IDetailData {
  nodeIP: string;
  nodeId: string;
}
interface IState {
  visible: boolean;
  detailData: null | IDetailData;
  urlQuery: Obj;
}

const ClusterNodes = () => {
  const [{ clusterName }, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const [{ visible, detailData, urlQuery }, updater, update] = useUpdate<IState>({
    visible: false,
    detailData: null,
    urlQuery: query,
  });

  React.useEffect(() => {
    updateSearch({ ...urlQuery });
  }, [urlQuery]);

  const inParams = { clusterName, ...urlQuery };

  const urlQueryChange = (val: Obj) => updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));

  const openDetail = (record: Obj, op: Obj) => {
    if (op.key === 'gotoNodeDetail') {
      const { IP, nodeId } = record;
      update({
        visible: true,
        detailData: { nodeId, nodeIP: IP },
      });
    }
  };

  const closeDetail = () => {
    update({ visible: false, detailData: null });
  };

  const chartProps = {
    grayBg: true,
    size: 'small',
  };

  return (
    <ClusterContainer>
      <div className="top-button-group">
        <K8sClusterTerminalButton clusterName={clusterName} />
      </div>
      <DiceConfigPage
        scenarioType={'cmp-dashboard-nodes'}
        scenarioKey={'cmp-dashboard-nodes'}
        inParams={inParams}
        customProps={{
          cpuChart: {
            props: chartProps,
          },
          memChart: {
            props: chartProps,
          },
          podChart: {
            props: chartProps,
          },
          filter: {
            op: {
              onFilterChange: urlQueryChange,
            },
          },
          table: {
            op: {
              onStateChange: urlQueryChange,
              clickTableItem: openDetail,
            },
          },
          tabs: {
            op: {
              onStateChange: urlQueryChange,
            },
          },
        }}
      />
      <Drawer visible={visible} onClose={closeDetail} width={'80%'} maskClosable getContainer={false}>
        {visible && detailData ? (
          <PureClusterNodeDetail className="mt-4" clusterName={clusterName} {...detailData} />
        ) : null}
      </Drawer>
    </ClusterContainer>
  );
};

export default ClusterNodes;
