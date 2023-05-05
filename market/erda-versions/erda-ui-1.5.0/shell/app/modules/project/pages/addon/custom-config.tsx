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

import i18n from 'i18n';
import { Alert, message, Modal } from 'antd';
import React from 'react';
import addonStore from 'common/stores/addon';
import { FileEditor } from 'common';
import { useUpdate } from 'common/use-hooks';

interface IProps {
  visible: boolean;
  onCancel: () => void;
}
const CustomAddonConfigModal = (props: IProps) => {
  const { visible, onCancel } = props;
  const [{ json }, updater] = useUpdate({
    json: '',
  });

  React.useEffect(() => {
    visible &&
      addonStore.getExportAddonSpec().then((data) => {
        try {
          updater.json(JSON.stringify(JSON.parse(data), null, 2));
        } catch (error) {
          message.error('parse response failed');
        }
      });
  }, [updater, visible]);

  return (
    <Modal
      title={i18n.t('dop:custom addon config')}
      width={800}
      visible={visible}
      onCancel={onCancel}
      destroyOnClose
      footer={null}
    >
      <Alert showIcon type="info" className="mb-2" message={i18n.t('dop:transfer-custom-addon')} />
      <FileEditor fileExtension="json" value={json} minLines={8} onChange={(value: string) => updater.json(value)} />
    </Modal>
  );
};

export default CustomAddonConfigModal;
