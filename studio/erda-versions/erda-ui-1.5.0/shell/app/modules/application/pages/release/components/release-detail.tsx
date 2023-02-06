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
import ReleaseInfo from './release-detail-info';
import ReleaseEdit from './release-detail-edit';
import DetailYml from './release-detail-yml';
import i18n from 'i18n';
import { useUpdate } from 'common/use-hooks';
import { useEffectOnce } from 'react-use';
import { isEmpty } from 'lodash';
import releaseStore from 'application/stores/release';
import './release-detail.scss';
import { appMode } from 'application/common/config';
import appStore from 'application/stores/application';

interface IProps {
  releaseId: string;
  data?: RELEASE.detail;
}

const ReleaseDetail = ({ releaseId, data }: IProps) => {
  const { getReleaseDetail, updateInfo } = releaseStore.effects;
  const appDetail = appStore.useStore((s) => s.detail);
  const { clearReleaseDetail } = releaseStore.reducers;
  const [state, updater] = useUpdate({
    chosenMenu: 'edit',
  });
  const { chosenMenu } = state;

  const changeMenu = (e: any) => {
    updater.chosenMenu(e.key);
  };

  useEffectOnce(() => {
    if (isEmpty(data)) {
      getReleaseDetail(releaseId);
    }
    return () => clearReleaseDetail();
  });

  return (
    <div className="release-detail">
      <div className="release-detail-header">
        <div className="tab-menu">
          <Menu onClick={changeMenu} selectedKeys={[state.chosenMenu]} mode="horizontal">
            <Menu.Item key="edit">{i18n.t('dop:details')}</Menu.Item>
            <Menu.Item key="info">{i18n.t('dop:basic information')}</Menu.Item>
            {![appMode.MOBILE].includes(appDetail.mode) && <Menu.Item key="yml">dice.yml</Menu.Item>}
          </Menu>
        </div>
      </div>

      {chosenMenu === 'edit' && <ReleaseEdit data={data} updateInfo={updateInfo} />}
      {chosenMenu === 'info' && <ReleaseInfo data={data} />}
      {chosenMenu === 'yml' && <DetailYml releaseId={releaseId} />}
    </div>
  );
};

export default ReleaseDetail;
