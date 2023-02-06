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

import { map } from 'lodash';
import i18n from 'i18n';
import React from 'react';
import { FormModal, MemberSelector } from 'common';
import routeInfoStore from 'core/stores/route';
import testPlanStore from 'project/stores/test-plan';
import iterationStore from 'project/stores/iteration';
import { FormModalList } from 'app/interface/common';
import { useLoading } from 'core/stores/loading';
import { useMount } from 'react-use';

interface IProps {
  visible: boolean;
  testPlanId?: string;
  textInfo?: string;
  mode: 'edit' | 'copy' | 'add' | '';
  onCancel: () => void;
  afterSubmit: () => void;
}

const TestPlanModal = (props: IProps) => {
  const { testPlanId, mode, visible, textInfo, onCancel, afterSubmit } = props;
  const [loading, setLoading] = React.useState(false);
  const params = routeInfoStore.useStore((s) => s.params);
  const planItem = testPlanStore.useStore((s) => s.planItem);
  const iterationList = iterationStore.useStore((s) => s.iterationList);
  const { getTestPlanItem, addTestPlan, updateTestPlan } = testPlanStore.effects;
  const { cleanTestPlan } = testPlanStore.reducers;
  const [loadingIterationList] = useLoading(iterationStore, ['getIterations']);

  const handleOk = (values: any) => {
    const copy = { ...values };
    if (copy.iterationID !== undefined) {
      copy.iterationID = +copy.iterationID;
    }
    setLoading(true);
    const close = () => {
      setLoading(false);
      afterSubmit?.();
      onCancel();
    };
    if (mode === 'edit') {
      updateTestPlan({ ...copy, id: testPlanId, status: planItem.status }).then(close);
    } else {
      addTestPlan(copy).then(close);
    }
  };

  useMount(() => {
    if (!iterationList.length && !loadingIterationList) {
      iterationStore.effects.getIterations({
        pageNo: 1,
        pageSize: 100,
        projectID: +params.projectId,
        withoutIssueSummary: true,
      });
    }
  });

  React.useEffect(() => {
    visible && testPlanId && getTestPlanItem(testPlanId);
    return () => {
      cleanTestPlan();
    };
  }, [visible, testPlanId, getTestPlanItem, cleanTestPlan]);

  const fieldsList: FormModalList = [
    {
      label: i18n.t('dop:plan name'),
      name: 'name',
      rules: [{ required: true, message: i18n.t('dop:please fill in the test plan name') }],
      initialValue: planItem.name,
      itemProps: {
        placeholder: i18n.t('dop:add test plan name'),
        maxLength: 50,
      },
    },
    {
      label: i18n.t('dop:principal'),
      name: 'ownerID',
      rules: [{ required: true, message: i18n.t('dop:please add a test leader') }],
      initialValue: planItem.ownerID,
      getComp: () => <MemberSelector allowClear={false} scopeType="project" scopeId={params.projectId} />,
    },
    {
      label: i18n.t('dop:participant'),
      name: 'partnerIDs',
      rules: [
        { required: true, message: i18n.t('dop:please add test participants') },
        {
          validator: (_rule: any, value: any, callback: Function) => {
            if (value && value.length > 60) {
              callback(i18n.t("dop:can't exceed 60 people"));
              return;
            }
            callback();
          },
        },
      ],
      initialValue: map(planItem.partnerIDs || [], (id) => id.toString()),
      config: {
        valuePropType: 'array',
      },
      getComp: () => <MemberSelector mode="multiple" scopeId={params.projectId} scopeType="project" />,
    },
    {
      label: i18n.t('dop:owned iteration'),
      name: 'iterationID',
      type: 'select',
      options: iterationList.map((iteration) => ({ name: iteration.title, value: String(iteration.id) })),
      itemProps: {
        placeholder: i18n.t('dop:select iteration'),
        allowClear: true,
      },
    },
  ];
  return (
    <FormModal
      title={
        mode === 'copy'
          ? i18n.t('dop:copy and create a new plan')
          : testPlanId
          ? i18n.t('dop:edit test plan')
          : i18n.t('dop:new test plan')
      }
      visible={visible}
      onOk={handleOk}
      modalProps={{
        destroyOnClose: true,
        confirmLoading: loading,
      }}
      onCancel={onCancel}
      fieldsList={fieldsList}
      formData={{ ...planItem, iterationID: String(planItem.iterationID || '') }}
    >
      {textInfo ? <div className="info">{textInfo}</div> : null}
    </FormModal>
  );
};

export type IPlanModal = Omit<IProps, 'onCancel'>;

export default TestPlanModal;
