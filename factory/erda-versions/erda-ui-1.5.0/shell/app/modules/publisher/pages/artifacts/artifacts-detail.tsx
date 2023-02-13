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
import { Menu } from 'antd';
import { ArtifactsInfo } from './artifacts-info';
import VersionList from './version-list';
import i18n from 'i18n';
import { useUpdate } from 'common/use-hooks';
import Authenticate from '../authenticate';
import SafetyManage from '../safety-manage';
import Statistics from '../statistics';
import ErrorReport from '../error-report';

import './artifacts-detail.scss';
import { ArtifactsTypeMap } from './config';
import { map } from 'lodash';

interface IProps {
  artifactsId: string;
  data: PUBLISHER.IArtifacts;
}

const ArtifactsDetail = ({ data, artifactsId }: IProps) => {
  const [{ chosenTab }, updater] = useUpdate({
    chosenTab: '',
  });

  React.useEffect(() => {
    updater.chosenTab('info');
  }, [artifactsId, updater]);

  const changeMenu = (e: any) => {
    updater.chosenTab(e.key);
  };
  const TabCompMap = {
    info: <ArtifactsInfo data={data} />,
    version: <VersionList artifacts={data} />,
    certification: <Authenticate artifacts={data} />,
    safety: <SafetyManage artifacts={data} />,
    statistics: <Statistics artifacts={data} />,
    errorReport: <ErrorReport artifacts={data} />,
  };
  const isMobileApp = data.type === ArtifactsTypeMap.MOBILE.value;

  const menuText = [
    ['info', i18n.t('configuration information')],
    ['version', i18n.t('publisher:version content')],
  ];
  if (isMobileApp) {
    menuText.push(
      ...[
        ['certification', i18n.t('publisher:authenticate list')],
        ['safety', i18n.t('publisher:safety management')],
        ['statistics', i18n.t('publisher:statistics dashboard')],
        ['errorReport', i18n.t('publisher:error report')],
      ],
    );
  }

  return (
    <div className="artifacts-detail">
      <div className="artifacts-detail-header">
        <div className="tab-menu">
          <Menu onClick={changeMenu} selectedKeys={[chosenTab]} mode="horizontal">
            {map(menuText, ([key, text]) => (
              <Menu.Item key={key}>{text}</Menu.Item>
            ))}
          </Menu>
        </div>
      </div>
      <div
        className={`artifacts-content ${['statistics', 'errorReport'].includes(chosenTab) ? 'bg-layout' : ''}`}
        id="artifacts-content"
      >
        {TabCompMap[chosenTab] || null}
      </div>
    </div>
  );
};

export default ArtifactsDetail;
