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
import { isEmpty, get } from 'lodash';
import { SettingTabs, ConfigLayout, MembersTable } from 'common';
import { AppInfo } from './components/app-info';
import { MergeDes } from './components/app-merge-description';
import NotifyGroup from './components/app-notify/common-notify-group';
import NotifyConfig from './components/app-notify/notify-config';
import { MobileConfig, PipelineConfig, DeployConfig } from './components/app-variable-config';
import VersionPushConfig from './components/app-version-push';
import LibraryImport from './components/app-library-reference';
import CertificateImport from './components/app-certificate-reference';
import { appMode } from 'application/common/config';
import memberStore from 'common/stores/application-member';
import i18n from 'i18n';
import { Link } from 'react-router-dom';
import { MemberScope } from 'common/stores/member-scope';
import BranchRule from 'project/common/components/branch-rule';
import { usePerm } from 'app/user/common';
import { goTo } from 'common/utils';
import './app-settings.scss';
import appStore from 'application/stores/application';
import routeInfoStore from 'core/stores/route';

const showMap = {
  [appMode.SERVICE]: ['common', 'work', 'repository', 'pipeline', 'deploy', 'notification'],
  [appMode.PROJECT_SERVICE]: ['common', 'work', 'repository', 'pipeline', 'notification'],
  [appMode.MOBILE]: ['common', 'work', 'repository', 'notification', 'deploy', 'more'],
  [appMode.LIBRARY]: ['common', 'work', 'repository', 'notification', 'deploy', 'more'],
  [appMode.BIGDATA]: ['common', 'work', 'repository', 'notification'],
  [appMode.ABILITY]: ['common', 'work', 'deploy', 'notification'],
};

