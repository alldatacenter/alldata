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

import getRuntimeRouter from 'runtime/router';
import i18n from 'i18n';
import { mrTabs } from './pages/repo/repo-mr';
import { problemTabs } from './pages/problem';
import { BRANCH_TABS } from './pages/repo/repo-branch';

function getAppRouter(): RouteConfigItem {
  return {
    path: 'apps/:appId',
    mark: 'application',
    breadcrumbName: '{appName}',
    routes: [
      {
        path: 'deploy',
        mark: 'deploy',
        breadcrumbName: i18n.t('dop:deployment center'),
        routes: [
          ...getRuntimeRouter(),
          {
            layout: { noWrapper: true },
            getComp: (cb) => cb(import('application/pages/deploy/deploy'), 'DeployWrap'),
          },
        ],
      },
      {
        path: 'ticket/:ticketType',
        breadcrumbName: i18n.t('dop:issues'),
        tabs: problemTabs,
        routes: [
          {
            layout: { fullHeight: true },
            getComp: (cb) => cb(import('application/pages/problem')),
          },
          {
            path: ':ticketId',
            breadcrumbName: i18n.t('dop:issue detail'),
            getComp: (cb) => cb(import('application/pages/problem/problem-detail')),
          },
        ],
      },
      {
        path: 'repo',
        mark: 'repo',
        breadcrumbName: i18n.t('dop:code'),
        pageName: i18n.t('dop:files'),
        routes: [
          {
            getComp: (cb) => cb(import('application/pages/repo/repo-tree')),
          },
          {
            path: 'tree/(.*)',
            mark: 'repoTree',
            breadcrumbName: i18n.t('dop:code'),
            getComp: (cb) => cb(import('application/pages/repo/repo-tree')),
          },
          {
            path: 'branches',
            tabs: BRANCH_TABS,
            breadcrumbName: i18n.t('dop:branch management'),
            routes: [
              {
                path: 'compare/:branches*',
                mark: 'repoCompare',
                breadcrumbName: i18n.t('dop:branch comparison'),
                getComp: (cb) => cb(import('application/pages/repo/branch-compare-detail'), 'BranchCompareDetail'),
              },
              {
                tabs: BRANCH_TABS,
                getComp: (cb) => cb(import('application/pages/repo/repo-branch')),
              },
            ],
          },
          {
            path: 'tags',
            tabs: BRANCH_TABS,
            breadcrumbName: i18n.t('dop:branch management'),
            routes: [
              {
                tabs: BRANCH_TABS,
                getComp: (cb) => cb(import('application/pages/repo/repo-tag')),
              },
            ],
          },
          {
            path: 'commit',
            routes: [
              {
                path: ':commitId',
                breadcrumbName: i18n.t('dop:commit details'),
                getComp: (cb) => cb(import('application/pages/repo/commit-detail')),
              },
            ],
          },
          {
            path: 'commits/(.*)', // commits后面可能有分支(包含/)，commit后面只有commitId
            breadcrumbName: i18n.t('dop:commit history'),
            getComp: (cb) => cb(import('application/pages/repo/repo-commit')),
          },
          {
            path: 'mr/:mrType',
            breadcrumbName: i18n.t('dop:merge requests'),
            tabs: mrTabs,
            routes: [
              {
                path: 'createMR',
                breadcrumbName: i18n.t('dop:new merge request'),
                getComp: (cb) => cb(import('application/pages/repo/repo-mr-creation'), 'RepoMRCreation'),
              },
              {
                path: ':mergeId',
                breadcrumbName: i18n.t('dop:merge request detail'),
                getComp: (cb) => cb(import('application/pages/repo/mr-detail')),
              },
              {
                tabs: mrTabs,
                getComp: (cb) => cb(import('application/pages/repo/repo-mr')),
              },
            ],
          },
          {
            path: 'backup',
            breadcrumbName: i18n.t('dop:repo backup'),
            getComp: (cb) => cb(import('app/modules/application/pages/repo/repo-backup')),
          },
        ],
      },
      {
        path: 'release',
        breadcrumbName: i18n.t('artifact management'),
        layout: { fullHeight: true, noWrapper: true },
        getComp: (cb) => cb(import('app/modules/application/pages/release/release-list')),
      },
      {
        path: 'apiDesign',
        mark: 'apiDesign',
        breadcrumbName: i18n.t('dop:API design'),
        routes: [
          {
            layout: { fullHeight: true },
            getComp: (cb) => cb(import('apiManagePlatform/pages/api-market/design')),
          },
        ],
      },
      {
        path: 'pipeline',
        mark: 'pipeline',
        breadcrumbName: i18n.t('pipeline'),
        pageName: i18n.t('pipeline'),
        getComp: (cb) => cb(import('application/pages/pipeline')),
        layout: { fullHeight: true },
      },
      {
        path: 'dataTask',
        mark: 'dataTask',
        pageName: `${i18n.t('dop:data task')}`,
        routes: [
          {
            path: ':pipelineID',
            breadcrumbName: `${i18n.t('dop:data task')}`,
            getComp: (cb) => cb(import('application/pages/build/dataTask'), 'DataTask'),
          },
          {
            getComp: (cb) => cb(import('application/pages/build/dataTask'), 'DataTask'),
          },
        ],
      },
      {
        path: 'dataModel',
        breadcrumbName: i18n.t('dop:data model'),
        routes: [
          {
            path: 'starChart/:filePath',
            breadcrumbName: i18n.t('dop:data model detail'),
            getComp: (cb) => cb(import('application/pages/data-model/model-star-chart'), 'ModelStarChart'),
          },
          {
            getComp: (cb) => cb(import('application/pages/data-model/data-model'), 'DataModel'),
          },
        ],
      },
      {
        path: 'dataMarket',
        breadcrumbName: i18n.t('dop:data market'),
        getComp: (cb) => cb(import('application/pages/data-market/data-market'), 'DataMarket'),
      },
      {
        path: 'test',
        routes: [
          {
            listKey: 'apps',
            breadcrumbName: i18n.t('dop:lists'),
            getComp: (cb) => cb(import('application/pages/test/test-list')),
          },
          {
            path: 'quality',
            breadcrumbName: i18n.t('dop:quality reports'),
            getComp: (cb) => cb(import('application/pages/quality'), 'CodeQualityWrap'),
          },
          {
            path: ':testId',
            breadcrumbName: i18n.t('dop:test detail'),
            getComp: (cb) => cb(import('application/pages/test/test-detail-container')),
          },
        ],
      },
      {
        path: 'setting',
        breadcrumbName: i18n.t('dop:application setting'),
        layout: { fullHeight: true },
        getComp: (cb) => cb(import('application/pages/settings/app-settings')),
      },
    ],
  };
}

export default getAppRouter;
