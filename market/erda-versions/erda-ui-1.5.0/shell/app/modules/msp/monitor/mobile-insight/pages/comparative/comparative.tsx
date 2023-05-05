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

import { map, isEmpty } from 'lodash';
import React from 'react';
import { Row, Col, Select } from 'antd';
import { MonitorChartNew } from 'charts';
import { EmptyHolder, Icon as CustomIcon } from 'common';
import { useEffectOnce } from 'react-use';
import comparativeStore from '../../stores/comparative';
import routeInfoStore from 'core/stores/route';
import monitorCommonStore from 'common/stores/monitorCommon';
import { goTo } from 'common/utils';
import './comparative.scss';
import i18n from 'i18n';

export const targetList = [
  { name: i18n.t('msp:user experience apdex'), key: 'apdex' },
  { name: i18n.t('msp:the whole page is loaded'), key: 'plt' },
];

export const featureList = [
  { name: i18n.t('msp:province'), key: 'province' },
  { name: i18n.t('msp:city'), key: 'city' },
  { name: i18n.t('msp:domain name'), key: 'host' },
  { name: i18n.t('msp:page'), key: 'doc_path' },
  { name: i18n.t('msp:system'), key: 'os' },
  { name: i18n.t('msp:device'), key: 'device' },
  { name: i18n.t('msp:app version'), key: 'av' },
];

const MIComparative = () => {
  const timeSpan = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan.range);
  const data = comparativeStore.useStore((s) => s.comparative);
  const { loadComparative: loadDetail } = comparativeStore.effects;
  const terminusKey = routeInfoStore.useStore((s) => s.params.terminusKey);
  const [target, setTarget] = React.useState('apdex');
  const [compareBy, setCompareBy] = React.useState('province');

  useEffectOnce(() => {
    changeTarget(targetList[0].key);
  });

  React.useEffect(() => {
    const { startTimeMs: start, endTimeMs: end } = timeSpan;
    let query = {};
    if (target === 'apdex') {
      // ranges: 响应时间层级(毫秒)
      // 0-2000（2秒内）为一个层级，对应满意的数据，2000-8000对应一般，8000以上对应不满
      query = {
        start,
        end,
        filter_type: 'mobile',
        filter_tk: terminusKey,
        group: compareBy,
        range: 'plt',
        ranges: '0:2000,2000:8000,8000:',
        source: true,
        fetchApi: 'ta_m_apdex/range',
        type: 'apdex',
      };
    } else {
      query = {
        start,
        end,
        filter_type: 'mobile',
        filter_tk: terminusKey,
        limit: 5,
        sort: 'count',
        split: 10,
        rangeSize: 1000,
        source: true,
        group: compareBy,
        range: target,
        fetchApi: 'ta_m_values_grade/range',
        type: 'timing',
      };
    }
    loadDetail({ query } as any);
  }, [target, compareBy, timeSpan, terminusKey, loadDetail]);

  const changeTarget = (value: string) => {
    setTarget(value);
  };
  const changeFeature = (value: string) => {
    setCompareBy(value);
  };
  const goBack = () => {
    goTo('../');
  };

  return (
    <div className="analysis-chart">
      <div className="analysis-chart-top">
        <span className="index-explain">
          {i18n.t('msp:comparative analysis of user features')}
          <span className="close-icon">
            <CustomIcon onClick={goBack} type="guanbi" />
          </span>
        </span>
        <div className="selector-wrapper">
          <span>{i18n.t('msp:selector')}</span>
          <div className="analysis-selector">
            <span>{i18n.t('msp:index')}</span>
            <Select
              key="select-one"
              className="time-range-selector one"
              defaultValue={targetList[0].key}
              onChange={changeTarget}
            >
              {map(targetList, (tgt, i) => (
                <Select.Option key={i} value={tgt.key}>
                  {tgt.name}
                </Select.Option>
              ))}
            </Select>
            <span>{i18n.t('msp:user features')}</span>
            <Select
              key="select-two"
              className="time-range-selector two"
              defaultValue={featureList[0].key}
              onChange={changeFeature}
            >
              {map(featureList, (feature, i) => (
                <Select.Option key={i} value={feature.key}>
                  {feature.name}
                </Select.Option>
              ))}
            </Select>
          </div>
        </div>
      </div>
      <div className="card-container">
        {isEmpty(data) ? (
          <EmptyHolder />
        ) : (
          <Row gutter={24} className="chart-row">
            {map(data, ({ results, xAxis, titleText }, i) => {
              return (
                <Col className="gutter-row" span={i ? 12 : 24} key={i}>
                  <CardContainer.ChartContainer title={titleText}>
                    <MonitorChartNew
                      seriesType="bar"
                      yAxisNames={[i18n.t('msp:request times')]}
                      data={{ results, xAxis }}
                      opt={
                        target === 'apdex'
                          ? {
                              grid: {
                                top: 30,
                                bottom: 0,
                                left: 25,
                              },
                            }
                          : {
                              grid: {
                                top: 30,
                                right: 35,
                                bottom: 0,
                              },
                              xAxis: [
                                {
                                  name: `${i18n.t('time')}(ms)`,
                                  nameGap: -20,
                                },
                              ],
                            }
                      }
                      isLabel
                      isBarChangeColor
                    />
                  </CardContainer.ChartContainer>
                </Col>
              );
            })}
          </Row>
        )}
      </div>
    </div>
  );
};

export default MIComparative;
