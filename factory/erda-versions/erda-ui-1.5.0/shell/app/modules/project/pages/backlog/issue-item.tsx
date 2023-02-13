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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { ISSUE_PRIORITY_MAP, ISSUE_OPTION } from 'project/common/components/issue/issue-config';
import { Ellipsis, Icon as CustomIcon, MemberSelector, Avatar } from 'common';
import { useDrag } from 'react-dnd';
import { WithAuth, usePerm, isAssignee, isCreator, getAuth } from 'user/common';
import { isPromise } from 'common/utils';
import { get, map } from 'lodash';
import i18n from 'i18n';
import { IssueIcon, getIssueTypeOption } from 'project/common/components/issue/issue-icon';
import { Menu, Dropdown, Modal, message } from 'antd';
import { Form } from 'dop/pages/form-editor/index';
import './issue-item.scss';
import routeInfoStore from 'core/stores/route';
import userStore from 'app/user/stores';
import { useUserMap } from 'core/stores/userMap';
import { useMount } from 'react-use';
import issueStore from 'project/stores/issues';
import issueFieldStore from 'org/stores/issue-field';
import IssueState from 'project/common/components/issue/issue-state';
import orgStore from 'app/org-home/stores/org';
import { getFieldsByIssue } from 'project/services/issue';
import { templateMap } from 'project/common/issue-config';
import issueWorkflowStore from 'project/stores/issue-workflow';

export enum BACKLOG_ISSUE_TYPE {
  iterationIssue = 'iterationIssue',
  undoneIssue = 'undoneIssue',
}

interface IIssueProps {
  data: ISSUE.Issue;
  issueType: BACKLOG_ISSUE_TYPE;
  onClickIssue?: (data: ISSUE.Issue) => void;
  onDragDelete?: () => void;
  onDelete?: (data: ISSUE.Issue) => void;
  deleteConfirmText?: string | React.ReactNode | ((name: string) => string | React.ReactNode);
  deleteText: string | React.ReactNode;
  undraggable?: boolean;
  showStatus?: boolean;
}

