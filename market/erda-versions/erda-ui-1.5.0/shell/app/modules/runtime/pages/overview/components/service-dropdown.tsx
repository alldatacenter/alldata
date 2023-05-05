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
import { Dropdown, Menu, Modal, List } from 'antd';
import { Icon as CustomIcon, Copy, Ellipsis, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { map, isEmpty } from 'lodash';
import { notify, insertWhen } from 'common/utils';
import ServiceResourceModal from './resource-modal';
import i18n from 'i18n';
import runtimeStore from 'runtime/stores/runtime';
import { usePerm } from 'user/common';
import runtimeDomainStore from 'runtime/stores/domain';
import './service-dropdown.scss';

const MenuItem = Menu.Item;
const forbiddenStatuses = ['WAITING', 'DEPLOYING', 'CANCELING'];

interface IProps {
  service: RUNTIME_SERVICE.Detail;
  isEndpoint: boolean;
  name: string;
  deployStatus: RUNTIME.DeployStatus;
  openSlidePanel: (type: string, options?: any) => void;
  openDomainModalVisible: () => void;
  updateServicesConfig: (payload: RUNTIME_SERVICE.PreOverlay) => void;
}

const ServiceDropdown = (props: IProps) => {
  const { service, name, updateServicesConfig, openSlidePanel, openDomainModalVisible, isEndpoint, deployStatus } =
    props;
  const permMap = usePerm((s) => s.app);

  const {
    status,
    addrs,
    envs,
    deployments: { replicas },
  } = service;
  const runtimeDetail = runtimeStore.useStore((s) => s.runtimeDetail);
  const domainMap = runtimeDomainStore.useStore((s) => s.domainMap);

  const { id: runtimeId, extra } = runtimeDetail;
  const env = extra.workspace;

  const [state, updater] = useUpdate({
    resourceVisible: false,
    domainVisible: false,
  });

  const showModalInfo = (title: string, content: React.ReactNode) => {
    Modal.info({
      className: 'modal-info',
      title,
      content,
      width: 500,
      okText: i18n.t('close'),
    });
  };

  const saveServiceConfig = (data: RUNTIME_SERVICE.PreOverlayService) => {
    updateServicesConfig({ services: { [name]: data } });
  };

  const getServiceOps = () => {
    const isOpsForbidden = forbiddenStatuses.includes(deployStatus);
    const warningMsg = i18n.t('runtime:deploying, please operate later');
    const envKey = (env || '').toLowerCase();

    const vipContent = !isEmpty(addrs) ? (
      <List
        itemLayout="horizontal"
        dataSource={map(addrs, (addr) => ({ addr }))}
        renderItem={({ addr }: { addr: string }) => {
          return (
            <div className="flex justify-between items-center">
              <span className="mr-2 vip-addr flex-1 nowrap">
                <Ellipsis title={addr} />
              </span>
              <Copy selector=".cursor-copy">
                <span className="cursor-copy copy-icon" data-clipboard-text={addr}>
                  <ErdaIcon type="copy" />
                </span>
              </Copy>
            </div>
          );
        }}
      />
    ) : (
      i18n.t('runtime:internal address does not exist')
    );
    const envContent = (
      <List
        itemLayout="horizontal"
        className="env-list"
        dataSource={map(envs, (value, key) => (
          <span className="env-item">{`${key}: ${value}`}</span>
        ))}
        renderItem={(item: JSX.Element) => {
          return <Ellipsis>{item}</Ellipsis>;
        }}
      />
    );
    const hasDeployAuth = (permMap.runtime[`${envKey}DeployOperation`] || {}).pass;
    const hasConsoleAuth = (permMap.runtime[`${envKey}Console`] || {}).pass;
    const ops = [
      ...insertWhen(hasDeployAuth, [
        {
          title: i18n.t('runtime:scale out'),
          onClick: () => {
            isOpsForbidden ? notify('warning', warningMsg) : updater.resourceVisible(true);
          },
        },
      ]),
      {
        title: i18n.t('runtime:history'),
        onClick: () => openSlidePanel('record'),
      },
      {
        title: i18n.t('runtime:internal address'),
        onClick: () => showModalInfo(i18n.t('runtime:internal address'), vipContent),
      },
    ];
    envs &&
      ops.push({
        title: i18n.t('runtime:environment variable'),
        onClick: () => showModalInfo(i18n.t('runtime:environment variable'), envContent),
      });
    isEndpoint &&
      hasDeployAuth &&
      ops.push({
        title: i18n.t('runtime:manage domain'),
        onClick: () => {
          if (isOpsForbidden) {
            notify('warning', warningMsg);
            return;
          } else if (isEmpty(domainMap)) {
            notify('warning', i18n.t('runtime:please operate after successful deployment'));
            return;
          }
          openDomainModalVisible();
        },
      });
    if (status === 'Healthy' && !isOpsForbidden && replicas !== 0) {
      ops.push(
        ...[
          {
            title: i18n.t('runtime:view log'),
            onClick: () => openSlidePanel('log'),
          },
          {
            title: i18n.t('container monitor'),
            onClick: () => openSlidePanel('monitor'),
          },
          ...insertWhen(hasConsoleAuth, [
            {
              title: i18n.t('console'),
              onClick: () => openSlidePanel('terminal'),
            },
          ]),
        ],
      );
    }
    return ops;
  };

  const serviceOps = getServiceOps();
  const menu = (
    <Menu>
      {map(serviceOps, (op) => {
        return (
          <MenuItem key={op.title}>
            <a onClick={op.onClick}>{op.title}</a>
          </MenuItem>
        );
      })}
    </Menu>
  );
  return (
    <React.Fragment>
      <Dropdown
        overlay={menu}
        placement="bottomRight"
        trigger={['click']}
        getPopupContainer={(triggerNode) => triggerNode.parentNode || document.body}
      >
        <CustomIcon className="text-2xl hover-active" type="more" />
      </Dropdown>
      <ServiceResourceModal
        visible={state.resourceVisible}
        service={service}
        onOk={saveServiceConfig}
        onCancel={() => updater.resourceVisible(false)}
      />
    </React.Fragment>
  );
};

export default ServiceDropdown;