export const PureAppSettings = () => {
  const appDetail = appStore.useStore((s) => s.detail);
  const params = routeInfoStore.useStore((s) => s.params);
  const branchRuleOperation = usePerm((s) => s.app.setting.branchRule.operation.pass);

  const memberTopContent = (
    <div className="member-table-top-content">
      <div className="title font-medium">{i18n.t('{name} member management', { name: i18n.t('application') })}</div>
      <div className="desc">
        {i18n.t('For editing members, setting member roles and role permissions, please refer to')}
        <Link to={goTo.resolve.perm({ scope: 'app' })} target="_blank">
          {i18n.t('role permissions description')}
        </Link>
      </div>
    </div>
  );

  const settingSource = [
    {
      groupTitle: i18n.t('dop:general settings'),
      groupKey: 'common',
      tabGroup: [
        {
          tabTitle: i18n.t('dop:application information'),
          tabKey: 'appInfo',
          content: <AppInfo />,
        },
        {
          tabTitle: i18n.t('dop:app member'),
          tabKey: 'appMember',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  children: <MembersTable topContent={memberTopContent} buttonInCard scopeKey={MemberScope.APP} />,
                },
                {
                  title: i18n.t('dop:view the project member'),
                  children: <MembersTable readOnly hideBatchOps hideRowSelect scopeKey={MemberScope.PROJECT} />,
                },
              ]}
            />
          ),
        },
      ],
    },
    {
      groupTitle: i18n.t('dop:repository'),
      groupKey: 'repository',
      tabGroup: [
        {
          tabTitle: i18n.t('dop:merge description'),
          tabKey: 'mrDesc',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:set the default merge request description template'),
                  children: <MergeDes />,
                },
              ]}
            />
          ),
        },
        {
          tabTitle: i18n.t('dop:branch rule'),
          tabKey: 'branchRule',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:application branch rule'),
                  desc: i18n.t('dop:application-branch-rule-desc'),
                  children: <BranchRule operationAuth={branchRuleOperation} scopeId={+params.appId} scopeType="app" />,
                },
              ]}
            />
          ),
        },
        // {
        //   tabTitle: i18n.t('dop:repository settings'),
        //   tabKey: 'repoSetting',
        //   content: (
        //     <ConfigLayout
        //       sectionList={[{
        //         title: i18n.t('dop:repository settings'),
        //         children: <RepoSetting appDetail={appDetail} />,
        //       }]}
        //     />
        //   ),
        // },
      ],
    },
    {
      groupTitle: i18n.t('pipeline'),
      groupKey: 'pipeline',
      tabGroup: [
        {
          tabTitle: i18n.t('dop:variable configuration'),
          tabKey: 'privateConfig',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:variable configuration'),
                  desc: i18n.t(
                    'dop:The same code can generate different artifacts by pipeline in different environments. Configure the environment here.',
                  ),
                  children: <PipelineConfig />,
                },
              ]}
            />
          ),
        },
      ],
    },
    {
      groupTitle: i18n.t('dop:deployment center'),
      groupKey: 'deploy',
      tabGroup: [
        {
          tabTitle: `${i18n.t('parameter setting')}`,
          tabKey: 'appConfig',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:variable-config'),
                  desc: i18n.t('dop:configure-deployment-environment'),
                  children: <DeployConfig />,
                },
              ]}
            />
          ),
        },
      ],
    },
    {
      groupTitle: i18n.t('dop:notification management'),
      groupKey: 'notification',
      tabGroup: [
        {
          tabTitle: i18n.t('notification'),
          tabKey: 'notifyConfig',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:help you better organize your notifications'),
                  children: (
                    <NotifyConfig
                      memberStore={memberStore}
                      commonPayload={{ scopeType: 'app', scopeId: params.appId, module: 'workbench' }}
                    />
                  ),
                },
              ]}
            />
          ),
        },
        {
          tabTitle: i18n.t('dop:notification group'),
          tabKey: 'notifyGroup',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:organize notification groups to set up notifications'),
                  children: (
                    <NotifyGroup
                      memberStore={memberStore}
                      commonPayload={{ scopeType: 'app', scopeId: params.appId }}
                    />
                  ),
                },
              ]}
            />
          ),
        },
      ],
    },
    {
      groupTitle: i18n.t('dop:more settings'), // 暂时只有库应用和移动应用才有
      groupKey: 'more',
      tabGroup: (() => {
        const list = [
          {
            tabTitle: i18n.t('dop:variable configuration'),
            tabKey: 'variableConfig',
            content: (
              <ConfigLayout
                sectionList={[
                  {
                    title: i18n.t('dop:variable configuration'),
                    desc: i18n.t(
                      'dop:The same code can generate different artifacts by pipeline in different environments. Configure the environment here.',
                    ),
                    children: <MobileConfig />,
                  },
                ]}
              />
            ),
          },
          {
            tabTitle: i18n.t('dop:version push'),
            tabKey: 'versionPush',
            content: (
              <ConfigLayout
                sectionList={[
                  {
                    title: i18n.t('dop:auto-push-after-relate'),
                    children: <VersionPushConfig />,
                  },
                ]}
              />
            ),
          },
        ];
        if ([appMode.MOBILE].includes(get(appDetail, 'mode'))) {
          list.push(
            {
              tabTitle: i18n.t('dop:subscribe module'),
              tabKey: 'libraryImport',
              content: (
                <ConfigLayout
                  sectionList={[
                    {
                      title: i18n.t('dop:refer-to-market'),
                      children: <LibraryImport />,
                    },
                  ]}
                />
              ),
            },
            {
              tabTitle: i18n.t('dop:reference certificate'),
              tabKey: 'certificateImport',
              content: (
                <ConfigLayout
                  sectionList={[
                    {
                      title: i18n.t('dop:apply-reference-certificate'),
                      children: <CertificateImport />,
                    },
                  ]}
                />
              ),
            },
          );
        }
        return list;
      })(),
    },
  ];
  let dataSource = settingSource;
  if (!isEmpty(appDetail)) {
    const { mode } = appDetail;
    dataSource = settingSource.filter((item) => showMap[mode]?.includes(item.groupKey));
  }

  return <SettingTabs className="app-settings-main" dataSource={dataSource} />;
};

export default PureAppSettings;
