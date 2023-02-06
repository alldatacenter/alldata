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

import { MemberSelector, Title, IF } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Button, Select, Table, Popconfirm, Tooltip, Empty } from 'antd';
import React, { useImperativeHandle } from 'react';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import issueStore from 'project/stores/issues';
import { useUpdateEffect } from 'react-use';
import { goTo } from 'common/utils';
import { debounce, map, filter, find } from 'lodash';
import iterationStore from 'project/stores/iteration';
import { Link } from 'react-router-dom';
import { getIssues as getIssuesService } from 'project/services/issue';
import { getIssueTypeOption, IssueIcon } from 'project/common/components/issue/issue-icon';
import { ISSUE_OPTION, ISSUE_TYPE, ISSUE_PRIORITY_MAP } from 'project/common/components/issue/issue-config';
import { WithAuth, usePerm, getAuth, isAssignee, isCreator } from 'user/common';
import moment from 'moment';
import { IssueItem, IssueForm, BACKLOG_ISSUE_TYPE } from 'project/pages/backlog/issue-item';
import './issue-relation.scss';
import IterationSelect from './iteration-select';
import IssueState from 'project/common/components/issue/issue-state';
import { ColumnProps } from 'core/common/interface';

interface IProps {
  issueDetail: ISSUE.IssueType;
  iterationID: number | undefined;
  onRelationChange?: () => void;
  type: 'inclusion' | 'connection';
}
type IDefaultIssueType = 'BUG' | 'TASK' | 'REQUIREMENT';

const { Option } = Select;

// 打开任务详情时，关联事项默认选中bug，打开缺陷详情或者需求详情时，关联事项默认选中task
// 如果是包含关系时，需求默认选中任务，任务默认选中需求
const initTypeMap = {
  connection: {
    TASK: 'BUG',
    REQUIREMENT: 'TASK',
    BUG: 'TASK',
  },
  inclusion: {
    TASK: 'REQUIREMENT',
    REQUIREMENT: 'TASK',
  },
};

const getAddTextMap = (relationType: string, issueType: string) => {
  if (relationType === 'connection') {
    return [i18n.t('dop:create and relate to the issue'), i18n.t('dop:related to existing issues')];
  } else if (relationType === 'inclusion') {
    if (issueType === ISSUE_TYPE.REQUIREMENT) {
      return [i18n.t('dop:create and include the task'), i18n.t('dop:include existing tasks')];
    }
  }
  return [];
};

