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

import { isEmpty } from 'lodash';
import React, { useState } from 'react';
import { Icon as CustomIcon, ErdaIcon } from 'common';
import i18n from 'i18n';
import { Badge, Button, Drawer, message, Spin, Table } from 'antd';
import { useInterval, useMount } from 'react-use';
import moment from 'moment';
import { useLoading } from 'core/stores/loading';
import testCaseStore from 'project/stores/test-case';
import userStore from 'app/user/stores';
import { useUserMap } from 'core/stores/userMap';
import './import-file.scss';

const ImportExportRecord = ({
  setShowRefresh,
  testSetId,
}: {
  setShowRefresh: (bool: boolean) => void;
  testSetId: number;
}) => {
  const [contentVisible, setContentVisible] = useState(false);
  const userMap = useUserMap();
  const [hasError, setHasError] = useState(false);
  const [isFinished, setIsFinished] = useState(false);
  const loginUser = userStore.useStore((s) => s.loginUser);
  const [list, setList] = useState([] as TEST_CASE.ImportExportRecordItem[]);
  const [counter, setCounter] = useState<TEST_CASE.ImportExportCounter>({});
  const { getImportExportRecords } = testCaseStore.effects;
  const [loading] = useLoading(testCaseStore, ['getImportExportRecords']);

  const getData = (firstTime: boolean) => {
    getImportExportRecords(['import', 'export'])
      .then((result: TEST_CASE.ImportExportResult) => {
        if (result?.list.every((item) => ['success', 'fail'].includes(item.state))) {
          setIsFinished(true);
        }

        if (!isEmpty(result)) {
          if (!firstTime && !contentVisible) {
            let haveJustFinishedJob = false;
            const myProcessingJob: Record<string, boolean> = {};
            list.forEach((item) => {
              if (item.operatorID === loginUser.id && ['processing', 'pending'].includes(item.state)) {
                myProcessingJob[item.id] = true;
              }
            });
            let haveJustSuccessJob = false;
            result?.list.forEach((item) => {
              if (
                item.operatorID === loginUser.id &&
                ['success', 'fail'].includes(item.state) &&
                myProcessingJob[item.id]
              ) {
                haveJustFinishedJob = true;
              }
              // new result state is success and not existing in list cache,  means it's newly import record
              const previousItem = list.find((origin) => origin.id === item.id);
              if (
                item.state === 'success' &&
                item.testSetID === testSetId &&
                (!previousItem || previousItem.state !== 'success')
              ) {
                haveJustSuccessJob = true;
              }
            });
            if (haveJustFinishedJob) {
              message.info(
                i18n.t('dop:The import and export tasks you submitted have status updates, please check the records'),
                4,
              );
            }
            if (haveJustSuccessJob) {
              setShowRefresh(true);
              setList(result?.list);
            }
          }
          setList(result?.list);
          setCounter(result?.counter);
        }
      })
      .catch(() => {
        setHasError(true);
      });
  };

  useMount(() => {
    getData(true);
  });

  useInterval(
    () => {
      getData(false);
    },
    isFinished || hasError ? null : 5000,
  );

  let badgeCount = 0;
  list.forEach((item) => {
    if (['pending', 'processing'].includes(item.state)) {
      badgeCount += 1;
    }
  });

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
    },
    {
      title: i18n.t('type'),
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: TEST_CASE.ImportOrExport) => (type === 'import' ? i18n.t('import') : i18n.t('export')),
    },
    {
      title: i18n.t('operator'),
      dataIndex: 'operatorID',
      key: 'operatorID',
      width: 150,
      render: (id: string) => userMap[id]?.nick || userMap[id]?.name,
    },
    {
      title: i18n.t('time'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (text: string) => moment(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('description'),
      dataIndex: 'description',
      key: 'description',
      width: 240,
    },
    {
      title: i18n.t('status'),
      dataIndex: 'state',
      key: 'state',
      width: 120,
      render: (state: TEST_CASE.ImportOrExportState) => {
        const stateToStatus = {
          success: 'success',
          processing: 'processing',
          fail: 'error',
          pending: 'default',
        };
        const stateToText = {
          success: i18n.t('succeed'),
          processing: i18n.t('processing'),
          fail: i18n.t('failed'),
          pending: i18n.t('queuing'),
        };
        return <Badge status={stateToStatus[state] as any} text={stateToText[state]} />;
      },
    },
    {
      title: i18n.t('result'),
      dataIndex: 'state',
      key: 'result',
      width: 200,
      render: (state: TEST_CASE.ImportOrExportState, record: TEST_CASE.ImportExportRecordItem) => {
        if (['success', 'fail'].includes(state)) {
          return (
            <a className="table-operations-btn flex" download={record.name} href={`/api/files/${record.apiFileUUID}`}>
              <ErdaIcon type="download" className="mr-2" /> {record.name}
            </a>
          );
        }
        return null;
      },
    },
  ];

  const Title = () => {
    return (
      <div>
        <span>{i18n.t('recent import and export records')}</span>
        {!!badgeCount && Object.keys(counter).length > 0 && (
          <span className="align-baseline">
            <CustomIcon type="warning" className="ml-4 font-bold text-sm text-warning" />
            <span className="text-sm text-dark-6 font-normal">
              {i18n.t(
                'dop:import and export tasks are in queue, there are export tasks({export}), import tasks({import}), please wait',
                { export: counter.export || 0, import: counter.import || 0 },
              )}
            </span>
          </span>
        )}
      </div>
    );
  };

  return (
    <>
      <Badge count={badgeCount}>
        <Button type="primary" ghost onClick={() => setContentVisible(true)}>
          {i18n.t('import and export records')}
        </Button>
      </Badge>

      <Drawer width="70%" onClose={() => setContentVisible(false)} visible={contentVisible} title={<Title />}>
        <Spin spinning={loading}>
          <Table rowKey="id" size="small" columns={columns} dataSource={list} scroll={{ x: '100%' }} />
        </Spin>
      </Drawer>
    </>
  );
};

export default ImportExportRecord;
