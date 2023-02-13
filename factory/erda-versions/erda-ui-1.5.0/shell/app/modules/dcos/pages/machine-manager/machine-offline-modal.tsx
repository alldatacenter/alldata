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

import { FormModal } from 'common';
import { notify } from 'common/utils';
import i18n from 'i18n';
import React from 'react';

interface IProps {
  visible: boolean;
  formData?: any;
  onCancel: () => void;
  onSubmit: (resp: { hosts: string[] }) => any;
}

const MachineOffLineModal = ({ visible, formData, onCancel, onSubmit = () => {} }: IProps) => {
  const beforeSubmit = (data: any) => {
    const { ip } = data;
    return new Promise((resolve, reject) => {
      if (formData.ip !== ip) {
        notify('error', i18n.t('cmp:IP input error, please check'));
        reject();
      } else {
        resolve(data);
      }
    });
  };
  const handelSubmit = (data: any) => {
    const { ip, ...rest } = data;
    const postData = {
      hosts: [ip],
      ...rest,
      clusterName: formData.clusterName,
    };
    onSubmit(postData);
  };

  const fieldsList = [
    {
      label: 'IP',
      name: 'ip',
    },
    {
      label: i18n.t('password'),
      name: 'password',
      itemProps: { type: 'password' },
    },
    {
      label: i18n.t('cmp:forced offline'),
      name: 'force',
      type: 'switch',
      initialValue: false,
    },
  ];
  return (
    <FormModal
      title={i18n.t('cmp:Please enter the following information to confirm to go offline.')}
      fieldsList={fieldsList}
      visible={visible}
      onOk={handelSubmit}
      beforeSubmit={beforeSubmit}
      onCancel={onCancel}
    />
  );
};

export default MachineOffLineModal;
