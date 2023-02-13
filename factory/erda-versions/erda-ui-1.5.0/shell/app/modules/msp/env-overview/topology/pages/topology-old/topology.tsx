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

/* eslint-disable react-hooks/exhaustive-deps */
import { find, get, isEmpty, map } from 'lodash';
import React from 'react';
import { ContractiveFilter } from 'common';
import i18n from 'i18n';
import NodeEle from './node-item';
import LinkText, { linkTextHoverAction } from './link-text';
import TopologyChart from './components';
import ServiceMeshDrawer from '../service-mesh/service-mesh-drawer';
import { ScaleSelector } from './components/scaleSelector';
import topologyStore from 'msp/env-overview/topology/stores/topology';
import routeInfoStore from 'core/stores/route';
import monitorCommonStore from 'common/stores/monitorCommon';
import topologyServiceStore from 'msp/stores/topology-service-analyze';
import { useLoading } from 'core/stores/loading';
import { useMount, useUnmount, useUpdateEffect } from 'react-use';
import { TimeSelectWithStore } from 'msp/components/time-select';
import './topology.scss';
import { goTo } from 'common/utils';
import mspStore from 'msp/stores/micro-service';

const emptyObj = { nodes: [] };

// 拓扑节点中的id，存在非法字符（不能作为id使用），重置id；
export const setNodeUniqId = (data: TOPOLOGY.ITopologyResp) => {
  const { nodes = [] } = data || {};
  let nodeId = 0;
  const allIds = {};
  const reNodes = map(nodes, (node) => {
    const { id, parents = [] } = node;
    if (!allIds[id]) {
      nodeId += 1;
      allIds[id] = `node-${nodeId}`;
    }
    return {
      ...node,
      originId: id,
      id: allIds[id],
      parents: map(parents, (parent) => {
        const { id: pId } = parent;
        if (!allIds[pId]) {
          nodeId += 1;
          allIds[pId] = `node-${nodeId}`;
        }
        return {
          ...parent,
          id: allIds[pId],
          originId: pId,
        };
      }),
    };
  });
  return {
    ...data,
    nodes: reNodes,
  };
};

const Topology = () => {
  const params = routeInfoStore.useStore((s) => s.params);
  const [range, metaData] = monitorCommonStore.useStore((s) => [
    s.globalTimeSelectSpan.range,
    s.globalTimeSelectSpan.data,
  ]);
  const { getProjectApps } = monitorCommonStore.effects;
  const [currentProject, mspMenu] = mspStore.useStore((s) => [s.currentProject, s.mspMenu]);
  const [isFetching] = useLoading(topologyStore, ['getMonitorTopology']);
  const { clearMonitorTopology, setScale } = topologyStore.reducers;
  const { setActivedNode } = topologyServiceStore;
  const { getMonitorTopology, getTopologyTags, getTagsOptions } = topologyStore.effects;
  const [sourceData, scale, topologyTags, tagOptionsCollection] = topologyStore.useStore((s) => [
    s.topologyData,
    s.scale,
    s.topologyTags,
    s.tagOptionsCollection,
  ]);

  const [topologyData, setTopologyData] = React.useState({} as TOPOLOGY.ITopologyResp);
  const [useData, setUseData] = React.useState({} as TOPOLOGY.ITopologyResp);
  const [filterTags, setFilterTags] = React.useState({});
  const [serviceMeshVis, setServiceMeshVis] = React.useState(false);
  const [chosenNode, setChosenNode] = React.useState(null as TOPOLOGY.INode | null);
  const [serviceMeshType, setServiceMeshType] = React.useState('');
  useMount(() => {
    getProjectApps();
  });

  useUnmount(() => {
    clearMonitorTopology();
  });

  React.useEffect(() => {
    setTopologyData(setNodeUniqId(sourceData));
  }, [sourceData]);

  const getData = () => {
    const { startTimeMs, endTimeMs } = range;

    // filterTags = { a: ['a1', 'a2'], b: ['b1']}
    // 需要转换成 ['a:a1', 'a:a2', 'b:b1']
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

  useUpdateEffect(() => {
    setActivedNode(undefined);
    if (params.terminusKey) {
      getData();
    }
  }, [range, params.terminusKey, filterTags]);

  React.useEffect(() => {
    if (!isEmpty(topologyData)) {
      setUseData(topologyData);
    } else {
      setUseData(emptyObj);
    }
  }, [topologyData]);

  const toggleDrawer = (_type?: string, _node?: any) => {
    setServiceMeshType(_type || '');
    setChosenNode(_node);
    setServiceMeshVis(!serviceMeshVis);
  };

  const clickNode = (detail: TOPOLOGY.INode) => {
    const { type, serviceId, serviceName, applicationId, name } = detail;
    const goToParams = {
      query: {
        mode: metaData.mode,
        quick: metaData.mode === 'quick' ? metaData.quick : undefined,
        start: range.startTimeMs,
        end: range.endTimeMs,
      },
      jumpOut: true,
    };
    if (name) {
      const childrenKeyMap = {
        registercenter: ['Services'],
        configcenter: ['Configs'],
      };
      switch (type?.toLowerCase()) {
        case 'service':
          goTo(goTo.pages.mspServiceAnalyze, {
            ...params,
            serviceName,
            serviceId: window.encodeURIComponent(serviceId || ''),
            applicationId: currentProject?.type === 'MSP' ? '-' : applicationId,
            ...goToParams,
          });
          break;
        case 'apigateway':
          goTo('./gateway-ingress', goToParams);
          break;
        case 'externalservice':
          goTo(`./ei/${encodeURIComponent(name)}/affairs`, goToParams);
          break;
        case 'registercenter':
        case 'configcenter':
          const curChildrenKey = childrenKeyMap[type];
          const subMenuList = get(
            find(mspMenu, ({ key }) => key.toLowerCase() === type),
            'subMenu',
            [],
          );
          let targetPath = '';
          map(curChildrenKey, (item) => {
            if (!targetPath) {
              targetPath = get(find(subMenuList, { key: item }), 'href', '');
            }
          });
          targetPath && goTo(targetPath, goToParams);
      }
    }
    setActivedNode(detail);
  };

  const nodeExternalParam = {
    terminusKey: params.terminusKey,
    timeSpan: range,
    linkTextHoverAction,
    originData: useData,
    toggleDrawer,
  };

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

  return (
    <div className="topology-container">
      <div className="topology-header pr-48 mr-2">
        <div className="left flex items-center">
          <div className="topology-filter">
            <ContractiveFilter
              delay={1000}
              values={filterTags}
              conditions={conditionsFilter}
              onChange={(e) => {
                setFilterTags(e);
              }}
            />
          </div>
        </div>
        <div className="right">
          <ScaleSelector scale={scale} onChange={(val) => setScale(val)} />
          <TimeSelectWithStore className="ml-3" />
        </div>
      </div>
      <div className="topology-content">
        <TopologyChart
          nodeExternalParam={nodeExternalParam}
          isFetching={isFetching}
          data={useData}
          onClickNode={clickNode}
          setScale={setScale}
          scale={scale}
          nodeEle={NodeEle}
          linkTextEle={LinkText}
        />
      </div>
      <ServiceMeshDrawer
        type={serviceMeshType}
        visible={serviceMeshVis}
        node={chosenNode}
        onClose={() => toggleDrawer()}
      />
    </div>
  );
};
export default Topology;
