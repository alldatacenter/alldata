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

import { size } from 'lodash';
import React, { useState, useCallback, useMemo } from 'react';
import { useUpdateEffect } from 'react-use';
import i18n from 'i18n';
import { Modal, message, Select } from 'antd';
import { priorityList } from '../../test-manage/constants';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import testCaseStore from 'project/stores/test-case';
import testPlanStore from 'project/stores/test-plan';
import { CaseTree, CaseTable } from '../../test-manage/components';
import { commonColumns } from '../../test-manage/case/columns';
import './index.scss';

interface IProps {
  visible: boolean;
  onCancel: () => any;
}
const { Option } = Select;
const defaultQuery = { testSetID: 0, pageNo: 1, pageSize: 15 };

const CaseImport = ({ visible, onCancel }: IProps) => {
  const [query, setQuery] = useState(defaultQuery as any);
  const { getCases: oldGetCases } = testCaseStore.effects;
  const { clearChoosenInfo } = testCaseStore.reducers;
  const { addToPlanInCaseModal } = testPlanStore.effects;
  const [{ testPlanId }, routeQuery] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const [modalCaseTotal, modalChoosenInfo] = testCaseStore.useStore((s) => [s.modalCaseTotal, s.modalChoosenInfo]);
  const { primaryKeys } = modalChoosenInfo;
  const checked = size(primaryKeys);
  const [confirmLoading] = useLoading(testPlanStore, ['addToPlanInCaseModal']);
  const [priorityFilter, setPriorityFilter] = useState(routeQuery.priority);

  React.useEffect(() => {
    if (!visible) {
      setQuery(defaultQuery);
    }
  }, [visible]);

  React.useEffect(() => {
    setPriorityFilter(routeQuery.priority);
  }, [routeQuery.priority]);

  useUpdateEffect(() => {
    getCases({
      ...query,
      pageNo: 1,
      priority: priorityFilter,
      notInTestPlanID: testPlanId,
      testPlanId: undefined, // 引用的弹框里不传testPlanId，要置为undefined
    });
  }, [priorityFilter]);

  const getCases = useCallback(
    (newQuery: TEST_CASE.QueryCase) => {
      oldGetCases({ ...newQuery, scope: 'caseModal' });
      setQuery(newQuery);
    },
    [oldGetCases],
  );

  const onSelect = useCallback(
    (info: any) => {
      if (!info) {
        return;
      }
      const { testSetID, parentID } = info;
      const searchQuery: any = { ...query, parentID, testSetID };
      // 左侧选择集合变了时，重置pageNO
      if (testSetID !== query.testSetID) {
        searchQuery.pageNo = 1;
      }

      getCases({
        ...searchQuery,
        priority: priorityFilter,
        notInTestPlanID: testPlanId,
        testPlanId: undefined, // 引用的弹框里不传testPlanId，要置为undefined
      });
    },
    [getCases, priorityFilter, query, testPlanId],
  );

  const onTableChange = useCallback(
    (newQuery: object) => {
      setQuery({ ...query, ...newQuery });
    },
    [query],
  );

  const onOk = () => {
    if (!modalCaseTotal || !checked) {
      message.error(i18n.t('dop:After the use case is selected, the batch operation can be performed.'));
      return;
    }
    setPriorityFilter(routeQuery.priority);
    const newQuery: any = { ...query, testPlanId };
    delete newQuery.pageNo;
    delete newQuery.pageSize;
    addToPlanInCaseModal(newQuery).then(() => {
      handleCancel();
    });
  };

  const modalQuery = useMemo(() => {
    return {
      testSetID: query.testSetID,
      pageNo: query.pageNo || 1,
      notInTestPlanID: testPlanId,
      pageSize: query.pageSize,
      priority: priorityFilter,
    };
  }, [query.testSetID, query.pageNo, query.pageSize, testPlanId, priorityFilter]);

  const handleCancel = () => {
    onCancel();
    setPriorityFilter(routeQuery.priority);
    clearChoosenInfo({ mode: 'caseModal' });
  };

  const handleFilter = (value: any) => {
    setPriorityFilter(value);
  };

  return (
    <Modal
      closable={false}
      visible={visible}
      title={i18n.t('dop:import use case')}
      width={1000}
      onOk={onOk}
      onCancel={handleCancel}
      confirmLoading={confirmLoading}
      destroyOnClose
    >
      <div className="case-import-content">
        <div className="left">
          <CaseTree mode="caseModal" readOnly onSelect={onSelect} />
        </div>
        <div className="right">
          <Select
            style={{ width: 240 }}
            placeholder={i18n.t('dop:filter by priority')}
            mode="multiple"
            value={priorityFilter}
            onChange={handleFilter}
            className="mb-4"
          >
            {priorityList.map((item) => (
              <Option value={item} key={item}>
                {item}
              </Option>
            ))}
          </Select>

          <CaseTable columns={commonColumns} scope="caseModal" onChange={onTableChange} modalQuery={modalQuery} />
        </div>
      </div>
    </Modal>
  );
};

export default CaseImport as any as (props: Omit<IProps, 'form'>) => JSX.Element;
