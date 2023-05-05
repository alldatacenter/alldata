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
import { TreeSelect } from 'antd';
import { getVersionTree } from 'apiManagePlatform/services/api-market';
import routeInfoStore from 'core/stores/route';
import i18n from 'i18n';

interface IProps {
  onChangeVersion: (id: number) => void;
}

const formatVersion = (data: API_MARKET.VersionTreeItem[], versionID: number) => {
  let selectID: number | undefined;
  const treeData = data.map(({ versions, swaggerVersion }) => {
    return {
      value: swaggerVersion,
      title: swaggerVersion,
      disabled: true,
      children: versions.map(({ major, minor, id, deprecated }) => {
        if (versionID === id) {
          selectID = versionID;
        }
        return {
          value: id,
          title: `V ${major}.${minor}.* ${deprecated ? `(${i18n.t('deprecated')})` : ''}`,
          disabled: false,
        };
      }),
    };
  });
  return [treeData, selectID];
};

const SelectVersion = ({ onChangeVersion }: IProps) => {
  const [versionList, setVersionList] = React.useState<API_MARKET.VersionTreeItem[]>([]);
  const [selectVersionID, setSelectVersionID] = React.useState<number | undefined>();
  const { assetID, versionID } = routeInfoStore.useStore((s) => s.params);
  const getVersionList = React.useCallback(() => {
    if (assetID) {
      getVersionTree<Promise<API_MARKET.CommonResList<API_MARKET.VersionTreeItem[]>>>({
        assetID,
        patch: false,
        instantiation: false,
        access: false,
      }).then((res) => {
        if (res.success) {
          const list = res.data.list || [];
          setVersionList(list);
        }
      });
    }
  }, [assetID]);
  React.useEffect(() => {
    getVersionList();
  }, [getVersionList]);
  const handleChange = (id: number) => {
    setSelectVersionID(id);
    onChangeVersion(id);
  };
  const treeData = React.useMemo(() => {
    const [tree, id] = formatVersion(versionList, +versionID);
    setSelectVersionID(id);
    return tree;
  }, [versionList, versionID]);
  return (
    <TreeSelect
      value={selectVersionID}
      treeData={treeData}
      placeholder={i18n.t('default:please select version')}
      style={{ width: '100%' }}
      dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
      treeDefaultExpandAll
      onSelect={handleChange}
      onFocus={getVersionList}
    />
  );
};
export default SelectVersion;
