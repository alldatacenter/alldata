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

import { includes, isEmpty } from 'lodash';
import React, { useState, useRef } from 'react';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import { Dropdown, Menu, Modal, message, Input } from 'antd';
import testSetStore from 'project/stores/test-set';
import testPlanStore from 'project/stores/test-plan';
import { recycledKey } from '../utils';
import { TEMP_MARK, TestOperation, editModeEnum, TestSetMenuType } from 'project/pages/test-manage/constants';

interface IMenuMeta {
  key: string;
  name: string;
  disabled?: boolean;
}

const getMenuMap = (type: TestSetMenuType, editMode: editModeEnum): IMenuMeta[] => {
  const menuItemsMap = {
    [TestSetMenuType.root]: [
      { key: TestOperation.add, name: i18n.t('dop:new sub testset') },
      { key: TestOperation.paste, name: i18n.t('dop:paste'), disabled: editMode === '' },
    ],
    [TestSetMenuType.normal]: [
      { key: TestOperation.add, name: i18n.t('dop:new sub testset') },
      { key: TestOperation.rename, name: i18n.t('dop:rename') },
      { key: TestOperation.copy, name: i18n.t('copy') },
      { key: TestOperation.clip, name: i18n.t('dop:cut') },
      { key: TestOperation.paste, name: i18n.t('dop:paste'), disabled: editMode === '' },
      { key: TestOperation.delete, name: i18n.t('delete') },
      { key: TestOperation.plan, name: i18n.t('dop:add to test plan') },
    ],
    [TestSetMenuType.recycled]: [
      { key: TestOperation.recover, name: i18n.t('dop:recover to') },
      { key: TestOperation.deleteEntirely, name: i18n.t('dop:delete completely') },
    ],
  };
  return menuItemsMap[type];
};

interface IActionItem {
  key: string;
  name: string;
  onclick: (prop: any) => void;
}

interface IProps {
  name: string;
  readOnly: boolean;
  id: number; // 测试集id
  editMode: editModeEnum;
  eventKey: string;
  recycled: boolean;
  className?: string;
  customActions?: IActionItem[];
  onOperateNode: (eventKey: string, action: string, data?: Record<string, any>) => void;
  onRemoveNode: (eventKey: string) => void;
  onUpdateNode: (eventKey: string, newId: number, newName: string) => void;
}

