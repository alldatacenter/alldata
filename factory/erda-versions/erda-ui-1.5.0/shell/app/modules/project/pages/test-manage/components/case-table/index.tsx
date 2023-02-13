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

import React, { useMemo, useEffect, useRef, useCallback } from 'react';
import { cloneDeep, find, get, isEmpty, map, forEach, reduce, isNaN, filter } from 'lodash';
import i18n from 'i18n';
import { Table } from 'antd';

import { Icon as CustomIcon, Ellipsis } from 'common';
import { updateSearch } from 'common/utils';
import layoutStore from 'layout/stores/layout';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import testCaseStore from 'project/stores/test-case';
import testSetStore from 'project/stores/test-set';
import AllCheckBox from './all-checkbox';
import CaseCheckBox from './case-checkbox';
import projectStore from 'project/stores/project';
import { ColumnProps } from 'core/common/interface';

const sorterMap: object = {
  ascend: 'ASC',
  descend: 'DESC',
};

const biggerSorterMap: object = {
  ASC: 'ascend',
  DESC: 'descend',
};

const metaColumnsMap = {
  priority: 'priority',
  result: 'testPlanUsecaseResult',
  testPlanStatus: 'testPlanStatus',
};

const getDataSource = (caseList: TEST_CASE.CaseDirectoryItem[], scope: TEST_CASE.PageScope, projectName = '') => {
  const expandedRowKeys: string[] = [];
  const dataSource: any[] = map(caseList, ({ testCases, ...others }) => {
    // eslint-disable-next-line no-param-reassign
    others.directory = `/${projectName}${others.directory}`.replace(/\/$/, '');
    expandedRowKeys.push(`${others.testSetID}-`);
    // 把父级放到子case中，便于获取父级信息
    let children = [];
    if (scope === 'testPlan') {
      children = testCases.map((item: any) => ({ ...item, planRelationCaseID: item.id, parent: { ...others } }));
    } else {
      children = testCases.map((item: any) => ({ ...item, testCaseID: item.id, parent: { ...others } }));
    }
    return { ...others, children };
  });
  return { dataSource, expandedRowKeys };
};

interface IProps {
  query?: any;
  columns: Array<ColumnProps<TEST_CASE.CaseTableRecord>>;
  testPlanId?: number;
  scope: TEST_CASE.PageScope;
  modalQuery?: any;
  onChange?: (query: object) => void;
  onClickRow?: (record: object, recordList?: object[]) => void;
}

const defaultPageNo = 1;
const defaultPageSize = 15;

