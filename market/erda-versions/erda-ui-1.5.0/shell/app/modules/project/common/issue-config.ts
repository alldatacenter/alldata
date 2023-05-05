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

import i18n, { isZh } from 'i18n';

export enum ISSUE_TYPE {
  ALL = 'ALL',
  EPIC = 'EPIC',
  REQUIREMENT = 'REQUIREMENT',
  TASK = 'TASK',
  BUG = 'BUG',
  TICKET = 'TICKET',
}

export const ISSUE_COMPLEXITY_MAP = {
  HARD: { value: 'HARD', label: i18n.t('dop:complex'), icon: 'fz3' },
  NORMAL: { value: 'NORMAL', label: i18n.t('medium'), icon: 'fz5' },
  EASY: { value: 'EASY', label: i18n.t('dop:easy'), icon: 'fz2' },
};

export const BUG_SEVERITY_MAP = {
  FATAL: { value: 'FATAL', label: `P0 ${i18n.t('dop:severity-fatal')}`, icon: 'yz5' },
  SERIOUS: { value: 'SERIOUS', label: `P1 ${i18n.t('dop:serious')}`, icon: 'yz4' },
  NORMAL: { value: 'NORMAL', label: `P2 ${i18n.t('dop:normal')}`, icon: 'yz3' },
  SLIGHT: { value: 'SLIGHT', label: `P3 ${i18n.t('dop:slight')}`, icon: 'yz2' },
  SUGGEST: { value: 'SUGGEST', label: `P4 ${i18n.t('dop:suggest')}`, icon: 'yz1' },
};

export const ISSUE_PRIORITY_MAP = {
  URGENT: { value: 'URGENT', label: i18n.t('dop:urgent'), icon: 'yx4' },
  HIGH: { value: 'HIGH', label: i18n.t('high'), icon: 'yx3' },
  NORMAL: { value: 'NORMAL', label: i18n.t('medium'), icon: 'yx2' },
  LOW: { value: 'LOW', label: i18n.t('low'), icon: 'yx1' },
};
export const ISSUE_PRIORITY_ICON_STYLE = { height: '20px', width: '20px', verticalAlign: 'sub' };
export const ISSUE_PRIORITY_LIST = Object.values(ISSUE_PRIORITY_MAP);

export const REQUIREMENT_PANEL_ICON = {
  OPEN: 'wks',
  WORKING: 'jxz1',
  TESTING: 'csz',
  DONE: 'yjs',
};

export const TASK_PANEL_ICON = {
  OPEN: 'wks',
  WORKING: 'jxz1',
  DONE: 'yjs',
};

export const ISSUE_BUTTON_STATE = {
  canOpen: { label: i18n.t('dop:open'), state: 'OPEN' },
  canDup: { label: i18n.t('dop:duplicated'), state: 'DUP' },
  canReOpen: { label: i18n.t('dop:reopen'), state: 'REOPEN' },
  canResolved: { label: i18n.t('dop:resolved'), state: 'RESOLVED' },
  canTesting: { label: i18n.t('dop:testing'), state: 'TESTING' },
  canWontfix: { label: i18n.t("dop:won't fix"), state: 'WONTFIX' },
  canWorking: { label: i18n.t('processing'), state: 'WORKING' },
  canClosed: { label: i18n.t('close'), state: 'CLOSED' },
  canDone: { label: i18n.t('dop:completed'), state: 'DONE' },
};

export const EDIT_PROPS = {
  [ISSUE_TYPE.REQUIREMENT]: {
    titlePlaceHolder: i18n.t('dop:input requirement name'),
    contentLabel: i18n.t('dop:requirement description'),
  },
  [ISSUE_TYPE.TASK]: {
    titlePlaceHolder: i18n.t('dop:input task name'),
    contentLabel: i18n.t('dop:task description'),
  },
  [ISSUE_TYPE.BUG]: {
    titlePlaceHolder: i18n.t('dop:input bug name'),
    contentLabel: i18n.t('dop:bug description'),
  },
  [ISSUE_TYPE.TICKET]: {
    titlePlaceHolder: i18n.t('dop:input ticket name'),
    contentLabel: i18n.t('dop:ticket description'),
    panelTitle: i18n.t('dop:related task'),
  },
  [ISSUE_TYPE.EPIC]: {
    titlePlaceHolder: i18n.t('dop:input milestone name'),
    contentLabel: i18n.t('dop:milestone description'),
  },
};

export enum ISSUE_OPTION {
  REQUIREMENT = 'REQUIREMENT',
  TASK = 'TASK',
  BUG = 'BUG',
}

export const templateMap = isZh()
  ? {
      [ISSUE_TYPE.REQUIREMENT]: `### 【用户故事/要解决的问题】*


### 【意向用户】*


### 【用户体验目标】*


### 【链接/参考】

`,
      [ISSUE_TYPE.TASK]: ``,
      [ISSUE_TYPE.BUG]: `### 【环境信息】


### 【缺陷描述】*


### 【重现步骤】


### 【实际结果】


### 【期望结果】*


### 【修复建议】

`,
    }
  : {
      [ISSUE_TYPE.REQUIREMENT]: `### [User story/problem to solve] *


### [Intended users] *


### [User experience Goals] *


### [Link/Reference]

`,
      [ISSUE_TYPE.TASK]: ``,
      [ISSUE_TYPE.BUG]: `### [Environment Information]


### [Defect Description] *


### [Reoccurrence Procedure]


### [Actual Result]


### [Expected Result] *


### [Repair suggestion]
`,
    };