const Title = ({
  eventKey,
  id,
  name: oldName,
  recycled,
  editMode,
  readOnly,
  className,
  onOperateNode,
  onRemoveNode,
  onUpdateNode,
  customActions = [],
}: IProps) => {
  const { renameTestSet, createTestSet, deleteTestSetToRecycle, deleteTestSetEntirely } = testSetStore.effects;
  const { openTreeModal } = testSetStore.reducers;
  const { openPlanModal } = testPlanStore.reducers;
  const [isEdit, setIsEdit] = useState(includes(eventKey, TEMP_MARK));
  const nameRef = useRef('');
  const inputRef = useRef(null as any);
  const isRecycledRoot = eventKey === recycledKey;
  const isRoot = eventKey === '0';

  const getParentId = () => {
    const list = eventKey.split('-');
    if (list.length === 2) {
      return 0;
    }
    return parseInt(list[list.length - 2], 10);
  };

  const onClick = ({ key, domEvent }: any) => {
    domEvent.stopPropagation();
    switch (key) {
      case TestOperation.rename:
        toggleEdit(true);
        break;
      case TestOperation.add:
        onOperateNode(eventKey, TestOperation.add);
        break;
      case TestOperation.recover:
        openTreeModal({
          ids: [id],
          action: key,
          type: 'collection', // 不传type是为了不打开tree弹框
          callback: (data: Record<string, any>) => onOperateNode(eventKey, TestOperation.recover, data),
        });
        break;
      case TestOperation.delete:
        Modal.confirm({
          title: i18n.t('delete'),
          content:
            i18n.t('dop:Deleting will put the current test set and included test cases into the recycle bin.') +
            i18n.t('are you sure?'),
          onOk: () => {
            deleteTestSetToRecycle(id).then(() => {
              onOperateNode(eventKey, TestOperation.delete);
            });
          },
        });
        break;
      case TestOperation.deleteEntirely:
        Modal.confirm({
          title: i18n.t('delete'),
          content: i18n.t('dop:It cannot be restored if completely deleted.') + i18n.t('is it confirmed?'),
          onOk: () => {
            deleteTestSetEntirely(id).then(() => {
              onOperateNode(eventKey, TestOperation.deleteEntirely);
            });
          },
        });
        break;
      case TestOperation.copy:
        openTreeModal({
          ids: [id],
          action: key,
          // type: 'collection', // 不传type是为了不打开tree弹框
        });
        onOperateNode(eventKey, TestOperation.copy);
        break;
      case TestOperation.clip:
        openTreeModal({
          ids: [id],
          action: key,
          // type: 'collection',
        });
        onOperateNode(eventKey, TestOperation.clip);
        break;
      case TestOperation.paste:
        onOperateNode(eventKey, TestOperation.paste);
        break;
      case TestOperation.plan:
        openPlanModal({
          ids: [id],
          type: 'collection',
        });
        break;
      default:
        break;
    }
  };

  const toggleEdit = (isEditNext: boolean, isRemove?: boolean) => {
    setIsEdit(isEditNext);
    if (isRemove) {
      onRemoveNode(eventKey);
    }
  };

  const handlePressEntry = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.keyCode === 13) {
      handleSave();
    }
  };

  const handleSave = () => {
    const name: string = inputRef.current?.input.value;
    if (name) {
      if (includes(name, '/') || includes(name, '\\')) {
        message.error(i18n.t('dop:The name cannot contain forward and backward slashes. Please enter again.'));
        return;
      }
      if (!includes(eventKey, TEMP_MARK)) {
        // 重命名
        renameTestSet({ name, testSetID: id }).then((info: TEST_SET.TestSet) => {
          nameRef.current = info.name || name;
          toggleEdit(false);
        });
        return;
      }
      // 新建
      createTestSet({ name, parentID: getParentId() }).then((info: TEST_SET.TestSet) => {
        nameRef.current = name;
        toggleEdit(false);
        onUpdateNode(eventKey, +info.id, info.name);
      });
    } else {
      message.warning(i18n.t('dop:name is required'));
    }
  };

  const getMenu = () => {
    let list: IMenuMeta[] = [];

    if (!isEmpty(customActions)) {
      const customClickDic = {};
      list = customActions.map(({ key, name, onclick }) => {
        customClickDic[key] = onclick;
        return { key, name };
      });

      const _onClick = ({ key, domEvent }: any) => {
        domEvent.stopPropagation();
        if (key === 'delete') {
          Modal.confirm({
            title: i18n.t('delete'),
            content: i18n.t('dop:It cannot be restored if completely deleted.') + i18n.t('is it confirmed?'),
            onOk: () => {
              customClickDic[key](id).then(() => {
                onOperateNode(eventKey, TestOperation.deleteEntirely);
              });
            },
          });
        }
      };

      return (
        <Menu onClick={_onClick}>
          {list.map(({ key, name, disabled }: IMenuMeta) => (
            <Menu.Item key={key} disabled={disabled}>
              {name}
            </Menu.Item>
          ))}
        </Menu>
      );
    }

    if (isRoot) {
      list = getMenuMap(TestSetMenuType.root, editMode);
    } else if (recycled) {
      list = getMenuMap(TestSetMenuType.recycled, editMode);
    } else {
      list = getMenuMap(TestSetMenuType.normal, editMode);
    }
    return (
      <Menu onClick={onClick}>
        {list.map(({ key, name, disabled }: IMenuMeta) => (
          <Menu.Item key={key} disabled={disabled}>
            {name}
          </Menu.Item>
        ))}
      </Menu>
    );
  };

  const value = nameRef.current || oldName;
  const isTemp = includes(eventKey, TEMP_MARK);

  if (!readOnly && isEdit) {
    return (
      <div className="flex-1 inline-flex justify-between items-center" onClick={(e) => e.stopPropagation()}>
        <Input
          autoFocus
          style={{ height: '32px', minWidth: '110px' }}
          size="small"
          defaultValue={value}
          maxLength={50}
          placeholder={i18n.t('dop:enter test set name')}
          ref={inputRef}
          onKeyUp={handlePressEntry}
        />
        <ErdaIcon type="check" className="ml-2 text-primary cursor-pointer" onClick={handleSave} />
        <ErdaIcon type="close" className="mx-2 text-primary cursor-pointer" onClick={() => toggleEdit(false, isTemp)} />
      </div>
    );
  }

  if (!readOnly && !isRecycledRoot && !isTemp) {
    return (
      <Dropdown overlay={getMenu()} trigger={['contextMenu']}>
        <div className={`flex-1 inline-flex justify-between items-center relative ${className}`}>
          <div className="flex-1 node-name">{value}</div>
          {!isRoot ? (
            <Dropdown
              overlay={getMenu()}
              overlayClassName="case-tree-menu"
              trigger={['click']}
              align={{ offset: [32, -32] }}
            >
              <div className="case-tree-op" onClick={(e: any) => e.stopPropagation()}>
                <ErdaIcon className="mr-4 align-middle" type="more1" color="white" />
              </div>
            </Dropdown>
          ) : undefined}
        </div>
      </Dropdown>
    );
  }
  return <div className={`flex-1 node-name ${className}`}>{value}</div>;
};

export default Title;
