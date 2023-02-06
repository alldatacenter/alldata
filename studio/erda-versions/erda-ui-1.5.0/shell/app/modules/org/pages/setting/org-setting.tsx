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
import { SettingTabs, ConfigLayout, MembersTable } from 'common';
import { goTo, insertWhen } from 'common/utils';
import orgStore from 'app/org-home/stores/org';
import NotifyGroup from 'application/pages/settings/components/app-notify/common-notify-group';
import memberStore from 'common/stores/org-member';
import BlockNetwork from 'org/pages/setting/block-network';
import { OrgInfo } from './org-info';
import NotifyChannel from './notice-channel';
import { OperationLogSetting } from './operation-log-setting';
import { MemberScope } from 'common/stores/member-scope';
import { MemberLabels } from './member-label';
import { Link } from 'react-router-dom';
import IssueFieldManage from '../projects/issue-field-manage';
import IssueTypeManage from '../projects/issue-type-manage';
import Announcement from 'org/pages/announcement';
import permStore from 'user/stores/permission';

import './org-setting.scss';

export const OrgSetting = () => {
  const orgId = orgStore.getState((s) => s.currentOrg.id);
  const orgPerm = permStore.getState((s) => s.org);

  const dataSource = [
    {
      groupTitle: i18n.t('dop:general settings'),
      groupKey: 'common',
      tabGroup: [
        {
          tabTitle: i18n.t('cmp:org info'),
          tabKey: 'orgInfo',
          content: <OrgInfo />,
        },
        {
          tabTitle: i18n.t('cmp:org member'),
          tabKey: 'orgMember',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('{name} member management', { name: i18n.t('organization') }),
                  desc: (
                    <div>
                      {i18n.t('For editing members, setting member roles and role permissions, please refer to')}
                      <Link to={goTo.resolve.perm({ scope: 'org' })} target="_blank">
                        {i18n.t('role permissions description')}
                      </Link>
                    </div>
                  ),
                  children: <MembersTable scopeKey={MemberScope.ORG} />,
                },
              ]}
            />
          ),
        },
        {
          tabTitle: i18n.t('member label'),
          tabKey: 'memberLabel',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('cmp:organization member label'),
                  children: <MemberLabels />,
                },
              ]}
            />
          ),
        },
        ...insertWhen(orgPerm.orgCenter.viewAnnouncement.pass, [
          {
            tabTitle: i18n.t('cmp:announcement management'),
            tabKey: 'announcement',
            content: <Announcement />,
          },
        ]),
      ],
    },
    {
      groupTitle: i18n.t('project'),
      groupKey: 'project',
      tabGroup: [
        {
          tabTitle: i18n.t('dop:joint issue type'),
          tabKey: 'issueType',
          content: <IssueTypeManage />,
        },
        {
          tabTitle: i18n.t('dop:issue custom fields'),
          tabKey: 'issueField',
          content: <IssueFieldManage />,
        },
      ],
    },
    {
      groupTitle: i18n.t('deploy'),
      groupKey: 'deploy',
      tabGroup: [
        {
          tabTitle: i18n.t('cmp:block network'),
          tabKey: 'block-network',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('cmp:block network'),
                  desc: i18n.t('cmp:precautions after network closure'),
                  children: <BlockNetwork />,
                },
              ]}
            />
          ),
        },
      ],
    },
    {
      groupTitle: i18n.t('log'),
      groupKey: 'log',
      tabGroup: [
        {
          tabTitle: i18n.t('cmp:audit log'),
          tabKey: 'operation log',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('cmp:audit log'),
                  desc: i18n.t('cmp:Clean up at 3am every day'),
                  children: <OperationLogSetting />,
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
          tabTitle: i18n.t('dop:notification group'),
          tabKey: 'notifyGroup',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('dop:organize notification groups to set up notifications'),
                  children: (
                    <NotifyGroup memberStore={memberStore} commonPayload={{ scopeType: 'org', scopeId: `${orgId}` }} />
                  ),
                },
              ]}
            />
          ),
        },
        {
          tabTitle: i18n.t('notification channel'),
          tabKey: 'notifyChannel',
          content: (
            <ConfigLayout
              sectionList={[
                {
                  title: i18n.t('Set up a notification channel to set a notification mode'),
                  desc: <div className="w-2/3 h-10">{i18n.t('notice-channel-desc')}</div>,
                  children: <NotifyChannel />,
                },
              ]}
            />
          ),
        },
      ],
    },
  ];

  return <SettingTabs className="org-settings-main" dataSource={dataSource} />;
};
