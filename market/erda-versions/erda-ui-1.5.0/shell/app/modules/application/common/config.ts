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
import ywyy_svg from 'app/images/ywyy.svg';
import android_svg from 'app/images/android.svg';
import kyy_svg from 'app/images/kyy.svg';
import dsjyy_svg from 'app/images/dsjyy.svg';
import xmjyy_svg from 'app/images/xmjyy.svg';
import githubImg from 'app/images/resources/github.png';
import gitlabImg from 'app/images/resources/gitlab.png';
import codingImg from 'app/images/resources/coding.png';

export const appMode = {
  SERVICE: 'SERVICE',
  MOBILE: 'MOBILE',
  LIBRARY: 'LIBRARY',
  BIGDATA: 'BIGDATA',
  ABILITY: 'ABILITY',
  PROJECT_SERVICE: 'PROJECT_SERVICE',
};

export const modeOptions = [
  {
    name: i18n.t('dop:business app'),
    value: appMode.SERVICE,
    src: ywyy_svg,
    desc: i18n.t('dop:Business-app-intro', { keySeparator: '>' }),
    groupIndex: 1,
    disabled: false,
  },
  {
    name: i18n.t('dop:mobile app'),
    value: appMode.MOBILE,
    src: android_svg,
    desc: i18n.t(
      'dop:Mobile application is a mobile client developed on Android and iOS platforms, including the overall process of development, release and submit to the app store.',
      { keySeparator: '>' },
    ),
    groupIndex: 1,
    disabled: false,
  },
  {
    name: i18n.t('dop:library/module'),
    value: appMode.LIBRARY,
    src: kyy_svg,
    desc: i18n.t('dop:Library-app-intro', { keySeparator: '>' }),
    groupIndex: 1,
    disabled: false,
  },
  {
    name: i18n.t('dop:bigData app'),
    value: appMode.BIGDATA,
    src: dsjyy_svg,
    desc: 'big data app',
    groupIndex: 1,
    disabled: false,
  },
  {
    name: i18n.t('dop:ability app'),
    value: appMode.ABILITY,
    src: dsjyy_svg,
    desc: 'ability app',
    groupIndex: 1,
    disabled: false,
  },
  {
    name: i18n.t('dop:project level app'),
    value: appMode.PROJECT_SERVICE,
    src: xmjyy_svg,
    desc: i18n.t('dop:project-level-app-form-tip'),
    groupIndex: 1,
    disabled: false,
  },
];

export const approvalStatus = {
  pending: i18n.t('dop:approval pending'),
  approved: i18n.t('dop:approved'),
  denied: i18n.t('dop:denied'),
  cancel: i18n.t('cancel'),
};

export enum RepositoryMode {
  Internal = 'internal',
  General = 'general',
  GitLab = 'gitlab',
  GitHub = 'github',
  Coding = 'coding',
}

export const repositoriesTypes = {
  [RepositoryMode.Internal]: {
    name: i18n.t('dop:System built-in Git repository'),
    value: RepositoryMode.Internal,
    displayname: '',
    logo: githubImg,
    usable: true,
    desc: null,
  },
  [RepositoryMode.General]: {
    name: i18n.t('dop:external general Git repository'),
    value: RepositoryMode.General,
    displayname: 'Git',
    logo: githubImg,
    usable: true,
    desc: i18n.t(
      'dop:If you choose to configure an external code repository, the DevOps platform will no longer provide functions such as  code browsing, submission history viewing, branch management or merge requests. Other functions such as pipeline and deployment will not be affected.',
    ),
  },
  // 3.16 只做外置通用git仓库
  [RepositoryMode.GitLab]: {
    name: i18n.t('dop:connect to {type}', { type: 'GitLab' }),
    value: RepositoryMode.GitLab,
    displayname: 'GitLab',
    logo: gitlabImg,
    usable: false,
    desc: i18n.t(
      'dop:If you choose to configure an external code repository, the DevOps platform will no longer provide functions such as  code browsing, submission history viewing, branch management or merge requests. Other functions such as pipeline and deployment will not be affected.',
    ),
  },
  [RepositoryMode.GitHub]: {
    name: i18n.t('dop:connect to {type}', { type: 'GitHub' }),
    value: RepositoryMode.GitHub,
    displayname: 'GitHub',
    logo: githubImg,
    usable: false,
    desc: i18n.t(
      'dop:If you choose to configure an external code repository, the DevOps platform will no longer provide functions such as  code browsing, submission history viewing, branch management or merge requests. Other functions such as pipeline and deployment will not be affected.',
    ),
  },
  [RepositoryMode.Coding]: {
    name: i18n.t('dop:connect to Coding'),
    value: RepositoryMode.Coding,
    displayname: 'Coding',
    logo: codingImg,
    usable: false,
    desc: i18n.t(
      'dop:If you choose to configure an external code repository, the DevOps platform will no longer provide functions such as  code browsing, submission history viewing, branch management or merge requests. Other functions such as pipeline and deployment will not be affected.',
    ),
  },
};
