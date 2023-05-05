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

import { get, isEmpty } from 'lodash';
import React from 'react';
import { EmptyHolder } from 'common';
import { Row, Col, Progress } from 'antd';
import { getFormatter } from 'charts/utils/formatter';
import i18n from 'i18n';

interface IData {
  [pro: string]: any;
  value: string;
  name: string;
  percent: number;
}
const renderProgress = (data: IData, i: number, shiftingLeft?: any) => (
  <div key={i} className="progress-row">
    <div className="progress-left">
      <span>{data.name}</span>
      <span>{getFormatter('TIME').format(data.value, 2)}</span>
    </div>
    <div className="progress-right">
      <div style={shiftingLeft ? { left: shiftingLeft[i], position: 'absolute', width: '100%' } : {}}>
        <Progress strokeLinecap="square" percent={data.percent} showInfo={false} />
      </div>
    </div>
  </div>
);

const SummaryDetail = ({ data }: { data: object }) => {
  const res = get(data, 'data') || {};
  if (!isEmpty(res)) {
    const pv = (res['count.plt'] || {}).data;
    const plt = (res['avg.plt'] || {}).data;
    const dit = (res['avg.dit'] || {}).data;
    const act = (res['avg.act'] || {}).data;
    const clt = (res['avg.clt'] || {}).data;
    const dns = (res['avg.dns'] || {}).data;
    const drt = (res['avg.drt'] || {}).data;
    const put = (res['avg.put'] || {}).data;
    const rpt = (res['avg.rpt'] || {}).data;
    const rrt = (res['avg.rrt'] || {}).data;
    const set = (res['avg.set'] || {}).data;
    const srt = (res['avg.srt'] || {}).data;
    const tcp = (res['avg.tcp'] || {}).data;
    const wst = (res['avg.wst'] || {}).data;
    const rdc = (res['sum.rdc'] || {}).data;
    const net = (res['avg.net'] || {}).data;

    const loadingData = [
      {
        name: i18n.t('msp:end user experience time'),
        value: plt,
        percent: 100,
      },
      {
        name: i18n.t('msp:html completes building'),
        value: dit,
        percent: (dit / plt) * 100,
      },
      {
        name: i18n.t('msp:blocking resource loading time'),
        value: drt,
        percent: (drt / plt) * 100,
      },
      {
        name: i18n.t('msp:first paint time'),
        value: wst,
        percent: (wst / plt) * 100,
      },
      {
        name: i18n.t('msp:network time consuming'),
        value: net,
        percent: (net / plt) * 100,
      },
    ];

    const paramsData = [
      {
        name: i18n.t('msp:page uninstall'),
        value: put,
        percent: (put / plt) * 100,
      },
      {
        name: i18n.t('msp:page jump'),
        value: rrt,
        percent: (rrt / plt) * 100,
      },
      {
        name: i18n.t('msp:query cache'),
        value: act,
        percent: (act / plt) * 100,
      },
      {
        name: i18n.t('msp:query dns'),
        value: dns,
        percent: (dns / plt) * 100,
      },
      {
        name: i18n.t('msp:building a tcp connection'),
        value: tcp,
        percent: (tcp / plt) * 100,
      },
      {
        name: i18n.t('msp:server response'),
        value: srt,
        percent: (srt / plt) * 100,
      },
      {
        name: i18n.t('msp:page download'),
        value: rpt,
        percent: (rpt / plt) * 100,
      },
    ];

    const buildingData = [
      {
        name: i18n.t('msp:dom parsing'),
        value: dit,
        percent: (dit / plt) * 100,
        key: 'dit',
      },
      {
        name: 'dom ready',
        value: drt,
        percent: (drt / plt) * 100,
        key: 'drt',
      },
      {
        name: i18n.t('msp:script execution'),
        value: set,
        percent: (set / plt) * 100,
        key: 'set',
      },
      {
        name: i18n.t('msp:load event'),
        value: clt,
        percent: (clt / plt) * 100,
        key: 'clt',
      },
    ];

    const keyList = ['put', 'rrt', 'act', 'dns', 'tcp', 'rqt', 'rpt', 'dit', 'drt', 'set', 'clt'];
    let timeCount = 0;
    // rqt包含rpt时间，重写为差值
    const temp: any = { ...performance, rqt: srt };
    const shiftingLeft = keyList.map((key, i) => {
      timeCount += i ? temp[keyList[i - 1]] : 0;
      return `${parseFloat(`${(timeCount / temp.plt) * 100}`)}%`;
    });

    const part2Left = shiftingLeft.slice(0, paramsData.length);
    const part3Left = shiftingLeft.slice(-buildingData.length);
    return (
      <div>
        <div className="positionDetail">
          <div className="progress-container">
            <Row>
              <Col span={8}>PV：&nbsp;&nbsp;{pv}</Col>
              <Col span={8}>
                {i18n.t('msp:DNS parsing times')}: &nbsp;&nbsp;{rdc}
              </Col>
            </Row>
          </div>
          <div className="progress-container">{loadingData.map((dt, i) => renderProgress(dt, i))}</div>
          <div className="progress-container">{paramsData.map((dt, i) => renderProgress(dt, i, part2Left))}</div>
          <div className="progress-container">{buildingData.map((dt, i) => renderProgress(dt, i, part3Left))}</div>
        </div>
      </div>
    );
  }
  return (
    <div style={{ height: '400px', backgroundColor: '#fff' }}>
      <EmptyHolder />
    </div>
  );
};

export default SummaryDetail;
