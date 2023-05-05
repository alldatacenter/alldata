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
import i18n from 'i18n';
import { Transfer, Radio } from 'antd';
import { TransferItem } from 'core/common/interface';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import routeInfoStore from 'core/stores/route';
import { map, get, forEach, reduce } from 'lodash';
import cloudServiceStore from 'dcos/stores/cloud-service';
import { rdsAccountType } from 'dcos/common/config.js';
import { useMount } from 'react-use';
import './change-permission-form.scss';

interface IProps {
  visible: boolean;
  data: CLOUD_SERVICE.IRDSAccountResp | null;
  onClose: () => any;
  handleSubmit: (permissionChoose: Record<string, string>) => void;
}

const { useEffect } = React;
const RadioGroup = Radio.Group;

const ChangePermissionForm = (props: IProps) => {
  const { visible, data, onClose, handleSubmit } = props;
  const RDSDatabaseList = cloudServiceStore.useStore((s) => s.RDSDatabaseList);
  const [rdsID, query] = routeInfoStore.useStore((s) => [s.params.rdsID, s.query]);
  const { getRDSDatabaseList } = cloudServiceStore.effects;

  const [{ targetKeys, permissionChoose }, updater, update] = useUpdate({
    targetKeys: [],
    permissionChoose: {},
  });

  useMount(() => {
    getRDSDatabaseList({
      id: rdsID,
      query,
    });
  });

  useEffect(() => {
    const newKeys: string[] = [];
    const newChoose = {};
    forEach(get(data, 'databasePrivileges'), (item) => {
      newKeys.push(item.dBName);
      newChoose[item.dBName] = item.accountPrivilege;
    });
    update({
      targetKeys: newKeys,
      permissionChoose: newChoose,
    });
  }, [data, update]);

  const handleChange = (keys: string[]) => {
    update({
      targetKeys: keys,
      permissionChoose: reduce(
        keys,
        (acc, key) => {
          const newPermissionChoose = {
            ...acc,
            [key]: permissionChoose[key] || rdsAccountType[0].value,
          };
          return newPermissionChoose;
        },
        {},
      ),
    });
  };

  const renderTransferItem = (item: TransferItem) => {
    const { key } = item;
    const label = (
      <>
        {item.key}&nbsp;
        <span className="permission-select" onClick={(e: any) => e.stopPropagation()}>
          <RadioGroup
            onChange={(e: any) => {
              updater.permissionChoose({
                ...permissionChoose,
                [key]: e.target.value,
              });
            }}
            value={permissionChoose[key]}
          >
            {rdsAccountType.map((per) => (
              <Radio key={per.value} value={per.value}>
                {per.name}
              </Radio>
            ))}
          </RadioGroup>
        </span>
      </>
    );
    return {
      label,
      value: item.key,
    };
  };

  const fieldsList = [
    {
      getComp: () => {
        return (
          <span>
            {i18n.t('cmp:database account')}: {get(data, 'AccountName')}
          </span>
        );
      },
    },
    {
      label: i18n.t('cmp:authorization database'),
      getComp: () => {
        return (
          <Transfer
            dataSource={map(RDSDatabaseList, (db) => ({
              key: db.dBName,
              title: db.dBName,
            }))}
            className="permission-transfer"
            targetKeys={targetKeys}
            onChange={handleChange}
            render={renderTransferItem}
            titles={[i18n.t('cmp:unauthorized database'), i18n.t('cmp:authorized database')]}
          />
        );
      },
    },
  ];

  return (
    <FormModal
      title={i18n.t('change account permissions')}
      visible={visible}
      fieldsList={fieldsList}
      width={800}
      onCancel={onClose}
      onOk={() => handleSubmit(permissionChoose)}
    />
  );
};

export default ChangePermissionForm;
