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

import { findIndex } from 'lodash';
import { PassAndNext } from 'project/pages/plan-detail/pass-and-next';
import { Button } from 'antd';
import { Icon as CustomIcon } from 'common';
import React from 'react';
import testCaseStore from 'project/stores/test-case';
import testPlanStore from 'project/stores/test-plan';
import i18n from 'i18n';

interface IProps {
  scope: 'testCase' | 'testPlan';
  caseList: TEST_CASE.TestCaseItem[];
  editMode: boolean;
  onClose: () => void;
  onOk: (bool: boolean) => void;
}

const CaseFooter = ({ scope, caseList, editMode, onClose, onOk }: IProps) => {
  const caseDetail = testCaseStore.useStore((s) => s.caseDetail);
  const { getCaseDetail } = testCaseStore.effects;
  const { updateCasesStatus } = testPlanStore.effects;
  if (scope === 'testPlan' && editMode) {
    let prevCase = undefined as any;
    let nextCase = undefined as any;
    const index = findIndex(caseList, { id: caseDetail.planRelationCaseID });
    prevCase = caseList[index - 1];
    nextCase = caseList[index + 1];

    const passAndNext = (status: string) => {
      updateCasesStatus({ relationIDs: [caseDetail.planRelationCaseID], execStatus: status });
      nextCase && getCaseDetail({ id: nextCase.id, scope });
    };

    const toPrev = () => {
      prevCase && getCaseDetail({ id: prevCase.id, scope });
    };

    const toNext = () => {
      nextCase && getCaseDetail({ id: nextCase.id, scope });
    };
    return (
      <>
        <PassAndNext current={caseDetail.execStatus} hasNext={!!nextCase} onClick={passAndNext} />
        <Button.Group className="ml-3">
          <Button>
            <span style={{ lineHeight: '1.5' }}>
              {index + 1} / {caseList.length}
            </span>
          </Button>
          <Button disabled={!prevCase} onClick={toPrev}>
            <CustomIcon type="chevron-up" />
          </Button>
          <Button disabled={!nextCase} onClick={toNext}>
            <CustomIcon type="chevron-down" />
          </Button>
        </Button.Group>
      </>
    );
  }
  if (editMode) {
    return (
      <>
        <Button type="primary" onClick={onClose}>
          {i18n.t('close')}
        </Button>
      </>
    );
  }
  return (
    <>
      <Button onClick={onClose}>{i18n.t('cancel')}</Button>
      <Button
        className="ml-3"
        type="primary"
        ghost
        onClick={() => {
          onOk(false);
        }}
      >
        {i18n.t('dop:save and continue adding')}
      </Button>
      <Button
        className="ml-3"
        type="primary"
        onClick={() => {
          onOk(true);
        }}
      >
        {i18n.t('save')}
      </Button>
    </>
  );
};

export default CaseFooter;
