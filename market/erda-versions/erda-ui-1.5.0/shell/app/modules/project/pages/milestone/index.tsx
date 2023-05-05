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

import { ContractiveFilter } from 'common';
import { useUpdate } from 'common/use-hooks';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';
import { map } from 'lodash';
import { Spin, DatePicker, Button } from 'antd';
import React from 'react';
import { ISSUE_TYPE, ISSUE_PRIORITY_MAP } from 'project/common/components/issue/issue-config';
import issueStore from 'project/stores/issues';
import routeInfoStore from 'core/stores/route';
import { mergeSearch, updateSearch } from 'common/utils';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import { usePerm, WithAuth } from 'user/common';
import labelStore from 'project/stores/label';
import './index.scss';
import MilestoneTable from './milestone-table';
import moment from 'moment';
import EditIssueDrawer, { CloseDrawerParam } from 'project/common/components/issue/edit-issue-drawer';

export const Milestone = () => {
  const [{ iterationIDs: iterIds }, { startFinishedAt, ...restQuery }] = routeInfoStore.useStore((s) => [
    s.params,
    s.query,
  ]);
  const labelList = labelStore.useStore((s) => s.list);
  const [loading] = useLoading(issueStore, ['getIssues']);
  const [epicList, epicPaging] = issueStore.useStore((s) => [s.epicList, s.epicPaging]);

  const { getIssues } = issueStore.effects;
  const { clearRequirementList: clearEpic } = issueStore.reducers;

  const [stateMap, updater] = useUpdate({
    iterationIDs: iterIds as unknown as undefined | number[],
    searchTime: (startFinishedAt && moment(Number(startFinishedAt))) || moment(),
    epicDetail: null as null | ISSUE.Epic,
    modalVisible: false,
    filterState: { ...restQuery } as Obj,
  });

  const { filterState } = stateMap;

  const loadData = () => {
    const { searchTime } = stateMap;
    getIssues({
      type: ISSUE_TYPE.EPIC,
      startFinishedAt: searchTime.startOf('year').valueOf(),
      endFinishedAt: searchTime.endOf('year').valueOf(),
      ...filterState,
    });
  };

  useEffectOnce(() => {
    const { searchTime } = stateMap;

    updateSearch({
      startFinishedAt: searchTime.startOf('year').valueOf(),
      endFinishedAt: searchTime.endOf('year').valueOf(),
      ...filterState,
    });
    loadData();

    return () => {
      clearEpic();
    };
  });

  useUpdateEffect(() => {
    !startFinishedAt && updater.searchTime(moment());
    updateSearch(filterState);
    loadData();
  }, [startFinishedAt, filterState]);

  const expandDetail = (epic: ISSUE.Epic) => {
    updater.epicDetail(epic);
    updater.modalVisible(true);
  };

  const conditionsFilter = React.useMemo(
    () => [
      {
        key: 'title',
        label: i18n.t('title'),
        emptyText: i18n.t('dop:all'),
        fixed: true,
        showIndex: 2,
        placeholder: i18n.t('filter by {name}', { name: i18n.t('title') }),
        type: 'input' as const,
      },
      {
        key: 'label',
        label: i18n.t('label'),
        emptyText: i18n.t('dop:all'),
        fixed: false,
        showIndex: 3,
        haveFilter: true,
        type: 'select' as const,
        placeholder: i18n.t('filter by {name}', { name: i18n.t('label') }),
        options: map(labelList, (item) => ({ label: item.name, value: `${item.id}` })),
      },
      {
        key: 'priority',
        label: i18n.t('dop:priority'),
        emptyText: i18n.t('dop:all'),
        fixed: false,
        showIndex: 4,
        type: 'select' as const,
        placeholder: i18n.t('filter by {name}', { name: i18n.t('dop:priority') }),
        options: map(ISSUE_PRIORITY_MAP),
      },
      {
        key: 'assignee',
        label: i18n.t('dop:assignee'),
        emptyText: i18n.t('dop:all'),
        fixed: false,
        showIndex: 5,
        customProps: {
          mode: 'multiple',
          scopeType: 'project',
        },
        type: 'memberSelector' as const,
      },
    ],
    [labelList],
  );

  const epicAuth = usePerm((s) => s.project.epic);

  const closeModal = ({ hasEdited, isCreate, isDelete }: CloseDrawerParam) => {
    updater.modalVisible(false);
    updater.epicDetail(null);
    if (hasEdited || isCreate || isDelete) {
      loadData();
    }
  };

  const onFilter = (val: Obj) => {
    updater.filterState(val);
  };

  const rangeFilter = (y: any) => {
    updater.searchTime(y);
    const time = { startFinishedAt: y.startOf('year').valueOf(), endFinishedAt: y.endOf('year').valueOf() };
    updateSearch({ ...time });
  };
  return (
    <div className="project-milestone">
      <div className="search-container bg-white">
        <DatePicker
          className="milestone-date-picker"
          onPanelChange={rangeFilter}
          mode="year"
          defaultValue={moment()}
          value={stateMap.searchTime}
          format="YYYY"
          allowClear={false}
        />
        <ContractiveFilter delay={1000} conditions={conditionsFilter} initValue={filterState} onChange={onFilter} />
        <WithAuth pass={epicAuth.create.pass} tipProps={{ placement: 'bottom' }}>
          <Button className="top-button-group" type="primary" onClick={() => updater.modalVisible(true)}>
            {i18n.t('dop:create milestone')}
          </Button>
        </WithAuth>
      </div>
      <Spin spinning={loading}>
        <MilestoneTable epic={epicList} onClickItem={expandDetail} paging={epicPaging} reload={loadData} />
      </Spin>
      <EditIssueDrawer
        iterationID={-1}
        id={stateMap.epicDetail?.id}
        issueType={ISSUE_TYPE.EPIC}
        shareLink={
          stateMap.epicDetail
            ? `${location.href.split('?')[0]}?${mergeSearch({ id: stateMap.epicDetail?.id }, true)}`
            : undefined
        }
        visible={stateMap.modalVisible}
        closeDrawer={closeModal}
      />
    </div>
  );
};
