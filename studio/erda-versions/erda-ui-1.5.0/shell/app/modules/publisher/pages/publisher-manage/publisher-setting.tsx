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
import { PublisherInfo } from './publisher-info';
import { useEffectOnce } from 'react-use';
import orgStore from 'app/org-home/stores/org';
import publisherStore from '../../stores/publisher';
import './publisher-setting.scss';

const PublisherSetting = () => {
  const { getPublisherDetail } = publisherStore.effects;
  const { clearPublisherDetail } = publisherStore.reducers;
  const data = publisherStore.useStore((s) => s.publisherDetail);
  const publisherId = orgStore.getState((s) => s.currentOrg.publisherId);

  const getDetail = () => {
    getPublisherDetail({ publisherId });
  };

  useEffectOnce(() => {
    getDetail();
    return () => {
      clearPublisherDetail();
    };
  });
  return <PublisherInfo data={data} getDetail={getDetail} />;
  // return (
  //   <SettingTabs
  //     className="publisher-settings-main"
  //     dataSource={[
  //       {
  //         tabTitle: i18n.t('basic information'),
  //         tabKey: 'publisherInfo',
  //         content: <PublisherInfo data={data} getDetail={getDetail} />,
  //       },
  //     ]}
  //   />
  // );
};

export default PublisherSetting;
