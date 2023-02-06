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
import i18n from 'i18n';
import { Tag } from 'antd';
import { groupBy, isEmpty } from 'lodash';
import ErdaIcon from 'common/components/erda-icon';
import { getAnalyzerOverview } from 'msp/services/service-list';
import routeInfoStore from 'core/stores/route';
import EChart from 'charts/components/echarts';
import moment from 'moment';
import monitorCommonStore from 'common/stores/monitorCommon';
import { genLinearGradient, newColorMap } from 'app/charts/theme';
import { getFormatter } from 'charts/utils';
import Ellipsis from 'common/components/ellipsis';

const formatTime = getFormatter('TIME', 'ns');

interface IProps {
  className: string;
  data: TOPOLOGY.TopoNode['metaData'];
  onCancel?: () => void;
  showRuntime?: boolean;
}

const metric = [
  {
    name: i18n.t('msp:throughput'),
    key: 'rps',
    util: 'reqs/s',
  },
  {
    name: i18n.t('msp:average response time'),
    key: 'rt',
    unit: 'ms',
  },
  {
    name: i18n.t('msp:error call times'),
    key: 'http_error',
  },
  {
    name: i18n.t('msp:error rate'),
    key: 'error_rate',
    unit: '%',
  },
];

const chartConfig = [
  {
    title: i18n.t('msp:throughput'),
    key: 'RPS',
    formatter: (param: Obj[]) => {
      const { data: count, marker, axisValue } = param[0] ?? [];
      return `${axisValue}</br>${marker} ${count} reqs/s`;
    },
  },
  {
    title: `${i18n.t('msp:average response time')}(ms)`,
    key: 'AvgDuration',
    formatter: (param: Obj[]) => {
      const { data: count, marker, axisValue } = param[0] ?? [];
      return `${axisValue}</br>${marker} ${formatTime.format(count * 1000000)}`;
    },
  },
  {
    title: i18n.t('msp:error rate'),
    key: 'ErrorRate',
    formatter: (param: Obj[]) => {
      const { data: count, marker, axisValue } = param[0] ?? [];
      return `${axisValue}</br>${marker} ${count} %`;
    },
  },
  {
    title: i18n.t('msp:HTTP status'),
    key: 'HttpCode',
  },
];

const axis = {
  splitLine: {
    show: false,
  },
  axisLabel: {
    textStyle: {
      color: 'rgba(255, 255, 255, 0.3)',
    },
  },
};

const TopologyDetail: React.FC<IProps> = ({ className, data, onCancel, showRuntime }) => {
  const [range] = monitorCommonStore.useStore((s) => [s.globalTimeSelectSpan.range]);
  const [visible, setVisible] = React.useState(false);
  const [charts] = getAnalyzerOverview.useState();
  React.useEffect(() => {
    setVisible(!isEmpty(data));
    if (data.serviceId) {
      const tenantId = routeInfoStore.getState((s) => s.params.terminusKey);
      getAnalyzerOverview.fetch({
        serviceIds: [data.serviceId],
        view: 'topology_service_node',
        tenantId,
        startTime: range.startTimeMs,
        endTime: range.endTimeMs,
      });
    }
  }, [data, range]);

  const handleCancel = () => {
    if (onCancel) {
      onCancel();
    } else {
      setVisible(false);
    }
  };

  React.useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      const wrapper = Array.from(document.querySelectorAll('.topology-detail'));
      const commonNode = Array.from(document.querySelectorAll('.topology-common-node'));
      const node = e.target as Node;
      const inner = wrapper.some((wrap) => wrap.contains(node));
      const inNode = commonNode.some((wrap) => wrap.contains(node));
      if (!(inner || inNode)) {
        handleCancel();
      }
    };
    document.body.addEventListener('click', handleClick);
    return () => {
      document.body.removeEventListener('click', handleClick);
    };
  }, []);

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
        xAxisData[type] = dimensions[name].map((t) => moment(t.timestamp).format('HH:mm:ss'));
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

  return (
    <div className={`topology-detail ${visible ? 'expand' : 'collapse'} ${className}`}>
      <div className="content h-full flex flex-col">
        <div className="flex px-4 justify-between h-12 items-center">
          <div className="flex-1 overflow-ellipsis overflow-hidden whitespace-nowrap text-white pr-4">
            <Ellipsis title={data.name} />
          </div>
          <div onClick={handleCancel} className="text-white-4 cursor-pointer hover:text-white">
            <ErdaIcon type="close" />
          </div>
        </div>
        <div className="flex-1 overflow-y-auto scroll-bar-dark">
          <div>
            {showRuntime ? (
              <p className="mb-2 px-4">
                <span className="text-white-6 mr-2">Runtime:</span>
                <span className="text-white-9 overflow-ellipsis overflow-hidden whitespace-nowrap">
                  {data.runtimeName}
                </span>
              </p>
            ) : null}
            <p className="mb-2 px-4">
              <span className="text-white-6 mr-2">{i18n.t('type')}:</span>
              <Tag color="#27C99A" className="border-0 bg-green bg-opacity-10">
                {data.typeDisplay}
              </Tag>
            </p>
            <div className="metric-detail flex flex-wrap justify-start pl-3">
              {metric.map((item) => {
                return (
                  <div key={item.key} style={{ width: 140 }} className="m-1 py-3">
                    <p className="text-white text-center leading-8 m-0">
                      <span>{data.metric?.[item.key]}</span>
                      {item.unit ? <span className="text-xs text-white-6 ml-1">{item.unit}</span> : null}
                    </p>
                    <p className="text-white-6 text-center text-xs m-0">{item.name}</p>
                  </div>
                );
              })}
            </div>
          </div>
          {data.serviceId ? (
            <div className="px-4">
              {chartConfig.map((item) => {
                const currentOption = {
                  backgroundColor: 'rgba(255, 255, 255, 0.02)',
                  yAxis: {
                    ...axis,
                    type: 'value',
                    splitLine: {
                      show: true,
                      lineStyle: {
                        color: ['rgba(255, 255, 255, 0.1)'],
                      },
                    },
                  },
                  grid: {
                    top: '10%',
                    left: '15%',
                    bottom: '30%',
                  },
                  xAxis: {
                    ...axis,
                    data: chartsData.xAxisData[item.key] ?? [],
                  },
                  legend: {
                    data: (chartsData.legendData[item.key] ?? []).map((t) => ({
                      ...axis.axisLabel,
                      name: t,
                    })),
                    pageIconInactiveColor: 'rgba(255, 255, 255, 0.4)',
                    pageIconColor: 'rgba(255, 255, 255, 0.8)',
                    pageIconSize: 12,
                    pageTextStyle: {
                      color: axis.axisLabel.textStyle.color,
                    },
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
                  <div>
                    <div className="text-white mt-4 mb-2">{item.title}</div>
                    <div style={{ height: '170px' }}>
                      <EChart style={{ width: '100%', height: '160px', minHeight: 0 }} option={currentOption} />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default TopologyDetail;
