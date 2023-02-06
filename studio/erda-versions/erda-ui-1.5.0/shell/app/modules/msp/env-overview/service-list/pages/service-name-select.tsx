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
import { Dropdown, Menu } from 'antd';
import routeInfoStore from 'core/stores/route';
import monitorCommonStore from 'common/stores/monitorCommon';
import serviceAnalyticsStore from 'msp/stores/service-analytics';
import { getServiceList } from 'msp/services/service-analytics';
import { EmptyListHolder, ErdaIcon } from 'common';
import i18n from 'i18n';
import { useUnmount } from 'react-use';

export function ServiceNameSelect() {
  const globalTimeSelectSpan = monitorCommonStore.useStore((s) => s.globalTimeSelectSpan);
  const [serviceId, serviceName] = serviceAnalyticsStore.useStore((s) => [s.serviceId, s.serviceName]);
  const { startTimeMs, endTimeMs } = globalTimeSelectSpan?.range || {};
  const params = routeInfoStore.useStore((s) => s.params);
  const serverListData = getServiceList.useData();
  const { updateState } = serviceAnalyticsStore;
  const serviceList = serverListData?.data || [];

  const configServiceData = (key: string) => {
    const service = serviceList.filter((v: TOPOLOGY_SERVICE_ANALYZE.ServiceData) => v.service_id === key);
    const _serviceId = service[0]?.service_id || serviceList[0]?.service_id;
    const _serviceName = service[0]?.service_name || serviceList[0]?.service_name;
    const applicationId = service[0]?.application_id || serviceList[0]?.application_id;
    updateState({
      requestCompleted: true,
      serviceId: _serviceId ? window.decodeURIComponent(_serviceId) : '',
      serviceName: _serviceName,
      applicationId,
    });
  };

  React.useEffect(() => {
    getServiceList.fetch({ start: startTimeMs, end: endTimeMs, terminusKey: params?.terminusKey });
  }, [getServiceList, startTimeMs, endTimeMs]);

  React.useEffect(() => {
    if (serviceId) {
      configServiceData(serviceId);
    } else if (params?.serviceId) {
      configServiceData(window.decodeURIComponent(params?.serviceId));
    } else if (!serviceId && serviceList?.length > 0) {
      configServiceData(serviceId);
    }
  }, [params.serviceId, serviceId, serviceList, updateState]);

  useUnmount(() => {
    updateState({
      serviceId: '',
      serviceName: '',
      applicationId: '',
    });
  });

  const menu = React.useMemo(() => {
    const handleChangeService = ({ key }: { key: string }) => {
      configServiceData(key);
    };

    return (
      <Menu onClick={handleChangeService}>
        {serviceList.map((x) => (
          <Menu.Item
            key={x.service_id}
            className={`${serviceId === x.service_id ? 'bg-light-primary text-primary' : ''}`}
          >
            {x.service_name}
          </Menu.Item>
        ))}
        {!serviceList?.length && <EmptyListHolder />}
      </Menu>
    );
  }, [serviceId, serviceList]);

  return (
    <div className="flex items-center">
      <div className="font-bold text-lg">{i18n.t('msp:service monitor')}</div>
      {serviceName ? (
        <>
          <span className="bg-dark-2 mx-5 w-px h-3" />
          <Dropdown overlay={menu} trigger={['click']}>
            <div className="font-bold text-lg h-8 rounded border border-solid border-transparent flex justify-center cursor-pointer">
              <span className="self-center text-lg">{serviceName} </span>
              <ErdaIcon className="self-center" type="caret-down" size="16" />
            </div>
          </Dropdown>
        </>
      ) : null}
    </div>
  );
}