const noop = () => Promise.resolve();
export const IssueItem = (props: IIssueProps) => {
  const {
    data,
    onDelete,
    onDragDelete,
    issueType,
    onClickIssue = noop,
    deleteText,
    deleteConfirmText,
    undraggable = false,
    showStatus = false,
  } = props;
  const workflowStateList = issueWorkflowStore.useStore((s) => s.workflowStateList);
  const { title, type, priority, creator, assignee, id } = data;
  const curPriority = ISSUE_PRIORITY_MAP[priority] || {};
  const userMap = useUserMap();
  const projectPerm = usePerm((s) => s.project);
  const permObj =
    type === ISSUE_OPTION.REQUIREMENT
      ? projectPerm.requirement
      : type === ISSUE_OPTION.TASK
      ? projectPerm.task
      : projectPerm.bug;
  const checkRole = [isCreator(creator), isAssignee(assignee)];
  const deleteAuth = getAuth(permObj.delete, checkRole);
  const editAuth = getAuth(permObj.edit, checkRole);

  const [_, drag] = useDrag({
    item: { type: issueType, data },
    canDrag: () => {
      return editAuth && !undraggable; // 拖拽权限等同修改权限
    },
    end: (__, monitor) => {
      const dropRes = monitor.getDropResult();
      // 获取drop中返回的promsie，确保修改事项结束后在拉取新的列表
      if (dropRes && dropRes.res && isPromise(dropRes.res)) {
        dropRes.res.then(() => {
          onDragDelete?.();
        });
      }
    },
  });
  const name = `${title}`;
  const user = get(userMap, data.assignee, {});
  const username = user.nick || user.name;

  const confirmDelete = (currentData: any) => {
    Modal.confirm({
      title: deleteConfirmText
        ? typeof deleteConfirmText === 'function'
          ? deleteConfirmText(name)
          : deleteConfirmText
        : `${i18n.t('common:confirm deletion')}(${name})`,
      onOk() {
        onDelete && onDelete(currentData);
      },
    });
  };

  const state = showStatus ? workflowStateList.find((item) => item.stateID === data.state) : null;
  return (
    <div
      className={`backlog-issue-item hover-active-bg cursor-pointer ${
        !undraggable && editAuth ? 'draggable' : 'cursor-default'
      }`}
      ref={drag}
      onClick={() => onClickIssue(data)}
    >
      <div className="issue-info h-full">
        <div className="backlog-item-content">
          <div className="w-20">{id}</div>
          <IssueIcon type={type as ISSUE_OPTION} />
          <Ellipsis className="font-bold" title={name} />
        </div>
        <div className="backlog-item-info text-sub flex items-center flex-wrap justify-end">
          <div className="backlog-item-priority mw-60">{curPriority.iconLabel}</div>
          {state ? (
            <div className="mr-4">
              <IssueState stateID={state.stateID} />
            </div>
          ) : null}
          <div className="w80 mr-2">
            <Avatar showName name={username} size={20} wrapClassName="w-full" />
          </div>
          {onDelete ? (
            <div>
              <Dropdown
                trigger={['click']}
                overlayClassName="contractive-filter-item-dropdown"
                overlay={
                  <Menu>
                    <WithAuth pass={deleteAuth}>
                      <Menu.Item
                        className="text-danger"
                        onClick={(e) => {
                          e.domEvent.stopPropagation();
                          confirmDelete(data);
                        }}
                      >
                        {deleteText || i18n.t('delete')}
                      </Menu.Item>
                    </WithAuth>
                  </Menu>
                }
                placement="bottomLeft"
              >
                <span className="op-icon" onClick={(e) => e.stopPropagation()}>
                  <CustomIcon className="hover-active" type="gd" />
                </span>
              </Dropdown>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};

interface IIssueFormProps {
  className?: string;
  defaultIssueType?: 'TASK' | 'REQUIREMENT' | 'BUG';
  onCancel: () => void;
  onOk: (data: ISSUE.BacklogIssueCreateBody) => Promise<any>;
  typeDisabled?: boolean;
}

const placeholderMap = {
  REQUIREMENT: i18n.t('{name} title', { name: i18n.t('requirement') }),
  TASK: i18n.t('{name} title', { name: i18n.t('task') }),
  BUG: i18n.t('{name} title', { name: i18n.t('bug') }),
};

export const IssueForm = (props: IIssueFormProps) => {
  const { onCancel = noop, onOk = noop, className = '', defaultIssueType, typeDisabled } = props;
  const [chosenType, setChosenType] = React.useState(defaultIssueType || ISSUE_OPTION.REQUIREMENT);
  const formRef = React.useRef(null as any);
  const { projectId } = routeInfoStore.getState((s) => s.params);
  const [shouldAutoFocus, setShouldAutoFocus] = React.useState(true);
  const { addFieldsToIssue } = issueStore.effects;
  const [bugStageList, taskTypeList] = issueFieldStore.useStore((s) => [s.bugStageList, s.taskTypeList]);
  const orgID = orgStore.useStore((s) => s.currentOrg.id);

  useMount(() => {
    setShouldAutoFocus(false);
  });

  const onAdd = () => {
    const curForm = formRef && formRef.current;
    if (curForm) {
      curForm.onSubmit((val: ISSUE.BacklogIssueCreateBody) => {
        if (val.title) {
          const data = {
            ...val,
            content: templateMap[val.type] || '',
            // some special fields for different type
            taskType: taskTypeList?.length ? taskTypeList[0].value : '',
            bugStage: bugStageList?.length ? bugStageList[0].value : '',
            owner: '',
          };
          onOk(data).then((newIssueID) => {
            curForm.reset('title');
            getFieldsByIssue({
              issueID: newIssueID,
              orgID,
              propertyIssueType: data.type as ISSUE_FIELD.IIssueType,
            }).then((res) => {
              const fieldList = res.data?.property;
              if (fieldList) {
                const property = map(fieldList, (item) => {
                  if (item && item.required) {
                    if (['Select', 'MultiSelect'].includes(item.propertyType)) {
                      return {
                        ...item,
                        values: [item.enumeratedValues?.[0].id as number],
                      };
                    }
                  }
                  return { ...item };
                });
                addFieldsToIssue({ property, issueID: newIssueID, orgID, projectID: +projectId });
              }
            });
          });
        } else {
          message.warn(i18n.t('please enter {name}', { name: i18n.t('title') }));
        }
      });
    }
  };

  const fields = [
    {
      label: '',
      component: 'select',
      key: 'type',
      required: true,
      disabled: typeDisabled,
      initialValue: chosenType,
      componentProps: {
        size: 'small',
        className: 'backlog-issue-add-type align-bottom',
        optionLabelProp: 'data-icon',
        dropdownMatchSelectWidth: false,
        style: { width: 50 },
        onChange: (val: ISSUE_OPTION) => {
          setChosenType(val);
        },
      },
      dataSource: {
        type: 'static',
        static: () => getIssueTypeOption(),
      },
      type: 'select',
    },
    {
      label: '',
      component: 'input',
      key: 'title',
      wrapperProps: {
        className: 'backlog-issue-add-title-box',
      },
      componentProps: {
        placeholder: `${placeholderMap[chosenType]}, ${i18n.t('enter key to save quickly')}`,
        maxLength: 255,
        size: 'small',
        autoFocus: shouldAutoFocus,
        className: 'backlog-issue-add-title',
        onPressEnter: onAdd,
      },
      type: 'input',
      requiredCheck: (v: string) => [v !== undefined && v !== '', ''],
    },
    {
      label: '',
      name: 'assignee',
      key: 'assignee',
      type: 'custom',
      componentProps: {
        size: 'small',
        className: 'mt-1 backlog-issue-add-assignee',
      },
      initialValue: userStore.getState((s) => s.loginUser.id),
      getComp: () => {
        return (
          <MemberSelector
            dropdownMatchSelectWidth={false}
            scopeType="project"
            scopeId={String(projectId)}
            allowClear={false}
            size="small"
          />
        );
      },
    },
  ];

  React.useEffect(() => {
    const curForm = formRef && formRef.current;
    if (curForm) {
      curForm.setFields(fields);
    }
  }, [chosenType, shouldAutoFocus]);

  return (
    <div className={`${className} backlog-issue-form flex justify-between items-center`}>
      <div className={'backlog-issue-form-box h-full'}>
        <Form
          fields={fields}
          formRef={formRef}
          formProps={{ layout: 'inline', className: 'backlog-issue-add items-center' }}
        />
      </div>
      <div className="table-operations ml-2">
        <span className="table-operations-btn" onClick={onAdd}>
          {i18n.t('save')}
        </span>
        <span className="table-operations-btn" onClick={onCancel}>
          {i18n.t('cancel')}
        </span>
      </div>
    </div>
  );
};
