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
import DiceConfigPage, { useMock } from 'app/config-page';
import { ISSUE_TYPE } from 'project/common/components/issue/issue-config';
import { getUrlQuery } from 'config-page/utils';
import { getAvatarChars, updateSearch, mergeSearch } from 'common/utils';
import { Badge, ErdaIcon, Ellipsis } from 'common';
import { useUserMap } from 'core/stores/userMap';
import { useUpdate, useSwitch } from 'common/use-hooks';
import { IssueIcon } from 'project/common/components/issue/issue-icon';
import routeInfoStore from 'core/stores/route';
import { Avatar, Select } from 'antd';
import moment from 'moment';
import { max } from 'lodash';
import i18n from 'i18n';
import EditIssueDrawer, { CloseDrawerParam } from 'project/common/components/issue/edit-issue-drawer';
import './index.scss';
import { useMount } from 'configForm/form/utils';

interface IBarProps {
  task: CP_GANTT.IGanttData;
  isHover?: boolean;
}
const BarContentRender = (props: IBarProps) => {
  const { task, isHover } = props;
  const barRef = React.useRef<HTMLDivElement>(null);
  const nameRef = React.useRef<HTMLDivElement>(null);
  const [linearPercent, setLinearPercent] = React.useState(100);

  const barWidth = barRef.current?.offsetWidth || 40;
  const nameWidth = nameRef.current?.offsetWidth || 40;
  React.useLayoutEffect(() => {
    setLinearPercent(((barWidth - 8) / nameWidth) * 100);
  }, [barWidth, nameWidth]);

  return (
    <div className={'relative h-full'} ref={barRef}>
      <div className={`flex items-center h-full w-full`}>
        <div style={{ flex: `0 0 ${max([barWidth, nameWidth])}px` }} className={` ml-2 `}>
          <span
            ref={nameRef}
            className="text-xs whitespace-nowrap"
            style={{
              padding: '14px 0',
              WebkitMaskImage: `linear-gradient(90deg, rgba(48,38,71,0.80) ${linearPercent}%, rgba(48,38,71,0.32) ${linearPercent}%)`,
            }}
          >
            {task.name}
          </span>
        </div>
        <div className={` ml-1 whitespace-nowrap text-sub text-xs ${isHover ? 'visible' : 'invisible'}`}>
          {moment(task.start).format('MM-DD')} ~ {moment(task.end).format('MM-DD')}
        </div>
      </div>
    </div>
  );
};

const TaskListHeader = (props: { headerHeight: number; rowWidth: number }) => {
  const { headerHeight, rowWidth } = props;
  // const [value, setValue] = React.useState('issue');
  return (
    <div
      className="erda-task-list-header"
      style={{ height: headerHeight, width: rowWidth, lineHeight: `${headerHeight}px` }}
    >
      {/* remove the demand selector temporarily, and keep the demand tree height */}
      {/* <Select
        className="erda-task-list-header-selector"
        dropdownClassName="py-0"
        suffixIcon={<ErdaIcon size={16} color="currentColor" type="caret-down" />}
        value={value}
        onChange={(v) => setValue(v)}
      >
        <Select.Option value="issue">{i18n.t('dop:display on demand')}</Select.Option>
      </Select>
      </Select> */}
    </div>
  );
};

interface ITreeNodeProps {
  node: CP_GANTT.IGanttData;
  clickNode?: (params: Obj) => void;
}

const TreeNodeRender = (props: ITreeNodeProps) => {
  const { node, clickNode } = props;
  const { extra, name } = node;
  const { status, type, user } = extra || {};
  const userMap = useUserMap();
  const curUser = userMap[user];
  const curUserName = curUser ? curUser.nick || curUser.name : user;

  return (
    <div
      className="flex items-center h-full"
      onClick={(e) => {
        e.stopPropagation();
        clickNode?.(node);
      }}
    >
      {<IssueIcon type={type} size={'16px'} />}
      <div className="truncate flex-1 ml-1">
        <Ellipsis title={name} />
      </div>
      <div className="flex items-center ml-2">
        <Avatar src={curUser?.avatar || undefined} size={20}>
          {getAvatarChars(curUserName || '')}
        </Avatar>
        {status ? (
          <div className="ml-1">
            <Badge showDot={false} text={status.text} status={status?.status || 'default'} />
          </div>
        ) : null}
      </div>
    </div>
  );
};

