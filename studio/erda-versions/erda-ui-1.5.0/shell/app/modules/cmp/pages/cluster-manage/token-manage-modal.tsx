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
import { Modal, Popconfirm, Button } from 'antd';
import i18n from 'i18n';
import { Copy, ErdaIcon } from 'common';
import { getToken, createToken, resetToken } from 'cmp/services/token-manage';

interface IProps {
  visible: boolean;
  onCancel: () => void;
  token?: string;
  clusterName: string;
}

const TokenManageModal = (props: IProps) => {
  const { visible, onCancel, token, clusterName } = props;
  return (
    <Modal
      className="relative"
      onCancel={onCancel}
      width={720}
      title={i18n.t('cmp:cluster Token Management')}
      visible={visible}
      footer={[
        token ? (
          <Popconfirm
            title={i18n.t('cmp:are you sure you want to reset?')}
            onConfirm={async () => {
              await resetToken.fetch({
                clusterName,
              });
              await getToken.fetch({
                clusterName,
              });
            }}
          >
            <Button danger>{i18n.t('cmp:reset Token')}</Button>
          </Popconfirm>
        ) : (
          <Button
            type="primary"
            onClick={async () => {
              await createToken.fetch({
                clusterName,
              });
              await getToken.fetch({
                clusterName,
              });
            }}
          >
            {i18n.t('cmp:create Token')}
          </Button>
        ),
        <Button onClick={onCancel}>{i18n.t('close')}</Button>,
      ]}
    >
      <div className="rounded-sm p-2 text-gray mb-6">
        {token ? (
          <div className="flex items-center mb-1">
            <span>token</span>
            <span className="ml-32">{token}</span>
          </div>
        ) : (
          <span className="text-center">{i18n.t('cmp:no token available')}</span>
        )}
      </div>

      {token ? (
        <div className="flex items-center text-primary">
          <ErdaIcon size="14" type="copy" className="mr-1" />
          <Copy selector=".container-key" copyText={token}>
            {i18n.t('copy')}
          </Copy>
        </div>
      ) : null}
    </Modal>
  );
};

export default TokenManageModal;
