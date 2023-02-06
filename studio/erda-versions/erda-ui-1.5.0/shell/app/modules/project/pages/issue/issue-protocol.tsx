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
import { ISSUE_TYPE, ISSUE_TYPE_MAP } from 'project/common/components/issue/issue-config';
import DiceConfigPage, { useMock } from 'app/config-page';
import { getUrlQuery } from 'config-page/utils';
import { useSwitch, useUpdate } from 'common/use-hooks';
import { qs, mergeSearch, updateSearch, setApiWithOrg } from 'common/utils';
import orgStore from 'app/org-home/stores/org';
import EditIssueDrawer, { CloseDrawerParam } from 'project/common/components/issue/edit-issue-drawer';
import { Badge, ErdaIcon } from 'common';
import { Button, Dropdown, Menu } from 'antd';
import routeInfoStore from 'core/stores/route';
import ImportFile from 'project/pages/issue/component/import-file';
import issueFieldStore from 'org/stores/issue-field';
import { useMount, useUpdateEffect } from 'react-use';
import i18n from 'i18n';

interface IProps {
  issueType: ISSUE_TYPE;
}

const getRealIssueType = (issueType: ISSUE_TYPE) => {
  if (issueType === ISSUE_TYPE.ALL) return [ISSUE_TYPE.EPIC, ISSUE_TYPE.REQUIREMENT, ISSUE_TYPE.TASK, ISSUE_TYPE.BUG];
  return issueType;
};

const compareObject = (sourceObj: object, targetObj: object) => {
  if (Object.keys(sourceObj).length === Object.keys(targetObj).length) {
    return Object.keys(sourceObj).filter((key) => sourceObj[key] !== targetObj[key]).length === 0;
  } else {
    return false;
  }
};

