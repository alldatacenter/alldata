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
import { Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Input, Menu, Dropdown, Tooltip, Modal } from 'antd';
import i18n from 'i18n';
import './index.scss';
import { map } from 'lodash';
import { API_TREE_OPERATION } from 'app/modules/apiManagePlatform/configs.ts';
import routeInfoStore from 'core/stores/route';

const NODE_OPERATIONS = [
  {
    key: 'rename',
    label: i18n.t('dop:rename'),
  },
  {
    key: 'delete',
    label: i18n.t('delete'),
  },
];

interface ITreeTitle {
  popToggle: (val: boolean) => void;
  name: string;
  icon?: string;
  readOnly?: string;
  inode?: string;
  pinode?: string;
  execOperation: (key: string, extraParam?: Obj) => void;
}
export const TreeTitle = ({
  popToggle,
  name,
  inode,
  readOnly,
  icon = 'yizhanzhifubaoxiaochengxu-',
  execOperation,
}: ITreeTitle) => {
  const [{ inputVisible, apiDocName }, updater] = useUpdate({
    inputVisible: false,
    apiDocName: name,
  });

  const { inode: inodeQuery } = routeInfoStore.useStore((s) => s.query);
  const dropDownRef = React.useRef(null);

  const operationHandle = (e: any, targetBranch: any) => {
    // eslint-disable-next-line no-unused-expressions
    e?.domEvent?.stopPropagation();

    if (e.key === API_TREE_OPERATION.rename) {
      updater.inputVisible(true);
    } else if (e.key === API_TREE_OPERATION.delete) {
      popToggle(false);
      Modal.confirm({
        title: `${i18n.t('common:confirm to delete')}?`,
        onOk: () => {
          execOperation(e.key, { name: targetBranch?.name });
        },
      });
    }
  };

  const onChangeName = (e: any) => {
    e.stopPropagation();
    updater.apiDocName(e.target.value);
  };
  const saveName = (e: any) => {
    e.stopPropagation();
    execOperation(API_TREE_OPERATION.rename, { name: apiDocName });
    popToggle(false);
  };

  const operations = (
    <Menu>
      {map(NODE_OPERATIONS, ({ key, label }) => {
        return inodeQuery === inode ? undefined : (
          <Menu.Item key={key} onClick={operationHandle}>
            {label}
          </Menu.Item>
        );
      })}
    </Menu>
  );

  const canOperate = React.useMemo(() => {
    return !inputVisible && inodeQuery !== inode && !readOnly;
  }, [inode, inodeQuery, inputVisible, readOnly]);

  return (
    <div className="api-tree-title flex justify-between items-center" ref={dropDownRef}>
      <div className="flex justify-between items-center">
        <CustomIcon type={icon} className="text-sub" />
        {inputVisible ? (
          <>
            <Input
              value={apiDocName}
              style={{ width: '180px', marginRight: '8px' }}
              onChange={onChangeName}
              onBlur={() => {
                setTimeout(() => {
                  updater.inputVisible(false);
                  updater.apiDocName(name);
                }, 200);
              }}
              onClick={(e) => e.stopPropagation()}
            />
            <CustomIcon className="mr-0" type="duigou" onClick={saveName} />
          </>
        ) : (
          <Tooltip title={name}>
            <div className="nowrap" style={{ maxWidth: '120px' }}>
              {name}
            </div>
          </Tooltip>
        )}
      </div>
      {canOperate && (
        <Dropdown overlay={operations} trigger={['hover']} getPopupContainer={() => dropDownRef && dropDownRef.current}>
          <CustomIcon type="gd" onClick={(e) => e.stopPropagation()} />
        </Dropdown>
      )}
    </div>
  );
};

export const BranchTitle = ({ name, icon = 'branch' }: { name: string; icon?: string }) => {
  return (
    <div className="flex items-center flex-wrap justify-start">
      <CustomIcon type={icon} className="text-sub" />
      <Tooltip title={name}>
        <div className="nowrap" style={{ maxWidth: '160px' }}>
          {name}
        </div>
      </Tooltip>
    </div>
  );
};
