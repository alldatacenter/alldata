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
import i18n from 'i18n';
import { Icon as CustomIcon } from 'common';
import permStore from 'user/stores/permission';
import { Tooltip } from 'antd';

export const ITERATION_DETAIL_TABS = (params: Obj) => {
  const { breadcrumbInfoMap } = params;
  const iterationName = breadcrumbInfoMap?.iterationName;
  const projectPerm = permStore.useStore((s) => s.project);
  return [
    {
      key: '../',
      hrefType: 'back',
      name: (
        <span>
          <CustomIcon type="back" />
          {i18n.t('dop:iteration')}
          {iterationName ? (
            iterationName.length > 8 ? (
              <Tooltip title={iterationName} placement="topLeft">
                ({iterationName.slice(0, 8)}...)
              </Tooltip>
            ) : (
              `(${iterationName})`
            )
          ) : (
            ''
          )}
        </span>
      ),
      show: projectPerm.iteration.read.pass,
    },
    {
      key: 'all',
      name: i18n.t('dop:issue list'),
      show: [projectPerm.requirement.read.pass, projectPerm.task.read.pass, projectPerm.bug.read.pass].some((k) => k),
    },
    {
      key: 'gantt',
      name: i18n.t('dop:gantt chart'),
    },
    {
      key: 'board',
      name: i18n.t('dop:board'),
    },
  ];
};

export const AUTO_TEST_SPACE_TABS = (params: Obj) => {
  const { breadcrumbInfoMap } = params;
  const autoTestSpaceName = breadcrumbInfoMap?.autoTestSpaceName;
  return [
    {
      key: '../',
      hrefType: 'back',
      name: (
        <span>
          <CustomIcon type="back" />
          {i18n.t('dop:test space')}
          {autoTestSpaceName ? `(${autoTestSpaceName})` : ''}
        </span>
      ),
    },
    {
      key: 'apis',
      name: i18n.t('dop:APIs'),
    },
    {
      key: 'scenes',
      name: i18n.t('dop:Scenes'),
    },
  ];
};

export const COLLABORATE_TABS = () => {
  const projectPerm = permStore.useStore((s) => s.project);

  return [
    {
      key: 'milestone',
      name: i18n.t('dop:milestone'),
      show: projectPerm.epic.read.pass,
    },
    {
      key: 'backlog',
      name: i18n.t('dop:backlog'),
      show: projectPerm.backLog.viewBackLog.pass,
    },

    {
      key: 'iteration',
      name: i18n.t('dop:sprint'),
      show: projectPerm.iteration.read.pass,
    },
    {
      key: 'all',
      name: i18n.t('dop:issue list'),
      show: [projectPerm.requirement.read.pass, projectPerm.task.read.pass, projectPerm.bug.read.pass].some((k) => k),
    },
    {
      key: 'gantt',
      name: i18n.t('dop:gantt chart'),
      show: projectPerm.requirement.read.pass,
    },
    {
      key: 'board',
      name: i18n.t('dop:board'),
      show: projectPerm.requirement.read.pass,
    },
  ];
};

export const MEASURE_TABS = [
  {
    key: 'task',
    name: i18n.t('requirement & task'),
  },
  {
    key: 'bug',
    name: i18n.t('bug'),
  },
];

export const MANUAL_TEST_TABS = [
  {
    key: 'testCase',
    name: i18n.t('dop:test case'),
  },
  {
    key: 'testPlan',
    name: i18n.t('dop:test plan'),
  },
  {
    key: 'testEnv',
    name: i18n.t('dop:parameter configuration'),
  },
];

export const TEST_STATISTICS_TABS = [
  {
    key: 'test-dashboard',
    name: i18n.t('dop:test statistics'),
  },
  {
    key: 'code-coverage',
    name: i18n.t('dop:code coverage statistics'),
  },
];

export const AUTO_TEST_TABS = [
  {
    key: 'testCase',
    name: i18n.t('dop:test case'),
  },
  {
    key: 'config-sheet',
    name: i18n.t('dop:config data'),
  },
  {
    key: 'testPlan',
    name: i18n.t('dop:test plan'),
  },
  {
    key: 'data-source',
    name: i18n.t('dop:data sources'),
  },

  {
    key: 'testEnv',
    name: i18n.t('dop:parameter configuration'),
  },
];
