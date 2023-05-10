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

import { map, debounce } from 'lodash';
import React, { useEffect } from 'react';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import testPlanStore from 'project/stores/test-plan';
import { FormModal } from 'common';

const commonQuery = { status: ['DOING', 'PAUSE'], type: 'm' };
const PlanModal = () => {
  const [relatedPlans, relatedPlansPaging, planModalInfo] = testPlanStore.useStore((s) => [
    s.relatedPlans,
    s.relatedPlansPaging,
    s.planModalInfo,
  ]);
  const [confirmLoading] = useLoading(testPlanStore, ['addToPlanByPlanModal']);
  const [name, setName] = React.useState('');
  const { closePlanModal } = testPlanStore.reducers;
  const { getPlanRelateMe, addToPlanByPlanModal } = testPlanStore.effects;
  const { type } = planModalInfo;
  const visible = !!type;

  useEffect(() => {
    if (visible) {
      getPlanRelateMe({ ...commonQuery, pageNo: 1 });
    }
    return () => {
      setName('');
    };
  }, [getPlanRelateMe, visible]);

  const onOk = (values: any) => {
    addToPlanByPlanModal(values.testPlanId).then(() => {
      closePlanModal();
    });
  };

  const handlePopupScroll = (e: React.UIEvent<HTMLDivElement>) => {
    e.persist();
    const { target } = e;
    // @ts-ignore
    const { scrollTop, offsetHeight, scrollHeight } = target;
    if (scrollTop + offsetHeight >= scrollHeight && relatedPlansPaging.hasMore) {
      getPlanRelateMe({ ...commonQuery, pageNo: relatedPlansPaging.pageNo + 1, name });
    }
  };

  const handleSearch = debounce((value: string) => {
    setName(value);
    getPlanRelateMe({ ...commonQuery, pageNo: 1, name: value });
  }, 1000);

  const fieldsList = [
    {
      label: i18n.t('dop:test plan'),
      name: 'testPlanId',
      type: 'select',
      options: map(relatedPlans, ({ id: value, name: planName }) => ({ value, name: planName })),
      itemProps: {
        placeholder: i18n.t('dop:Search for the test plan I am involved in/responsible for.'),
        onSearch: handleSearch,
        onPopupScroll: handlePopupScroll,
        showSearch: true,
        filterOption: false,
      },
    },
  ];

  return (
    <FormModal
      visible={visible}
      title={i18n.t('dop:add to test plan')}
      fieldsList={fieldsList}
      onCancel={closePlanModal}
      onOk={onOk}
      confirmLoading={confirmLoading}
      destroyOnClose
    />
  );
};

export default PlanModal;
