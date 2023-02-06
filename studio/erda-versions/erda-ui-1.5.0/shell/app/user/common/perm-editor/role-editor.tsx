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
import { Button, Input, Switch } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { map, cloneDeep, uniq, compact } from 'lodash';
import { FormModal } from 'app/configForm/nusi-form/form-modal';
import i18n from 'i18n';

export interface IRoleData {
  name: string;
  value: string;
}
interface IProps {
  data: Obj<IRoleData>;
  updateRole: (arg: Obj) => void;
}

const AddRole = (props: IProps) => {
  const { data, updateRole } = props;
  const [{ visible, roleData, editData }, updater, update] = useUpdate({
    visible: false,
    roleData: [],
    editData: undefined as any,
  });

  React.useEffect(() => {
    updater.roleData(cloneDeep(map(data)));
  }, [data, updater]);

  const onOk = (v: any) => {
    const { roles } = v;
    const newRole = {};
    map(roles, (item) => {
      newRole[item.value] = item;
    });
    updateRole(newRole);
    onCancel();
  };

  const onOpen = () => {
    update({
      editData: { roles: roleData },
      visible: true,
    });
  };

  const onCancel = () => {
    update({
      visible: false,
      editData: undefined,
    });
  };

  const fields = [
    {
      label: i18n.t('role'),
      component: 'arrayObj',
      key: 'roles',
      required: true,
      wrapperProps: {
        extra: '修改角色key或删除角色后，导出数据会删除对应的角色',
      },
      componentProps: {
        defaultItem: { value: '', name: '', isCustomRole: false },
        itemRender: (_data: Obj, updateItem: Function) => {
          return [
            <Input
              key="value"
              value={_data.value}
              onChange={(e: any) => updateItem({ value: e.target.value })}
              placeholder={i18n.t('please enter')}
            />,
            <Input
              key="name"
              value={_data.name}
              onChange={(e: any) => updateItem({ name: e.target.value })}
              placeholder={i18n.t('please enter')}
            />,
            <div key="isCustomRole" className="flex justify-between items-center">
              <span className="text-desc nowrap">
                {i18n.t('customize')} {i18n.t('role')}
              </span>
              <Switch
                key="isCustomRole"
                checked={!!_data.isCustomRole}
                onChange={(v) => updateItem({ isCustomRole: v })}
                checkedChildren={i18n.t('common:yes')}
                unCheckedChildren={i18n.t('common:no')}
              />
            </div>,
          ];
        },
      },
      rules: [
        {
          validator: (val = []) => {
            let tip = '';
            const valueArr = map(val, 'value');
            const nameArr = map(val, 'name');
            if (compact(valueArr).length !== val.length || compact(nameArr).length !== val.length) {
              tip = i18n.t('dop:this item cannot be empty');
            } else if (uniq(nameArr).length !== val.length) {
              tip = i18n.t('{name} already exists', { name: i18n.t('name') });
            } else if (uniq(valueArr).length !== val.length) {
              tip = i18n.t('{name} already exists', { name: 'key' });
            }
            if (!tip) {
              const keyReg = /^[a-zA-Z]+$/;
              valueArr.forEach((item) => {
                if (!keyReg.test(item)) {
                  tip = i18n.t('key only can be letters');
                }
              });
            }
            return [!tip, tip];
          },
        },
      ],
    },
  ];

  return (
    <div className="flex justify-between items-center">
      <Button size="small" onClick={onOpen}>
        {i18n.t('edit {name}', { name: i18n.t('role') })}
      </Button>
      <FormModal
        width={650}
        visible={visible}
        title={i18n.t('edit {name}', { name: i18n.t('role') })}
        onOk={onOk}
        onCancel={onCancel}
        fieldList={fields}
        formData={editData}
      />
    </div>
  );
};

export default AddRole;
