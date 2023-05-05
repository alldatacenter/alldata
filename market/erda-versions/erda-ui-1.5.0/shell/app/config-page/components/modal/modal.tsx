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
import { ErdaIcon } from 'common';
import { map } from 'lodash';
import { statusColorMap } from 'app/config-page/utils';
import i18n from 'i18n';

const widthMap = {
  small: 350,
  default: 500,
  big: 650,
  large: 800,
};

const stateIconMap = {
  info: <ErdaIcon type="info" size="24" fill={statusColorMap.info} />,
  success: <ErdaIcon type="check-one" size="24" fill={statusColorMap.success} />,
  warning: <ErdaIcon type="attention" size="24" fill={statusColorMap.warning} />,
  danger: <ErdaIcon type="close-one" size="24" fill={statusColorMap.danger} />,
};

const CP_MODAL = (props: CP_MODAL.Props) => {
  const { props: configProps, children, footer, state, operations, updateState, execOperation } = props || {};
  const [visible, setVisible] = React.useState(state?.visible ?? false);

  React.useEffect(() => {
    setVisible(state?.visible ?? false);
  }, [state]);

  const { size = 'default', status, title, content, ...rest } = configProps || {};

  const onCancel = () => {
    setVisible(false);
    updateState({ visible: false });
    operations?.onCancel && execOperation(operations.onCancel);
  };

  const onOk = () => {
    setVisible(false);
    operations?.onOk && execOperation(operations.onOk);
  };

  const _title = (status || title) && (
    <div className="flex items-center">
      {status && stateIconMap[status]}
      <span>{title}</span>
    </div>
  );

  return (
    <Modal
      visible={visible}
      destroyOnClose
      closable
      width={widthMap[size]}
      okText={i18n.t('ok')}
      cancelText={i18n.t('cancel')}
      onCancel={onCancel}
      onOk={onOk}
      footer={footer}
      title={_title}
      {...rest}
    >
      {children}
      {(content?.split('\n') || []).map((cItem) => (
        <span key={cItem}>
          {cItem}
          <br />
        </span>
      ))}
    </Modal>
  );
};

export default CP_MODAL;