const IssuePlan = () => {
  const [{ projectId, iterationId }, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { id: queryId, pId: queryParentId, iterationID: queryItertationID, type: _queryType, ...restQeury } = query;
  const queryType = _queryType && _queryType.toUpperCase();
  const [drawerVisible, openDrawer, closeDrawer] = useSwitch(false);
  const [
    { urlQuery, filterObj, chosenIssueType, chosenIteration, chosenIssueId, chosenParentId, isFullScreen },
    updater,
    update,
  ] = useUpdate({
    filterObj: {},
    urlQuery: restQeury,
    chosenParentId: queryParentId || 0,
    chosenIssueId: queryId,
    chosenIteration: queryItertationID || 0,
    chosenIssueType: queryType as undefined | ISSUE_TYPE,
    isFullScreen: false,
  });
  const ganttRef = React.useRef<HTMLDivElement>(null);

  const onChosenIssue = (val: Obj) => {
    const { id, extra, pId } = val || {};
    if (id && extra?.iterationID && extra?.type) {
      update({
        chosenParentId: pId,
        chosenIssueId: val.id,
        chosenIteration: extra.iterationID,
        chosenIssueType: extra.type.toUpperCase() as ISSUE_TYPE,
      });
      openDrawer();
    }
  };

  const reloadRef = React.useRef(null as any);

  React.useEffect(() => {
    updateSearch({ ...urlQuery });
  }, [urlQuery]);

  React.useEffect(() => {
    const buttonEle = document.getElementsByClassName('top-button-group');
    if (buttonEle.length > 0) {
      buttonEle[0].style.display = isFullScreen ? 'none' : 'flex';
    }
  }, [isFullScreen]);

  const inParams = { projectId, fixedIteration: iterationId, ...urlQuery };

  const urlQueryChange = (val: Obj) => updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));

  const onCreate = (val: any) => {
    const filterIterationIDs = filterObj?.iterationIDs || [];
    const createTypeMap = {
      createRequirement: ISSUE_TYPE.REQUIREMENT,
      createTask: ISSUE_TYPE.TASK,
      createBug: ISSUE_TYPE.BUG,
    };
    const curType = createTypeMap[val?.key];
    if (curType) {
      // 当前选中唯一迭代，创建的时候默认为这个迭代，否则，迭代为0
      update({
        chosenIteration: iterationId || (filterIterationIDs.length === 1 ? filterIterationIDs[0] : 0),
        chosenIssueType: curType,
      });
      openDrawer();
    }
  };

  const reloadData = (_inParams: Obj = {}) => {
    if (reloadRef.current && reloadRef.current.reload) {
      reloadRef.current.reload({ inParams: _inParams });
    }
  };

  const onCloseDrawer = ({ hasEdited, isCreate, isDelete }: CloseDrawerParam) => {
    closeDrawer();
    if (hasEdited || isCreate || isDelete) {
      // 有变更再刷新列表
      let reInParams: number[] = [];
      // if create or delete or update task, reload root
      if ((chosenParentId === 0 && (isDelete || isCreate)) || chosenParentId !== 0) {
        reInParams = [chosenParentId];
      } else {
        reInParams = [chosenParentId, chosenIssueId];
      }
      reloadData({ parentId: reInParams });
    }
    update({
      chosenParentId: 0,
      chosenIssueId: 0,
      chosenIteration: 0,
      chosenIssueType: undefined,
    });
  };

  const handleScreenChange = (value: boolean) => {
    updater.isFullScreen(value);
  };

  return (
    <div className={`h-full ${isFullScreen ? 'gantt-fullscreen' : ''}`} ref={ganttRef}>
      <DiceConfigPage
        ref={reloadRef}
        scenarioType={'issue-gantt'}
        scenarioKey={'issue-gantt'}
        inParams={inParams}
        customProps={{
          topHead: {
            props: {
              isTopHead: true,
            },
          },
          ganttContainer: {
            props: { flexHeight: true },
          },
          page: {
            props: { fullHeight: true, overflowHidden: true },
          },
          gantt: {
            props: {
              BarContentRender,
              TaskListHeader,
              TreeNodeRender: (p) => <TreeNodeRender {...p} clickNode={onChosenIssue} />,
              onScreenChange: handleScreenChange,
              rootWrapper: ganttRef,
            },
          },
          issueAddButton: {
            props: {
              disabled: false,
              menu: [
                {
                  disabled: false,
                  disabledTip: '',
                  key: 'requirement',
                  operations: {
                    click: {
                      key: 'createRequirement',
                      reload: false,
                    },
                  },
                  prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                  text: i18n.t('requirement'),
                },
                {
                  disabled: false,
                  disabledTip: '',
                  key: 'task',
                  operations: {
                    click: {
                      key: 'createTask',
                      reload: false,
                    },
                  },
                  prefixIcon: 'ISSUE_ICON.issue.TASK',
                  text: i18n.t('task'),
                },
                {
                  disabled: false,
                  disabledTip: '',
                  key: 'bug',
                  operations: {
                    click: {
                      key: 'createBug',
                      reload: false,
                    },
                  },
                  prefixIcon: 'ISSUE_ICON.issue.BUG',
                  text: i18n.t('bug'),
                },
              ],
              operations: {
                click: {
                  key: '',
                  reload: false,
                },
              },
              suffixIcon: 'di',
              text: i18n.t('dop:create issue'),
              type: 'primary',
            },
            op: {
              // 添加：打开滑窗
              click: onCreate,
            },
          },
          filter: {
            op: {
              onFilterChange: (val: Obj) => {
                updater.filterObj(val);
                urlQueryChange(val);
              },
            },
          },
        }}
      />

      {chosenIssueType ? (
        <EditIssueDrawer
          iterationID={chosenIteration}
          issueType={chosenIssueType as ISSUE_TYPE}
          visible={drawerVisible}
          closeDrawer={onCloseDrawer}
          id={chosenIssueId}
          shareLink={`${location.href.split('?')[0]}?${mergeSearch(
            { id: chosenIssueId, pId: chosenParentId, iterationID: chosenIteration, type: chosenIssueType },
            true,
          )}`}
        />
      ) : null}
    </div>
  );
};

export default IssuePlan;
