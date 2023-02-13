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

import i18n from 'i18n';
import { regRules } from 'common/utils';

export enum ISSUE_FIELD_TYPES {
  Text = 'Text',
  Select = 'Select',
  MultiSelect = 'MultiSelect',
  Date = 'Date',
  Person = 'Person',
  Email = 'Email',
  Number = 'Number',
  URL = 'URL',
  Phone = 'Phone',
}

export const ISSUE_LIST_MAP = {
  requirement: {
    type: 'REQUIREMENT',
    name: i18n.t('dop:requirements'),
  },
  task: {
    type: 'TASK',
    name: i18n.t('task'),
  },
  bug: {
    type: 'BUG',
    name: i18n.t('bug'),
  },
  epic: {
    type: 'EPIC',
    name: i18n.t('dop:milestone'),
  },
};

export const FIELD_TYPE_ICON_MAP = {
  Text: {
    icon: 'text',
    name: 'Text',
    value: 'Text',
    component: 'input',
  },
  Number: {
    icon: 'shuzi',
    name: 'Number',
    value: 'Number',
    component: 'custom',
  },
  Select: {
    icon: 'angle-down-copy',
    name: 'Select',
    value: 'Select',
    component: 'select',
  },
  MultiSelect: {
    icon: 'duoxuan',
    name: 'MultiSelect',
    value: 'MultiSelect',
    component: 'select',
    mode: 'multiple',
  },
  Date: {
    icon: 'riqiqishu',
    name: 'Date',
    value: 'Date',
    component: 'datePicker',
  },
  Person: {
    icon: 'renyuan',
    name: 'Person',
    value: 'Person',
    component: 'custom',
  },
  URL: {
    icon: 'link',
    name: 'URL',
    value: 'URL',
    component: 'custom',
    rule: regRules.url,
  },
  Email: {
    icon: 'email',
    name: 'Email',
    value: 'Email',
    component: 'custom',
    rule: regRules.email,
  },
  Phone: {
    icon: 'phone2',
    name: 'Phone',
    value: 'Phone',
    component: 'custom',
    rule: regRules.mobile,
  },

  EPIC: {
    icon: 'bb1',
    color: 'primary',
    value: 'EPIC',
    name: i18n.t('dop:milestone'),
  },
  REQUIREMENT: {
    icon: 'xq1',
    color: 'palegreen',
    name: i18n.t('requirement'),
    value: 'REQUIREMENT',
  },
  TASK: {
    icon: 'rw1',
    color: 'darkcyan',
    name: i18n.t('task'),
    value: 'TASK',
  },
  BUG: {
    icon: 'bug',
    color: 'red',
    name: i18n.t('bug'),
    value: 'BUG',
  },
};

export const FIELD_WITH_OPTION = {
  Select: true,
  MultiSelect: true,
};

export const COMMON_FIELDS = [
  {
    propertyName: 'state',
    displayName: i18n.t('dop:state'),
    propertyType: 'Select',
  },
  {
    propertyName: 'priority',
    displayName: i18n.t('dop:priority'),
    propertyType: 'Select',
  },
  {
    propertyName: 'assignee',
    displayName: i18n.t('designated person'),
    propertyType: 'Person',
  },
  {
    propertyName: 'planFinishedAt',
    displayName: i18n.t('deadline'),
    propertyType: 'Date',
  },
  {
    propertyName: 'labels',
    displayName: i18n.t('tag'),
    propertyType: 'MultiSelect',
  },
];

const TASK_FIELDS = [
  ...COMMON_FIELDS,
  {
    propertyName: 'iterationID',
    displayName: i18n.t('dop:owned iteration'),
    propertyType: 'Select',
  },
  {
    propertyName: 'complexity',
    displayName: i18n.t('dop:complexity'),
    propertyType: 'Select',
  },
  {
    propertyName: 'taskType',
    displayName: i18n.t('task type'),
    propertyType: 'Select',
  },
  {
    propertyName: 'estimateTime',
    displayName: i18n.t('dop:EstimateTime'),
    propertyType: 'Text',
  },
  {
    propertyName: 'issueManHour',
    displayName: i18n.t('dop:Time tracking'),
    propertyType: 'Text',
  },
];

export const DEFAULT_ISSUE_FIELDS_MAP = {
  TASK: TASK_FIELDS,
  BUG: [
    ...COMMON_FIELDS,
    {
      propertyName: 'iterationID',
      displayName: i18n.t('dop:owned iteration'),
      propertyType: 'Select',
    },
    {
      propertyName: 'severity',
      displayName: i18n.t('dop:severity'),
      propertyType: 'Select',
    },
    {
      propertyName: 'owner',
      displayName: i18n.t('dop:responsible person'),
      propertyType: 'Person',
    },
    {
      propertyName: 'bugStage',
      displayName: i18n.t('dop:import source'),
      propertyType: 'Select',
    },
    {
      propertyName: 'estimateTime',
      displayName: i18n.t('dop:EstimateTime'),
      propertyType: 'Text',
    },
    {
      propertyName: 'issueManHour',
      displayName: i18n.t('dop:Time tracking'),
      propertyType: 'Text',
    },
  ],
  REQUIREMENT: [
    ...COMMON_FIELDS,
    {
      propertyName: 'iterationID',
      displayName: i18n.t('dop:owned iteration'),
      propertyType: 'Select',
    },
    {
      propertyName: 'complexity',
      displayName: i18n.t('dop:complexity'),
      propertyType: 'Select',
    },
    {
      propertyName: 'estimateTime',
      displayName: i18n.t('dop:EstimateTime'),
      propertyType: 'Text',
    },
    {
      propertyName: 'issueManHour',
      displayName: i18n.t('dop:Time tracking'),
      propertyType: 'Text',
    },
  ],
  EPIC: [
    ...COMMON_FIELDS,
    {
      propertyName: 'complexity',
      displayName: i18n.t('dop:complexity'),
      propertyType: 'Select',
    },
  ],
};

export const TASK_SP_FIELD = {
  displayName: i18n.t('task type'),
  propertyName: i18n.t('task type'),
  required: true,
  propertyType: 'Select',
  relatedIssue: [i18n.t('task')],
  isSpecialField: true,
  propertyIssueType: 'TASK',
};

export const BUG_SP_FIELD = {
  displayName: i18n.t('dop:import source'),
  propertyName: i18n.t('dop:import source'),
  required: true,
  propertyType: 'Select',
  relatedIssue: [i18n.t('bug')],
  isSpecialField: true,
  propertyIssueType: 'BUG',
};
