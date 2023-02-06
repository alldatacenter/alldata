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

import React, { useState, useEffect } from 'react';
import { Row, Col } from 'antd';
import { isEmpty, map } from 'lodash';
import { daysRange } from 'common/utils';
import AlarmChart from './alarm-chart';
import clusterStore from 'cmp/stores/cluster';
import { ClusterSelector } from '../../common/components/cluster-selector';
import orgStore from 'app/org-home/stores/org';

import './index.scss';

// const ALARM_TYPE = {
//   machine: i18n.t('machine'),
//   dice_addon: i18n.t('cmp:Erda addon'),
//   dice_component: i18n.t('cmp:Erda component'),
//   kubernetes: 'kubernetes',
// };

const { AlarmTrendChart, AlarmTypeProportionChart, AlarmProportionChart } = AlarmChart;

const AlarmAnalyze = () => {
  const orgId = orgStore.useStore((s) => s.currentOrg.id);
  const orgClusterList = clusterStore.useStore((s) => s.list);
  const [filterClusters, setFilterClusters] = useState([] as string[]);
  const [fullCluster, setFullCluster] = useState([] as string[]);
  const [listFilter, setListFilter] = useState({});
  // const [filterType, setFilterType] = useState(undefined);
  useEffect(() => {
    clusterStore.effects.getClusterList({ orgId });
    setListFilter({ orgID: orgId });
  }, [orgId]);

  useEffect(() => {
    if (!isEmpty(orgClusterList)) {
      const clusterNames = map(orgClusterList, (item) => item.name);
      setFullCluster(clusterNames);
      setFilterClusters(clusterNames);
    }
  }, [orgClusterList]);

  const changeCluster = (val: string) => {
    setFilterClusters(val ? [val] : fullCluster);
    setListFilter(val ? { targetID: val } : { orgID: orgId });
  };

  const shouldLoad = !isEmpty(filterClusters);
  return (
    <>
      <div className="org-cluster-filter">
        <ClusterSelector clusterList={orgClusterList} onChange={changeCluster} />
      </div>
      <Row gutter={[20, 20]}>
        <Col span={24}>
          <AlarmTrendChart
            query={{
              constQuery: daysRange(7),
              in_cluster_name: filterClusters,
              filter_alert_scope_id: orgId,
            }}
            shouldLoad={shouldLoad}
          />
        </Col>
        <Col span={12} style={{ paddingRight: 10 }}>
          <AlarmTypeProportionChart
            query={{
              constQuery: daysRange(7),
              in_cluster_name: filterClusters,
              filter_alert_scope_id: orgId,
            }}
            shouldLoad={shouldLoad}
          />
        </Col>
        <Col span={12} style={{ paddingLeft: 10 }}>
          <AlarmProportionChart
            query={{
              constQuery: daysRange(7),
              in_cluster_name: filterClusters,
              filter_alert_scope_id: orgId,
            }}
            shouldLoad={shouldLoad}
          />
        </Col>
      </Row>
      {/* <div className="alarm-message">
        <p className="section-title">{i18n.t('cmp:alarm information')}</p>
        <Select
          className="w-52 mb-4"
          placeholder={i18n.t('filter by type')}
          allowClear
          onChange={(value) => setFilterType(value as any)}
        >
          {map(ALARM_TYPE, (name, value) => <Select.Option key={value} value={value}>{name}</Select.Option>)}
        </Select>
        <AlarmList query={listFilter} filterType={filterType} />
      </div> */}
    </>
  );
};

export default AlarmAnalyze;
