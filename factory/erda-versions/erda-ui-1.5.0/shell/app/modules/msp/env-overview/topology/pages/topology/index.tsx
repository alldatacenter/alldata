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
import TopologyComp, { ITopologyRef } from 'msp/env-overview/topology/pages/topology/component/topology-comp';
import TopologyOverview, { INodeKey } from 'msp/env-overview/topology/pages/topology/component/topology-overview';
import TopologyDetail from 'msp/env-overview/topology/pages/topology/component/topology-detail';
import { ContractiveFilter } from 'common';
import i18n from 'i18n';
import { get } from 'lodash';
import topologyStore from 'msp/env-overview/topology/stores/topology';
import { useMount, useUnmount, useUpdateEffect } from 'react-use';
import monitorCommonStore from 'common/stores/monitorCommon';
import routeInfoStore from 'core/stores/route';
import { Spin } from 'antd';
import { useLoading } from 'core/stores/loading';
import { TimeSelectWithStore } from 'msp/components/time-select';
import './index.scss';

const Topology = () => {
  const [filterTags, setFilterTags] = React.useState({});
  const [nodeType, setNodeType] = React.useState<INodeKey>('node');
  const [currentNode, setCurrentNode] = React.useState<TOPOLOGY.TopoNode['metaData']>(
    {} as TOPOLOGY.TopoNode['metaData'],
  );
  const topologyRef = React.useRef<ITopologyRef>(null);
  const params = routeInfoStore.useStore((s) => s.params);
  const [range] = monitorCommonStore.useStore((s) => [s.globalTimeSelectSpan.range, s.globalTimeSelectSpan.data]);
  const { getProjectApps } = monitorCommonStore.effects;
  const { getMonitorTopology, getTopologyTags, getTagsOptions } = topologyStore.effects;
  const { clearMonitorTopology } = topologyStore.reducers;
  const [isLoading] = useLoading(topologyStore, ['getMonitorTopology']);
  const [topologyData, topologyTags, tagOptionsCollection] = topologyStore.useStore((s) => [
    s.topologyData,
    s.topologyTags,
    s.tagOptionsCollection,
  ]);

  const getData = () => {
    const { startTimeMs, endTimeMs } = range;
    const tags = Object.keys(filterTags || {}).reduce(
      (acc: string[], key) => [...acc, ...(filterTags[key] || []).map((x: string) => `${key}:${x}`)],
      [],
    );

    const query = {
      startTime: startTimeMs,
      endTime: endTimeMs,
      terminusKey: params.terminusKey,
      tags,
    };
    getMonitorTopology(query);
  };

  useMount(() => {
    getProjectApps();
  });

  useUnmount(() => {
    clearMonitorTopology();
  });

  useUpdateEffect(() => {
    if (params.terminusKey) {
      getData();
    }
  }, [range, params.terminusKey, filterTags]);

  React.useEffect(() => {
    const { startTimeMs, endTimeMs } = range;
    const query = {
      startTime: startTimeMs,
      endTime: endTimeMs,
      terminusKey: params.terminusKey,
    };

    topologyTags.forEach((item: TOPOLOGY.ISingleTopologyTags) => {
      // 这个接口可能有问题，可能不需要依赖于startTime和endTime
      getTagsOptions({ ...query, tag: item.tag });
    });
  }, [topologyTags, range]);

  React.useEffect(() => {
    if (params.terminusKey) {
      getTopologyTags({ terminusKey: params.terminusKey }).then((res) => {
        const initialTags = {};
        res.forEach((item: TOPOLOGY.ISingleTopologyTags) => {
          Object.assign(initialTags, { [item.tag]: [] });
        });

        setFilterTags(initialTags);
      });
    }
  }, [params.terminusKey]);
  const conditionsFilter = React.useMemo(
    () =>
      topologyTags.map((item: TOPOLOGY.ISingleTopologyTags) => ({
        type: 'select',
        key: item.tag,
        label: item.label,
        fixed: item.tag === 'application',
        showIndex: 0,
        haveFilter: true,
        emptyText: i18n.t('dop:all'),
        options: (get(tagOptionsCollection, item.tag, []) || []).map((x: string) => ({ value: x, label: x })),
      })),
    [tagOptionsCollection, topologyTags],
  );

  const handleSelectNodeType = (key: INodeKey) => {
    setNodeType(key);
  };

  const handleClickNode = (data: TOPOLOGY.TopoNode['metaData']) => {
    setCurrentNode(data);
  };

  return (
    <div className="topology h-full">
      <Spin className="spin" spinning={isLoading}>
        <div className="h-full flex flex-col">
          <div className="topology-filter flex justify-between items-center h-12 bg-white-02 px-4">
            <ContractiveFilter
              delay={1000}
              values={filterTags}
              conditions={conditionsFilter}
              onChange={(e) => {
                setFilterTags(e);
              }}
            />
            <TimeSelectWithStore className="ml-3" />
          </div>
          <div className="flex-1 flex min-h-0">
            <TopologyOverview data={topologyData} onClick={handleSelectNodeType} />
            <div className="flex-1 topology-container relative min-w-0">
              {topologyData.nodes?.length ? (
                <TopologyComp
                  ref={topologyRef}
                  key={nodeType}
                  data={topologyData}
                  filterKey={nodeType}
                  clockNode={handleClickNode}
                  defaultZoom={0.6}
                />
              ) : null}
            </div>
          </div>
        </div>
        <TopologyDetail
          className="absolute h-full top-0"
          data={currentNode}
          onCancel={() => {
            topologyRef.current?.cancelSelectNode();
            setCurrentNode({});
          }}
        />
      </Spin>
    </div>
  );
};

export default Topology;
