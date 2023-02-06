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
import { Row, Col, Radio, Select, Table, Tooltip } from 'antd';
import { map, get } from 'lodash';
import { Icon as CustomIcon, BoardGrid, TimeSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import moment from 'moment';
import { SelectValue, ColumnProps } from 'core/common/interface';
import errorReportStore from 'app/modules/publisher/stores/error-report';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';
import monitorCommonStore from 'common/stores/monitorCommon';
import { goTo } from 'common/utils';
import publisherStore from 'publisher/stores/publisher';
import './index.scss';

interface IProps {
  artifacts: PUBLISHER.IArtifacts;
}

const RadioGroup = Radio.Group;
const { Option } = Select;

const layout = {
  xl: { span: 6 },
  lg: { span: 12 },
  sm: { span: 24 },
};

const formatTrendData = (data?: PUBLISHER.IStatisticsTrend | PUBLISHER.IErrorTrend) => {
  return [
    {
      label: i18n.t('publisher:number of crashes'),
      data: get(data, 'crashTimes') || 0,
    },
    {
      label: i18n.t('publisher:crash rate'),
      data: get(data, 'crashRate') || 0,
      subData: get(data, 'crashRateGrowth') || 0,
    },
    {
      label: i18n.t('publisher:affect the number of users'),
      data: get(data, 'affectUsers') || 0,
    },
    {
      label: i18n.t('publisher:affect the proportion of users'),
      data: get(data, 'affectUsersProportion') || 0,
      subData: get(data, 'affectUsersProportionGrowth') || 0,
    },
  ];
};

const getLineChartLayout = (data: any) => {
  let xData = [] as string[];
  const metricData = [] as any[];
  const { time, results } = data;
  xData = map(time, (item) => moment(item).format('MM-DD HH:mm'));
  const resultData = get(results, '[0].data[0]');
  map(resultData, (item) => {
    metricData.push({
      name: item.tag || item.name,
      type: 'line',
      data: item.data,
      unit: item.unit,
    });
  });
  return {
    xData,
    metricData,
  };
};

const FilterVersion = ({
  value,
  onChange,
  className,
  groups,
}: {
  value?: string;
  onChange: (val: string | undefined) => void;
  className?: string;
  groups: Array<{ label: string; value: string }>;
}) => {
  return (
    <Select
      className={`${className || ''}`}
      onChange={(val: SelectValue) => {
        onChange(val as string);
      }}
      allowClear
      value={value}
      placeholder={i18n.t('publisher:select version')}
    >
      {map(groups, ({ value: val, label }) => {
        return (
          <Option key={val} value={val}>
            {label}
          </Option>
        );
      })}
    </Select>
  );
};

const FilterTab = ({
  value,
  onChange,
  className,
}: {
  value?: string;
  onChange: (val: string) => void;
  className?: string;
}) => {
  const tabs = [
    { label: i18n.t('publisher:number of crashes'), value: 'tags.error' },
    { label: i18n.t('publisher:crash rate'), value: 'crashRate' },
    { label: i18n.t('publisher:affect user'), value: 'tags.uid' },
    { label: i18n.t('publisher:affect the proportion of users'), value: 'affectUser' },
  ];
  return (
    <RadioGroup
      className={`${className || ''}`}
      size="small"
      value={value || tabs[0].value}
      onChange={(e: any) => onChange(e.target.value)}
    >
      {map(tabs, ({ label, value: val }) => {
        return (
          <Radio.Button key={val} value={val}>
            {label}
          </Radio.Button>
        );
      })}
    </RadioGroup>
  );
};

const ErrorList = ({
  artifactsId,
  timeSpan,
  version,
  monitorKey,
}: {
  artifactsId: string;
  timeSpan: ITimeSpan;
  version: string | undefined;
  monitorKey: PUBLISHER.MonitorKey;
}) => {
  const errorList = errorReportStore.useStore((s) => s.errorList);
  const { getErrorList } = errorReportStore.effects;
  const { clearErrorList } = errorReportStore.reducers;
  const [loading] = useLoading(errorReportStore, ['getErrorList']);
  React.useEffect(() => {
    getErrorList({
      artifactsId,
      start: timeSpan.startTimeMs,
      end: timeSpan.endTimeMs,
      filter_av: version,
      ...monitorKey,
    });
    return clearErrorList;
  }, [artifactsId, clearErrorList, getErrorList, monitorKey, timeSpan, version]);
  const { startTimeMs, endTimeMs } = timeSpan;
  const link = `./error?start=${startTimeMs}&end=${endTimeMs}&ak=${monitorKey.ak}&ai=${monitorKey.ai}`;

  const columns: Array<ColumnProps<PUBLISHER.ErrorItem>> = [
    {
      title: i18n.t('publisher:error summary'),
      dataIndex: 'errSummary',
      width: 400,
      ellipsis: {
        showTitle: false,
      },
      render: (text) => {
        const decoded = decodeURIComponent(text);
        return (
          <Tooltip title={decoded} placement="topLeft">
            <span
              className="fake-link"
              onClick={() => {
                goTo(`${link}&filter=${encodeURIComponent(text).replace(/\*/g, '%2A')}`); // *为监控特殊保留字符
              }}
            >
              {decoded}
            </span>
          </Tooltip>
        );
      },
    },
    {
      title: i18n.t('version'),
      dataIndex: 'appVersion',
      width: 80,
      ellipsis: true,
    },
    {
      title: i18n.t('publisher:time first occurred'),
      dataIndex: 'timeOfFirst',
      width: 160,
      ellipsis: {
        showTitle: false,
      },
      render: (text: number) => {
        const val = moment(text).format('YYYY-MM-DD HH:mm:ss');
        return <Tooltip title={val}>{val}</Tooltip>;
      },
    },
    {
      title: i18n.t('publisher:last occurred'),
      dataIndex: 'timeOfRecent',
      width: 160,
      ellipsis: {
        showTitle: false,
      },
      render: (text: number) => {
        const val = moment(text).format('YYYY-MM-DD HH:mm:ss');
        return <Tooltip title={val}>{val}</Tooltip>;
      },
    },
    {
      title: i18n.t('publisher:cumulative error times'),
      dataIndex: 'totalErr',
      width: 110,
      ellipsis: true,
    },
    {
      title: i18n.t('publisher:cumulative number of users affected'),
      dataIndex: 'affectUsers',
      width: 130,
      ellipsis: true,
    },
  ];

  return (
    <>
      <Table
        columns={columns}
        loading={loading}
        dataSource={errorList.map((item, i) => ({ ...item, key: i }))}
        pagination={false}
        scroll={{ x: '100%' }}
      />
    </>
  );
};

const formatFilterData = (data: any) => {
  const result = get(data, 'results[0].data');
  const list = [] as Array<{ label: string; value: string }>;
  map(result, (item) => {
    map(item, ({ tag }) => list.push({ label: tag, value: tag }));
  });
  return list;
};

const ErrorReport = (props: IProps) => {
  const { artifacts } = props;
  const timeSpan = monitorCommonStore.useStore((s) => s.timeSpan);
  const publishItemMonitors = publisherStore.useStore((s) => s.publishItemMonitors);
  const publisherItemId = artifacts.id;
  const { getErrorTrend, getErrorChart, getAllVersion } = errorReportStore.effects;
  const [{ errorTrend, lineChartType, lineData, lineVersion, groups, selectMonitorKey }, updater] = useUpdate({
    errorTrend: formatTrendData(),
    lineChartType: 'tags.error',
    lineVersion: undefined as string | undefined,
    lineData: {},
    groups: [],
    selectMonitorKey: Object.keys(publishItemMonitors)[0],
  });

  const monitorKey = React.useMemo(() => {
    const { ak, ai } = publishItemMonitors[selectMonitorKey] || {};
    return { ak, ai };
  }, [publishItemMonitors, selectMonitorKey]);

  React.useEffect(() => {
    getErrorTrend({ publisherItemId, ...monitorKey }).then((res) => updater.errorTrend(formatTrendData(res)));
    getAllVersion({
      publisherItemId,
      group: 'tags.av',
      count: 'tags.cid',
      start: moment().subtract(32, 'days').valueOf(),
      end: Date.now(),
      ...monitorKey,
    }).then((res) => updater.groups(formatFilterData(res)));
  }, [getAllVersion, getErrorTrend, monitorKey, publisherItemId, updater]);

  const lineChartQuery = React.useMemo(() => {
    let queryKey = 'cardinality';
    if (lineChartType === 'tags.error') queryKey = 'count';
    return {
      start: timeSpan.startTimeMs,
      end: timeSpan.endTimeMs,
      [queryKey]: lineChartType,
      filter_av: lineVersion,
      align: false,
      points: 7,
      ...monitorKey,
    };
  }, [timeSpan, lineChartType, lineVersion, monitorKey]);

  React.useEffect(() => {
    if (lineChartQuery.cardinality || lineChartQuery.count) {
      getErrorChart({ ...lineChartQuery, publisherItemId }).then((res) => updater.lineData(res));
    }
  }, [getErrorChart, lineChartQuery, publisherItemId, updater]);

  const lineChart = [
    {
      w: 24,
      h: 6,
      x: 0,
      y: 0,
      i: 'line',
      moved: false,
      static: false,
      view: {
        chartType: 'chart:line',
        hideReload: true,
        hideHeader: true,
        staticData: getLineChartLayout(lineData),
      },
    },
  ];

  return (
    <div className="artifacts-error-report">
      <Select
        value={selectMonitorKey}
        style={{ width: 200 }}
        className="mb-2"
        onChange={(k) => {
          updater.selectMonitorKey(k);
        }}
      >
        {map(publishItemMonitors, (_, key) => (
          <Select.Option key={key} value={key}>
            {key}
          </Select.Option>
        ))}
      </Select>
      <div className="total-trend block-container">
        <div className="title font-bold text-base">{i18n.t("publisher:Today's error trend")}</div>
        <Row>
          {map(errorTrend, (info, idx) => {
            return (
              <Col key={`${idx}`} {...layout}>
                <div className="info-block border-bottom">
                  <div className="data">
                    <span className="main-data">{info.data}</span>
                    <span className="sub-data">
                      {info.subData !== undefined ? (
                        <>
                          {info.subData}
                          {`${info.subData}`.startsWith('-') ? (
                            <CustomIcon className="text-red" type="arrow-down" />
                          ) : (
                            <CustomIcon className="text-green" type="arrow-up" />
                          )}
                        </>
                      ) : null}
                    </span>
                  </div>
                  <div className="label">{info.label}</div>
                </div>
              </Col>
            );
          })}
        </Row>
      </div>
      <div className="mt-4">
        <TimeSelector className="ml-0 mr-3" inline disabledDate={() => false} />
        <FilterVersion
          className="version-selector"
          value={lineVersion}
          onChange={updater.lineVersion}
          groups={groups}
        />
      </div>
      <div className="mt-4 block-container">
        <div className="title flex justify-between items-center">
          <span className="font-bold text-base">{i18n.t('publisher:error statistics')}</span>
        </div>
        <div>
          <FilterTab className="m-2" value={lineChartType} onChange={updater.lineChartType} />
          <BoardGrid.Pure layout={lineChart} />
        </div>
      </div>
      <div className="mt-4 block-container">
        <div className="title flex justify-between items-center">
          <span className="font-bold text-base">{i18n.t('publisher:error list')}</span>
        </div>
        <div>
          <ErrorList monitorKey={monitorKey} artifactsId={publisherItemId} timeSpan={timeSpan} version={lineVersion} />
        </div>
      </div>
    </div>
  );
};
export default ErrorReport;
