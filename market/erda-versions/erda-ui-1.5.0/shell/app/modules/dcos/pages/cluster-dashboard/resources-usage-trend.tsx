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
import { CardContainer, ContractiveFilter, Holder, Title } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Col, DatePicker, Radio, Row, Table } from 'antd';
import { getResourceClass, getClusterTrend, getProjectTrend } from 'dcos/services/dashboard';
import { map, merge, update } from 'lodash';
import moment from 'moment';
import React from 'react';
import { useMount } from 'configForm/form/utils';
import i18n from 'i18n';
import { getProjectListNew } from 'project/services/project';
import { colorMap } from 'charts/theme';

const defaultData = {
  cluster: { series: null },
  owner: { series: null },
  project: { series: null },
};
export const ResourcesUsagePie = React.memo(({ clusterNameStr }: { clusterNameStr: string }) => {
  const [state, updater] = useUpdate({
    resourceType: 'cpu' as ORG_DASHBOARD.CpuMem,
    clusterDateRange: [moment().startOf('day'), moment().endOf('day')],
    pieSelectedLegend: {} as Obj<Obj<boolean>>,
  });
  const [data, loading] = getResourceClass.useState();
  React.useEffect(() => {
    getResourceClass.fetch({
      resourceType: state.resourceType,
      clusterName: clusterNameStr.split(','),
    });
  }, [clusterNameStr, state.clusterDateRange, state.resourceType]);

  const getPieOption = (ops: ORG_DASHBOARD.PieChartOption, key: string) => {
    const legendData = ops.series?.length ? ops.series[0].data.map((a) => a.name) : [];
    const option = {
      tooltip: {
        trigger: 'item',
        confine: true,
        formatter: '{a} <br/>{b} ({d}%)',
      },
      legend: {
        type: 'scroll',
        orient: 'vertical',
        left: 'right',
        top: 'middle',
        data: legendData,
      },
      series: map(ops.series, (serie) => {
        const selectedLegend = state.pieSelectedLegend[key] || {};
        const total = serie.data
          .reduce((all, cur) => all + (selectedLegend[cur.name] === false ? 0 : cur.value), 0)
          .toFixed(1);
        return {
          name: serie.name,
          type: 'pie',
          radius: ['55%', '75%'],
          center: ['35%', '45%'],
          label: {
            normal: {
              show: true,
              position: 'center',
              formatter: `${i18n.t('cmp:Total')}\n ${total} ${state.resourceType === 'cpu' ? i18n.t('cmp:Core') : 'G'}`,
              color: colorMap.gray,
              textStyle: {
                fontSize: '20',
                fontWeight: 'bold',
              },
            },
            emphasis: {
              show: false,
            },
          },
          itemStyle: {
            emphasis: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
          data: serie.data,
        };
      }),
    };
    return option;
  };

  const onEvents = (key: string) => ({
    legendselectchanged: (params: { selected: Obj<boolean> }) => {
      updater.pieSelectedLegend((prev: any) => ({ ...prev, [key]: params.selected }));
    },
  });

  return (
    <>
      <Title
        level={1}
        title={i18n.t('cmp:resource type')}
        operations={[
          <>
            <Radio.Group
              buttonStyle="solid"
              onChange={(e) => updater.resourceType(e.target.value)}
              defaultValue={state.resourceType}
            >
              <Radio.Button value="cpu">CPU</Radio.Button>
              <Radio.Button value="memory">{i18n.t('cmp:Memory')}</Radio.Button>
            </Radio.Group>
          </>,
        ]}
      />
      <Row justify="space-between" gutter={12}>
        {map(data || defaultData, (item, key) => {
          return (
            <Col key={key} span={8}>
              <CardContainer.ChartContainer title={item.series ? item.series[0].name : ''} holderWhen={!item.series}>
                {item.series && (
                  <Echarts
                    style={{ height: '320px' }}
                    onEvents={onEvents(key)}
                    showLoading={loading}
                    option={getPieOption(item, key)}
                  />
                )}
              </CardContainer.ChartContainer>
            </Col>
          );
        })}
      </Row>
      <ResourceTrend clusterNameStr={clusterNameStr} resourceType={state.resourceType} />
    </>
  );
});

export const ResourceTrend = React.memo(
  ({ clusterNameStr, resourceType }: { clusterNameStr: string; resourceType: ORG_DASHBOARD.CpuMem }) => {
    const [state, updater] = useUpdate({
      interval: 'day',
      projectId: [333, 735],
      clusterDateRange: [moment().startOf('day'), moment().endOf('day')],
    });
    const [clusterTrend, loadingClusterTrend] = getClusterTrend.useState();
    const [projectTrend, loadingProjectTrend] = getProjectTrend.useState();
    React.useEffect(() => {
      if (clusterNameStr) {
        getClusterTrend.fetch({
          clusterName: clusterNameStr.split(','),
          resourceType,
          interval: state.interval,
          start: state.clusterDateRange[0].valueOf(),
          end: state.clusterDateRange[1].valueOf(),
        });
        getProjectTrend.fetch({
          clusterName: clusterNameStr.split(','),
          resourceType,
          projectId: state.projectId,
          interval: state.interval,
          start: state.clusterDateRange[0].valueOf(),
          end: state.clusterDateRange[1].valueOf(),
        });
      }
    }, [clusterNameStr, resourceType, state.clusterDateRange, state.interval, state.projectId]);

    const projectList = getProjectListNew.useData();
    useMount(() => {
      getProjectListNew.fetch({ pageNo: 1, pageSize: 1000 }); // TODO: if total is more than 1000
    });

    const getBarOption = (option: ORG_DASHBOARD.EchartOption) => {
      if (!option) return {};
      let newOption = option;
      const defaultOption = {
        xAxis: { splitLine: { show: false } },
        tooltip: { trigger: 'axis' },
        yAxis: { type: 'value' },
        series: option.series.map((item: Obj) => ({
          type: 'bar',
          barWidth: '60%',
          ...item,
          label: item.label ? { show: true, ...item.label } : undefined,
        })),
        grid: {
          top: '10%',
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true,
        },
      };
      newOption = merge(defaultOption, option);
      return newOption;
    };

    const conditionsFilter = [
      {
        type: 'select',
        key: 'projectNames',
        label: i18n.t('project'),
        haveFilter: true,
        fixed: true,
        emptyText: i18n.t('dop:all'),
        showIndex: 1,
        options: (projectList?.list || []).map((prj) => ({ label: prj.displayName || prj.name, value: prj.id })),
        customProps: {
          mode: 'single',
        },
      },
    ];

    return (
      <>
        <Title
          level={2}
          mt={16}
          title={i18n.t('cmp:Trend of cluster resources')}
          operations={[
            <DatePicker.RangePicker
              allowClear={false}
              allowEmpty={[false, false]}
              value={state.clusterDateRange}
              ranges={{
                Today: [moment().startOf('day'), moment().endOf('day')],
                'This Month': [moment().startOf('month'), moment().endOf('month')],
              }}
              onChange={(dates) => updater.clusterDateRange(dates)}
            />,
            <Radio.Group
              buttonStyle="solid"
              onChange={(e) => updater.interval(e.target.value)}
              defaultValue={state.interval}
            >
              <Radio.Button value="day">{i18n.t('cmp:Daily')}</Radio.Button>
              <Radio.Button value="week">{i18n.t('cmp:Weekly')}</Radio.Button>
              <Radio.Button value="month">{i18n.t('cmp:Monthly')}</Radio.Button>
            </Radio.Group>,
          ]}
        />
        <Row justify="space-between" gutter={12}>
          <Col span={12}>
            <CardContainer.ChartContainer title={clusterTrend?.series[0]?.name} holderWhen={!clusterTrend}>
              <Echarts
                style={{ height: '320px' }}
                showLoading={loadingClusterTrend}
                option={getBarOption(clusterTrend)}
              />
            </CardContainer.ChartContainer>
          </Col>
          <Col span={12}>
            <CardContainer.ChartContainer
              title={projectTrend?.series[0]?.name}
              operation={
                <ContractiveFilter delay={1000} conditions={conditionsFilter} onChange={(values) => update(values)} />
              }
              holderWhen={!projectTrend}
            >
              <Echarts
                style={{ height: '320px' }}
                showLoading={loadingProjectTrend}
                option={getBarOption(projectTrend)}
              />
            </CardContainer.ChartContainer>
          </Col>
        </Row>
      </>
    );
  },
);
