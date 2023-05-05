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
import { K8sClusterTerminalButton } from './cluster-terminal';
import { updateSearch } from 'common/utils';
import { useUpdate } from 'common/use-hooks';
import { Drawer } from 'antd';
import { PureClusterWorkloadDetail } from './cluster-workload-detail';
import { ClusterContainer } from './index';

interface IDetailData {
  workloadId: string;
  podId?: string;
}
interface IState {
  visible: boolean;
  detailData: null | IDetailData;
  urlQuery: Obj;
}

const ClusterWorkload = () => {
  const [{ clusterName }, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const [{ visible, detailData, urlQuery }, updater, update] = useUpdate<IState>({
    visible: false,
    detailData: null,
    urlQuery: query,
  });

  const reloadRef = React.useRef<Obj | null>(null);
  React.useEffect(() => {
    updateSearch({ ...urlQuery });
  }, [urlQuery]);

  const openDetail = (record: Obj, op: Obj) => {
    if (op.key === 'openWorkloadDetail') {
      const { podId, id } = record;
      update({
        visible: true,
        detailData: { workloadId: id, podId },
      });
    }
  };

  const closeDetail = () => {
    update({ visible: false, detailData: null });
  };

  const inParams = { clusterName, ...urlQuery };

  const urlQueryChange = (val: Obj) => updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));

  const onDeleteDetail = () => {
    closeDetail();
    if (reloadRef.current && reloadRef.current.reload) {
      reloadRef.current.reload();
    }
  };

  const customProps = {
    chartContainer: {
      props: {
        span: [4, 20],
      },
    },
    workloadTotal: {
      props: {
        grayBg: true,
        fullHeight: true,
        flexCenter: true,
      },
    },
    workloadChart: {
      props: {
        grayBg: true,
        chartStyle: { height: 136 },
      },
    },
  };

  return (
    <ClusterContainer>
      <div className="top-button-group" style={{ right: 162 }}>
        <K8sClusterTerminalButton clusterName={clusterName} />
      </div>
      <DiceConfigPage
        scenarioType={'cmp-dashboard-workloads-list'}
        scenarioKey={'cmp-dashboard-workloads-list'}
        inParams={inParams}
        ref={reloadRef}
        customProps={{
          ...customProps,
          filter: {
            op: {
              onFilterChange: urlQueryChange,
            },
          },
          workloadTable: {
            op: {
              onStateChange: urlQueryChange,
              clickTableItem: openDetail,
            },
          },
        }}
      />
      <Drawer visible={visible} onClose={closeDetail} width={'80%'} maskClosable getContainer={false}>
        {visible && detailData ? (
          <PureClusterWorkloadDetail
            className="mt-4"
            clusterName={clusterName}
            {...detailData}
            onDelete={onDeleteDetail}
          />
        ) : null}
      </Drawer>
    </ClusterContainer>
  );
};

export default ClusterWorkload;
