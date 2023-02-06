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
import { Col, Row, Tooltip } from 'antd';
import serviceAnalyticsStore from 'msp/stores/service-analytics';
import NoServicesHolder from 'msp/env-overview/service-list/pages/no-services-holder';
import { TimeSelectWithStore } from 'msp/components/time-select';
import { auxiliaryColorMap, functionalColor } from 'common/constants';
import DiceConfigPage from 'config-page';
import monitorCommonStore from 'common/stores/monitorCommon';
import { getAnalyzerOverview } from 'msp/services/service-list';
import routeInfoStore from 'core/stores/route';
import EChart from 'charts/components/echarts';
import { groupBy, reduce, uniqBy } from 'lodash';
import moment from 'moment';
import { genLinearGradient, newColorMap } from 'charts/theme';
import i18n from 'i18n';
import { getFormatter } from 'charts/utils';
import topologyStore from 'msp/env-overview/topology/stores/topology';
import TopologyComp from 'msp/env-overview/topology/pages/topology/component/topology-comp';
import { Cards, TopologyOverviewWrapper } from 'msp/env-overview/topology/pages/topology/component/topology-overview';
import ErdaIcon from 'common/components/erda-icon';
import { useFullScreen } from 'common/use-hooks';
import './index.scss';

const formatTime = getFormatter('TIME', 'ns');

const chartConfig = [
  {
    title: i18n.t('msp:throughput'),
    key: 'RPS',
    unit: 'reqs/s',
    formatter: (param: Obj[]) => {
      const { data: count, marker, axisValue } = param[0] ?? [];
      return `${axisValue}</br>${marker} ${count} reqs/s`;
    },
  },
  {
    title: i18n.t('response time'),
    key: 'AvgDuration',
    unit: 'ms',
    formatter: (param: Obj[]) => {
      const { data: count, marker, axisValue } = param[0] ?? [];
      return `${axisValue}</br>${marker} ${formatTime.format(count * 1000000)}`;
    },
  },
  {
    title: i18n.t('msp:HTTP status'),
    key: 'HttpCode',
  },
  {
    title: i18n.t('msp:request error rate'),
    key: 'ErrorRate',
    unit: '%',
    formatter: (param: Obj[]) => {
      const { data: count, marker, axisValue } = param[0] ?? [];
      return `${axisValue}</br>${marker} ${count} %`;
    },
  },
];
const topNConfig = [
  {
    key: 'pathRpsMaxTop5',
    color: functionalColor.actions,
    icon: 'jiekoutuntu',
  },
  {
    key: 'pathSlowTop5',
    color: functionalColor.success,
    icon: 'jiekoutiaoyong',
  },
  {
    key: 'pathErrorRateTop5',
    color: functionalColor.error,
    icon: 'jiekoucuowushuai',
  },
  {
    key: 'pathClientRpsMaxTop5',
    color: auxiliaryColorMap.purple.deep,
    icon: 'kehuduantiaoyong',
  },
  {
    key: 'sqlSlowTop5',
    color: functionalColor.info,
    icon: 'SQLtiaoyong',
  },
  {
    key: 'exceptionCountTop5',
    color: functionalColor.warning,
    icon: 'fuwuyichang',
  },
];

const topNMap = reduce(
  topNConfig,
  (prev, next) => {
    return {
      ...prev,
      [`service-overview@${next.key}`]: {
        props: {
          theme: [
            {
              titleIcon: next.icon,
              color: next.color,
            },
          ],
        },
      },
    };
  },
  {},
);

const axis = {
  splitLine: {
    show: false,
  },
};

