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

import i18n from 'i18n';
import { Button, Modal, Table, Tree } from 'antd';
import React, { useImperativeHandle } from 'react';
import { Avatar, TableActions, UserInfo } from 'common';
import { useUpdate } from 'common/use-hooks';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import { get } from 'lodash';
import { AntTreeNodeSelectedEvent, ColumnProps } from 'core/common/interface';
import moment from 'moment';
import ExportFile from 'apiManagePlatform/pages/api-market/components/export-file';
import { RelationMode } from 'apiManagePlatform/pages/api-market/components/relation';
import { UnityAuthWrap } from 'apiManagePlatform/components/auth-wrap';
import { goTo } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import { protocolMap } from 'apiManagePlatform/pages/api-market/components/config';

const formatVersionTree = (data: API_MARKET.VersionTreeItem[]) => {
  const tree = (data || []).map(({ versions, swaggerVersion }) => {
    return {
      title: swaggerVersion,
      key: swaggerVersion,
      isLeaf: false,
      selectable: false,
      children: versions.map(({ major, minor }) => {
        return {
          title: `${major}.${minor}.*`,
          key: `${swaggerVersion}-${major}.${minor}`,
          isLeaf: true,
          major,
          minor,
          swaggerVersion,
        };
      }),
    };
  });
  return tree;
};

export interface ChooseVersion {
  swaggerVersion: string;
  major: number;
  minor: number;
  selectedKeys: string[];
}

interface IProps {
  assetID: string;
  versionRef: React.Ref<any>;
  onRelation: (mode: RelationMode) => void;
  onSelectVersion?: (data: ChooseVersion) => void;
}

interface IState {
  showExport: boolean;
  versionItem: API_MARKET.AssetVersion;
  chooseVersionInfo: ChooseVersion;
  expandedKeys: string[];
}

