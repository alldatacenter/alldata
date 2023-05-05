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

import { isNull, omitBy, size, omit } from 'lodash';
import React, { useState, useMemo, useCallback } from 'react';
import i18n from 'i18n';
import { Menu, Modal, message } from 'antd';
import { DropdownSelect, FormModal, MemberSelector } from 'common';
import { qs } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import testCaseStore from 'project/stores/test-case';
import testPlanStore from 'project/stores/test-plan';
import { formatQuery } from 'project/utils/test-case';

interface IProps {
  afterDelete: (data: number[]) => void;
}

const BatchProcessing = ({ afterDelete }: IProps) => {
  const [visible, setVisible] = useState(false);
  const [caseTotal, choosenInfo] = testCaseStore.useStore((s) => [s.caseTotal, s.choosenInfo]);
  const { isAll, primaryKeys } = choosenInfo;
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { openRemarkModal } = testPlanStore.reducers;
  const { planUserCaseBatch, deleteRelations, exportFiles } = testPlanStore.effects;
  const checked = isAll || !!size(primaryKeys);
  const afterDeleteRef = React.useRef(afterDelete);
  const onClick = useCallback(
    ({ key }: any) => {
      if (key !== 'excel' && (!caseTotal || !checked)) {
        message.error(i18n.t('dop:After the use case is selected, the batch operation can be performed.'));
        return;
      }
      let selectProjectId;
      let searchQuery = {};
      switch (key) {
        case 'delete':
          Modal.confirm({
            title: i18n.t('remove'),
            content: i18n.t('dop:plan-remove-case-confirm'),
            onOk: () => deleteRelations({ type: 'multi', relationIDs: [] }).then(afterDeleteRef.current),
          });
          break;
        case 'actor':
          setVisible(true);
          break;
        case 'remark':
          openRemarkModal({ type: 'multi', remark: '' });
          break;
        case 'excel':
          selectProjectId = params.projectId;
          searchQuery = omit(formatQuery(query), ['selectProjectId', 'testPlanId', 'testSetId', 'eventKey']);

          // eslint-disable-next-line no-case-declarations
          let queryParam = qs.stringify(omitBy({ selectProjectId, ...params }, isNull), { arrayFormat: 'none' });
          queryParam += qs.stringify(searchQuery, { arrayFormat: 'none' });
          queryParam += `&${qs.stringify({ relationID: primaryKeys }, { arrayFormat: 'none' })}`;
          exportFiles(`${queryParam}&fileType=excel`);
          break;
        default:
          break;
      }
    },
    [caseTotal, checked, deleteRelations, exportFiles, openRemarkModal, params, query, primaryKeys],
  );

  const menus = useMemo(() => {
    return (
      <Menu onClick={onClick}>
        <Menu.Item key="delete">
          <span>{i18n.t('delete')}</span>
        </Menu.Item>
        <Menu.Item key="actor">
          <span>{i18n.t('dop:change executor')}</span>
        </Menu.Item>
        {/* <Menu.Item key="remark">
          <span>添加备注</span>
        </Menu.Item> */}
        <Menu.Item key="excel">
          <span>{i18n.t('dop:export Excel')}</span>
        </Menu.Item>
      </Menu>
    );
  }, [onClick]);

  const onCancel = () => {
    setVisible(false);
  };

  const handleOk = (data: Pick<TEST_PLAN.PlanBatch, 'executorID'>) => {
    planUserCaseBatch(data).then(() => {
      onCancel();
    });
  };

  const fieldsList = [
    {
      label: i18n.t('dop:executor'),
      name: 'executorID',
      getComp: () => <MemberSelector scopeType="project" scopeId={params.projectId} />,
    },
  ];
  return (
    <>
      <DropdownSelect overlay={menus} buttonText={i18n.t('dop:batch processing')} />
      <FormModal
        title={i18n.t('dop:change executor')}
        visible={visible}
        onOk={handleOk}
        onCancel={onCancel}
        fieldsList={fieldsList}
      />
    </>
  );
};

export default BatchProcessing;