const IssueProtocol = ({ issueType }: IProps) => {
  const [{ projectId, iterationId }, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { id: queryId, iterationID: queryItertationID, type: _queryType, ...restQuery } = query;
  const orgID = orgStore.getState((s) => s.currentOrg.id);
  const queryType = _queryType && _queryType.toUpperCase();
  const [
    { importFileVisible, filterObj, chosenIssueType, chosenIssueId, chosenIteration, urlQuery, urlQueryChangeByQuery },
    updater,
    update,
  ] = useUpdate({
    importFileVisible: false,
    filterObj: {},
    chosenIssueId: queryId,
    chosenIteration: queryItertationID || 0,
    urlQuery: restQuery,
    chosenIssueType: queryType as undefined | ISSUE_TYPE,
    pageNo: 1,
    viewType: '',
    viewGroup: '',
    urlQueryChangeByQuery: restQuery, // Only used to listen for changes to update the page after url change
  });
  const { getFieldsByIssue: getCustomFieldsByProject } = issueFieldStore.effects;
  useMount(() => {
    getCustomFieldsByProject({
      propertyIssueType: issueType,
      orgID,
    });
  });

  const reloadRef = React.useRef(null as any);
  const filterObjRef = React.useRef(null as any);

  const queryRef = React.useRef(restQuery);

  const [drawerVisible, openDrawer, closeDrawer] = useSwitch(queryId || false);

  const inParams = {
    fixedIteration: iterationId,
    fixedIssueType: issueType,
    projectId,
    ...(urlQuery || {}),
  };

  const getDownloadUrl = (IsDownload = false) => {
    const pageData = reloadRef.current?.getPageConfig();
    const useableFilterObj = pageData?.protocol?.state?.IssuePagingRequest || {};

    return setApiWithOrg(
      `/api/issues/actions/export-excel?${qs.stringify(
        { ...useableFilterObj, pageNo: 1, projectID: projectId, type: getRealIssueType(issueType), IsDownload, orgID },
        { arrayFormat: 'none' },
      )}`,
    );
  };

  const reloadData = () => {
    if (reloadRef.current && reloadRef.current.reload) {
      reloadRef.current.reload();
    }
  };

  React.useEffect(() => {
    filterObjRef.current = filterObj;
  }, [filterObj]);

  useUpdateEffect(() => {
    const { id: _id, iterationID: _iterationID, type: _type, ..._restQuery } = query;
    queryRef.current = _restQuery;
  }, [query]);

  useUpdateEffect(() => {
    if (!compareObject(urlQuery, queryRef.current)) {
      queryRef.current = urlQuery;
      updateSearch({ ...(urlQuery || {}) });
    }
  }, [urlQuery]);

  useUpdateEffect(() => {
    if (!compareObject(urlQuery, queryRef.current)) {
      // Execute only after url change such as page go back
      update({
        urlQuery: queryRef.current,
        urlQueryChangeByQuery: queryRef.current, // Only used to listen for changes to update the page
      });
    }
  }, [queryRef.current]);

  useUpdateEffect(() => {
    reloadData();
  }, [urlQueryChangeByQuery]);

  const onChosenIssue = (val: ISSUE.Issue) => {
    update({
      chosenIssueId: val.id,
      chosenIteration: val.iterationID,
      chosenIssueType: val.type as ISSUE_TYPE,
    });
    openDrawer();
  };

  const onCloseDrawer = ({ hasEdited, isCreate, isDelete }: CloseDrawerParam) => {
    closeDrawer();
    update({
      chosenIssueId: 0,
      chosenIteration: 0,
      chosenIssueType: undefined,
    });
    if (hasEdited || isCreate || isDelete) {
      // 有变更再刷新列表
      reloadData();
    }
  };

  const onCreate = (curType?: string) => {
    const filterIterationIDs = filterObj?.values?.iterationIDs || [];
    // 当前选中唯一迭代，创建的时候默认为这个迭代，否则，迭代为0
    update({
      chosenIteration: iterationId || (filterIterationIDs.length === 1 ? filterIterationIDs[0] : 0),
      chosenIssueType: curType || issueType,
    });
    openDrawer();
  };

  const dropdownMenu = (
    <Menu
      onClick={(e) => {
        e.domEvent.stopPropagation();
        onCreate(e.key);
      }}
    >
      {[ISSUE_TYPE_MAP.REQUIREMENT, ISSUE_TYPE_MAP.TASK, ISSUE_TYPE_MAP.BUG].map((mItem) => {
        return <Menu.Item key={mItem.value}>{mItem.iconLabel}</Menu.Item>;
      })}
    </Menu>
  );

  return (
    <>
      <div className="top-button-group">
        {issueType === ISSUE_TYPE.ALL ? (
          <Dropdown overlay={dropdownMenu}>
            <Button type="primary" className="flex items-center">
              {i18n.t('new {name}', { name: i18n.t('dop:issue') })}
              <ErdaIcon type="caret-down" size="18" className="ml-1" />
            </Button>
          </Dropdown>
        ) : (
          <Button type={'primary'} onClick={() => onCreate(issueType)}>
            {i18n.t('new {name}', { name: ISSUE_TYPE_MAP[issueType]?.label })}
          </Button>
        )}
      </div>
      <DiceConfigPage
        scenarioKey="issue-manage"
        scenarioType="issue-manage"
        showLoading
        inParams={inParams}
        ref={reloadRef}
        customProps={{
          issueManage: {
            props: { spaceSize: 'none' },
          },
          // 后端未对接，由前端接管的事件
          issueAddButton: {
            op: {
              // 添加：打开滑窗
              click: onCreate,
            },
          },
          issueFilter: {
            op: {
              // filter: 改变url
              onFilterChange: (val: Obj) => {
                updater.filterObj(val);
                updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));
              },
            },
            props: {
              processField: (field: CP_CONFIGURABLE_FILTER.Condition) => {
                if (field.key === 'priorities') {
                  return {
                    ...field,
                    options: field.options?.map((item) => ({
                      ...item,
                      icon: `ISSUE_ICON.priority.${item.value}`,
                    })),
                  };
                } else if (field.key === 'severities') {
                  return {
                    ...field,
                    options: field.options?.map((item) => ({
                      ...item,
                      icon: `ISSUE_ICON.severity.${item.value}`,
                    })),
                  };
                } else {
                  return field;
                }
              },
            },
          },
          issueViewGroup: {
            op: {
              // 视图切换： 改变url
              onStateChange: (val: Obj) => {
                updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));
                // updater.viewType(val?.value);
                // updater.viewType(val?.childrenValue?.kanban);
              },
            },
          },
          issueTable: {
            props: {
              menuItemRender: (item: { text: string; status: string }) => (
                <Badge text={item.text} status={item.status} showDot={false} />
              ),
            },
            op: {
              // 表格视图： pageNo改变url，点击item打开滑窗详情
              onStateChange: (val: Obj) => {
                updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));
                // updater.pageNo(val?.pageNo || 1);
              },
              clickTableItem: (_data: ISSUE.Issue) => {
                onChosenIssue(_data);
              },
            },
          },
          issueKanban: {
            op: {
              // 看板：点击单个看板节点，打开滑窗
              clickNode: (_data: ISSUE.Issue) => {
                onChosenIssue(_data);
              },
            },
          },
          issueGantt: {
            op: {
              // 点击单个看板任务：打开滑窗
              onStateChange: (val: Obj) => {
                updater.urlQuery((prev: Obj) => ({ ...prev, ...getUrlQuery(val) }));
                updater.pageNo(val?.pageNo || 1);
              },
              clickTableItem: (_data: ISSUE.Issue) => {
                onChosenIssue(_data);
              },
            },
          },
          issueImport: {
            op: {
              // 导入
              click: () => {
                updater.importFileVisible(true);
              },
            },
          },
          issueExport: {
            op: {
              // 导出
              click: () => window.open(getDownloadUrl()),
            },
          },
        }}
      />
      {[ISSUE_TYPE.BUG, ISSUE_TYPE.REQUIREMENT, ISSUE_TYPE.TASK].includes(issueType) ? (
        <ImportFile
          issueType={issueType}
          download={getDownloadUrl(true)}
          projectID={projectId}
          visible={importFileVisible}
          onClose={() => {
            updater.importFileVisible(false);
          }}
          afterImport={() => {
            reloadData();
          }}
        />
      ) : null}

      {[ISSUE_TYPE.BUG, ISSUE_TYPE.REQUIREMENT, ISSUE_TYPE.TASK].includes(chosenIssueType) ? (
        <EditIssueDrawer
          iterationID={chosenIteration}
          id={chosenIssueId}
          issueType={chosenIssueType as ISSUE_TYPE}
          shareLink={`${location.href.split('?')[0]}?${mergeSearch(
            { id: chosenIssueId, iterationID: chosenIteration, type: chosenIssueType },
            true,
          )}`}
          visible={drawerVisible}
          closeDrawer={onCloseDrawer}
        />
      ) : null}
    </>
  );
};

export default IssueProtocol;
