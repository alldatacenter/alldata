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
import { BoardGrid, Holder, TimeSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Spin } from 'antd';
import { isEmpty } from 'lodash';
import CommonDashboardStore from 'common/stores/dashboard';
import { useLoading } from 'core/stores/loading';
import monitorCommonStore from 'common/stores/monitorCommon';
import { useMount } from 'react-use';
import i18n from 'i18n';

const DashBoard = React.memo(BoardGrid.Pure);

interface IState {
  chartLayout: DC.Layout;
}
interface IProps {
  nodeIP: string;
  nodeId: string;
  clusterName: string;
  className?: string;
}

export const PureClusterNodeDetail = (props: IProps) => {
  const { nodeIP, nodeId, clusterName, className = '' } = props;
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);

  const globalVariable = React.useMemo(
    () => ({
      startTime: timeSpan.startTimeMs,
      endTime: timeSpan.endTimeMs,
      nodeIP,
      clusterName,
    }),
    [timeSpan, nodeIP, clusterName],
  );

  const [chartLoading] = useLoading(CommonDashboardStore, ['getCustomDashboard']);

  const [{ chartLayout }, updater, update] = useUpdate<IState>({
    chartLayout: [],
  });

  useMount(() => {
    CommonDashboardStore.getCustomDashboard({ id: 'cmp-dashboard-nodeDetail', isSystem: true }).then((res) =>
      updater.chartLayout(res),
    );
  });
  const inParams = { clusterName, nodeId };

  return (
    <div className={className}>
      <div>
        <DiceConfigPage
          scenarioType={'cmp-dashboard-nodeDetail'}
          scenarioKey={'cmp-dashboard-nodeDetail'}
          inParams={inParams}
        />
      </div>
      <Spin spinning={chartLoading} wrapperClassName="mt-8">
        <div className="text-xl font-medium mb-4">{i18n.t('cmp:resource monitor')}</div>
        <TimeSelector className="mb-4" />
        <Holder when={isEmpty(chartLayout)}>
          <DashBoard layout={chartLayout} globalVariable={globalVariable} />
        </Holder>
      </Spin>
    </div>
  );
};

const ClusterNodesDetail = () => {
  const [{ clusterName, nodeId }, { nodeIP }] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const props = { clusterName, nodeId, nodeIP };
  return <PureClusterNodeDetail {...props} />;
};

export default ClusterNodesDetail;
