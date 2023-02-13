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
import { FileEditor, Icon as CustomIcon } from 'common';
import { isEmpty } from 'lodash';
import './start-tip.scss';
import FileContainer from 'application/common/components/file-container';
import i18n from 'i18n';
import appStore from 'application/stores/application';
import { Tooltip } from 'antd';
import repoStore from 'application/stores/repo';

interface IProps {
  name: string;
  projectName: string;
  showCreateFile: boolean;
}
const repoIntro = ({ name: repoName, showCreateFile, gitRepoNew }: IProps) => {
  const { protocol } = window.location;
  const repo = `${protocol}//${gitRepoNew}`;
  const { changeMode } = repoStore.reducers;

  return (
    <div className="repo-start-tip">
      <div>
        <div className="section-title mt-0">
          {i18n.t('dop:new application')}
          {showCreateFile ? (
            <Tooltip title={i18n.t('dop:create new file')}>
              <CustomIcon type="xjym" onClick={() => changeMode({ addFile: true })} />
            </Tooltip>
          ) : null}
        </div>
        <div className="sub-title">1. {i18n.t('dop:clone application')}</div>
        <FileContainer name="clone.sh">
          <FileEditor fileExtension="sh" value={`git clone ${repo}`} readOnly />
        </FileContainer>
        <div className="sub-title">2. {i18n.t('dop:commit branch')}</div>
        <FileContainer name="push.sh">
          <FileEditor
            fileExtension="sh"
            value={`cd ${repoName}
touch readme.md
git add readme.md
git commit -m "add README"
git push -u origin master`}
            readOnly
          />
        </FileContainer>
      </div>
      <div>
        <div className="section-title mt-8">{i18n.t('dop:existing application')}</div>
        <FileContainer name="existing.sh">
          <FileEditor
            fileExtension="sh"
            value={`cd existing_repo
git remote add dice ${repo}
git push -u dice --all
git push -u dice --tags`}
            readOnly
          />
        </FileContainer>
      </div>
    </div>
  );
};

const StartTip = ({ showCreateFile = false }) => {
  const appDetail = appStore.getState((s) => s.detail);
  return isEmpty(appDetail) ? null : repoIntro({ showCreateFile, ...(appDetail as any) });
};

export default StartTip;
