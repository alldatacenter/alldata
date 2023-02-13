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
import { Modal } from 'antd';
import { Panel } from 'common';
import i18n from 'i18n';
import SLASelect from 'apiManagePlatform/components/sla-select';
import { insertWhen } from 'common/utils';
import moment from 'moment';

interface IProps {
  confirmLoading: boolean;
  slaList: API_ACCESS.SlaItem[];
  metaData: {
    curSLAName?: string;
    currentSLAID: number | undefined;
    defaultSLAID: number | undefined;
    committedAt?: string;
  };
  visible: boolean;
  onCancel: () => void;
  onOk: (id: number) => void;
}

const UpdateSLA = ({ visible, onCancel, metaData, slaList, onOk, confirmLoading }: IProps) => {
  const [slaID, setSlaID] = React.useState(metaData.defaultSLAID);
  const firstSlaId = slaList[0]?.id;
  React.useEffect(() => {
    const defaultSla = metaData.defaultSLAID || metaData.defaultSLAID === 0 ? metaData.defaultSLAID : firstSlaId;
    setSlaID(defaultSla);
  }, [metaData.defaultSLAID, firstSlaId]);
  const currentSLA = React.useMemo<API_ACCESS.SlaItem>(
    () => slaList.find((sla) => sla.id === metaData.currentSLAID) as API_ACCESS.SlaItem,
    [slaList, metaData.currentSLAID],
  );
  const handleCancel = () => {
    onCancel();
  };
  const handleOk = () => {
    onOk(slaID as number);
  };
  const fields = [
    {
      label: i18n.t('SLA name'),
      value: metaData.curSLAName || currentSLA?.name,
    },
    ...insertWhen(!!metaData.committedAt, [
      {
        label: i18n.t('commit date'),
        value: moment(metaData.committedAt).format('YYYY-MM-DD HH:mm:ss'),
      },
    ]),
  ];
  return (
    <Modal
      title={i18n.t('replace SLA')}
      width={600}
      visible={visible}
      onCancel={handleCancel}
      onOk={handleOk}
      destroyOnClose
      confirmLoading={confirmLoading}
    >
      <div className="text-base font-medium mb-3">{i18n.t('current SLA')}</div>
      <Panel fields={fields} />
      <div className="text-base font-medium mb-3">{i18n.t('replace SLA')}</div>
      <SLASelect
        dataSource={slaList}
        defaultSelectKey={metaData.defaultSLAID || metaData.defaultSLAID === 0 ? metaData.defaultSLAID : slaList[0]?.id}
        onChange={setSlaID}
      />
    </Modal>
  );
};

export default UpdateSLA;
