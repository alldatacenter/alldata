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

import { isEmpty } from 'lodash';
import React from 'react';
import moment from 'moment';
import { Tooltip } from 'antd';
import { Avatar } from 'common';
import { usePerm } from 'app/user/common';
import GotoCommit from 'application/common/components/goto-commit';
import MarkdownEditor from 'common/components/markdown-editor';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import i18n from 'i18n';
import { useUserMap } from 'core/stores/userMap';
import releaseStore from 'app/modules/application/stores/release';

interface IProps {
  data?: RELEASE.detail;
  updateInfo: typeof releaseStore.effects.updateInfo;
}

const ReleaseDetailEdit = (props: IProps) => {
  const { data, updateInfo } = props;
  const userMap = useUserMap();

  const infoEditAuth = usePerm((s) => s.app.release.info.edit.pass);
  if (!data || isEmpty(data)) {
    return null;
  }
  const { labels = {}, version, userId, createdAt, orgId, applicationId, projectId } = data;
  const fieldsList = [
    {
      name: 'releaseId',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('version'),
      getComp: () => version ?? '--',
    },
    {
      label: i18n.t('dop:branch'),
      getComp: () => labels.gitBranch,
    },
    {
      label: i18n.t('dop:submit id'),
      getComp: () => (
        <GotoCommit commitId={labels.gitCommitId} projectId={projectId} appId={applicationId} length={6} />
      ),
      itemProps: {
        type: 'hidden',
      },
      showInfo: true,
    },
    {
      label: i18n.t('operator'),
      getComp: () => {
        const userInfo = userMap[userId];
        if (!userInfo) {
          return <span>--</span>;
        }
        const { nick, name } = userInfo;
        return (
          <span>
            <Avatar className="mr-1 mb-1" name={nick || name} />
            <Tooltip title={name || '暂无'}>{nick || '暂无'}</Tooltip>
          </span>
        );
      },
    },
    {
      label: i18n.t('create time'),
      getComp: () => moment(createdAt).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      label: i18n.t('description'),
      name: 'desc',
      viewType: 'markdown',
      getComp: () => <MarkdownEditor />,
    },
  ];
  const doUpdate = (values: any) => {
    updateInfo({ ...values, version, orgId, applicationId, projectId });
  };
  return (
    <div className="release-detail-page">
      <SectionInfoEdit hasAuth={infoEditAuth} data={data} fieldsList={fieldsList} updateInfo={doUpdate} />
    </div>
  );
};

export default ReleaseDetailEdit;