const OverView = () => {
  const [serviceId, requestCompleted] = serviceAnalyticsStore.useStore((s) => [s.serviceId, s.requestCompleted]);
  const range = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan.range);
  const tenantId = routeInfoStore.useStore((s) => s.params.terminusKey);
  const { clearMonitorTopology } = topologyStore.reducers;
  const { getMonitorTopology } = topologyStore.effects;
  const [topologyData] = topologyStore.useStore((s) => [s.topologyData]);
  const serviceTopologyRef = React.useRef<HTMLDivElement>(null);
  const [isFullScreen, { toggleFullscreen }] = useFullScreen(serviceTopologyRef);
  const [charts] = getAnalyzerOverview.useState();

  React.useEffect(() => {
    if (serviceId) {
      getMonitorTopology({
        startTime: range.startTimeMs,
        endTime: range.endTimeMs,
        terminusKey: tenantId,
        tags: [`service:${serviceId}`],
      });
      getAnalyzerOverview.fetch({
        tenantId,
        view: 'topology_service_node',
        serviceIds: [serviceId],
        startTime: range.startTimeMs,
        endTime: range.endTimeMs,
      });
    }
    return () => {
      clearMonitorTopology();
    };
  }, [serviceId, range, tenantId]);

  const chartsData = React.useMemo(() => {
    const { views } = charts?.list[0] ?? {};
    const legendData = {};
    const xAxisData = {};
    const seriesData = {};
    (views || []).forEach(({ type, view }) => {
      const dimensions = groupBy(view, 'dimension');
      const dimensionsArr = Object.keys(dimensions);
      legendData[type] = dimensionsArr;
      seriesData[type] = [];
      dimensionsArr.forEach((name) => {
        xAxisData[type] = dimensions[name].map((t) => moment(t.timestamp).format('YYYY-MM-DD HH:mm:ss'));
        let series: Record<string, any> = {
          name,
          data: dimensions[name].map((t) => (type === 'AvgDuration' ? t.value / 1000000 : t.value)),
        };
        if (!(type === 'HttpCode' && name !== '200')) {
          series = {
            ...series,
            itemStyle: {
              normal: {
                lineStyle: {
                  color: newColorMap.primary4,
                },
              },
            },
            areaStyle: {
              normal: {
                color: genLinearGradient(newColorMap.primary4),
              },
            },
          };
        }
        seriesData[type].push(series);
      });
    });
    return { legendData, xAxisData, seriesData };
  }, [charts]);

  const overviewList = React.useMemo(() => {
    const { metric } = topologyData.nodes?.find((t) => t.serviceId === serviceId) ?? ({} as TOPOLOGY.INode);
    return [
      {
        key: 'rps',
        label: i18n.t('msp:average throughput'),
        value: metric?.rps,
        unit: 'reqs/s',
      },
      {
        key: 'rt',
        label: i18n.t('msp:average response time'),
        value: metric?.rt,
        unit: 'ms',
      },
      {
        key: 'error_rate',
        label: i18n.t('msp:request error rate'),
        value: metric?.error_rate,
        unit: '%',
      },
      {
        key: 'running',
        label: i18n.t('msp:service instance'),
        value: metric?.running,
      },
    ];
  }, [topologyData, serviceId]);

  const topologyList = React.useMemo(() => {
    const currentNode = topologyData.nodes?.find((t) => t.serviceId === serviceId);
    if (currentNode) {
      const temp =
        topologyData.nodes.filter(
          (item) => item.serviceId !== serviceId && item.parents.some((t) => t.serviceId === serviceId),
        ) ?? [];
      const nodes = temp.map((item) => {
        return {
          ...item,
          parents: item.parents?.filter((t) => t.serviceId === serviceId),
        };
      });
      return { nodes: uniqBy([...nodes, currentNode, ...(currentNode?.parents || [])], 'id') };
    } else {
      return { nodes: [] };
    }
  }, [topologyData, serviceId]);

  const handleScreen = () => {
    toggleFullscreen();
  };

  if (!serviceId && requestCompleted) {
    return <NoServicesHolder />;
  }

  return (
    <div className="service-overview bg-white">
      <div className="h-12 flex justify-end items-center px-4 bg-lotion">
        <TimeSelectWithStore className="m-0" />
      </div>
      <div
        className={`service-overview-topology flex flex-col overflow-hidden ${isFullScreen ? '' : 'fixed-height'}`}
        ref={serviceTopologyRef}
      >
        <div className="h-12 flex justify-between items-center px-4 bg-white-02 text-white font-medium">
          {i18n.t('msp:service topology')}
          <Tooltip
            getTooltipContainer={(e) => e.parentNode}
            placement={isFullScreen ? 'bottomRight' : undefined}
            title={isFullScreen ? i18n.t('exit full screen') : i18n.t('full screen')}
          >
            <ErdaIcon
              onClick={handleScreen}
              type={isFullScreen ? 'off-screen-one' : 'full-screen-one'}
              className="text-white-4 hover:text-white cursor-pointer"
            />
          </Tooltip>
        </div>
        <div className="flex-1 flex topology-wrapper">
          <TopologyOverviewWrapper>
            <div className="pt-3">
              <Cards list={overviewList} canSelect={false} />
            </div>
          </TopologyOverviewWrapper>
          <div className="flex-1 h-full relative">
            {topologyList.nodes?.length ? (
              <TopologyComp allowScroll={false} data={topologyList} filterKey={'node'} />
            ) : null}
          </div>
        </div>
      </div>
      <div className="h-12 flex justify-start items-center px-4 bg-lotion text-default font-medium">
        {i18n.t('msp:service request overview')}
      </div>
      <div className="mx-5 mb-4 mt-3">
        <Row gutter={8}>
          {chartConfig.map((item) => {
            const currentOption = {
              backgroundColor: 'rgba(255, 255, 255, 0.02)',
              yAxis: {
                ...axis,
                type: 'value',
                name: item.unit,
                nameTextStyle: {
                  padding: [0, 7, 0, 0],
                  align: 'right',
                },
                splitLine: {
                  show: true,
                },
              },
              grid: {
                top: '17%',
                left: '10%',
              },
              xAxis: {
                ...axis,
                data: chartsData.xAxisData[item.key] ?? [],
              },
              legend: {
                data: (chartsData.legendData[item.key] ?? []).map((t) => ({
                  name: t,
                })),
                pageIconSize: 12,
                bottom: '1%',
              },
              tooltip: {
                trigger: 'axis',
                formatter: item.formatter,
              },
              series: (chartsData.seriesData[item.key] ?? []).map((t) => ({
                ...t,
                type: 'line',
                smooth: false,
              })),
            };
            return (
              <Col span={12} className="my-1">
                <div className="bg-default-01">
                  <div className="pt-3 mb-3 px-4 text-default-8">{item.title}</div>
                  <div className="px-4" style={{ height: '170px' }}>
                    <EChart style={{ width: '100%', height: '160px', minHeight: 0 }} option={currentOption} />
                  </div>
                </div>
              </Col>
            );
          })}
        </Row>
      </div>
      <div className="h-12 flex justify-start items-center px-4 bg-lotion text-default font-medium">
        {i18n.t('msp:service invocation analysis')}
      </div>
      <DiceConfigPage
        showLoading
        scenarioType="service-overview"
        scenarioKey="service-overview"
        forceUpdateKey={['inParams']}
        inParams={{ tenantId, serviceId, startTime: range.startTimeMs, endTime: range.endTimeMs }}
        fullHeight={false}
        customProps={{
          grid: {
            props: {
              span: [8, 8, 8, 8, 8, 8],
            },
          },
          ...topNMap,
        }}
      />
    </div>
  );
};
export default OverView;