const CaseTable = ({ query: queryProp, columns, onClickRow, scope, onChange, testPlanId, modalQuery = {} }: IProps) => {
  const isModal = scope === 'caseModal';
  const client = layoutStore.useStore((s) => s.client);
  const projectInfo = projectStore.useStore((s) => s.info);
  const { getCases: oldGetCases } = testCaseStore.effects;
  const { updateBreadcrumb } = testSetStore.effects;
  const [loading] = useLoading(testCaseStore, ['getCases']);
  const routeQuery = queryProp || routeInfoStore.useStore((s) => s.query);
  const [metaFields, pageCaseList, pageCaseTotal, modalCaseList, modalCaseTotal] = testCaseStore.useStore((s) => [
    s.metaFields,
    s.caseList,
    s.caseTotal,
    s.modalCaseList,
    s.modalCaseTotal,
  ]);
  const caseList = isModal ? modalCaseList : pageCaseList;
  const caseTotal = (isModal ? modalCaseTotal : pageCaseTotal) as number;

  const getCases = useCallback(
    (payload: any) => {
      oldGetCases({ selected: [], ...payload, scope, testPlanId });
    },
    [scope, oldGetCases, testPlanId],
  );

  // 使用弹框里可能传入的参数覆盖url上的参数
  const query = { ...routeQuery, ...modalQuery };
  const { dataSource, expandedRowKeys } = useMemo(
    () => getDataSource(caseList, scope, projectInfo.name),
    [caseList, projectInfo.name, scope],
  );

  useEffect(() => {
    if (query.caseId && onClickRow) {
      let target: any;
      forEach(dataSource, (item) => {
        target = target || find(item.children, { id: +query.caseId });
      });
      if (target) {
        updateBreadcrumb({
          pathName: target.parent.directory,
          testSetID: target.testSetID,
          testPlanID: testPlanId,
        });
        onClickRow(target, dataSource);
      }
    }
  }, [query.caseId, dataSource, onClickRow]);

  // 计算是否需要左右滑动
  const ref = useRef(null);
  const hasMount = !!ref.current;
  // 不必去除 react-hooks/exhaustive-deps, 无法识别 clientWidth
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const tableWidth = useMemo(
    () => get(document.querySelector('.case-table'), ['clientWidth'], 0),
    [client.width, hasMount],
  );
  const tempProps: any = {};
  const className = 'case-table';
  // if (mode !== 'modal') { // 非弹框
  const configWidth = reduce(
    columns,
    (temp, { width }: any) => temp + (isNaN(parseInt(width, 10)) ? 380 : parseInt(width, 10)),
    0,
  );
  // 操作列
  const operationWidth = (columns[columns.length - 1].width || 60) as number;
  if (tableWidth + operationWidth < configWidth) {
    tempProps.scroll = { x: configWidth };
  }
  // }

  const isScroll = !!tempProps.scroll;
  const { orderBy, orderRule } = query;
  const metaMap = useMemo(() => {
    const temp = {};
    forEach(metaFields, ({ enums }) => {
      forEach(enums, ({ fieldUniqueName, showName, value }) => {
        temp[`${fieldUniqueName}-${value}`] = showName;
      });
    });
    return temp;
  }, [metaFields]);

  const newColumns = useMemo(() => {
    // 初始化表格columns
    const tempColumns = cloneDeep(columns);
    // ID列
    const idColumn = find(tempColumns, ({ key }: any) => key === 'id');
    if (idColumn) {
      Object.assign(idColumn, {
        dataIndex: 'id',
        className: 'case-table-id',
        render: (id: string, record: any) => {
          if (id) {
            return <span className="text-normal">{`#${record.testCaseID}`}</span>;
          }
          return {
            children: <Ellipsis className="text-desc" title={record.directory} />,
            props: {
              colSpan: 5,
            },
          };
        },
      });
    }
    // 标题列(只针对当前页操作)
    const nameColumn = find(tempColumns, ({ key }: any) => key === 'name');
    if (nameColumn) {
      Object.assign(nameColumn, {
        // title: <ChooseTitle mode={mode} />,
        title: <span>{i18n.t('dop:use case title')}</span>,
        render: (name: string, record: any) => {
          const obj = {
            children: <Ellipsis className="font-bold" title={name} />,
            props: {},
          };
          if (!record.id) {
            obj.props.colSpan = 0;
          }
          return obj;
        },
      });
    }
    // 全选列
    const checkColumn = find(tempColumns, ({ key }: any) => key === 'checkbox');
    if (checkColumn) {
      Object.assign(checkColumn, {
        title: <AllCheckBox mode={scope} />,
        className: 'case-table-checkbox',
        render: (_text: any, record: any) => {
          if (record.id) {
            return <CaseCheckBox mode={scope} id={record.id} />;
          }
          return <CustomIcon type="wjj1" className="text-warning" />;
        },
      });
    }
    // 正在排序的列
    if (orderBy) {
      const sortColumn = find(tempColumns, ({ key }: any) => key === orderBy);
      if (!sortColumn || !biggerSorterMap[orderRule]) return;
      Object.assign(sortColumn, {
        defaultSortOrder: biggerSorterMap[orderRule],
      });
    }
    // 测试元数据相关列
    forEach(tempColumns, (single: any) => {
      const { dataIndex, key } = single;
      const fieldUniqueName = metaColumnsMap[dataIndex] || metaColumnsMap[key];
      if (fieldUniqueName && !single.render) {
        // eslint-disable-next-line no-param-reassign
        single.render = (value: any) => metaMap[`${fieldUniqueName}-${value}`] || value;
      }
    });
    // 操作列
    const operationColumn = filter(tempColumns, ({ fixed }: any) => fixed);
    if (!isEmpty(operationColumn)) {
      operationColumn.width = 120;
      forEach(operationColumn, (single: any) => {
        Object.assign(single, {
          fixed: isScroll ? single.fixed : undefined,
        });
      });
    }

    return tempColumns;
  }, [orderBy, orderRule, columns, scope, metaMap, isScroll]);

  const handleChange = (pagination: any, _filters: any, sorter: any) => {
    let newSorter = {};
    if (!sorter?.order) {
      delete query.orderBy;
      delete query.orderRule;
      newSorter = { orderBy: undefined, orderRule: undefined };
    } else {
      newSorter = { orderBy: sorter.columnKey, orderRule: sorterMap[sorter.order] };
    }
    const newQuery = { ...query, testPlanId, pageNo: pagination.current, pageSize: pagination.pageSize, ...newSorter };
    // 不在弹框里时，不改变url参数
    if (scope !== 'caseModal') {
      updateSearch(newQuery);
    }
    getCases(newQuery);
    if (onChange) {
      onChange(newQuery);
    }
  };

  const handleClickRow = (record: TEST_CASE.CaseTableRecord) => {
    if (record.children) {
      return;
    }
    updateBreadcrumb({
      pathName: record.parent.directory,
      testSetID: record.testSetID,
      testPlanID: testPlanId,
    });
    onClickRow && onClickRow(record, dataSource);
  };

  return (
    <Table
      ref={ref}
      loading={loading}
      className={className}
      indentSize={0}
      expandedRowKeys={expandedRowKeys}
      dataSource={dataSource}
      columns={newColumns}
      rowKey={(record: any) => `${record.testSetID}-${record.id || ''}`}
      onChange={handleChange}
      expandIcon={() => null}
      onRow={(record: TEST_CASE.CaseTableRecord) => ({
        onClick: () => handleClickRow(record),
      })}
      {...tempProps}
      pagination={{
        total: caseTotal,
        current: parseInt(query.pageNo, 10) || defaultPageNo,
        pageSize: parseInt(query.pageSize, 10) || defaultPageSize,
        showSizeChanger: true,
      }}
      scroll={{ x: 1500 }}
    />
  );
};

export default CaseTable;
