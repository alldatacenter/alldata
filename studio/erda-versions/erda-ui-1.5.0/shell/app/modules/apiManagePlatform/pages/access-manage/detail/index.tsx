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
import { Button, Modal, Spin } from 'antd';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import { useEffectOnce } from 'react-use';
import routeInfoStore from 'core/stores/route';
import { DetailsPanel, Icon as CustomIcon, Ellipsis } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { get, isEmpty } from 'lodash';
import { authenticationMap, authorizationMap } from 'apiManagePlatform/pages/access-manage/components/config';
import AuthorizationUser from 'apiManagePlatform/pages/access-manage/detail/authorization-user';
import Sla from 'apiManagePlatform/pages/access-manage/detail/sla';
import SlaEditor from 'apiManagePlatform/pages/access-manage/detail/sla-editor';
import { goTo } from 'common/utils';
import { envMap } from 'msp/config';
import { useLoading } from 'core/stores/loading';
import { WithAuth } from 'user/common';
import OverviewChart from 'apiManagePlatform/pages/access-manage/detail/overview-chart';

interface IState {
  visible: boolean;
}

const AccessDetail = () => {
  const [{ visible }, updater] = useUpdate<IState>({
    visible: false,
  });
  const [accessDetail] = apiAccessStore.useStore((s) => [s.accessDetail]);
  const permission = accessDetail.permission || {};
  const params = routeInfoStore.useStore((s) => s.params);
  const { getAccessDetail, getClient, deleteAccess } = apiAccessStore.effects;
  const { clearAccessDetail } = apiAccessStore.reducers;
  const [isFetch] = useLoading(apiAccessStore, ['getAccessDetail']);
  useEffectOnce(() => {
    if (params.accessID) {
      getAccessDetail({ accessID: +params.accessID }).then(({ access }) => {
        const { assetID, swaggerVersion } = access;
        getClient({ assetID, swaggerVersion, paging: true, pageNo: 1 });
      });
    }
    return () => {
      clearAccessDetail();
    };
  });
  const gotoDetail = () => {
    goTo(`../../access/edit/${accessDetail.access.id}`);
  };
  const apiStrategy = React.useMemo(() => {
    const { projectID, workspace, endpointID } = accessDetail.access;
    const { TenantGroupID } = accessDetail.tenantGroup;
    if (!(projectID && endpointID && workspace && TenantGroupID)) {
      return '';
    }
    return goTo.resolve.mspApiStrategy({
      projectId: projectID,
      env: workspace,
      tenantGroup: TenantGroupID,
      packageId: endpointID,
    });
  }, [accessDetail.access, accessDetail.tenantGroup]);
  const apiAssetUrl = React.useMemo(() => {
    const { assetID } = accessDetail.access;
    if (!assetID) {
      return '';
    }
    return goTo.resolve.apiManageAssetVersions({
      scope: 'all',
      assetID,
    });
  }, [accessDetail.access]);
  const handleDelete = () => {
    Modal.confirm({
      title: i18n.t('confirm to {action}', { action: i18n.t('delete') }),
      onOk: async () => {
        await deleteAccess({ accessID: accessDetail.access.id });
        goTo(goTo.pages.apiAccessManage);
      },
    });
  };

  const domains = React.useMemo(() => {
    if (isEmpty(accessDetail.access.bindDomain)) {
      return null;
    }
    return accessDetail.access.bindDomain.map((domain) => <Ellipsis key={domain} title={domain} />);
  }, [accessDetail.access.bindDomain]);

  const fields = {
    base: [
      {
        label: i18n.t('reference API'),
        value: (
          <a href={apiAssetUrl} target="_blank" rel="noreferrer noopener">
            {get(accessDetail, ['access', 'assetName'])}
          </a>
        ),
      },
      {
        label: i18n.t('resource version'),
        value: isEmpty(accessDetail.access)
          ? ''
          : `V${get(accessDetail, ['access', 'major'])}.${get(accessDetail, ['access', 'minor'])}.*`,
      },
      {
        label: i18n.t('entry domain'),
        value: domains,
      },
      {
        label: i18n.t('authentication method'),
        value: authenticationMap[get(accessDetail, ['access', 'authentication'])]?.name,
      },
      {
        label: i18n.t('authorization method'),
        value: authorizationMap[get(accessDetail, ['access', 'authorization'])]?.name,
      },
      {
        label: i18n.t('related project'),
        value: get(accessDetail, ['access', 'projectName']),
      },
      {
        label: i18n.t('related environment'),
        value: envMap[get(accessDetail, ['access', 'workspace'])],
      },
    ],
    api: [
      {
        label: i18n.t('API strategy'),
        value: apiStrategy ? (
          <a href={apiStrategy} target="_blank" rel="noopener noreferrer">
            {i18n.t('update strategy')}
          </a>
        ) : null,
      },
    ],
  };
  return (
    <Spin spinning={isFetch}>
      <div className="access-detail-page">
        <div className="top-button-group">
          <WithAuth pass={permission.delete || false} tipProps={{ placement: 'bottom' }}>
            <Button danger onClick={handleDelete}>
              {i18n.t('delete')}
            </Button>
          </WithAuth>
          <WithAuth pass={permission.edit || false}>
            <Button type="primary" onClick={gotoDetail}>
              {i18n.t('edit')}
            </Button>
          </WithAuth>
        </div>
        <DetailsPanel
          baseInfoConf={{
            title: i18n.t('basic information'),
            panelProps: {
              fields: fields.base,
            },
          }}
          linkList={[
            {
              key: 'client',
              linkProps: {
                title: i18n.t('client'),
                icon: <CustomIcon type="shouquanyonghu" color />,
              },
              getComp: () => (
                <AuthorizationUser
                  assetID={accessDetail.access?.assetID}
                  swaggerVersion={accessDetail.access?.swaggerVersion}
                />
              ),
            },
            {
              key: 'API strategy',
              linkProps: {
                title: i18n.t('API strategy'),
                icon: <CustomIcon type="apicelve" color />,
              },
              panelProps: {
                fields: fields.api,
              },
            },
            {
              key: 'SLA',
              titleProps: {
                operations: [
                  {
                    title: (
                      <WithAuth pass={permission.edit || false}>
                        <Button
                          onClick={() => {
                            updater.visible(true);
                          }}
                        >
                          {i18n.t('add {name}', { name: 'SLA' })}
                        </Button>
                      </WithAuth>
                    ),
                  },
                ],
              },
              linkProps: {
                title: 'SLA',
                icon: <CustomIcon type="SLA" color />,
              },
              getComp: () => <Sla />,
            },
          ]}
        >
          <OverviewChart
            queries={{
              workspace: accessDetail.access.workspace?.toLowerCase(),
              projectID: accessDetail.access.projectID,
              endpoint: accessDetail.access.endpointName,
            }}
          />
        </DetailsPanel>
      </div>
      <SlaEditor
        mode="add"
        visible={visible}
        onCancel={() => {
          updater.visible(false);
        }}
      />
    </Spin>
  );
};

export default AccessDetail;
