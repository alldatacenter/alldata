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

import AddonSettings from 'common/components/addon-settings';
import { useLoading } from 'core/stores/loading';
import { Copy, SettingTabs } from 'common';
import { isZh } from 'i18n';
import React from 'react';
import { useEffectOnce } from 'react-use';
import mspInfoStore from '../../stores/info';
import routeInfoStore from 'core/stores/route';

const { PureAddonSettings } = AddonSettings;

const MSComponentInfo = () => {
  const infoList = mspInfoStore.useStore((s) => s.infoList);
  const { getMSComponentInfo } = mspInfoStore.effects;
  const { clearMSComponentInfo } = mspInfoStore.reducers;
  const [loading] = useLoading(mspInfoStore, ['getMSComponentInfo']);
  const insId = routeInfoStore.useStore((s) => s.params.insId);

  useEffectOnce(() => {
    getMSComponentInfo();
    return clearMSComponentInfo;
  });

  const dataSource = infoList.map((info) => {
    return {
      tabTitle: isZh() ? info.cnName : info.enName,
      tabKey: info.addonName,
      content: <PureAddonSettings insId={insId} addonConfig={info} isFetching={loading} />,
    };
  });
  return (
    <>
      <SettingTabs dataSource={dataSource} />
      <Copy selector=".cursor-copy" />
    </>
  );
};

export default MSComponentInfo;
