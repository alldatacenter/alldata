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

import { Echarts } from 'charts';
import Table from 'common/components/table';
import { ColumnProps } from 'common/components/table/interface';
import { colorMap } from 'charts/theme';
import { ContractiveFilter, CardContainer, ErdaIcon, Title } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Button, Col, InputNumber, Progress, Radio, Row, Select, Spin, Tooltip, Modal } from 'antd';
import { getResourceGauge, getResourceTable } from 'dcos/services/dashboard';
import { map } from 'lodash';
import React from 'react';
import routeInfoStore from 'core/stores/route';
import { useMount } from 'react-use';
import clusterStore from 'cmp/stores/cluster';
import { statusColorMap } from 'app/config-page/utils';
import i18n, { isZh } from 'i18n';
import { ColumnsType } from 'antd/es/table';
import './resources-summary.scss';

const defaultGaugeData = {
  cpu: {
    title: '',
    name: '',
    value: [],
    split: [],
  },
  memory: {
    title: '',
    name: '',
    value: [],
    split: [],
  },
  nodes: {
    title: '',
    name: '',
    value: [],
    split: [],
  },
};
export const ResourceSummary = React.memo(({ clusterNameStr }: { clusterNameStr: string }) => {
  const localCacheUnit = window.localStorage.getItem('cluster-summary-unit');
  const localCache = localCacheUnit ? localCacheUnit.split('-').map((a) => +a) : [8, 32];
  const cpuAndMem = React.useRef({
    cpuPerNode: localCache[0] || 8,
    memPerNode: localCache[1] || 32,
  });
  const [state, updater] = useUpdate({
    showCalculate: false,
  });

  const [data, loading] = getResourceGauge.useState();
  React.useEffect(() => {
    if (clusterNameStr) {
      getResourceGauge.fetch({ clusterName: clusterNameStr.split(','), ...cpuAndMem.current });
    }
  }, [clusterNameStr]);

  const getOption = (item: ORG_DASHBOARD.GaugeChartBody) => {
    const colors = [colorMap.blue, colorMap.green];
    const [assigned, used] = item.name.split('\n');
    const option = {
      tooltip: {
        formatter: '{a} <br/>{b} : {c}',
      },
      series: [
        {
          type: 'gauge',
          radius: '90%',
          startAngle: 200,
          endAngle: -20,
          axisLine: {
            lineStyle: {
              // width: 14,
              color: [...item.split, 1].map((a, i) => [a, colors[i]]),
            },
          },
          itemStyle: {
            shadowColor: 'rgba(0, 0, 0, 0.5)',
            shadowBlur: 6,
            shadowOffsetX: 2,
            shadowOffsetY: 2,
            color: colorMap.red,
          },
          splitLine: {
            // 分隔线
            length: 12, // 属性length控制线长
          },
          detail: {
            color: colorMap.red,
            formatter: used ? [`{assigned|${assigned}}`, `{used|${used}}`].join('\n') : `{assigned|${assigned}}`,
            rich: {
              assigned: {
                color: colorMap.blue,
                fontSize: 18,
              },
              used: {
                color: colorMap.red,
                marginTop: '20px',
                fontSize: 18,
              },
            },
            offsetCenter: [0, '60%'],
          },
          label: {},
          title: {
            color: colorMap.red,
          },
          data: item.value,
        },
      ],
    };
    return option;
  };

  return (
    <>
      {/* <Title
        level={2}
        title={i18n.t('cmp:resource distribute')}
        tip={
          <div className="text-xs">
            <div>
              {i18n.t('cmp:Allocated resources&#58; The resources reserved by project resource Quota are configured')}
            </div>
            <div>
              {i18n.t(
                'cmp:Occupied resource&#58; The portion of allocated resource actually occupied by Kubernetes Request resource Request',
              )}
            </div>
          </div>
        }
        tipStyle={{ width: '500px' }}
        operations={}
      /> */}
      {/* <Row justify="space-between" gutter={12}>
        {map(data || defaultGaugeData, (item, key) => (
          <Col key={key} span={8}>
            <CardContainer.ChartContainer title={item?.title} holderWhen={!item}>
              <Echarts style={{ height: '320px' }} showLoading={loading} option={getOption(item)} />
            </CardContainer.ChartContainer>
          </Col>
        ))}
      </Row> */}
      <ResourceTable cpuPerNode={cpuAndMem.current.cpuPerNode} memPerNode={cpuAndMem.current.memPerNode} />
    </>
  );
});

