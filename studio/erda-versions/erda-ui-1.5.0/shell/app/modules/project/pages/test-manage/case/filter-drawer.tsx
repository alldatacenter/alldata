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

import { FilterGroup, MemberSelector } from 'common';
import { getTimeRanges } from 'common/utils';
import React from 'react';
// import LabelSelect from 'project/common/components/issue/label-select';
import { priorityList } from '../constants';
import { DatePicker } from 'antd';
import moment from 'moment';
import { CaseStatus } from '../../plan-detail/status-toggle';
import { isEmpty } from 'lodash';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';

interface IProps {
  visible: boolean;
  onSearch: (query: object) => any;
  onClose: () => any;
}

export default ({ visible, onSearch, onClose }: IProps) => {
  const [query, params] = routeInfoStore.useStore((s) => [s.query, s.params]);
  const filterList: any[] = [
    {
      type: 'select',
      label: i18n.t('dop:priority'),
      name: 'priority',
      value: query.priority,
      options: priorityList.map((v) => ({ name: v, value: v })),
      placeholder: i18n.t('dop:unlimited'),
      mode: 'multiple',
    },
    // 3.38 未做
    // {
    //   type: 'custom',
    //   name: 'labelIds',
    //   label: i18n.t('label'),
    //   Comp: <LabelSelect type="test" value={query.labelIds} fullWidth />,
    // },
    {
      type: 'custom',
      name: 'updaterID',
      label: i18n.t('dop:updater'),
      value: query.updaterID,
      Comp: (
        <MemberSelector
          mode="multiple"
          scopeType="project"
          scopeId={params.projectId}
          selectNoneInOption
          selectSelfInOption
        />
      ),
    },
    {
      type: 'custom',
      name: 'updateTime',
      label: i18n.t('update time'),
      value: query.timestampSecUpdatedAtBegin
        ? [moment(query.timestampSecUpdatedAtBegin * 1000), moment(query.timestampSecUpdatedAtEnd * 1000)]
        : [],
      Comp: <DatePicker.RangePicker ranges={getTimeRanges()} />,
    },
  ];

  // 如果路径上有testPlanId，认为在计划页面
  if (params.testPlanId) {
    filterList.push(
      {
        type: 'custom',
        name: 'executorID',
        label: i18n.t('dop:executor'),
        value: query.executorID,
        Comp: (
          <MemberSelector
            mode="multiple"
            scopeType="project"
            scopeId={params.projectId}
            selectNoneInOption
            selectSelfInOption
          />
        ),
      },
      {
        type: 'select',
        name: 'execStatus',
        label: i18n.t('dop:execute result'),
        placeholder: i18n.t('dop:unlimited'),
        mode: 'multiple',
        options: [
          { value: CaseStatus.INIT, name: i18n.t('dop:not performed') },
          { value: CaseStatus.PASSED, name: i18n.t('passed') },
          { value: CaseStatus.FAIL, name: i18n.t('dop:not passed') },
          { value: CaseStatus.BLOCK, name: i18n.t('dop:blocking') },
        ],
        value: query.execStatus,
      },
    );
  }

  const handleSearch = (values: any) => {
    const { priority, updateTime, updaterID, executorID, labelIds, execStatus } = values;
    const searchQuery: any = {
      priority,
      updaterID,
      executorID,
      execStatus,
    };
    if (!isEmpty(updateTime)) {
      const [startAt, endAt] = updateTime;
      searchQuery.timestampSecUpdatedAtBegin = startAt.valueOf();
      searchQuery.timestampSecUpdatedAtEnd = endAt.valueOf();
    } else {
      searchQuery.timestampSecUpdatedAtBegin = undefined;
      searchQuery.timestampSecUpdatedAtEnd = undefined;
    }
    if (!isEmpty(labelIds)) {
      searchQuery.labelIds = labelIds.map((label: any) => label.id);
    } else {
      searchQuery.labelIds = undefined;
    }
    onSearch({ ...searchQuery, pageNo: 1 }); // 查询条件变化，重置pageNo
  };

  return <FilterGroup.Drawer visible={visible} list={filterList} onSearch={handleSearch} onClose={onClose} />;
};
