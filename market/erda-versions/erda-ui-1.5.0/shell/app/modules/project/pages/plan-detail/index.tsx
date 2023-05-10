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

import { debounce, get } from 'lodash';
import React from 'react';
import i18n from 'i18n';
import { Button, Tabs, Input, Spin } from 'antd';
import { DropdownSelect, Icon as CustomIcon, ErdaIcon } from 'common';
import { SplitPage } from 'app/layout/common';
import TestEnvDrawer from 'project/pages/test-manage/case/test-env-drawer';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import testCaseStore from 'project/stores/test-case';
import testPlanStore from 'project/stores/test-plan';
import { StatusToggle } from './status-toggle';
import CaseImport from './case-import-button';
import ChangeResult from './change-result';
import BatchProcessing from './batch-processing';
import { CaseTree, CaseTable } from '../test-manage/components';
import CaseFilterDrawer from '../test-manage/case/filter-drawer';
import { getColumns } from './columns';
import Report from './report';
import PlanModal, { IPlanModal } from '../test-plan/plan-modal';
import { updateSearch, loopTimer } from 'common/utils';
import TestRecords from './test-record/test-record';
import { getExecuteRecords as getExecuteRecordsService } from '../../services/test-plan';
import { EnvSelect, BaseInfo } from 'project/pages/plan-detail/common-comp';
import CaseDrawer from 'project/pages/test-manage/case/case-drawer';
import './index.scss';
import moment from 'moment';

const { TabPane } = Tabs;

