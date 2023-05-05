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
import { useDebounce, useUnmount } from 'react-use';
import { CustomFilter, TableActions, UserInfo } from 'common';
import { useUpdate } from 'common/use-hooks';
import apiMarketStore from 'app/modules/apiManagePlatform/stores/api-market';
import { useLoading } from 'core/stores/loading';
import { Input, Button, Table, Tooltip } from 'antd';
import i18n from 'i18n';
import { goTo } from 'common/utils';
import AssetModal, { IMode, IScope } from 'app/modules/apiManagePlatform/pages/api-market/components/asset-modal';
import ApplyModal from 'apiManagePlatform/pages/api-market/components/apply-modal';
import { ColumnProps, PaginationProps } from 'core/common/interface';
import routeInfoStore from 'core/stores/route';
import moment from 'moment';
import './index.scss';

export const assetTabs: Array<{ key: API_MARKET.AssetScope; name: string }> = [
  {
    key: 'mine',
    name: i18n.t('my responsibility'),
  },
  {
    key: 'all',
    name: i18n.t('all'),
  },
];

interface IState {
  keyword: string;
  visible: boolean;
  showApplyModal: boolean;
  scope: IScope;
  mode: IMode;
  assetDetail: API_MARKET.Asset;
}

const commonQuery: API_MARKET.CommonQueryAssets = {
  hasProject: false,
  paging: true,
  latestVersion: true,
  latestSpec: false,
};

const ApiMarketList = () => {
  const [{ keyword, ...state }, updater, update] = useUpdate<IState>({
    keyword: '',
    visible: false,
    scope: 'asset',
    mode: 'add',
    assetDetail: {},
    showApplyModal: false,
  });
  const [assetList, assetListPaging] = apiMarketStore.useStore((s) => [s.assetList, s.assetListPaging]);
  const { scope } = routeInfoStore.useStore((s) => s.params) as { scope: API_MARKET.AssetScope };
  const { getAssetList } = apiMarketStore.effects;
  const { resetAssetList } = apiMarketStore.reducers;
  const [isFetchList] = useLoading(apiMarketStore, ['getAssetList']);
  useUnmount(() => {
    resetAssetList();
  });
  const getList = (params) => {
    getAssetList({
      ...commonQuery,
      ...params,
    });
  };
  useDebounce(
    () => {
      getList({ keyword, pageNo: 1, scope });
    },
    200,
    [keyword, scope],
  );

  const reload = () => {
    getList({ keyword, pageNo: 1, scope });
  };

  const filterConfig = React.useMemo(
    (): FilterItemConfig[] => [
      {
        type: Input.Search,
        name: 'keyword',
        customProps: {
          placeholder: i18n.t('default:search by keywords'),
          autoComplete: 'off',
        },
      },
    ],
    [],
  );

  const handleSearch = (query: Record<string, any>) => {
    updater.keyword(query.keyword);
  };

  const handleTableChange = ({ pageSize, current }: PaginationProps) => {
    getList({ keyword, pageNo: current, pageSize, scope });
  };

  const handleManage = (e: React.MouseEvent<HTMLSpanElement>, { assetID }: API_MARKET.Asset) => {
    e.stopPropagation();
    goTo(goTo.pages.apiManageAssetVersions, { scope, assetID });
  };

  const gotoVersion = ({ asset, latestVersion }: API_MARKET.AssetListItem) => {
    goTo(goTo.pages.apiManageAssetDetail, { assetID: asset.assetID, scope, versionID: latestVersion.id });
  };

  const handleApply = (e: React.MouseEvent<HTMLSpanElement>, record: API_MARKET.Asset) => {
    e.stopPropagation();
    update({
      showApplyModal: true,
      assetDetail: record || {},
    });
  };

  const closeModal = () => {
    update({
      visible: false,
      showApplyModal: false,
      assetDetail: {},
    });
  };

  const showAssetModal = (
    assetScope: IScope,
    mode: IMode,
    e?: React.MouseEvent<HTMLElement>,
    record?: API_MARKET.Asset,
  ) => {
    e && e.stopPropagation();
    update({
      scope: assetScope,
      mode,
      visible: true,
      assetDetail: record || {},
    });
  };

  const columns: Array<ColumnProps<API_MARKET.AssetListItem>> = [
    {
      title: i18n.t('API name'),
      dataIndex: ['asset', 'assetName'],
      width: 240,
    },
    {
      title: i18n.t('API description'),
      dataIndex: ['asset', 'desc'],
    },
    {
      title: 'API ID',
      dataIndex: ['asset', 'assetID'],
      width: 200,
    },
    {
      title: i18n.t('update time'),
      dataIndex: ['asset', 'updatedAt'],
      width: 200,
      render: (date) => moment(date).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('creator'),
      dataIndex: ['asset', 'creatorID'],
      width: 160,
      render: (text) => (
        <Tooltip title={<UserInfo id={text} />}>
          <UserInfo id={text} />
          <></>
        </Tooltip>
      ),
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'permission',
      width: 280,
      fixed: 'right',
      render: ({ manage, addVersion, hasAccess }: API_MARKET.AssetPermission, { asset }) => {
        return (
          <TableActions>
            {manage ? (
              <span
                onClick={(e) => {
                  handleManage(e, asset);
                }}
              >
                {i18n.t('manage')}
              </span>
            ) : null}
            {addVersion ? (
              <span
                onClick={(e) => {
                  showAssetModal('version', 'add', e, asset);
                }}
              >
                {i18n.t('add {name}', { name: i18n.t('version') })}
              </span>
            ) : null}
            {hasAccess ? (
              <span
                onClick={(e) => {
                  handleApply(e, asset);
                }}
              >
                {i18n.t('apply to call')}
              </span>
            ) : null}
          </TableActions>
        );
      },
    },
  ];

  return (
    <div className="api-market-list">
      <div className="top-button-group">
        <Button
          type="primary"
          onClick={() => {
            showAssetModal('asset', 'add');
          }}
        >
          {i18n.t('default:create resource')}
        </Button>
      </div>
      <CustomFilter config={filterConfig} onSubmit={handleSearch} />
      <Table
        rowKey="asset.assetID"
        columns={columns}
        dataSource={assetList}
        pagination={{
          ...assetListPaging,
          current: assetListPaging.pageNo,
        }}
        onRow={(record) => {
          return {
            onClick: () => {
              gotoVersion(record);
            },
          };
        }}
        onChange={handleTableChange}
        loading={isFetchList}
        scroll={{ x: 1300 }}
      />
      <AssetModal
        visible={state.visible}
        scope={state.scope}
        mode={state.mode}
        formData={state.assetDetail as API_MARKET.Asset}
        onCancel={closeModal}
        afterSubmit={reload}
      />
      <ApplyModal
        visible={state.showApplyModal}
        onCancel={closeModal}
        dataSource={state.assetDetail as API_MARKET.Asset}
      />
    </div>
  );
};

export default ApiMarketList;
