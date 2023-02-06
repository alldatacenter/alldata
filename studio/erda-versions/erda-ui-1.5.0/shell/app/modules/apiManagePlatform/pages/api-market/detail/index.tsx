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
import routeInfoStore from 'core/stores/route';
import apiMarketStore from 'app/modules/apiManagePlatform/stores/api-market';
import { useLoading } from 'core/stores/loading';
import { message, Spin, Button, Modal, Table } from 'antd';
import layoutStore from 'layout/stores/layout';
import i18n from 'i18n';
import ApplyModal from 'apiManagePlatform/pages/api-market/components/apply-modal';
import ApiView from 'app/modules/apiManagePlatform/pages/api-market/detail/components/api-view';
import { OpenAPI } from 'openapi-types';
import { goTo } from 'common/utils';
import yaml from 'js-yaml';
import { ColumnProps } from 'core/common/interface';
import { Avatar, TableActions, UserInfo } from 'common';
import moment from 'moment';
import { exportSwagger } from 'apiManagePlatform/services/api-market';
import { map, pickBy } from 'lodash';
import { protocolMap } from 'apiManagePlatform/pages/api-market/components/config';
import './index.scss';

const ApiAssetDetail = () => {
  const [visible, setVisible] = React.useState(false);
  const [versionListVisible, setVersionListVisible] = React.useState(false);
  const { assetID, versionID } = routeInfoStore.useStore((s) => s.params);
  const [assetVersionDetail, assetDetail, hasAccess, version, assetVersionList] = apiMarketStore.useStore((s) => [
    s.assetVersionDetail.spec,
    s.assetVersionDetail.asset,
    s.assetVersionDetail.hasAccess,
    s.assetVersionDetail.version,
    s.assetVersionList,
  ]);
  const { getAssetVersionDetail, getListOfVersions } = apiMarketStore.effects;
  const { clearState } = apiMarketStore.reducers;
  const [isLoading, isFetchVersionList] = useLoading(apiMarketStore, ['getAssetVersionDetail', 'getListOfVersions']);
  React.useEffect(() => {
    if (assetID && versionID) {
      getAssetVersionDetail({ assetID, versionID, asset: true, spec: true });
    }
    return () => {
      clearState({ key: 'assetVersionDetail', value: { asset: {}, spec: {}, version: {}, access: {} } });
      clearState({ key: 'assetVersionList', value: [] });
    };
  }, [assetID, clearState, getAssetVersionDetail, versionID]);

  const currentVersion = React.useMemo((): API_MARKET.AssetVersionItem<OpenAPI.Document> => {
    if (!assetVersionDetail.spec) {
      return {} as API_MARKET.AssetVersionItem<OpenAPI.Document>;
    }
    let spec = {} as OpenAPI.Document;
    try {
      if (['oas2-yaml', 'oas3-yaml'].includes(assetVersionDetail.specProtocol)) {
        spec = yaml.load(assetVersionDetail.spec);
      } else if (['oas2-json', 'oas3-json'].includes(assetVersionDetail.specProtocol)) {
        spec = JSON.parse(assetVersionDetail.spec);
      }
    } catch (e) {
      message.error(i18n.t('default:failed to parse API description document'));
    }
    return {
      spec,
    };
  }, [assetVersionDetail.spec, assetVersionDetail.specProtocol]);

  const handleChangeVersion = (id: number) => {
    if (id) {
      goTo(`../${id}`);
    }
  };

  const handleApply = () => {
    setVisible(true);
  };

  const showVersionList = () => {
    getListOfVersions({ major: version.major, minor: version.minor, spec: false, assetID }).then(() => {
      setVersionListVisible(true);
    });
  };

  const handleExport = (specProtocol: API_MARKET.SpecProtocol, id: number) => {
    window.open(exportSwagger({ assetID, versionID: id, specProtocol }));
  };

  const renderExport = (id: number, redord: API_MARKET.VersionItem) => {
    const filterProtocolMap = pickBy(protocolMap, (_, protocol) => {
      const protocolPrefix = redord.version?.specProtocol?.substr(0, 4) || '';
      return protocol.indexOf(protocolPrefix) > -1;
    });
    return (
      <TableActions>
        {map(filterProtocolMap, ({ name }, key: API_MARKET.SpecProtocol) => {
          return (
            <span
              key={key}
              onClick={() => {
                handleExport(key, id);
              }}
            >
              {i18n.t('export {type}', { type: name })}
            </span>
          );
        })}
      </TableActions>
    );
  };

  const columns: Array<ColumnProps<API_MARKET.VersionItem>> = [
    {
      title: i18n.t('default:version number'),
      dataIndex: ['version', 'major'],
      width: 140,
      render: (_text, { version: { major, minor, patch } }) => `${major}.${minor}.${patch}`,
    },
    {
      title: i18n.t('API description document protocol'),
      dataIndex: ['version', 'specProtocol'],
      width: 200,
      render: (text) => protocolMap[text].fullName,
    },
    {
      title: i18n.t('creator'),
      dataIndex: ['version', 'creatorID'],
      width: 200,
      render: (text) => <Avatar showName name={<UserInfo id={text} />} />,
    },
    {
      title: i18n.t('create time'),
      dataIndex: ['version', 'createdAt'],
      width: 200,
      render: (text) => (text ? moment(text).format('YYYY-MM-DD HH:mm:ss') : ''),
    },
    {
      title: i18n.t('operate'),
      dataIndex: ['version', 'id'],
      width: 180,
      render: renderExport,
    },
  ];
  return (
    <div className="api-market-detail full-spin-height">
      <div className="top-button-group">
        <Button onClick={showVersionList}>{i18n.t('version list')}</Button>
        {hasAccess ? (
          <Button type="primary" onClick={handleApply}>
            {i18n.t('apply to call')}
          </Button>
        ) : null}
      </div>
      <Spin wrapperClassName="api-market-detail-loading" spinning={isLoading || isFetchVersionList}>
        <ApiView
          deprecated={version.deprecated}
          dataSource={currentVersion}
          onChangeVersion={handleChangeVersion}
          specProtocol={version.specProtocol}
        />
      </Spin>
      <ApplyModal
        visible={visible}
        onCancel={() => {
          setVisible(false);
        }}
        dataSource={assetDetail as API_MARKET.Asset}
      />
      <Modal
        width={960}
        title={`${i18n.t('version list')}(${version.major}.${version.minor}.*)`}
        visible={versionListVisible}
        onCancel={() => {
          setVersionListVisible(false);
        }}
        footer={null}
      >
        <Table
          rowKey={({ version: { major, minor, patch } }) => `${major}-${minor}-${patch}`}
          columns={columns}
          dataSource={assetVersionList}
          pagination={false}
          scroll={{ x: '100%' }}
        />
      </Modal>
    </div>
  );
};

export default ApiAssetDetail;