export const IssueRelation = React.forwardRef((props: IProps, ref: any) => {
  const { issueDetail, iterationID, onRelationChange, type: relationType } = props;

  const [relatingList, setRelatingList] = React.useState([] as ISSUE.IssueType[]);
  const [relatedList, setRelatedList] = React.useState([] as ISSUE.IssueType[]);
  const [activeButtonType, setActiveButtonType] = React.useState('');
  const addIssueRelationRef = React.useRef({});

  const [{ projectId }, { type: routeIssueType }] = routeInfoStore.getState((s) => [s.params, s.query]);
  const issueType = issueDetail?.type || routeIssueType;
  const defaultIssueType = initTypeMap[relationType][issueType];
  const { getIssueRelation, addIssueRelation, deleteIssueRelation } = issueStore.effects;

  const getList = React.useCallback(() => {
    issueDetail?.id &&
      getIssueRelation({ id: issueDetail.id, type: relationType }).then((res) => {
        setRelatingList(res[0] || []);
        setRelatedList(res[1] || []);
      });
  }, [getIssueRelation, issueDetail, relationType]);

  useImperativeHandle(ref, () => ({ getList }), [getList]);

  const curIterationID = React.useMemo(() => {
    return issueDetail?.iterationID || iterationID;
  }, [issueDetail?.iterationID, iterationID]);

  React.useEffect(() => {
    getList();
  }, [getList]);

  const authObj = usePerm((s) => s.project.task);

  const updateRecord = (record: ISSUE.Task, key: string, val: any) => {
    issueStore.effects.updateIssue({ ...record, [key]: val }).finally(() => {
      getIssueRelation({ id: issueDetail.id, type: relationType });
    });
  };
  const getColumns = (beRelated = false): Array<ColumnProps<ISSUE.IssueType>> => [
    {
      title: i18n.t('{name} title', { name: i18n.t('dop:issue') }),
      dataIndex: 'title',
      render: (v: string, record: ISSUE.IssueType) => {
        const { type, id, iterationID: _iterationID } = record;
        const url =
          type === ISSUE_TYPE.TICKET
            ? goTo.resolve.ticketDetail({ projectId, issueId: id })
            : _iterationID === -1
            ? goTo.resolve.backlog({ projectId, issueId: id, issueType: type })
            : goTo.resolve.issueDetail({
                projectId,
                issueType: type.toLowerCase(),
                issueId: id,
                iterationId: _iterationID,
              });
        return (
          <Tooltip title={`${v}`}>
            <Link to={url} target="_blank" className="flex items-center justify-start  w-full">
              <IssueIcon type={record.type as any} />
              <span className="flex-1 nowrap">{`${v}`}</span>
            </Link>
          </Tooltip>
        );
      },
    },
    {
      title: i18n.t('status'),
      dataIndex: 'state',
      width: 96,
      render: (v: number, record: any) => {
        const currentState = find(record?.issueButton, (item) => item.stateID === v);
        return currentState ? <IssueState stateID={currentState.stateID} /> : undefined;
      },
    },
    {
      title: i18n.t('dop:priority'),
      dataIndex: 'priority',
      width: 80,
      render: (v: string) => (v ? ISSUE_PRIORITY_MAP[v]?.iconLabel : null),
    },
    {
      title: i18n.t('dop:assignee'),
      dataIndex: 'assignee',
      width: 240,
      render: (userId: string, record: ISSUE.Task) => {
        const checkRole = [isCreator(record.creator), isAssignee(record.assignee)];
        const editAuth = getAuth(authObj.edit, checkRole);
        return (
          <WithAuth pass={editAuth}>
            <MemberSelector
              scopeType="project"
              scopeId={projectId}
              allowClear={false}
              disabled={!editAuth}
              value={userId}
              dropdownMatchSelectWidth={false}
              onChange={(val) => {
                updateRecord(record, 'assignee', val);
              }}
            />
          </WithAuth>
        );
      },
    },
    {
      title: i18n.t('create time'),
      dataIndex: 'createdAt',
      width: 176,
      render: (v: string) => moment(v).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: null,
      dataIndex: 'operate',
      render: (_, record: ISSUE.IssueType) => {
        return [
          <WithAuth pass={authObj.edit.pass} key="remove-relation">
            <Popconfirm
              title={`${i18n.t('confirm remove relation?')}`}
              placement="bottom"
              onConfirm={() => onDelete(record, beRelated)}
            >
              <span className="fake-link">{i18n.t('dop:disassociate')}</span>
            </Popconfirm>
          </WithAuth>,
        ];
      },
      width: authObj.edit.pass ? 80 : 0,
    },
  ];

  const addRelation = (val: number) => {
    addIssueRelation({ relatedIssues: val, id: issueDetail.id, projectId: +projectId, type: relationType }).then(() => {
      onRelationChange && onRelationChange();
      getList();
      addIssueRelationRef.current.getIssueList?.();
    });
  };

  const onDelete = (val: ISSUE.IssueType, beRelated = false) => {
    const payload = beRelated
      ? { id: val.id, relatedIssueID: issueDetail.id, type: relationType }
      : { id: issueDetail.id, relatedIssueID: val.id, type: relationType };
    deleteIssueRelation(payload).then(() => {
      onRelationChange && onRelationChange();
      getList();
      addIssueRelationRef.current.getIssueList?.();
    });
  };
  const createAuth: boolean = usePerm((s) => s.project[issueType?.toLowerCase()]?.create.pass);
  if (!issueDetail) return null;
  return (
    <div className="issue-relation">
      <div>
        {issueDetail.type === ISSUE_TYPE.TICKET ? null : (
          // (
          //   <TransformToIssue issue={issue as ISSUE.Ticket} onSaveRelation={addRelation} />
          // )
          <>
            <div>
              <WithAuth pass={createAuth}>
                <Button
                  type={activeButtonType === 'create' ? 'primary' : 'default'}
                  onClick={() => setActiveButtonType('create')}
                >
                  {getAddTextMap(relationType, issueType)[0]}
                </Button>
              </WithAuth>
              <WithAuth pass={authObj.edit.pass}>
                <Button
                  type={activeButtonType === 'exist' ? 'primary' : 'default'}
                  onClick={() => setActiveButtonType('exist')}
                  className="ml-3"
                >
                  {getAddTextMap(relationType, issueType)[1]}
                </Button>
              </WithAuth>
            </div>
            <IF check={activeButtonType === 'create'}>
              <AddNewIssue
                onSaveRelation={addRelation}
                onCancel={() => setActiveButtonType('')}
                iterationID={curIterationID}
                defaultIssueType={defaultIssueType}
                typeDisabled={relationType === 'inclusion'}
              />
            </IF>
            <IF check={activeButtonType === 'exist'}>
              <AddIssueRelation
                ref={addIssueRelationRef}
                editAuth
                onSave={addRelation}
                onCancel={() => setActiveButtonType('')}
                projectId={projectId}
                iterationID={curIterationID}
                currentIssue={issueDetail}
                defaultIssueType={defaultIssueType}
                typeDisabled={relationType === 'inclusion'}
                relationType={relationType}
              />
            </IF>
          </>
        )}
      </div>
      {relationType === 'inclusion' ? (
        <div className="mt-2">
          {relatingList?.map((item) => (
            <IssueItem
              data={item}
              key={item.id}
              onClickIssue={(record) => {
                goTo(goTo.pages.issueDetail, {
                  projectId,
                  issueType: record.type.toLowerCase(),
                  issueId: record.id,
                  iterationId: record.iterationID,
                  jumpOut: true,
                });
              }}
              onDelete={(val) => onDelete(val)}
              deleteConfirmText={(name: string) => i18n.t('dop:Are you sure to disinclude {name}', { name })}
              deleteText={i18n.t('dop:release relationship')}
              issueType={BACKLOG_ISSUE_TYPE.undoneIssue}
              showStatus
              undraggable
            />
          )) || <Empty />}
        </div>
      ) : null}

      {relationType === 'connection' ? (
        <>
          <Title level={2} mt={8} title={i18n.t('dop:related to these issues')} />
          <Table
            columns={getColumns()}
            dataSource={relatingList}
            pagination={false}
            rowKey={(rec: ISSUE.IssueType, i: number | undefined) => `${i}${rec.id}`}
            scroll={{ x: 900 }}
          />
        </>
      ) : null}

      {relationType === 'connection' ? (
        <>
          <Title level={2} mt={16} title={i18n.t('dop:related by these issues')} />
          <Table
            columns={getColumns(true)}
            dataSource={relatedList}
            pagination={false}
            rowKey={(rec: ISSUE.IssueType, i: number | undefined) => `${i}${rec.id}`}
            scroll={{ x: 900 }}
          />
        </>
      ) : null}
    </div>
  );
});

