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

import i18n from 'i18n';
import { Table, Select, Button, Popconfirm, Tooltip, message } from 'antd';
import React from 'react';
import { ColumnProps } from 'core/common/interface';
import { ISSUE_PRIORITY_MAP, ISSUE_TYPE } from 'project/common/components/issue/issue-config';
import IssueState, { issueMainStateMap } from 'project/common/components/issue/issue-state';

import moment from 'moment';
import testCaseStore from 'project/stores/test-case';
import { MemberSelector, Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import routeInfoStore from 'core/stores/route';
import { map } from 'lodash';
import { getIssues } from 'project/services/issue';
import { IssueIcon } from 'project/common/components/issue/issue-icon';
import './related-bugs.scss';
import { goTo } from 'common/utils';
import { useUpdateEffect } from 'react-use';

const { Option } = Select;

interface IProps {
  relationID: number;
}

const RelatedBugs = ({ relationID }: IProps) => {
  const { projectId } = routeInfoStore.useStore((s) => s.params);
  const issueBugs = testCaseStore.useStore((s) => s.issueBugs);
  const [{ showFilterBug, bugList, creator, priority, title, selectBug }, updater, update] = useUpdate({
    title: '',
    showFilterBug: false,
    bugList: [],
    creator: [],
    priority: undefined,
    selectBug: undefined,
  });
  const { removeRelation, getDetailRelations, addRelation } = testCaseStore.effects;
  const loadData = React.useCallback(
    (query: Record<string, any>) => {
      getIssues({ projectID: +projectId, type: 'BUG', pageSize: 20, ...query } as ISSUE.IssueListQuery).then((res) => {
        if (res.success) {
          updater.bugList(res.data.list || []);
        }
      });
    },
    [projectId, updater],
  );
  useUpdateEffect(() => {
    if (showFilterBug) {
      loadData({ creator, priority, title });
    } else {
      update({
        selectBug: undefined,
        priority: undefined,
        creator: undefined,
      });
    }
  }, [creator, loadData, priority, title, showFilterBug]);
  const disRelated = (ids: number[]) => {
    removeRelation({
      issueTestCaseRelationIDs: ids,
      id: relationID,
    }).then(() => {
      getDetailRelations({ id: relationID });
    });
  };

  const handleSelectCreator = (v: any) => {
    update({
      creator: v,
      selectBug: undefined,
    });
  };

  const handleSelectPriority = (v: any) => {
    update({
      priority: v,
      selectBug: undefined,
    });
  };

  const handleAddRelation = () => {
    if (!selectBug) {
      return;
    }
    if ((issueBugs || []).some((item) => item.issueID === selectBug)) {
      message.info(i18n.t('dop:This bug has been associated with the current use case. Please select again.'));
      return;
    }
    addRelation({ issueIDs: [selectBug], id: relationID }).then(() => {
      getDetailRelations({ id: relationID });
    });
  };

  const relatedBugsColumns: Array<ColumnProps<TEST_CASE.RelatedBug>> = [
    {
      title: i18n.t('default:title'),
      dataIndex: 'title',
      render: (text: string, record) => {
        return (
          <Tooltip placement="topLeft" title={text}>
            <div
              className="flex items-center justify-start text-link nowrap truncate"
              onClick={() => {
                goToBugs(record);
              }}
            >
              <IssueIcon type={ISSUE_TYPE.BUG} />
              {text}
            </div>
          </Tooltip>
        );
      },
    },
    {
      title: i18n.t('default:status'),
      dataIndex: 'state',
      width: 96,
      render: (stateName: string, record: any) => {
        const curState = issueMainStateMap.BUG[record.stateBelong];
        return stateName ? <IssueState stateName={stateName} status={curState?.status} /> : undefined;
      },
    },
    {
      title: i18n.t('dop:priority'),
      dataIndex: 'priority',
      width: 96,
      render: (text: string) =>
        ISSUE_PRIORITY_MAP[text] ? (
          <>
            {ISSUE_PRIORITY_MAP[text].icon}
            {ISSUE_PRIORITY_MAP[text].label}
          </>
        ) : (
          ''
        ),
    },
    {
      title: i18n.t('default:create time'),
      dataIndex: 'createdAt',
      width: 200,
      render: (text: string) => (text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : ''),
    },
    {
      title: null,
      dataIndex: 'operate',
      width: 120,
      fixed: 'right',
      render: (text, { issueRelationID }: TEST_CASE.RelatedBug) => {
        return [
          <div className="table-operations">
            <Popconfirm
              title={i18n.t('confirm remove relation?')}
              onConfirm={() => {
                disRelated([issueRelationID]);
              }}
            >
              <span className="table-operations-btn">{i18n.t('remove relation')}</span>
            </Popconfirm>
          </div>,
        ];
      },
    },
  ];

  const goToBugs = (record: TEST_CASE.RelatedBug) => {
    const { issueID, iterationID } = record;
    goTo(goTo.pages.bugList, {
      projectId,
      jumpOut: true,
      query: {
        iterationID,
        id: issueID,
        type: 'BUG',
      },
    });
  };
  return (
    <div className="related-bugs">
      <div className="mb-2 flex justify-between items-center">
        {i18n.t('related bugs')}
        <Button
          onClick={() => {
            updater.showFilterBug(true);
          }}
        >
          {i18n.t('related bugs')}
        </Button>
      </div>
      {showFilterBug ? (
        <div className="flex justify-between items-center flex-1 filter-select-wrap mb-3">
          <div className="flex justify-between items-center flex-1">
            <MemberSelector
              mode="multiple"
              className="filter-select"
              scopeType="project"
              scopeId={projectId}
              placeholder={i18n.t('please select {name}', { name: i18n.t('creator') })}
              onChange={handleSelectCreator}
              allowClear
            />
            <Select
              className="filter-select"
              onChange={handleSelectPriority}
              placeholder={i18n.t('dop:priority')}
              allowClear
            >
              {map(ISSUE_PRIORITY_MAP, (item) => {
                return (
                  <Option key={item.value} value={item.value}>
                    <CustomIcon className="priority-icon mr-2" type={item.icon} color />
                    {item.label}
                  </Option>
                );
              })}
            </Select>
            <Select
              className="filter-select"
              value={selectBug}
              onSelect={(v) => updater.selectBug(v)}
              onSearch={(q) => updater.title(q)}
              showSearch
              filterOption={false}
              placeholder={i18n.t('name')}
            >
              {map(bugList, (item) => {
                return (
                  <Option key={item.id} value={item.id} title={`${item.id}-${item.title}`}>
                    <div className="flex items-center justify-start nowrap">
                      <IssueIcon type={ISSUE_TYPE.BUG} />
                      {item.id}-{item.title}
                    </div>
                  </Option>
                );
              })}
            </Select>
          </div>
          <Button disabled={!selectBug} className="ml-3" onClick={handleAddRelation}>
            {i18n.t('default:ok')}
          </Button>
          <Button
            type="link"
            onClick={() => {
              updater.showFilterBug(false);
            }}
          >
            {i18n.t('default:cancel')}
          </Button>
        </div>
      ) : null}
      <Table columns={relatedBugsColumns} dataSource={issueBugs || []} pagination={false} scroll={{ x: 660 }} />
    </div>
  );
};

export default RelatedBugs;
