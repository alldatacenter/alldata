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
import React from 'react';
import i18n from 'i18n';
import { Button, Popconfirm, Spin, Table, Alert } from 'antd';
import { useLoading } from 'core/stores/loading';
import testEnvStore from 'project/stores/test-env';
import { TestEnvDetail } from './test-env-detail';
import './test-env.scss';
import { PAGINATION } from 'app/constants';
import routeInfoStore from 'core/stores/route';
import { insertWhen } from 'common/utils';
import { ColumnProps } from 'core/common/interface';
import { scopeMap } from 'project/common/components/pipeline-manage/config';

interface IProps {
  envID: number;
  envType: TEST_ENV.EnvType;
  isSingle: boolean;
  testType: string;
}

const TestEnv = ({ testType = 'manual', envID: _envID, envType: _envType, isSingle }: IProps): JSX.Element => {
  const { projectId } = routeInfoStore.useStore((s) => s.params);
  const routeEnvID = +projectId;

  const envID = _envID || routeEnvID;
  const envType = _envType || 'project';

  const [modalVisible, setModalVisible] = React.useState(false);
  const [editable, setEditable] = React.useState(true);
  const [envList, autoEnvList, active] = testEnvStore.useStore((s) => [s.envList, s.autoEnvList, s.active]);

  const [isEnvLoading, isAutoEnvLoading] = useLoading(testEnvStore, ['getTestEnvList', 'getAutoTestEnvList']);

  const loading = React.useMemo(() => {
    return testType === 'manual' ? isEnvLoading : isAutoEnvLoading;
  }, [isAutoEnvLoading, isEnvLoading, testType]);

  React.useEffect(() => {
    if (testType === 'manual') {
      testEnvStore.getTestEnvList({ envID, envType });
    } else {
      testEnvStore.getAutoTestEnvList({ scopeID: envID, scope: scopeMap.autoTest.scope });
    }
    return () => {
      testEnvStore.clearEnvList();
      testEnvStore.clearAutoTestEnvList();
    };
  }, [envID, envType, testType]);

  const handleOpenDetail = (record?: any, edit?: boolean) => {
    testEnvStore.setActiveEnv(record);
    setEditable(edit || false);
    setModalVisible(true);
  };

  const onDeleteHandle = React.useCallback(
    (record: any) => {
      if (testType === 'manual') {
        testEnvStore.deleteTestEnv(record.id, { envID, envType });
      } else {
        testEnvStore.deleteAutoTestEnv(record.ns, { scope: scopeMap.autoTest.scope, scopeID: envID });
      }
    },
    [envID, envType, testType],
  );

  const columns = React.useMemo(
    () =>
      [
        ...insertWhen(testType === 'manual', [
          {
            title: i18n.t('dop:environment name'),
            dataIndex: 'name',
            width: 300,
          },
          {
            title: i18n.t('dop:environmental domain name'),
            dataIndex: 'domain',
            render: (text: string) => text || '--',
          },
        ]),
        ...insertWhen(testType === 'auto', [
          {
            title: i18n.t('name'),
            dataIndex: 'displayName',
            width: 300,
          },
          {
            title: i18n.t('description'),
            dataIndex: 'desc',
          },
        ]),
        {
          title: i18n.t('operation'),
          key: 'ops',
          width: 180,
          fixed: 'right',
          render: (_text: any, record: TEST_ENV.Item) => (
            <div className="table-operations">
              <span
                className="table-operations-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  handleOpenDetail(record, true);
                }}
              >
                {i18n.t('edit')}
              </span>
              <Popconfirm
                title={i18n.t('dop:confirm to delete?')}
                onConfirm={(e) => {
                  if (e !== undefined) {
                    e.stopPropagation();
                  }
                  onDeleteHandle(record);
                }}
                onCancel={(e) => e && e.stopPropagation()}
              >
                <span className="table-operations-btn" onClick={(e) => e.stopPropagation()}>
                  {i18n.t('delete')}
                </span>
              </Popconfirm>
            </div>
          ),
        },
      ] as Array<ColumnProps<TEST_ENV.Item>>,
    [onDeleteHandle, testType],
  );

  const onRowClick = React.useCallback((record: TEST_ENV.Item | TEST_ENV.IAutoEnvItem) => {
    return {
      onClick: () => {
        handleOpenDetail(record, false);
      },
    };
  }, []);

  return (
    <Spin spinning={loading}>
      {isSingle ? (
        isEmpty(envList) ? (
          <Button type="primary" ghost className="mb-3" onClick={() => handleOpenDetail({}, true)}>
            {i18n.t('dop:add configuration')}
          </Button>
        ) : null
      ) : (
        <div className="top-button-group">
          <Button type="primary" onClick={() => handleOpenDetail({}, true)}>
            {i18n.t('dop:add configuration')}
          </Button>
        </div>
      )}
      <Alert
        className="text-desc mb-2"
        message={
          testType === 'manual'
            ? i18n.t('dop:This parameter is provided to the use case interface of test case in manual test')
            : i18n.t(
                'dop:This parameter is provided to the use case interface of test case in automated interface test',
              )
        }
        type="info"
        showIcon
      />
      <Table
        rowKey={testType === 'manual' ? 'id' : 'ns'}
        columns={columns}
        dataSource={testType === 'manual' ? envList : autoEnvList}
        pagination={{ pageSize: PAGINATION.pageSize }}
        onRow={onRowClick}
        scroll={{ x: 800 }}
      />
      <TestEnvDetail
        envID={envID}
        envType={envType}
        data={active}
        testType={testType}
        visible={modalVisible}
        disabled={!editable}
        onCancel={() => setModalVisible(false)}
      />
    </Spin>
  );
};

export const ManualTestEnv = () => <TestEnv testType="manual" />;
export const AutoTestEnv = () => <TestEnv testType="auto" />;

// export default TestEnv;