let clear = () => {};
const TestPlanDetail = () => {
  const planItemDetail = testPlanStore.useStore((s) => s.planItemDetail);
  const params = routeInfoStore.useStore((s) => s.params);
  const { getCases, getCaseDetail } = testCaseStore.effects;

  const {
    getTestPlanItemDetail,
    updatePlanStatus,
    addSingleCaseToTestPlan,
    executeCaseApi,
    getExecuteRecords,
    cancelBuild: cancelPipeline,
    deleteTestPlans,
  } = testPlanStore.effects;
  const [isFetching] = useLoading(testPlanStore, ['getTestPlanItemDetail']);
  React.useEffect(() => {
    if (params.testPlanId) {
      getTestPlanItemDetail(+params.testPlanId);
    }
  }, [getTestPlanItemDetail, params.testPlanId]);
  const [importVisible, setImportVisible] = React.useState(false);
  const [drawerVisible, setDrawerVisible] = React.useState(false);
  const [record, setRecord] = React.useState({} as TEST_CASE.CaseTableRecord); // 因为case详情里没有状态，只能从table里取
  const [recordList, setRecordList] = React.useState([] as TEST_CASE.TestCaseItem[]); // 因为case详情里没有状态，只能从table里取
  const [modalProp, setModalProp] = React.useState({ visible: false, mode: '' } as IPlanModal);
  const [enhanceFilterVisible, setEnhanceFilterVisible] = React.useState(false);
  const [loadingRecords, setLoadingRecords] = React.useState(false);
  const [firstRecord, setFirstRecord] = React.useState({} as TEST_PLAN.Pipeline);
  const caseRef = React.useRef(null as any);

  // TODO: ws推送推不过来，先用轮询处理，ws可用后就不需要了
  const loop = React.useCallback(
    (clearTimer?: any) => {
      // 执行后记录翻到第一页并更新状态
      getExecuteRecords({ pageNo: 1, pageSize: 15 }).then((res: any) => {
        if (res.total) {
          setFirstRecord(res.list[0]);
          if (res.list[0].status !== 'Running') {
            clearTimer && clearTimer();
            getCases();
          }
        } else {
          clearTimer && clearTimer();
        }
      });
    },
    [getCases, getExecuteRecords],
  );
  const handleExecute = (data: Omit<TEST_PLAN.CaseApi, 'testPlanID'>) => {
    executeCaseApi(data).then(() => loop());
    clear = loopTimer(loop, 3000);
  };

  React.useEffect(() => {
    setLoadingRecords(true);
    // 从pipeline记录第一条拿执行状态，需要改为后端把状态放在计划详情里，为了不影响记录那里的分页，这里单独调用
    getExecuteRecordsService({ projectId: +params.projectId, pageNo: 1, pageSize: 1, testPlanId: params.testPlanId })
      .then((res: any) => {
        if (res.success) {
          const first: TEST_PLAN.Pipeline = get(res, 'data.pipelines[0]');
          setFirstRecord(first);
          // 如果再进来时还是Running状态，继续定时拉取
          if (first && first.status === 'Running') {
            clear = loopTimer(loop, 3000);
          }
        }
      })
      .finally(() => {
        setLoadingRecords(false);
      });
    return () => {
      clear();
    };
  }, [loop, params.projectId, params.testPlanId]);

  const [reportKey, setReportKey] = React.useState(0);
  const changeTabKey = (actKey: string) => {
    if (actKey === 'report') {
      setReportKey(reportKey + 1);
    }
  };
  const closeEnhanceFilter = () => {
    setEnhanceFilterVisible(false);
  };

  const onSearch = (q: any) => {
    const { timestampSecUpdatedAtBegin, timestampSecUpdatedAtEnd } = q;
    const currentQ = { ...q };
    if (timestampSecUpdatedAtBegin) {
      currentQ.timestampSecUpdatedAtBegin = moment(+timestampSecUpdatedAtBegin)
        .startOf('day')
        .format('X');
    }
    if (timestampSecUpdatedAtEnd) {
      currentQ.timestampSecUpdatedAtEnd = moment(+timestampSecUpdatedAtEnd)
        .endOf('day')
        .format('X');
    }
    updateSearch(currentQ);
    getCases(currentQ);
    closeEnhanceFilter();
  };
  const debouncedSearch = debounce(onSearch, 1000);

  const updateModalProp = (a: object) => {
    setModalProp({
      ...modalProp,
      ...a,
    });
  };

  if (!planItemDetail.id) {
    return null;
  }

  const clickMenu = ({ key }: any) => {
    switch (key) {
      case 'add':
        setDrawerVisible(true);
        break;
      case 'import':
        setImportVisible(true);
        break;
      default:
        break;
    }
  };

  const handleAutoRelatedNewCase = async (_data: TEST_CASE.CaseBody, isEdit: boolean, testCaseID: number) => {
    if (!isEdit) {
      return addSingleCaseToTestPlan({ testCaseIDs: [testCaseID] });
    }
  };

  const afterDeleteTestCase = (ids: number[]) => {
    caseRef.current && caseRef.current.removeMenu(ids);
  };

  return (
    <div className="test-plan-detail">
      <div className="top-button-group flex items-center">
        <StatusToggle
          isPlan
          state={planItemDetail.status}
          onChange={(status: TEST_PLAN.PlanStatus) => updatePlanStatus({ id: +params.testPlanId, status })}
        />
        {firstRecord && firstRecord.status === 'Running' ? (
          <Button loading={loadingRecords} onClick={() => cancelPipeline({ pipelineID: firstRecord.id })}>
            {i18n.t('dop:cancel interface test')}
          </Button>
        ) : (
          <EnvSelect execute={handleExecute}>
            <Button loading={loadingRecords} className="items-center flex">
              {i18n.t('dop:start interface test')} <ErdaIcon type="down" size="16" />
            </Button>
          </EnvSelect>
        )}
        <Button
          type="primary"
          ghost
          onClick={() =>
            updateModalProp({
              visible: true,
              mode: 'edit',
              afterSubmit: () => getTestPlanItemDetail(+params.testPlanId),
            })
          }
        >
          {i18n.t('edit')}
        </Button>
        <Button type="primary" onClick={() => updateModalProp({ visible: true, mode: 'copy' })}>
          {i18n.t('copy')}
        </Button>
        <PlanModal
          testPlanId={params.testPlanId}
          {...modalProp}
          onCancel={() => {
            updateModalProp({ visible: false, mode: '' });
            setReportKey(reportKey + 1);
          }}
        />
      </div>
      <Spin spinning={isFetching}>
        <BaseInfo />
      </Spin>
      <Tabs className="test-detail-tabs" type="card" onChange={changeTabKey}>
        <TabPane tab={i18n.t('dop:test case')} key="case">
          <SplitPage className="full-tab-content">
            <SplitPage.Left fixedSplit className="case-tree-container">
              <CaseTree
                ref={(e) => {
                  caseRef.current = e;
                }}
                needBreadcrumb
                mode="testPlan"
                testPlanID={+params.testPlanId}
                customActions={[{ key: 'delete', name: i18n.t('delete'), onclick: deleteTestPlans }]}
              />
            </SplitPage.Left>
            <SplitPage.Right>
              <div className="flex justify-between items-center mb-3 mt-3">
                <div className="ml-3-group">
                  <DropdownSelect
                    menuList={[
                      { name: i18n.t('dop:new'), key: 'add' },
                      { name: i18n.t('dop:reference'), key: 'import' },
                    ]}
                    buttonText={i18n.t('dop:add use case')}
                    btnProps={{
                      type: 'primary',
                      ghost: true,
                    }}
                    onClickMenu={clickMenu}
                  />
                  <CaseImport visible={importVisible} onCancel={() => setImportVisible(false)} />
                  <ChangeResult />

                  <BatchProcessing afterDelete={afterDeleteTestCase} />
                </div>
                <div className="mr-3-group">
                  <Input
                    style={{ width: '160px' }}
                    placeholder={i18n.t('dop:search for')}
                    onChange={(e) => debouncedSearch({ query: e.target.value })}
                    prefix={<ErdaIcon type="search1" className="mr-1 mt-0.5" size="14" />}
                  />
                  <Button onClick={() => setEnhanceFilterVisible(true)}>
                    <CustomIcon type="filter" />
                  </Button>
                  <CaseFilterDrawer visible={enhanceFilterVisible} onSearch={onSearch} onClose={closeEnhanceFilter} />
                </div>
              </div>
              <CaseTable
                testPlanId={+params.testPlanId}
                columns={getColumns({ afterDelete: afterDeleteTestCase })}
                scope="testPlan"
                onClickRow={(row: TEST_CASE.CaseTableRecord, dataSource?: TEST_CASE.CaseTableRecord[]) => {
                  if (row.testCaseID) {
                    setRecord(row);
                    const testSetID = row?.parent?.testSetID;
                    const currentTestSet = dataSource?.filter((item) => item.testSetID === testSetID);
                    if (currentTestSet && currentTestSet.length) {
                      setRecordList(currentTestSet[0].children as TEST_CASE.TestCaseItem[]);
                    }
                    getCaseDetail({ id: row.id, scope: 'testPlan' });
                    setDrawerVisible(true);
                  }
                }}
              />
              <TestEnvDrawer testType="manual" />
              <CaseDrawer
                visible={drawerVisible}
                scope="testPlan"
                caseList={recordList}
                onClose={() => {
                  setDrawerVisible(false);
                }}
                afterSave={handleAutoRelatedNewCase}
                afterClose={(saved) => {
                  saved && getCases();
                }}
              />
            </SplitPage.Right>
          </SplitPage>
        </TabPane>
        <TabPane tab={i18n.t('dop:test report')} key="report">
          <Report key={reportKey} />
        </TabPane>
        <TabPane tab={i18n.t('dop:interface test record')} key="pipeline">
          <div className="tab-content">
            <TestRecords />
          </div>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default TestPlanDetail;
