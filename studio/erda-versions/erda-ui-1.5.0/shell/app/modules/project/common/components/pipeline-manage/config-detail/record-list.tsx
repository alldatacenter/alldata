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
import { Popover, Button, Table } from 'antd';
import { isEmpty, get, map } from 'lodash';
import { Icon as CustomIcon } from 'common';
import { useLoading } from 'core/stores/loading';
import { ColumnProps } from 'core/common/interface';
import { useEffectOnce } from 'react-use';
import moment from 'moment';
import { useUserMap } from 'core/stores/userMap';
import autoTestStore from 'project/stores/auto-test-case';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import './record-list.scss';

interface IProps {
  caseId?: string;
  curPipelineDetail?: AUTO_TEST.ICaseDetail;
  onSelectPipeline: (p: AUTO_TEST.ICaseDetail | null) => void;
}
const RecordList = React.forwardRef((props: IProps, ref: any) => {
  const { curPipelineDetail, onSelectPipeline, caseId: propsCaseId } = props;
  const query = routeInfoStore.useStore((s) => s.query);
  const curCaseId = propsCaseId || query.caseId;
  const [configDetailRecordList] = autoTestStore.useStore((s) => [s.configDetailRecordList]);
  const { clearConfigDetailRecordList, getConfigDetailRecordList } = autoTestStore;
  const [loading] = useLoading(autoTestStore, ['getPipelineRecordList']);
  useEffectOnce(() => {
    getList();
    return () => {
      clearConfigDetailRecordList();
    };
  });
  React.useEffect(() => {
    if (ref) {
      // eslint-disable-next-line no-param-reassign
      ref.current = {
        reload: () => getList(),
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ref]);
  const setRowClassName = (record: any) => {
    return get(record, 'meta.historyID') === get(curPipelineDetail, 'meta.historyID')
      ? 'selected-row font-medium'
      : 'pipeline-record-list';
  };
  const getList = () => {
    curCaseId && getConfigDetailRecordList(curCaseId);
  };
  const renderRecordList = () => {
    const userMap = useUserMap();
    if (isEmpty(configDetailRecordList)) {
      return <p>{i18n.t('common:no data')}</p>;
    }
    const columns: Array<ColumnProps<any>> = [
      {
        title: i18n.t('version'),
        dataIndex: 'runIndex',
        width: 80,
        align: 'center',
        render: (runIndex: any) => <span className="run-index">{runIndex}</span>,
      },
      {
        title: i18n.t('dop:updater'),
        dataIndex: 'updaterID',
        width: 100,
        align: 'center',
        render: (updaterID: any) => {
          const curUser = userMap[updaterID];
          return <span className="run-index">{curUser ? curUser.nick || curUser.name : updaterID || '-'}</span>;
        },
      },
      {
        title: i18n.t('default:update time'),
        dataIndex: 'updatedAt',
        width: 200,
        render: (updatedAt: number) => moment(updatedAt).format('YYYY-MM-DD HH:mm:ss'),
      },
    ];
    const startIndex = configDetailRecordList.length;
    const dataSource = map(configDetailRecordList, (item, index) => {
      return { ...item, runIndex: '#'.concat(String(startIndex - index)) };
    });
    return (
      <div className="pipeline-file-record-list">
        <div
          className="pipeline-refresh-btn"
          onClick={() => {
            onSelectPipeline(null);
          }}
        >
          <CustomIcon type="shuaxin" />
          {i18n.t('fetch latest records')}
        </div>
        <Table
          rowKey="updatedAt"
          className="pipeline-record-list-table"
          columns={columns}
          loading={loading}
          dataSource={dataSource}
          scroll={{ y: 240 }}
          rowClassName={setRowClassName}
          onRow={(p) => ({
            onClick: (e: any) => {
              e.stopPropagation();
              onSelectPipeline(p);
            },
          })}
        />
      </div>
    );
  };
  return (
    <Popover
      placement="bottomRight"
      title={i18n.t('dop:modify record')}
      content={renderRecordList()}
      arrowPointAtCenter
    >
      <Button>{i18n.t('dop:modify record')}</Button>
    </Popover>
  );
});
export default RecordList;
