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

import * as React from 'react';
import clusterStore from 'cmp/stores/cluster';
import { Select, Tooltip } from 'antd';
import { goTo } from 'common/utils';
import { EMPTY_CLUSTER, replaceContainerCluster } from 'cmp/pages/cluster-manage/config';
import i18n from 'i18n';

const ClusterSelector = () => {
  const [useableK8sClusters, chosenCluster] = clusterStore.useStore((s) => [s.useableK8sClusters, s.chosenCluster]);
  const { setChosenCluster } = clusterStore.reducers;
  const { ready = [], unReady = [] } = useableK8sClusters || {};
  const list: Array<{ cluster: string; status: string }> = [];
  ready?.forEach((item: string) => list.push({ cluster: item, status: 'ready' }));
  unReady?.forEach((item: string) => list.push({ cluster: item, status: 'unReady' }));

  const pageNameMap = {
    nodes: i18n.t('node'),
    pods: 'Pods',
    workload: i18n.t('cmp:Workload'),
    'event-log': i18n.t('cmp:Event Log'),
  };

  const curPage = location.pathname.split('/').pop();
  const pageName = pageNameMap[curPage as string] || i18n.t('container resource');
  return (
    <div className="flex items-center">
      <span className="flex items-center font-bold text-lg">{pageName}</span>
      <span className="bg-dark-2 mx-5" style={{ width: 1, height: 12 }} />
      <Select
        bordered={false}
        dropdownMatchSelectWidth={false}
        className={'hover:bg-hover-gray-bg rounded'}
        getPopupContainer={() => document.body}
        value={chosenCluster === EMPTY_CLUSTER ? undefined : chosenCluster}
        placeholder={i18n.t('choose cluster')}
        onChange={(v) => {
          setChosenCluster(v);
          goTo(replaceContainerCluster(v));
        }}
      >
        {list?.map((item) => {
          const [disabled, disabledTip] =
            item.status === 'unReady' ? [true, i18n.t('cmp:cluster is not ready, please try it later')] : [false, ''];
          return (
            <Select.Option key={item.cluster} value={item.cluster} disabled={disabled}>
              <Tooltip title={disabledTip}>{item.cluster}</Tooltip>
            </Select.Option>
          );
        })}
      </Select>
    </div>
  );
};

export default ClusterSelector;
