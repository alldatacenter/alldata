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

import { Badge } from 'common';
import React from 'react';
import issueWorkflowStore from 'project/stores/issue-workflow';
import i18n from 'i18n';

const { BadgeStatus } = Badge;

interface IProps {
  stateID?: number;
  stateName?: string;
  status?: string;
  className?: string;
}

export const issueMainStateMap = {
  EPIC: {
    OPEN: { stateName: i18n.t('dop:pending'), status: BadgeStatus.warning },
    WORKING: { stateName: i18n.t('processing'), status: BadgeStatus.processing },
    DONE: { stateName: i18n.t('dop:completed'), status: BadgeStatus.success },
  },
  TASK: {
    OPEN: { stateName: i18n.t('dop:pending'), status: BadgeStatus.warning },
    WORKING: { stateName: i18n.t('processing'), status: BadgeStatus.processing },
    DONE: { stateName: i18n.t('dop:completed'), status: BadgeStatus.success },
  },
  REQUIREMENT: {
    OPEN: { stateName: i18n.t('dop:pending'), status: BadgeStatus.warning },
    WORKING: { stateName: i18n.t('processing'), status: BadgeStatus.processing },
    DONE: { stateName: i18n.t('dop:completed'), status: BadgeStatus.success },
  },
  BUG: {
    OPEN: { stateName: i18n.t('dop:pending'), status: BadgeStatus.warning },
    WORKING: { stateName: i18n.t('processing'), status: BadgeStatus.processing },
    WONTFIX: { stateName: i18n.t("dop:won't fix"), status: BadgeStatus.default },
    REOPEN: { stateName: i18n.t('dop:reopen'), status: BadgeStatus.error },
    RESOLVED: { stateName: i18n.t('dop:resolved'), status: BadgeStatus.success },
    CLOSED: { stateName: i18n.t('closed'), status: BadgeStatus.success },
  },
};

export const ticketMainStateMap = {
  TICKET: {
    OPEN: { stateName: i18n.t('dop:pending'), status: BadgeStatus.warning },
    WORKING: { stateName: i18n.t('processing'), status: BadgeStatus.processing },
    WONTFIX: { stateName: i18n.t("dop:won't fix"), status: BadgeStatus.default },
    REOPEN: { stateName: i18n.t('dop:reopen'), status: BadgeStatus.error },
    RESOLVED: { stateName: i18n.t('dop:resolved'), status: BadgeStatus.success },
    CLOSED: { stateName: i18n.t('closed'), status: BadgeStatus.success },
  },
};

const IssueState = (props: IProps) => {
  const { stateID, stateName, status, className = '' } = props;
  const workflowStateList = issueWorkflowStore.useStore((s) => s.workflowStateList);
  const curState = workflowStateList.find((item) => item.stateID === stateID);
  let curStatus = status || BadgeStatus.default;
  let curName = stateName;
  const totalMainStateMap = { ...issueMainStateMap, ...ticketMainStateMap };
  const stateObj = curState && totalMainStateMap[curState.issueType]?.[curState.stateBelong];
  if (stateObj) {
    curStatus = stateObj.status;
    curName = curState?.stateName;
  }
  return <Badge status={curStatus} text={curName || ''} showDot={false} className={className} />;
};

export default IssueState;
