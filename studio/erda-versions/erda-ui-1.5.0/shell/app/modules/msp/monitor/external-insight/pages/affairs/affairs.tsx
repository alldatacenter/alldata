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
import { Row, Col, Drawer } from 'antd';
import { SimpleLog } from 'common';
import { useSwitch } from 'common/use-hooks';
import routeInfoStore from 'core/stores/route';
import AffairsMap from './config/chartMap';
import TopTabRight from 'external-insight/common/components/tab-right';
import CommonPanel from 'trace-insight/pages/trace-querier/trace-common-panel';
import PureTraceDetail from 'trace-insight/pages/trace-querier/trace-detail-new';
import monitorCommonStore from 'common/stores/monitorCommon';
import traceQuerierStore from 'trace-insight/stores/trace-querier';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';

const Affairs = () => {
  const { getTraceDetailContent, getSpanDetailContent } = traceQuerierStore.effects;
  const spanDetailContent = traceQuerierStore.useStore((s) => s.spanDetailContent);
  const [isTraceDetailContentFetching] = useLoading(traceQuerierStore, ['getTraceDetailContent']);
  const chosenSortItem = monitorCommonStore.useStore((s) => s.chosenSortItem);

  const [terminusKey, hostName] = routeInfoStore.useStore((s) => [s.params.terminusKey, s.params.hostName]);
  const filterQuery = {
    filter_http_url: decodeURIComponent(hostName),
    filter_source_terminus_key: terminusKey,
  };
  const [logVisible, openLog, closeLog] = useSwitch(false);
  const [tracingVisible, tracingOn, tracingOff] = useSwitch(false);
  const [traceRecords, setTraceRecords] = React.useState({});
  const [logQuery, setQuery] = React.useState({});
  const [applicationId, setApplicationId] = React.useState({});

  const chartQuery = chosenSortItem
    ? { ...filterQuery, filter_source_service_name: chosenSortItem }
    : { ...filterQuery };

  const fetchTraceContent = ({ requestId }: any) => {
    tracingOn();
    getTraceDetailContent({ traceId: requestId, needReturn: true }).then((content: any) => {
      setTraceRecords(content);
    });
  };

  const viewLog = ({ requestId, applicationId: appId }: any) => {
    setQuery(requestId);
    setApplicationId(appId);
    openLog();
  };

  return (
    <div>
      <TopTabRight />
      <Row gutter={20}>
        <Col span={8}>
          <div className="monitor-sort-panel">
            <AffairsMap.sortTab />
            <AffairsMap.sortList query={filterQuery} />
          </div>
        </Col>
        <Col span={16}>
          <AffairsMap.responseTimes query={chartQuery} />
          <AffairsMap.throughput query={chartQuery} />
          <AffairsMap.httpError query={filterQuery} />
          <AffairsMap.slowTrack query={filterQuery} viewLog={viewLog} fetchTraceContent={fetchTraceContent} />
          <AffairsMap.errorTrack query={filterQuery} viewLog={viewLog} fetchTraceContent={fetchTraceContent} />
        </Col>
      </Row>
      <Drawer destroyOnClose title={i18n.t('runtime:monitor log')} width="80%" visible={logVisible} onClose={closeLog}>
        <SimpleLog requestId={logQuery} applicationId={applicationId} />
      </Drawer>
      <Drawer
        destroyOnClose
        title={i18n.t('msp:transactions')}
        width="80%"
        visible={tracingVisible}
        onClose={tracingOff}
      >
        <CommonPanel
          title={
            <div className="flex justify-between items-center">
              <h3 className="trace-common-panel-title font-medium">{i18n.t('msp:link information')}</h3>
            </div>
          }
          className="trace-status-list-ct"
        >
          <PureTraceDetail
            spanDetailContent={spanDetailContent}
            traceDetailContent={traceRecords}
            isTraceDetailContentFetching={isTraceDetailContentFetching}
            getSpanDetailContent={getSpanDetailContent}
          />
        </CommonPanel>
      </Drawer>
    </div>
  );
};

export default Affairs;
