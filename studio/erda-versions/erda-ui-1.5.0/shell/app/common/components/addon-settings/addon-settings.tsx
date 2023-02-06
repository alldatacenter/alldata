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

import { Copy, ConfirmDelete, IF } from 'common';
import { useUpdate } from 'common/use-hooks';
import addonStore from 'common/stores/addon';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import { goTo } from 'common/utils';
import i18n from 'i18n';
import { map, isEmpty } from 'lodash';
import { Spin } from 'antd';
import React from 'react';
import './addon-settings.scss';
import { useMount } from 'react-use';

interface IProps {
  insId: string;
  addonConfig?: { config: Obj; canDel?: boolean; [k: string]: any };
  isFetching: boolean;
}

const filterMySqlConfig = (config: Obj) => {
  const keepMysqlKey = ['ADDON_HAS_ENCRIPY', 'MYSQL_HOST', 'MYSQL_PORT'];
  const reConfig = {};
  map(config, (v, k) => {
    if (keepMysqlKey.includes(k)) {
      reConfig[k] = v;
    }
  });
  return reConfig;
};

const PureAddonSettings = ({ insId, addonConfig, isFetching }: IProps) => {
  if (!addonConfig || isEmpty(addonConfig)) return null;

  const deleteAddon = () => {
    addonStore.deleteAddonIns(insId).then(() => {
      goTo(goTo.pages.dopRoot);
    });
  };
  const { config, canDel = false } = addonConfig;
  let reConfig = config;
  if (addonConfig.addonName === 'mysql') {
    reConfig = filterMySqlConfig(config);
  }
  return (
    <div className="addon-settings-panel">
      <Spin spinning={isFetching}>
        <div className="settings-detail">
          <div className="settings-params-header">
            <span>{i18n.t('common:service basic parameters')}</span>
          </div>
          <div className="settings-params-content">
            {map(reConfig, (v, k) => (
              <div key={k}>
                <div className="param-k nowrap">{k}</div>
                <IF check={v}>
                  <div className="param-v cursor-copy" data-clipboard-text={v}>
                    {v}
                    <span className="copy-tip">({i18n.t('click to copy')})</span>
                  </div>
                  <IF.ELSE />
                  <div className="param-v">***</div>
                </IF>
              </div>
            ))}
            <Copy selector=".cursor-copy" />
          </div>
        </div>
        <IF check={canDel}>
          <div className="settings-delete">
            <div className="settings-params-header">
              <span>{i18n.t('common:delete service')}</span>
            </div>
            <div className="settings-delete-content">
              <ConfirmDelete deleteItem={i18n.t('service')} onConfirm={deleteAddon} />
            </div>
          </div>
        </IF>
      </Spin>
    </div>
  );
};

const AddonSettings = () => {
  const insId = routeInfoStore.useStore((s) => s.params.insId);
  const [isFetching] = useLoading(addonStore, ['getAddonDetail']);
  const [{ addonConfig }, updater] = useUpdate({
    addonConfig: {},
  });

  useMount(() => {
    addonStore.getAddonDetail(insId, true).then((res) => {
      updater.addonConfig(res);
    });
  });

  return <PureAddonSettings insId={insId} addonConfig={addonConfig as ADDON.Instance} isFetching={isFetching} />;
};

AddonSettings.PureAddonSettings = PureAddonSettings;

export default AddonSettings;
