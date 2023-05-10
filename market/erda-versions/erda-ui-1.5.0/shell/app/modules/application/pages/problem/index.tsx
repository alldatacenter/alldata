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
import { Button } from 'antd';
import { goTo } from 'common/utils';
import { useSwitch, useMultiFilter } from 'common/use-hooks';
import { ProblemList } from './problem-list';
import { ProblemForm } from './problem-form';
import i18n from 'i18n';
import problemStore from '../../stores/problem';
import routeInfoStore from 'core/stores/route';
import { useUnmount } from 'react-use';
import orgStore from 'app/org-home/stores/org';

export const problemTabs = () => {
  const openTotal = problemStore.useStore((s) => s.openTotal);
  return [
    {
      key: 'all',
      name: i18n.t('all'),
    },
    {
      key: 'open',
      name: (
        <span>
          {i18n.t('dop:pending')}
          <span className="dice-badge">{openTotal}</span>
        </span>
      ),
    },
    {
      key: 'closed',
      name: i18n.t('closed'),
    },
  ];
};

interface IProps {
  scope?: string;
}

const Ticket = ({ scope }: IProps) => {
  const [visible, openModal, closeModal] = useSwitch(false);
  const [params] = routeInfoStore.useStore((s) => [s.params]);
  const orgId = orgStore.getState((s) => s.currentOrg.id);
  const { ticketType: tabKey } = params;

  const { addTicket } = problemStore.effects;
  const { clearTicketList } = problemStore.reducers;

  const { getTicketList } = problemStore.effects;

  const multiFilterProps = useMultiFilter({
    getData: [getTicketList],
    extraQueryFunc: (activeGroup) => ({
      targetID: scope === 'org' ? orgId : +params.appId,
      targetType: scope === 'org' ? 'org' : 'application',
      status: activeGroup === 'all' ? undefined : activeGroup,
    }),
    shareQuery: true,
    multiGroupEnums: ['open', 'all', 'closed'],
    groupKey: 'ticketType',
    activeKeyInParam: true,
  });

  useUnmount(() => {
    clearTicketList();
  });

  const onOk = (payload: any) => {
    openModal();
    addTicket({
      ...payload,
      targetID: scope === 'org' ? String(orgId) : String(params.appId),
      targetType: scope === 'org' ? 'org' : 'application',
      status: tabKey,
    })
      .then(() => {
        if (tabKey === 'open') {
          multiFilterProps.fetchDataWithQuery(1);
        } else {
          goTo('../open');
        }
      })
      .finally(() => {
        closeModal();
      });
  };

  return (
    <div>
      <div className="top-button-group">
        <Button type="primary" onClick={() => openModal()}>
          {i18n.t('dop:add ticket')}
        </Button>
        <ProblemForm visible={visible} onOk={onOk} onCancel={closeModal} />
      </div>
      <ProblemList {...multiFilterProps} />
    </div>
  );
};

export default Ticket;