interface IAddProps {
  editAuth: boolean;
  projectId: string;
  currentIssue: ISSUE.IssueType;
  iterationID: number | undefined;
  defaultIssueType: IDefaultIssueType;
  onSave: (v: number) => void;
  onCancel: () => void;
  typeDisabled?: boolean;
  relationType?: string;
}

const initState = {
  issueList: [],
  chosenIssueType: undefined as undefined | string,
  chosenIssue: undefined as undefined | number,
  chosenIterationID: undefined as undefined | number | 'ALL',
};

const AddIssueRelation = React.forwardRef(
  (
    {
      onSave,
      editAuth,
      projectId,
      iterationID,
      currentIssue,
      onCancel,
      defaultIssueType,
      typeDisabled,
      relationType,
    }: IAddProps,
    ref,
  ) => {
    const [{ chosenIssueType, chosenIterationID, issueList, chosenIssue }, updater, update] = useUpdate({
      ...initState,
      chosenIterationID: 'ALL',
      chosenIssueType: defaultIssueType,
    });
    const getIssueList = (extra: Obj = {}) => {
      const type = chosenIssueType || extra.type || map(ISSUE_OPTION);
      const validIterationID = chosenIterationID === 'ALL' ? '' : chosenIterationID;
      getIssuesService({
        projectID: +projectId,
        pageSize: 50,
        pageNo: 1,
        iterationID: validIterationID,
        notIncluded: relationType === 'inclusion',
        ...extra,
        type,
      }).then((res: any) => {
        if (res.success) {
          res.data.list && updater.issueList(res.data.list);
        }
      });
    };

    React.useImperativeHandle(ref, () => ({
      getIssueList,
    }));

    const onClose = () => {
      update({
        chosenIssue: undefined,
        chosenIssueType: defaultIssueType,
        chosenIterationID: iterationID,
        issueList: [],
      });
      onCancel();
    };

    React.useEffect(() => {
      getIssueList({ type: chosenIssueType });
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [chosenIssueType]);

    useUpdateEffect(() => {
      getIssueList();
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [chosenIterationID]);

    const debounceSearch = debounce((val: string) => {
      getIssueList({ title: val });
    }, 1000);

    return (
      <div className="issue-relation-box mt-3">
        <div className="flex items-center justify-start">
          <div className="mr-3">{i18n.t('dop:filter condition')}:</div>
          <IterationSelect
            value={chosenIterationID}
            placeholder={i18n.t('dop:owned iteration')}
            width="174px"
            onChange={(v: number) => {
              update({ chosenIterationID: v, chosenIssue: undefined });
            }}
            disabled={!editAuth}
            addAllOption
          />
          <Select
            className="ml-2"
            style={{ width: '174px' }}
            onChange={(v: any) => update({ chosenIssueType: v, chosenIssue: undefined })}
            value={chosenIssueType}
            allowClear
            disabled={typeDisabled}
            placeholder={i18n.t('dop:issue type')}
          >
            {getIssueTypeOption()}
          </Select>
        </div>
        <div className="flex justify-between items-center">
          <Select
            className="issue-list flex-1 mt-3"
            onSelect={(v: any) => updater.chosenIssue(v)}
            showSearch
            value={chosenIssue}
            filterOption={false}
            onSearch={(v: string) => debounceSearch(v)}
            getPopupContainer={() => document.body}
            disabled={!chosenIterationID}
            placeholder={i18n.t('please select {name}', { name: i18n.t('dop:issue') })}
          >
            {map(
              filter(issueList, (item) => item.id !== currentIssue.id),
              (issue) => {
                return (
                  <Option key={issue.id} value={issue.id}>
                    <div className="flex items-center justify-start">
                      <IssueIcon type={issue.type} />
                      <span className="nowrap">{issue.title}</span>
                    </div>
                  </Option>
                );
              },
            )}
          </Select>

          <Button
            type="primary"
            className="ml-3 mt-3"
            disabled={!chosenIssue}
            onClick={() => {
              if (chosenIssue) {
                onSave(chosenIssue);
                updater.chosenIssue(undefined);
              }
            }}
          >
            {i18n.t('ok')}
          </Button>
          <Button type="link" className="mt-3" onClick={onClose}>
            {i18n.t('cancel')}
          </Button>
        </div>
      </div>
    );
  },
);

interface IAddNewIssueProps {
  iterationID: number | undefined;
  defaultIssueType: IDefaultIssueType;
  onSaveRelation: (v: number) => void;
  onCancel: () => void;
  typeDisabled?: boolean;
}

const AddNewIssue = ({ onSaveRelation, iterationID, onCancel, defaultIssueType, typeDisabled }: IAddNewIssueProps) => {
  const { createIssue } = iterationStore.effects;
  const { projectId } = routeInfoStore.getState((s) => s.params);

  return (
    <IssueForm
      key="add"
      className="mt-3"
      onCancel={onCancel}
      defaultIssueType={defaultIssueType}
      onOk={(val: ISSUE.BacklogIssueCreateBody) => {
        return createIssue({
          // 创建事件
          projectID: +projectId,
          iterationID: +iterationID,
          priority: 'LOW',
          ...val,
        }).then((res: number) => {
          onSaveRelation(res); // 添加关联
          return res;
        });
      }}
      typeDisabled={typeDisabled}
    />
  );
};
