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
import { Spin, Button, Modal, Menu, Dropdown } from 'antd';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import { ossImg, goTo } from 'common/utils';
import { UserInfo, Icon as CustomIcon, DetailsPanel, Ellipsis, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { UnityAuthWrap } from 'apiManagePlatform/components/auth-wrap';
import AssetModal, { IScope, IMode } from 'apiManagePlatform/pages/api-market/components/asset-modal';
import RelationModal, { RelationMode } from 'apiManagePlatform/pages/api-market/components/relation';
import VersionInfo, { ChooseVersion } from 'apiManagePlatform/pages/api-market/version/version-info';
import { ClickParam } from 'core/common/interface';
import { get, pick } from 'lodash';
import moment from 'moment';
import i18n from 'i18n';
import './index.scss';

type KeyAuth = 'public' | 'private';

const confirmTips: { [key in KeyAuth]: {} } = {
  public: {
    title: i18n.t('confirm to {action}', { action: i18n.t('set as {type}', { type: i18n.t('public') }) }),
    content: i18n.t('All members of the organization can view'),
  },
  private: {
    title: i18n.t('confirm to {action}', { action: i18n.t('set as {type}', { type: i18n.t('private') }) }),
    content: i18n.t('Members under the project/application associated with the current API can view it.'),
  },
};

interface IState {
  scope: IScope;
  mode: IMode;
  visible: boolean;
  showRelation: boolean;
  relationMode: RelationMode;
  chooseVersion: ChooseVersion;
}
const ApiVersions = () => {
  const versionRef = React.useRef(null as any);
  const [assetDetail] = apiMarketStore.useStore((s) => [s.assetDetail]);
  const params = routeInfoStore.useStore((s) => s.params);
  const [isFetchDetail, isFetchList] = useLoading(apiMarketStore, ['getAssetDetail', 'getListOfVersions']);
  const { getAssetDetail, deleteAsset, getListOfVersions, editAsset, getVersionTree } = apiMarketStore.effects;
  const { resetVersionList, clearAssetDetail, clearVersionTree } = apiMarketStore.reducers;
  const { asset } = assetDetail;
  const creatorID = get(assetDetail, ['asset', 'creatorID']);
  const [state, updater, update] = useUpdate<IState>({
    visible: false,
    showRelation: false,
    scope: 'asset',
    mode: 'add',
    relationMode: 'asset',
    chooseVersion: {},
  });
  React.useEffect(() => {
    if (params.assetID) {
      getAssetDetail({ assetID: params.assetID }, true);
    }
    return () => {
      clearAssetDetail();
      resetVersionList();
      clearVersionTree();
    };
  }, [clearAssetDetail, clearVersionTree, getAssetDetail, params.assetID, resetVersionList]);
  const showAssetModal = (scope: IScope, mode: IMode) => {
    update({
      scope,
      mode,
      visible: true,
    });
  };
  const handleSelectVersion = React.useCallback(
    (data: ChooseVersion) => {
      updater.chooseVersion(data);
    },
    [updater],
  );
  const showRelation = (relationMode: RelationMode) => {
    update({
      showRelation: true,
      relationMode,
    });
  };
  const closeModal = () => {
    update({
      visible: false,
      showRelation: false,
    });
  };
  const handleDeleteAsset = () => {
    Modal.confirm({
      title: i18n.t('default:confirm to delete the current API resource?'),
      onOk: async () => {
        await deleteAsset({ assetID: params.assetID });
        goTo(goTo.pages.apiManageRoot);
      },
    });
  };
  const toggleAssetPublic = ({ key }: Merge<ClickParam, { key: KeyAuth }>) => {
    Modal.confirm({
      ...confirmTips[key],
      onOk: () => {
        const data = pick(asset, ['assetName', 'desc', 'logo', 'assetID']);
        editAsset({
          ...data,
          public: key === 'public',
        }).then(() => {
          getAssetDetail({ assetID: params.assetID });
        });
      },
    });
  };
  const reloadPage = React.useCallback(
    (data) => {
      if (state.scope === 'asset') {
        getAssetDetail({ assetID: params.assetID });
      } else if (state.scope === 'version') {
        getListOfVersions({ assetID: params.assetID, major: data.major, minor: data.minor, spec: false });
        getVersionTree({ assetID: params.assetID, patch: false, instantiation: false, access: false }).then(() => {
          versionRef.current.handleTreeSelect({
            swaggerVersion: data.swaggerVersion,
            major: data.major,
            minor: data.minor,
          });
        });
      }
    },
    [state.scope, getAssetDetail, params.assetID, getListOfVersions, getVersionTree],
  );
  const fields = React.useMemo(() => {
    const updatedAt = get(asset, 'updatedAt');
    return {
      base: [
        {
          label: i18n.t('API name'),
          value: <Ellipsis title={get(asset, 'assetName')} />,
        },
        {
          label: 'API ID',
          value: <Ellipsis title={get(asset, 'assetID')} />,
        },
        {
          label: i18n.t('API description'),
          value: <Ellipsis title={get(asset, 'desc', '-')} />,
        },
        {
          label: i18n.t('update time'),
          value: updatedAt && moment(updatedAt).format('YYYY-MM-DD HH:mm:ss'),
        },
        {
          label: i18n.t('creator'),
          value: <UserInfo id={get(asset, 'creatorID')} />,
        },
        {
          label: i18n.t('API logo'),
          value: (
            <span className="asset-logo">
              {asset.logo ? <img src={ossImg(asset.logo, { w: 100 })} alt="logo" /> : <CustomIcon color type="mrapi" />}
            </span>
          ),
        },
      ],
      relation: [
        {
          label: i18n.t('project name'),
          value: get(asset, 'projectName'),
        },
        {
          label: i18n.t('dop:app name'),
          value: get(asset, 'appName'),
        },
      ],
    };
  }, [asset]);
  const menu = (
    <Menu onClick={toggleAssetPublic}>
      <Menu.Item key="public">{i18n.t('public')}</Menu.Item>
      <Menu.Item key="private">{i18n.t('private')}</Menu.Item>
    </Menu>
  );
  return (
    <Spin spinning={isFetchList || isFetchDetail}>
      <div className="version-list">
        <div className="top-button-group">
          <UnityAuthWrap path={['apiMarket', 'delete']} userID={creatorID}>
            <Button danger onClick={handleDeleteAsset}>
              {i18n.t('delete')}
            </Button>
          </UnityAuthWrap>
          <UnityAuthWrap path={['apiMarket', 'publicAsset']} userID={creatorID}>
            <Dropdown overlay={menu}>
              <Button>
                <div className="flex items-center">
                  {asset.public ? i18n.t('public') : i18n.t('private')}
                  <ErdaIcon type="caret-down" size="18px" />
                </div>
              </Button>
            </Dropdown>
          </UnityAuthWrap>
          <UnityAuthWrap path={['apiMarket', 'edit']} userID={creatorID}>
            <Button
              type="primary"
              onClick={() => {
                showAssetModal('asset', 'edit');
              }}
            >
              {i18n.t('default:edit')}
            </Button>
          </UnityAuthWrap>
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
              key: 'connection relation',
              linkProps: {
                title: i18n.t('connection relation'),
                icon: <CustomIcon type="guanlianguanxi" color />,
              },
              titleProps: {
                operations: [
                  {
                    title: (
                      <UnityAuthWrap path={['apiMarket', 'relatedProjectOrApp']} userID={creatorID}>
                        <Button
                          onClick={() => {
                            showRelation('asset');
                          }}
                        >
                          {i18n.t('edit')}
                        </Button>
                      </UnityAuthWrap>
                    ),
                  },
                ],
              },
              panelProps: {
                fields: fields.relation,
              },
            },
            {
              key: 'version manage',
              linkProps: {
                title: i18n.t('default:version management'),
                icon: <CustomIcon type="banbenguanli" color />,
              },
              titleProps: {
                operations: [
                  {
                    title: (
                      <UnityAuthWrap path={['apiMarket', 'addVersion']} userID={creatorID}>
                        <Button
                          onClick={() => {
                            showAssetModal('version', 'add');
                          }}
                        >
                          {i18n.t('add {name}', { name: i18n.t('version') })}
                        </Button>
                      </UnityAuthWrap>
                    ),
                  },
                ],
              },
              crossLine: true,
              getComp: () => (
                <VersionInfo
                  ref={versionRef}
                  assetID={params.assetID}
                  onRelation={showRelation}
                  onSelectVersion={handleSelectVersion}
                />
              ),
            },
          ]}
        />
      </div>
      <AssetModal
        visible={state.visible}
        scope={state.scope}
        mode={state.mode}
        onCancel={closeModal}
        formData={asset}
        afterSubmit={reloadPage}
      />
      <RelationModal
        visible={state.showRelation}
        mode={state.relationMode}
        versionInfo={state.chooseVersion as ChooseVersion}
        onCancel={closeModal}
      />
    </Spin>
  );
};

export default ApiVersions;