const VersionInfo = ({ assetID, onRelation, onSelectVersion, versionRef }: IProps) => {
  const params = routeInfoStore.useStore((s) => s.params);
  const { getVersionTree, getListOfVersions, getInstance, deleteAssetVersion, updateAssetVersion } =
    apiMarketStore.effects;
  const [assetVersionList, versionTree, instance, assetDetail, instancePermission] = apiMarketStore.useStore((s) => [
    s.assetVersionList,
    s.versionTree,
    s.instance,
    s.assetDetail,
    s.instancePermission,
  ]);
  const creatorID = get(assetDetail, ['asset', 'creatorID']);
  const instanceUrl = get(instance, 'url');
  const [state, updater, update] = useUpdate<IState>({
    chooseVersionInfo: {},
    expandedKeys: [],
    versionItem: {},
    showExport: false,
  });
  const refreshVersionTree = React.useCallback(() => {
    if (!assetID) {
      return;
    }
    getVersionTree({ assetID, instantiation: false, patch: false, access: false }).then(({ list }) => {
      const swaggerVersion = get(list, ['0', 'swaggerVersion']);
      const major = get(list, ['0', 'versions', '0', 'major']);
      const minor = get(list, ['0', 'versions', '0', 'minor']);
      getListOfVersions({ assetID, major, minor, spec: false });
      getInstance({ swaggerVersion, assetID, minor, major });
      const temp = {
        major,
        minor,
        swaggerVersion,
        selectedKeys: [`${swaggerVersion}-${major}.${minor}`],
      };
      update({
        chooseVersionInfo: temp,
        expandedKeys: [swaggerVersion],
      });
      onSelectVersion && onSelectVersion(temp);
    });
  }, [assetID, getInstance, getListOfVersions, getVersionTree, onSelectVersion, update]);
  React.useEffect(() => {
    refreshVersionTree();
  }, [refreshVersionTree]);
  useImperativeHandle(versionRef, () => ({
    handleTreeSelect,
  }));
  const handleTreeSelect = ({
    swaggerVersion,
    major,
    minor,
  }: {
    swaggerVersion: string;
    major: number;
    minor: number;
  }) => {
    const temp = {
      major,
      minor,
      swaggerVersion,
      selectedKeys: [`${swaggerVersion}-${major}.${minor}`],
    };
    update({
      chooseVersionInfo: temp,
      expandedKeys: [...state.expandedKeys, swaggerVersion],
    });
  };
  const goToDetail = ({ id }: API_MARKET.AssetVersion) => {
    goTo(goTo.pages.apiManageAssetDetail, { assetID, versionID: id, scope: params.scope });
  };
  const closeModal = () => {
    updater.showExport(false);
  };
  const handleExport = (record: API_MARKET.AssetVersion) => {
    update({
      showExport: true,
      versionItem: record,
    });
  };
  const handleExpand = (expandedKeys: string[]) => {
    updater.expandedKeys(expandedKeys);
  };
  const handleSelectVersion = (selectedKeys: string[], { selected, node }: AntTreeNodeSelectedEvent) => {
    if (!selected) {
      return;
    }
    const { major, minor, swaggerVersion } = node.props;
    const temp = {
      major,
      minor,
      selectedKeys,
      swaggerVersion,
    };
    if (state.chooseVersionInfo.swaggerVersion !== swaggerVersion || state.chooseVersionInfo.minor !== minor) {
      getInstance({ assetID, major, minor, swaggerVersion });
    }
    updater.chooseVersionInfo(temp);
    onSelectVersion && onSelectVersion(temp);
    getListOfVersions({ assetID, major, minor, spec: false });
  };
  const handleDeleteVersion = ({ id }: API_MARKET.AssetVersion, e: React.MouseEvent<HTMLSpanElement>) => {
    e.stopPropagation();
    const { major, minor } = state.chooseVersionInfo;
    Modal.confirm({
      title: i18n.t('default:confirm to delete the current version?'),
      onOk: async () => {
        await deleteAssetVersion({ versionID: id, assetID });
        // 当前minor中还有patch版本
        if (assetVersionList.length > 1) {
          getListOfVersions({ assetID, major, minor, spec: false });
        } else {
          // 当前minor中patch全部删除
          // 刷新左侧版本树
          refreshVersionTree();
        }
      },
    });
  };
  const toggleDeprecated = (
    { id, deprecated, major, minor, patch, assetName }: API_MARKET.AssetVersion,
    e: React.MouseEvent<HTMLSpanElement>,
  ) => {
    e.stopPropagation();
    const name = `${assetName} ${major}.${minor}.${patch}`;
    let icon: string | undefined = 'warning';
    let title = i18n.t('deprecated version');
    let content = i18n.t('Are you sure you want to deprecate {name}?', { name });
    if (deprecated) {
      icon = undefined;
      title = i18n.t('revert deprecated version');
      content = i18n.t('Are you sure you want to revert the deprecated status of {name}?', { name });
    }
    Modal.confirm({
      title,
      content,
      icon,
      onOk: () => {
        updateAssetVersion({ assetID, versionID: id, deprecated: !deprecated }).then(() => {
          getListOfVersions({ assetID, major, minor, spec: false });
        });
      },
    });
  };
  const columns: Array<ColumnProps<API_MARKET.VersionItem>> = [
    {
      title: i18n.t('default:version number'),
      dataIndex: ['version', 'major'],
      width: 120,
      render: (_text, { version: { major, minor, patch } }) => `${major}.${minor}.${patch}`,
    },
    {
      title: i18n.t('API description document protocol'),
      dataIndex: ['version', 'specProtocol'],
      render: (text) => protocolMap[text].fullName,
    },
    {
      title: i18n.t('creator'),
      dataIndex: ['version', 'creatorID'],
      width: 120,
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
      width: 280,
      fixed: 'right',
      render: (_text, { version }) => (
        <TableActions>
          <span
            onClick={(e) => {
              handleExport(version, e);
            }}
          >
            {i18n.t('export')}
          </span>
          <UnityAuthWrap wrap={false} userID={creatorID} path={['apiMarket', 'deleteVersion']}>
            <span
              onClick={(e) => {
                handleDeleteVersion(version, e);
              }}
            >
              {i18n.t('delete')}
            </span>
          </UnityAuthWrap>
          <UnityAuthWrap wrap={false} userID={creatorID} path={['apiMarket', 'addVersion']}>
            <span
              onClick={(e) => {
                toggleDeprecated(version, e);
              }}
            >
              {version.deprecated ? i18n.t('revert deprecated version') : i18n.t('deprecated version')}
            </span>
          </UnityAuthWrap>
        </TableActions>
      ),
    },
  ];
  const treeData = React.useMemo(() => formatVersionTree(versionTree), [versionTree]);
  const handleRelation = () => {
    if (instancePermission.edit === false) {
      Modal.info({
        title: i18n.t(
          'The current version has been referenced by the management entry. Please dereference before editing.',
        ),
      });
      return;
    }
    onRelation('instance');
  };
  return (
    <div className="flex justify-between items-start content-wrap relative">
      <div className="left pr-4">
        <Tree
          blockNode
          defaultExpandParent
          selectedKeys={state.chooseVersionInfo.selectedKeys}
          expandedKeys={state.expandedKeys}
          treeData={treeData}
          onSelect={handleSelectVersion}
          onExpand={handleExpand}
        />
      </div>
      <div className="right flex-1 pl-4">
        <div className="flex justify-between items-center">
          <div className="title text-normal font-medium text-base my-3">{i18n.t('related instance')}</div>
          <UnityAuthWrap userID={creatorID} path={['apiMarket', 'relatedInstance']}>
            <Button onClick={handleRelation}>{i18n.t('edit')}</Button>
          </UnityAuthWrap>
        </div>
        {instance.type === 'dice' ? (
          <>
            <div className="text-desc instance-label">{i18n.t('service name')}</div>
            <div className="text-sub font-medium instance-name mb-3">{get(instance, 'serviceName', '-')}</div>
            <div className="text-desc instance-label">{i18n.t('msp:deployment branch')}</div>
            <div className="text-sub font-medium instance-name mb-3">{get(instance, 'runtimeName', '-')}</div>
          </>
        ) : null}
        <div className="text-desc instance-label">{i18n.t('related instance')}</div>
        <div className="text-sub font-medium instance-name mb-6">{instanceUrl || '-'}</div>
        <div className="title text-normal font-medium text-base mb-3">{i18n.t('version list')}</div>
        <Table<API_MARKET.VersionItem>
          rowKey={({ version: { major, minor, patch } }) => `${major}-${minor}-${patch}`}
          columns={columns}
          dataSource={assetVersionList}
          pagination={false}
          onRow={({ version }) => {
            return {
              onClick: () => {
                goToDetail(version);
              },
            };
          }}
          scroll={{ x: 800 }}
        />
      </div>
      <ExportFile
        visible={state.showExport}
        versionID={state.versionItem.id}
        assetID={state.versionItem.assetID}
        specProtocol={state.versionItem.specProtocol}
        onCancel={closeModal}
      />
    </div>
  );
};

export default React.forwardRef((props: Omit<IProps, 'versionRef'>, ref) => (
  <VersionInfo {...props} versionRef={ref} />
));
