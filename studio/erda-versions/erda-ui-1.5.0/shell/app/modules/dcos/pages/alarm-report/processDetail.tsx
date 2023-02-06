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
import { Icon } from 'common';
import { cutStr, qs } from 'common/utils';
import { Row, Col } from 'antd';
import processChartList from './config/processChartList';
import i18n from 'i18n';
import './processDetail.scss';
import routeInfoStore from 'core/stores/route';
import alarmReportStore from 'cmp/stores/alarm-report';

interface IProcessDetailInfo {
  processName: string;
  id: string;
  cmdline: string;
  clusterName: string;
  ip: string;
}

const ProcessDetailInfo = ({ processName, id, cmdline, ip, clusterName }: IProcessDetailInfo) => {
  return (
    <div className="process-detail-info">
      <div className="process-status-panel">
        <div className="process-status">
          <Icon type="lc" className="process-status-icon" />
        </div>
        <div className="process-host">{processName}</div>
      </div>
      <div className="process-configuration-panel">
        <div>
          <span>ID :</span>
          <span>{id}</span>
        </div>
        <div>
          <span>CmdLine :</span>
          <span>{`${cutStr(cmdline, 50)}`}</span>
        </div>
        <div>
          <span>{i18n.t('machine')} IP :</span>
          <span>{ip}</span>
        </div>
        <div>
          <span>{i18n.t('cmp:owned cluster')} :</span>
          <span>{clusterName}</span>
        </div>
      </div>
    </div>
  );
};

const ProcessDetail = () => {
  const processCmdline = alarmReportStore.useStore((s) => s.processCmdline);
  const { getProcessCmdline } = alarmReportStore.effects;
  const params = routeInfoStore.useStore((s) => s.params);
  const { clusterName: filter_cluster_name, processId: filter_pid } = params;
  const { ip: filter_host_ip, name: filter_process_name, timestamp }: any = qs.parse(window.location.search);
  const query = {
    filter_cluster_name,
    filter_pid,
    filter_host_ip,
    filter_process_name,
    timestamp,
  };

  const processChartAttrs = {
    shouldLoad: filter_cluster_name && filter_pid && filter_host_ip && filter_process_name && timestamp,
    query,
  };

  React.useEffect(() => {
    getProcessCmdline({
      timestamp,
      filter_cluster_name,
      filter_pid,
      process_name: filter_process_name,
    });
  }, [filter_cluster_name, filter_pid, filter_process_name, getProcessCmdline, timestamp]);

  return (
    <div className="process-detail">
      <ProcessDetailInfo
        processName={filter_process_name}
        id={filter_pid}
        ip={filter_host_ip}
        clusterName={filter_cluster_name}
        cmdline={processCmdline}
      />
      <Row gutter={20}>
        {processChartList.map((ProcessChart: any, key) => (
          <Col span={12} key={String(key)}>
            <ProcessChart {...processChartAttrs} />
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default ProcessDetail;
