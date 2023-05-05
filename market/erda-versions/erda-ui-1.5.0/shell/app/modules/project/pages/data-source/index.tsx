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
import dopStore from 'dop/stores';
import addonStore from 'common/stores/addon';
import customAddonStore from 'project/stores/custom-addon';
import { Button, Tooltip } from 'antd';
import AddonModal from '../third-service/components/addon-modal';
import { EmptyHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import { AddonCardList } from 'addonPlatform/pages/common/components/addon-card-list';
import { isEmpty } from 'lodash';
import { Link } from 'react-router-dom';
import { goTo } from 'common/utils';
import routeInfoStore from 'core/stores/route';

const displayName = ['MySQL', 'Redis', 'Custom', 'AliCloud-Rds', 'AliCloud-Redis'];
export default function DataSourceManagement() {
  const addonList = dopStore.useStore((s) => s.addonList);
  const { projectId } = routeInfoStore.useStore((s) => s.params);
  const addonSpecList = customAddonStore
    .useStore((s) => s.addonList)
    .filter((item: any) => displayName.includes(item.displayName));
  const timer = React.useRef<any>(0);
  const [loadingAddons] = useLoading(dopStore, ['getDataSourceAddons']);
  const [state, updater, update, reset] = useUpdate({
    modalVisible: false,
    editData: null,
  });
  const { getDataSourceAddons } = dopStore;

  React.useEffect(() => {
    customAddonStore.getAddonsList();
    getDataSourceAddons({ displayName });
  }, [getDataSourceAddons]);

  const onEditAddon = (content: ADDON.Instance) => {
    const { instanceId } = content;
    addonStore.getAddonDetail(instanceId, true).then((detail: ADDON.Instance) => {
      update({
        modalVisible: true,
        editData: detail,
      });
    });
  };

  const loopAddonList = (id: string) => {
    let idExist = false;
    timer.current = setTimeout(() => {
      getDataSourceAddons({ displayName }).then((res) => {
        if (!isEmpty((res || []).find((addon) => addon.instanceId === id))) {
          idExist = true;
        }
        if (!idExist) {
          loopAddonList(id);
        } else if (timer.current) {
          clearTimeout(timer.current);
        }
      });
    }, 5000);
  };

  const handleOk = (values: any) => {
    const after = () => {
      getDataSourceAddons({ displayName });
      closeModal();
    };
    if (state.editData) {
      const { instanceId, projectId: currentProjectId, orgId } = state.editData;
      return customAddonStore
        .updateCustomAddonConfig({
          config: values.configs,
          instanceId,
          projectId: +currentProjectId,
          orgId,
        })
        .then(after);
    } else {
      const { addonName, name, plan, addonInstanceRoutingId, configs } = values;
      const newAddonType = addonSpecList.find((a) => a.addonName === addonName);
      // 添加租户 addon
      if (addonInstanceRoutingId) {
        const data = { name, addonInstanceRoutingId, configs };
        return customAddonStore.addTenantAddonIns(data).then((addonInstanceID) => {
          after();
          // 轮询addon list
          loopAddonList(addonInstanceID);
        });
      }
      if (newAddonType?.category === 'custom') {
        // 添加云或自定义 addon
        return customAddonStore.addCustomAddonIns(values).then(after);
      }
      // 添加普通 addon
      const data: Omit<CUSTOM_ADDON.AddDiceAddOns, 'projectId' | 'clusterName'> = {
        addons: {
          [name]: {
            plan,
            options: configs,
          },
        },
        workspace: values.workspace,
        shareScope: 'PROJECT',
      };
      return customAddonStore.addDiceAddonIns(data).then(() => {
        after();
      });
    }
  };

  const closeModal = () => {
    reset();
  };

  return (
    <>
      {addonList ? (
        <AddonCardList
          isFetching={loadingAddons}
          searchPlaceHolder={i18n.t('dop:filter by data source name')}
          searchProps={['name']}
          hideSearch
          showDataSourceSearch
          showDataSourceSelect
          addonList={addonList}
          onEitAddon={onEditAddon}
        />
      ) : (
        <EmptyHolder relative />
      )}
      <div className="top-button-group">
        <Tooltip
          title={
            <div>
              {i18n.t('dop:add-data-source-tip-1')}
              <Link to={goTo.resolve.projectService({ projectId })}>{i18n.t('dop:add-data-source-tip-2')}</Link>
            </div>
          }
          placement="left"
        >
          <Button type="primary">{i18n.t('dop:add data source')}</Button>
        </Tooltip>
      </div>
      <AddonModal
        category="DATA_SOURCE"
        editData={state.editData as ADDON.Instance | null}
        visible={state.modalVisible}
        addonInsList={addonList}
        addonSpecList={addonSpecList}
        onOk={handleOk}
        onCancel={closeModal}
      />
    </>
  );
}
