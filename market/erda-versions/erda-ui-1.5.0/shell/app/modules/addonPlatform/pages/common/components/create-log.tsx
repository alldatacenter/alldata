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
import { Button, Modal } from 'antd';
import React from 'react';
import clusterStore from 'cmp/stores/cluster';
import { useUpdate } from 'common/use-hooks';
import './addon-cards.scss';

interface IProps {
  visible?: boolean;
  recordId: string;
  toggleModal: (visible: boolean) => void;
}

const CreateLog = (props: IProps) => {
  const { visible, recordId, toggleModal } = props;
  const [{ record }, updater] = useUpdate({
    record: null,
  });

  React.useEffect(() => {
    if (visible) {
      clusterStore.effects.getClusterLogTasks({ recordIDs: recordId }).then((res) => {
        if (res.list && res.list[0]) {
          updater.record(res.list[0]);
        }
      });
      return () => {
        updater.record(null);
      };
    }
  }, [recordId, updater, visible]);

  return (
    <Modal
      title={i18n.t('log')}
      visible={visible}
      onCancel={() => toggleModal(false)}
      width={750}
      footer={<Button onClick={() => toggleModal(false)}>{i18n.t('close')}</Button>}
    >
      <pre className="code-block mt-4" style={{ whiteSpace: 'pre-wrap' }}>
        {record ? JSON.stringify(record.detail ? JSON.parse(record.detail) : record.status, null, 2) : 'loading...'}
      </pre>
    </Modal>
  );
};

export default CreateLog;
