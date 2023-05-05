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
import { Popover, Button, Badge, Table, Drawer } from 'antd';
import { isEmpty, map, get } from 'lodash';
import { Icon as CustomIcon } from 'common';
import { useLoading } from 'core/stores/loading';
import { ColumnProps } from 'core/common/interface';
import { ciStatusMap } from './config';
import { useEffectOnce } from 'react-use';
import moment from 'moment';
import autoTestStore from 'project/stores/auto-test-case';
import i18n from 'i18n';
import { insertWhen } from 'common/utils';
import { scopeConfig } from '../scope-config';
import './record-list.scss';

interface IProps {
  curPipelineDetail?: PIPELINE.IPipeline;
  onSelectPipeline: (p: PIPELINE.IPipeline) => void;
  scope: string;
}

const RecordList = React.forwardRef((props: IProps, ref: any) => {
  const { curPipelineDetail, onSelectPipeline, scope } = props;
  const [pipelineRecordList, pipelineRecordPaging, caseDetail, pipelineReportList] = autoTestStore.useStore((s) => [
    s.pipelineRecordList,
    s.pipelineRecordPaging,
    s.caseDetail,
    s.pipelineReportList,
  ]);
  const { getPipelineRecordList, clearPipelineRecord, getPipelineReport, clearPipelineReport } = autoTestStore;
  const [loading] = useLoading(autoTestStore, ['getPipelineRecordList', 'getPipelineReport']);
  const { total, pageNo, pageSize } = pipelineRecordPaging;
  const [isDrawerVisible, setIsDrawerVisible] = React.useState(false);
  const [isPopoverVisible, setIsPopoverVisible] = React.useState(false);

  const scopeConfigData = scopeConfig[scope];
  useEffectOnce(() => {
    getList({}, true);
    return () => {
      clearPipelineRecord();
      clearPipelineReport();
    };
  });

  React.useEffect(() => {
    if (ref) {
      // eslint-disable-next-line no-param-reassign
      ref.current = {
        reload: () => getList({ pageNo }),
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageNo, ref]);

  const hidePopover = () => {
    setIsPopoverVisible(false);
  };

  const handlePopoverVisible = (visible: boolean) => {
    setIsPopoverVisible(visible);
  };

  const setRowClassName = (record: any) => {
    return record.id !== get(curPipelineDetail, 'id') ? 'pipeline-record-list' : 'selected-row font-medium';
  };

  const getList = (q: any = {}, forceUpdate = false) => {
    getPipelineRecordList({
      ymlNames: caseDetail.inode,
      sources: scopeConfigData.runPipelineSource,
      pageNo: 1,
      ...q,
    }).then((res: any) => {
      if (forceUpdate || isEmpty(curPipelineDetail)) {
        onSelectPipeline(get(res, 'list[0]'));
      }
    });
  };

  const renderRecordList = () => {
    if (isEmpty(pipelineRecordList)) {
      return <p>{i18n.t('common:no data')}</p>;
    }
    const columns: Array<ColumnProps<any>> = [
      {
        title: i18n.t('version'),
        dataIndex: 'runIndex',
        width: 80,
        align: 'center',
        render: (runIndex: any) => (
          <span className="run-index">
            {/* {record.triggerMode === 'cron' && <CustomIcon type="clock" />} */}
            {runIndex}
          </span>
        ),
      },
      {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
        align: 'center',
      },
      {
        title: i18n.t('status'),
        dataIndex: 'status',
        width: 100,
        render: (status: string) => (
          <span>
            <span className="nowrap">{ciStatusMap[status].text}</span>
            <Badge className="ml-1" status={ciStatusMap[status].status} />
          </span>
        ),
      },
      {
        title: i18n.t('dop:executor'),
        dataIndex: ['extra', 'runUser', 'name'],
        width: 100,
        align: 'center',
      },
      {
        title: i18n.t('trigger time'),
        dataIndex: 'timeCreated',
        width: 200,
        render: (timeCreated: number) => moment(new Date(timeCreated)).format('YYYY-MM-DD HH:mm:ss'),
      },
      ...insertWhen(scope === 'autoTest', [
        {
          title: i18n.t('dop:report'),
          key: 'report',
          width: 200,
          render: (_: any, record: any) => (
            <div className="table-operations">
              <span className="table-operations-btn" onClick={() => viewReport(record.id)}>
                {i18n.t('common:view')}
              </span>
            </div>
          ),
        },
      ]),
    ];

    const startIndex = total - pageSize * (pageNo - 1);
    const dataSource = map(pipelineRecordList, (item, index) => {
      return { ...item, runIndex: '#'.concat(String(startIndex - index)) };
    });

    return (
      <div className="pipeline-record-list">
        <div
          className="pipeline-refresh-btn"
          onClick={() => {
            getList({ pageNo: 1 });
          }}
        >
          <CustomIcon type="shuaxin" />
          {i18n.t('fetch latest records')}
        </div>
        <Table
          rowKey="runIndex"
          className="pipeline-record-list-table"
          columns={columns}
          loading={loading}
          dataSource={dataSource}
          scroll={{ y: 240 }}
          rowClassName={setRowClassName}
          pagination={{ pageSize, total, current: pageNo, onChange: (_no) => getList({ pageNo: _no }) }}
          onRow={(p) => ({
            onClick: () => {
              onSelectPipeline(p);
            },
          })}
        />
      </div>
    );
  };

  const viewReport = (id: number) => {
    hidePopover();
    getPipelineReport({ pipelineID: id });
    setIsDrawerVisible(true);
  };

  const reportColumns = [
    {
      title: i18n.t('dop:total number of interfaces'),
      dataIndex: 'autoTestNum',
      align: 'center',
      render: (_: any, record: any) => get(record, 'meta.autoTestNum', '-'),
    },
    {
      title: i18n.t('dop:number of case'),
      dataIndex: 'autoTestCaseNum',
      align: 'center',
      render: (_: any, record: any) => get(record, 'meta.autoTestCaseNum', '-'),
    },
    {
      title: i18n.t('dop:interface execution rate'),
      dataIndex: 'apiExecutionRate',
      align: 'center',
      render: (_: any, record: any) => {
        const { autoTestNum, autoTestNotExecNum } = get(record, 'meta');
        const executionRate = (autoTestNum - autoTestNotExecNum) / autoTestNum;
        return Number(executionRate) === executionRate ? `${(executionRate * 100).toFixed(2)}%` : '-';
      },
    },
    {
      title: i18n.t('dop:interface pass rate'),
      dataIndex: 'apiPassRate',
      align: 'center',
      render: (_: any, record: any) => {
        const { autoTestNum, autoTestSuccessNum } = get(record, 'meta');
        const passRate = autoTestSuccessNum / autoTestNum;
        return Number(passRate) === passRate ? `${(passRate * 100).toFixed(2)}%` : '-';
      },
    },
  ];

  return (
    <>
      <Popover
        placement="bottomRight"
        title={i18n.t('dop:execute records')}
        content={renderRecordList()}
        trigger="hover"
        visible={isPopoverVisible}
        onVisibleChange={handlePopoverVisible}
        arrowPointAtCenter
      >
        <Button>{i18n.t('dop:execute records')}</Button>
      </Popover>
      <Drawer
        width="50%"
        visible={isDrawerVisible}
        title={i18n.t('dop:automatic test report')}
        onClose={() => setIsDrawerVisible(false)}
        destroyOnClose
        className="site-message-drawer"
      >
        <Table
          dataSource={pipelineReportList}
          columns={reportColumns}
          loading={loading}
          rowKey="id"
          scroll={{ x: '100%' }}
        />
      </Drawer>
    </>
  );
});

export default RecordList;
