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
import { Col, Row } from 'antd';

import { useLoading } from 'core/stores/loading';
import testPlanStore from 'project/stores/test-plan';
import { BuildHistory } from './build-history';
import { PipelineDetail } from './pipeline-detail';
import { useEffectOnce } from 'react-use';

const TestRecord = () => {
  const [pipelineDetail, executeRecords = [], changeType] = testPlanStore.useStore((s) => [
    s.pipelineDetail,
    s.executeRecords,
    s.changeType,
  ]);
  const [isFetchingDetail] = useLoading(testPlanStore, ['getPipelineDetail']);
  const { getPipelineDetail, getExecuteRecords } = testPlanStore.effects;
  const { clearPipelineDetail, clearExecuteRecords } = testPlanStore.reducers;

  const [activeItem, setActiveItem] = React.useState(executeRecords[0] || {});

  const getExecuteRecordsByPageNo = ({ pageNo }: { pageNo: number }) => {
    getExecuteRecords({ pageNo });
  };

  useEffectOnce(() => {
    getExecuteRecordsByPageNo({ pageNo: 1 });
    return () => {
      clearPipelineDetail();
      clearExecuteRecords();
    };
  });

  React.useEffect(() => {
    if (executeRecords.length) {
      setActiveItem(executeRecords[0]);
    }
  }, [executeRecords]);

  React.useEffect(() => {
    activeItem.id && getPipelineDetail({ pipelineID: activeItem.id });
  }, [activeItem, getPipelineDetail]);

  return (
    <Row type="flex" gutter={20}>
      <Col span={8}>
        <BuildHistory
          activeItem={activeItem}
          onClickRow={(record: any) => getPipelineDetail({ pipelineID: record.id })}
        />
      </Col>
      <Col span={16}>
        <PipelineDetail changeType={changeType} pipelineDetail={pipelineDetail} isFetching={isFetchingDetail} />
      </Col>
    </Row>
  );
};

export default TestRecord;
