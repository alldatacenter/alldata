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
import { get } from 'lodash';
import { notify } from 'common/utils';
import i18n from 'i18n';
import React from 'react';

interface IProps {
  visible: boolean;
  curCluster: ORG_CLUSTER.ICluster | null;
  onCancel: () => void;
  onSubmit: (resp: { clusterName: string }) => any;
}

const DeleteClusterModal = ({ visible, curCluster, onCancel, onSubmit = () => {} }: IProps) => {
  const beforeSubmit = (data: any) => {
    const { clusterName } = data;
    const prevName = get(curCluster, 'displayName') || get(curCluster, 'name');
    return new Promise((resolve, reject) => {
      if (!curCluster || (prevName && prevName !== clusterName)) {
        notify('error', i18n.t('cmp:cluster name input error, please check'));
        reject();
      } else {
        resolve({ clusterName: get(curCluster, 'name') });
      }
    });
  };
  const handelSubmit = (data: any) => {
    onSubmit(data);
  };

  const fieldsList = [
    {
      label: i18n.t('cluster name'),
      name: 'clusterName',
    },
  ];

  return (
    <FormModal
      title={i18n.t('cmp:please enter the cluster name to confirm offline')}
      fieldsList={fieldsList}
      visible={visible}
      width={300}
      onOk={handelSubmit}
      beforeSubmit={beforeSubmit}
      onCancel={onCancel}
    />
  );
};

export default DeleteClusterModal;