const arrSortMinToMax = (_a: string, _b: string) => {
  const a = String(_a);
  const b = String(_b);
  let cReg =
    /^[\u4E00-\u9FCC\u3400-\u4DB5\uFA0E\uFA0F\uFA11\uFA13\uFA14\uFA1F\uFA21\uFA23\uFA24\uFA27-\uFA29]|[\ud840-\ud868][\udc00-\udfff]|\ud869[\udc00-\uded6\udf00-\udfff]|[\ud86a-\ud86c][\udc00-\udfff]|\ud86d[\udc00-\udf34\udf40-\udfff]|\ud86e[\udc00-\udc1d]/;
  if (!cReg.test(a) || !cReg.test(b)) {
    return a.localeCompare(b);
  } else {
    return a.localeCompare(b, 'zh');
  }
};

const PureResourceTable = React.memo(({ rankType }: { rankType: string }) => {
  const { getClusterList } = clusterStore.effects;

  const [{ ownerIds, projectIds, clusterName, clusters, showCalculate }, updater, update] = useUpdate({
    ownerIds: [],
    projectIds: [],
    clusterName: [],
    clusters: [],
    showCalculate: false,
  });
  useMount(() => {
    getClusterList().then((res: ORG_CLUSTER.ICluster[]) => {
      updater.clusters(res);
    });
  });

  const localCacheUnit = window.localStorage.getItem('cluster-summary-unit');
  const localCache = localCacheUnit ? localCacheUnit.split('-').map((a) => +a) : [8, 32];
  const cpuAndMem = React.useRef({
    cpuPerNode: localCache[0] || 8,
    memPerNode: localCache[1] || 32,
  });

  const { cpuPerNode, memPerNode } = cpuAndMem.current;

  const [data, loading] = getResourceTable.useState();

  const getResourceList = React.useCallback(() => {
    if (clusters.length) {
      const curCluster = clusterName?.length ? clusterName : clusters.map((item) => item.name);
      getResourceTable.fetch({ clusterName: curCluster, cpuPerNode, memPerNode, groupBy: rankType });
    }
  }, [clusters, clusterName, rankType, cpuPerNode, memPerNode]);

  React.useEffect(() => {
    getResourceList();
  }, [getResourceList]);

  const mergedList = (data?.list || []).map((item) => ({
    ...item,
    projectName: item.projectDisplayName || item.projectName,
    ownerUserName: item.ownerUserNickname || item.ownerUserName,
  }));

  const columnsMap = {
    project: [
      {
        title: i18n.t('Project'),
        dataIndex: 'projectName',
        key: 'projectName',
        width: 300,
        subTitle: (_: string, record: ORG_DASHBOARD.ResourceTableRecord) => record.projectDesc,
      },
      {
        title: i18n.t('cmp:Owner'),
        dataIndex: 'ownerUserName',
        key: 'ownerUserName',
      },
    ],
    owner: [
      {
        title: i18n.t('cmp:Owner'),
        dataIndex: 'ownerUserName',
        key: 'ownerUserName',
      },
      {
        title: i18n.t('cmp:project count'),
        dataIndex: 'projectTotal',
        key: 'projectTotal',
        align: 'right',
        sorter: {
          compare: (a, b) => a.projectTotal - b.projectTotal,
        },
      },
    ],
  };

  const getStrokeColor = (val: number) => {
    if (val >= 80 && val < 100) {
      return statusColorMap.warning;
    } else if (val >= 100) {
      return statusColorMap.error;
    }
    return statusColorMap.success;
  };
  const columns: Array<ColumnProps<ORG_DASHBOARD.ResourceTableRecord>> = [
    ...columnsMap[rankType],
    {
      title: () => (
        <div className="inline-flex flex-col justify-center align-center">
          <div className="text-sm">{i18n.t('cmp:Number of used nodes')}</div>
          <div className="text-xs text-black-400">
            {`(${i18n.t('cmp:one node')}: ${cpuAndMem.current.cpuPerNode} Core ${cpuAndMem.current.memPerNode} GiB)`}
          </div>
        </div>
      ),
      dataIndex: 'nodes',
      key: 'nodes',
      align: 'right',
      sorter: {
        compare: (a, b) => a.nodes - b.nodes,
      },
      render: (text: string) => text,
    },
    {
      title: `${i18n.t('cmp:CPU quota')}`,
      dataIndex: 'cpuQuota',
      key: 'cpuQuota',
      align: 'right',
      sorter: {
        compare: (a, b) => a.cpuQuota - b.cpuQuota,
      },
      render: (text: string) => `${(+text || 0).toFixed(1)} Core`,
    },
    {
      title: i18n.t('cmp:CPU quota usage'),
      dataIndex: 'cpuWaterLevel',
      key: 'cpuWaterLevel',
      sorter: {
        compare: (a, b) => a.cpuWaterLevel - b.cpuWaterLevel,
      },
      render: (_val: string, record: ORG_DASHBOARD.ResourceTableRecord) => {
        let value = +(_val ?? 0);
        value = +(`${value}`.indexOf('.') ? value.toFixed(2) : value);
        return !isNaN(+_val) ? (
          <Tooltip title={`${record.cpuRequest} / ${record.cpuQuota}`}>
            <Progress
              percent={value}
              type="circle"
              width={20}
              strokeWidth={18}
              format={(v) => null}
              strokeColor={getStrokeColor(value)}
            />
            <span className="text-dark-8  ml-2">{`${value.toFixed(1)}%`}</span>
          </Tooltip>
        ) : (
          _val
        );
      },
    },
    {
      title: `${i18n.t('cmp:Memory quota')}`,
      dataIndex: 'memQuota',
      key: 'memQuota',
      align: 'right',
      sorter: {
        compare: (a, b) => a.memQuota - b.memQuota,
      },
      render: (text: string) => `${(+text || 0).toFixed(1)} GiB`,
    },
    {
      title: i18n.t('cmp:Memory quota usage'),
      dataIndex: 'memWaterLevel',
      key: 'memWaterLevel',
      sorter: {
        compare: (a, b) => a.memWaterLevel - b.memWaterLevel,
      },
      render: (_val: string, record: ORG_DASHBOARD.ResourceTableRecord) => {
        let value = +(_val ?? 0);
        value = +(`${value}`.indexOf('.') ? value.toFixed(2) : value);
        return !isNaN(+_val) ? (
          <Tooltip title={`${record.memRequest} / ${record.memQuota}`}>
            <Progress
              type="circle"
              percent={value}
              width={20}
              strokeWidth={18}
              format={(v) => null}
              strokeColor={getStrokeColor(value)}
            />
            <span className="text-dark-8 ml-2">{`${value.toFixed(1)}%`}</span>
          </Tooltip>
        ) : (
          _val
        );
      },
    },
  ];

  const ownerList: Array<{ label: string; value: number }> = [];
  const projectList: Array<{ label: string; value: number }> = [];
  data?.list?.forEach((item) => {
    if (!ownerList.find((o) => o.value === item.ownerUserID)) {
      ownerList.push({
        label: item.ownerUserNickname || item.ownerUserName,
        value: item.ownerUserID,
      });
    }
    if (!projectList.find((p) => p.value === item.projectID)) {
      projectList.push({
        label: item.projectDisplayName || item.projectName,
        value: item.projectID,
      });
    }
  });
  const conditionMap = {
    project: [
      {
        type: 'select',
        key: 'projectIds',
        label: i18n.t('Project'),
        haveFilter: true,
        fixed: true,
        emptyText: i18n.t('dop:all'),
        options: projectList,
      },
      {
        type: 'select',
        key: 'ownerIds',
        label: i18n.t('cmp:Owner'),
        haveFilter: true,
        fixed: true,
        emptyText: i18n.t('dop:all'),
        options: ownerList,
      },
    ],
    owner: [
      {
        type: 'select',
        key: 'ownerIds',
        label: i18n.t('cmp:Owner'),
        haveFilter: true,
        fixed: true,
        emptyText: i18n.t('dop:all'),
        options: ownerList,
      },
    ],
  };
  const conditionsFilter = [
    {
      type: 'select',
      key: 'clusterName',
      label: i18n.t('cluster'),
      haveFilter: true,
      fixed: true,
      emptyText: i18n.t('dop:all'),
      options: clusters.map((item) => ({ label: item.name, value: item.name })),
    },
    ...conditionMap[rankType],
  ];

  let filterData = mergedList;
  if (ownerIds.length) {
    filterData = filterData.filter((a) => ownerIds.includes(a.ownerUserID));
  }
  if (projectIds.length) {
    filterData = filterData.filter((a) => projectIds.includes(a.projectID));
  }
  let cpuTotal = 0;
  let memoryTotal = 0;
  let nodeTotal = 0;
  filterData.forEach((a) => {
    cpuTotal += a.cpuQuota;
    memoryTotal += a.memQuota;
    nodeTotal += a.nodes;
  });
  return (
    <>
      {/* <Title
        level={2}
        mt={16}
        title={
          <span>
            {i18n.t('cmp:Allocation of project resources')}
            <span className="ml-1 text-desc text-xs">
              {i18n.t('cmp:The total number of selected resources')}: CPU: {cpuTotal.toFixed(2)} {i18n.t('cmp:Core')},{' '}
              {i18n.t('cmp:Memory')}: {memoryTotal.toFixed(2)} G, {i18n.t('cmp:Conversion nodes')}:{' '}
              {Math.ceil(nodeTotal)} {isZh() ? '个' : ''}
            </span>
          </span>
        }
      /> */}
      <Table
        slot={
          <div className="flex justify-between align-center">
            <ContractiveFilter
              delay={1000}
              conditions={conditionsFilter}
              onChange={(values) => {
                const curVal = {
                  ...values,
                  ownerIds: values.ownerIds || [],
                  projectIds: values.projectIds || [],
                };
                update(curVal);
              }}
            />
            <ErdaIcon
              className="ml-3 resource-summary-op-icon p-2"
              onClick={() => updater.showCalculate(true)}
              type="calculator-one"
              color="currentColor"
            />
          </div>
        }
        rowKey="projectID"
        loading={loading}
        columns={columns}
        dataSource={filterData}
        onChange={() => getResourceList()}
      />

      <Modal
        visible={showCalculate}
        title={i18n.t('cmp:Node conversion formula')}
        onCancel={() => updater.showCalculate(false)}
        onOk={() => {
          window.localStorage.setItem(
            'cluster-summary-unit',
            `${cpuAndMem.current.cpuPerNode}-${cpuAndMem.current.memPerNode}`,
          );
          updater.showCalculate(false);
        }}
      >
        <div className="flex items-center">
          <InputNumber
            min={1}
            max={9999}
            defaultValue={cpuAndMem.current.cpuPerNode}
            onChange={(value) => {
              cpuAndMem.current.cpuPerNode = value;
            }}
            size="small"
            style={{ width: '80px' }}
          />
          <span className="mx-1">Core</span>
          <InputNumber
            min={1}
            max={9999999}
            defaultValue={cpuAndMem.current.memPerNode}
            onChange={(value) => {
              cpuAndMem.current.memPerNode = value;
            }}
            size="small"
            className="ml-1"
            style={{ width: '80px' }}
          />
          <span className="ml-1">GiB = {i18n.t('cmp:one node')}</span>
        </div>
      </Modal>
    </>
  );
});

export const ResourceTable = () => {
  const rankType = routeInfoStore.useStore((s) => s.params.rankType);
  return <PureResourceTable rankType={rankType} key={rankType} />;
};
