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
import { indexOf, isEmpty, map, sortBy, unset } from 'lodash';
import { useDrop } from 'react-dnd';
import { Button, Pagination, Popconfirm, Spin, Tooltip } from 'antd';
import { ContractiveFilter, ErdaIcon, Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { useLoading } from 'core/stores/loading';
import { usePerm, WithAuth } from 'user/common';
import iterationStore from 'project/stores/iteration';
import labelStore from 'project/stores/label';
import { useEffectOnce } from 'react-use';
import issueStore from 'project/stores/issues';
import { BACKLOG_ISSUE_TYPE, IssueForm, IssueItem } from './issue-item';
import EditIssueDrawer, { CloseDrawerParam } from 'project/common/components/issue/edit-issue-drawer';
import { mergeSearch, qs, setApiWithOrg, updateSearch } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import { ISSUE_OPTION, ISSUE_PRIORITY_MAP, ISSUE_TYPE_MAP } from 'project/common/components/issue/issue-config';
import { ISSUE_TYPE_ICON_MAP } from 'project/common/components/issue/issue-icon';
import backlog_db_svg from 'app/images/backlog-db.svg';
import i18n from 'i18n';
import issueWorkflowStore from 'project/stores/issue-workflow';
import './backlog.scss';

const Backlog = () => {
  const [backlogIssues, backlogIssuesPaging] = iterationStore.useStore((s) => [s.backlogIssues, s.backlogIssuesPaging]);
  const { pageSize, total, pageNo } = backlogIssuesPaging;
  const { getBacklogIssues, createIssue } = iterationStore.effects;
  const { clearBacklogIssues } = iterationStore.reducers;
  const { deleteIssue, updateIssue } = issueStore.effects;
  const labelList = labelStore.useStore((s) => s.list);
  const { getLabels } = labelStore.effects;
  const [loading] = useLoading(iterationStore, ['getBacklogIssues']);
  const [{ projectId }, { id: queryId, issueType: queryType, ...restQuery }] = routeInfoStore.getState((s) => [
    s.params,
    s.query,
  ]);
  const workflowStateList = issueWorkflowStore.useStore((s) => s.workflowStateList);

  const allStateIds = React.useRef<number[]>([]);

  const [{ isAdding, curIssueDetail, drawerVisible, filterState }, updater, update] = useUpdate({
    isAdding: false,
    curIssueDetail: {} as ISSUE.Issue,
    drawerVisible: false,
    filterState: { ...restQuery } as Obj,
  });

  const stateCollection: Array<{ label: string | React.ReactNode; children: Array<{ label: string; value: string }> }> =
    React.useMemo(() => {
      const stateIds: number[] = [];
      const initState: string[] = [];
      const typeArr = ['REQUIREMENT', 'TASK', 'BUG'];
      const collection =
        workflowStateList?.reduce((acc, current) => {
          const { issueType, stateName, stateID, stateBelong } = current;
          if (!typeArr.includes(issueType)) {
            return acc;
          }
          if (!['CLOSED', 'DONE'].includes(stateBelong)) {
            initState.push(`${stateID}`);
          }
          if (acc[issueType]) {
            acc[issueType].push({ label: stateName, value: `${stateID}` });
          } else {
            acc[issueType] = [{ label: stateName, value: `${stateID}` }];
          }
          if (!allStateIds.current.length) {
            stateIds.push(stateID);
          }
          return acc;
        }, {}) || {};
      if (!allStateIds.current.length) {
        allStateIds.current = stateIds;
      }

      updater.filterState((prev: Obj) => ({ state: initState, ...prev }));
      const options = sortBy(
        map(collection, (stateArray, issueType) => {
          const label = ISSUE_TYPE_ICON_MAP[issueType];
          return {
            label: (
              <span>
                {label.icon}
                {label.name}
              </span>
            ),
            labelValue: label.value,
            children: stateArray,
          };
        }),
        (item) => indexOf(typeArr, item.labelValue),
      );
      return options;
    }, [workflowStateList]);

  React.useEffect(() => {
    getList();
  }, [stateCollection]);

  useEffectOnce(() => {
    if (!labelList.length) {
      getLabels({ type: 'issue' });
    }
    if (queryId && queryType) {
      update({
        curIssueDetail: { id: queryId, type: queryType } as ISSUE.Issue,
        drawerVisible: true,
      });
    }
    return () => {
      clearBacklogIssues();
    };
  });

  const addAuth = usePerm((s) => s.project.requirement.create.pass); // 目前迭代、任务、缺陷添加权限都一致

  const onIssueDrop = (val: ISSUE.IssueType) => {
    return updateIssue({ ...val, iterationID: -1 }).then(() => {
      getList({ pageNo: 1 });
    });
  };

  const [{ isOver }, drop] = useDrop({
    accept: BACKLOG_ISSUE_TYPE.iterationIssue,
    drop: (item: any) => ({ res: onIssueDrop(item.data) }), // drop需要返回一个Obj，如果直接返回Promise是无效的
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });

  React.useEffect(() => {
    updateSearch(filterState);
  }, [filterState]);

  const getList = React.useCallback(
    (filters: Obj = {}, goTop = true) => {
      goTop && (listRef.current.scrollTop = 0);
      const submitValues = { ...filterState, ...filters };
      const { finishedAtStartEnd, createdAtStartEnd, state } = submitValues;
      if (finishedAtStartEnd) {
        unset(submitValues, 'finishedAtStartEnd');
        submitValues.startFinishedAt = finishedAtStartEnd[0];
        submitValues.endFinishedAt = finishedAtStartEnd[1];
      }
      if (createdAtStartEnd) {
        unset(submitValues, 'createdAtStartEnd');
        submitValues.startCreatedAt = createdAtStartEnd[0];
        submitValues.endCreatedAt = createdAtStartEnd[1];
      }
      return getBacklogIssues({ ...submitValues, state: (state as number[])?.length ? state : allStateIds.current });
    },
    [filterState, getBacklogIssues],
  );

  const onDelete = (val: ISSUE.Issue) => {
    deleteIssue(val.id).then(() => {
      getList({ pageNo: 1 });
    });
  };

  const onAdd = () => updater.isAdding(true);

  const onClickIssue = (val: ISSUE.Issue) => {
    update({
      drawerVisible: true,
      curIssueDetail: val,
    });
  };

  const closeDrawer = ({ hasEdited, isCreate, isDelete }: CloseDrawerParam) => {
    update({
      drawerVisible: false,
      curIssueDetail: {} as ISSUE.Issue,
    });
    if (hasEdited || isCreate || isDelete) {
      getList();
    }
  };
  const conditionsFilter = React.useMemo(
    () => [
      {
        type: 'select',
        key: 'type',
        label: i18n.t('type'),
        placeholder: i18n.t('filter by {name}', { name: i18n.t('type') }),
        fixed: false,
        emptyText: i18n.t('dop:all'),
        showIndex: 1,
        options: [ISSUE_TYPE_MAP.REQUIREMENT, ISSUE_TYPE_MAP.TASK, ISSUE_TYPE_MAP.BUG],
      },
      {
        key: 'priority',
        label: i18n.t('dop:priority'),
        emptyText: i18n.t('dop:all'),
        fixed: false,
        showIndex: 2,
        type: 'select' as const,
        placeholder: i18n.t('filter by {name}', { name: i18n.t('dop:priority') }),
        options: map(ISSUE_PRIORITY_MAP),
      },
      {
        key: 'state',
        label: i18n.t('dop:state'),
        type: 'select' as const,
        options: stateCollection,
        allowClear: false,
        fixed: false,
        showIndex: 3,
      },
      {
        key: 'label',
        label: i18n.t('label'),
        emptyText: i18n.t('dop:all'),
        fixed: false,
        haveFilter: true,
        type: 'select' as const,
        placeholder: i18n.t('filter by {name}', { name: i18n.t('label') }),
        options: map(labelList, (item) => ({ label: item.name, value: `${item.id}` })),
      },
      {
        key: 'assignee',
        label: i18n.t('dop:assignee'),
        fixed: false,
        type: 'memberSelector',
        customProps: {
          mode: 'multiple',
        },
      },
      {
        key: 'creator',
        label: i18n.t('creator'),
        fixed: false,
        type: 'memberSelector',
        customProps: {
          mode: 'multiple',
        },
      },
      {
        key: 'finishedAtStartEnd',
        label: i18n.t('deadline'),
        fixed: false,
        type: 'dateRange',
      },
      {
        key: 'createdAtStartEnd',
        label: i18n.t('dop:creation date'),
        fixed: false,
        type: 'dateRange',
      },
      {
        key: 'title',
        emptyText: i18n.t('dop:all'),
        fixed: true,
        placeholder: i18n.t('dop:please enter title or ID'),
        type: 'input' as const,
      },
    ],
    [labelList, stateCollection],
  );

  const onFilter = (val: Obj) => {
    updater.filterState(val);
    getList({ ...val, pageNo: 1 });
  };

  const curType = isEmpty(filterState.type) ? map(ISSUE_OPTION) : filterState.type;
  const downloadUrl = setApiWithOrg(
    `/api/issues/actions/export-excel?${qs.stringify(
      { ...filterState, iterationID: -1, projectID: projectId, type: curType },
      { arrayFormat: 'none' },
    )}`,
  );

  const handleChangePage = (curPage: number, curSize?: number) => {
    getList({
      pageNo: curPage,
      pageSize: curSize,
    });
  };

  const listRef = React.useRef(null as any);
  const isHide = !!listRef.current && listRef.current.scrollTop;
  return (
    <div className="backlog-issues flex flex-col justify-center h-full" ref={drop}>
      <div className="backlog-issues-title flex justify-between items-center mb-2">
        <div className="flex items-center">
          <span className="font-bold text-base mr-2">{i18n.t('dop:backlog')}</span>
          <Tooltip
            placement="right"
            title={i18n.t('dop:this is mainly for items that have not been scheduled for a specific iteration')}
          >
            <ErdaIcon type="help" className="cursor-pointer mr-2" />
          </Tooltip>
          <span className="text-desc">{i18n.t('{num} {type}', { num: total, type: i18n.t('dop:issue') })}</span>
        </div>
        <div>
          <WithAuth pass={addAuth}>
            <Button className="mr-2" type="primary" onClick={onAdd}>
              <CustomIcon type="cir-add" className="mr-1" />
              {i18n.t('add {name}', { name: i18n.t('dop:issue') })}
            </Button>
          </WithAuth>

          <Popconfirm title={i18n.t('dop:confirm to export')} onConfirm={() => window.open(downloadUrl)}>
            <Button className="ml-2 px-2">
              <CustomIcon type="daochu" />
            </Button>
          </Popconfirm>
        </div>
      </div>
      <div className={'backlog-filter'}>
        <ContractiveFilter delay={1000} conditions={conditionsFilter} initValue={filterState} onChange={onFilter} />
      </div>
      <div className={`backlog-issues-content spin-full-height ${isOver ? 'drag-over' : ''}`} ref={drop}>
        <Spin spinning={!isHide && loading}>
          {isEmpty(backlogIssues) && !isAdding && <EmptyBacklog addAuth={addAuth} onAdd={onAdd} />}
          <div className="list-container">
            {
              <div className="backlog-issues-list" ref={listRef}>
                {isAdding ? (
                  <IssueForm
                    key="add"
                    className="backlog-issue-item hover-active-bg"
                    onCancel={() => updater.isAdding(false)}
                    onOk={(val: ISSUE.BacklogIssueCreateBody) => {
                      return createIssue({ ...val }).finally(() => {
                        updater.isAdding(true);
                        getList();
                      });
                    }}
                  />
                ) : null}
                {map(backlogIssues, (item) => (
                  <IssueItem
                    data={item}
                    key={item.id}
                    onDelete={onDelete}
                    issueType={BACKLOG_ISSUE_TYPE.undoneIssue}
                    onDragDelete={() => {
                      getList({ pageNo: 1 });
                    }}
                    onClickIssue={onClickIssue}
                  />
                ))}
              </div>
            }
            <Pagination
              className="flex items-center flex-wrap justify-end pt-2"
              showSizeChanger
              total={total}
              current={pageNo}
              pageSize={pageSize}
              onChange={handleChangePage}
            />
          </div>
        </Spin>
      </div>

      {drawerVisible ? (
        <EditIssueDrawer
          iterationID={-1}
          id={curIssueDetail.id}
          shareLink={`${location.href.split('?')[0]}?${mergeSearch(
            { id: curIssueDetail.id, issueType: curIssueDetail.type },
            true,
          )}`}
          issueType={curIssueDetail.type}
          visible={drawerVisible}
          closeDrawer={closeDrawer}
        />
      ) : null}
    </div>
  );
};

const EmptyBacklog = ({ onAdd, addAuth }: { onAdd: () => void; addAuth: boolean }) => (
  <div className="backlog-issues-empty-holder">
    <img src={backlog_db_svg} className="mb-3" />
    <div className="text-2xl font-bold my-2">{i18n.t('dop:backlog')}</div>
    <div className="desc">
      {i18n.t('dop:add-todo-issue-tip1')}
      <WithAuth pass={addAuth}>
        <Button className="px-2" size="small" type="primary" ghost onClick={onAdd}>
          <CustomIcon type="cir-add" className="mr-1" />
          {i18n.t('add {name}', { name: i18n.t('dop:issue') })}
        </Button>
      </WithAuth>
      {i18n.t('dop:Create a new issue, and you can drag it to the iteration on the right and set its priority.')}
    </div>
  </div>
);

export default Backlog;
