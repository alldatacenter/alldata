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
import { protocolMap } from './config';
import { FormModal, IFormItem } from 'common';
import { exportSwagger } from 'apiManagePlatform/services/api-market';
import i18n from 'i18n';
import { map, pickBy } from 'lodash';

interface IProps {
  assetID: string;
  versionID: number;
  visible: boolean;
  specProtocol: API_MARKET.SpecProtocol;
  onCancel: () => void;
}

const ExportFile = ({ visible, onCancel, versionID, assetID, specProtocol: curProtocol }: IProps) => {
  const fieldsList: IFormItem[] = React.useMemo(() => {
    const filterProtocolMap = pickBy(protocolMap, (_, protocol) => {
      const protocolPrefix = curProtocol?.substr(0, 4) || '';
      return protocol.indexOf(protocolPrefix) > -1;
    });
    return [
      {
        label: i18n.t('export format'),
        name: 'specProtocol',
        type: 'select',
        options: map(filterProtocolMap, ({ name }, key) => ({ name, value: key })),
      },
    ];
  }, [curProtocol]);
  const handleExport = ({ specProtocol }: { specProtocol: API_MARKET.SpecProtocol }) => {
    window.open(exportSwagger({ assetID, versionID, specProtocol }));
    onCancel();
  };
  return (
    <FormModal
      title={i18n.t('export')}
      visible={visible}
      fieldsList={fieldsList}
      onOk={handleExport}
      onCancel={onCancel}
    />
  );
};

export default ExportFile;
