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
import { Tooltip } from 'antd';
import { Icon as CustomIcon, IF } from 'common';
import { cutStr, fromNow } from 'common/utils';
import i18n from 'i18n';
import './release-item.scss';

interface IProps {
  data: RELEASE.detail;
  isActive?: boolean;
  onClick?: (o?: any) => any;
}

export const ReleaseItem = (props: IProps) => {
  const { data, isActive, onClick } = props;
  const { version, labels = {}, createdAt, releaseId, clusterName, applicationName, projectName } = data;
  const clickItem = () => {
    if (onClick) onClick();
  };
  const displayVersion = version || releaseId;
  const commitId = labels && labels.gitCommitId;
  return (
    <Tooltip
      title={
        <>
          <div className="sub-info">
            <IF check={clusterName}>{`${i18n.t('dop:owned cluster')}：${clusterName}`}</IF>
          </div>
          <div className="sub-info">
            <IF check={projectName}>{`${i18n.t('dop:owned project')}：${projectName}`}</IF>
          </div>
          <div className="sub-info">
            <IF check={applicationName}>{`${i18n.t('dop:owned application')}：${applicationName}`}</IF>
          </div>
        </>
      }
    >
      <div className={`release-item ${isActive ? 'active' : ''}`} onClick={clickItem}>
        <div className="title">
          <CustomIcon type="bb" />
          <Tooltip title={displayVersion}>
            <span className="nowrap">{displayVersion}</span>
          </Tooltip>
        </div>
        <div className="sub-info mb-3">
          <IF check={commitId}>
            <React.Fragment>
              <CustomIcon type="commit" />
              {cutStr(commitId || '', 6, { suffix: '' })}
            </React.Fragment>
          </IF>
          {<span className="time">{fromNow(createdAt)}</span>}
        </div>
      </div>
    </Tooltip>
  );
};
