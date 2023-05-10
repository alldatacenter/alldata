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
import { Drawer as PureDrawer, Button as NusiButton } from 'antd';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import './drawer.scss';

const widthMap = {
  s: 256,
  m: 560,
  l: 800,
  xl: 1100,
};

export const Drawer = (props: CP_DRAWER.Props) => {
  const { updateState, execOperation, operations, content, footer, props: configProps, state: propsState } = props;
  const { visible: pVisible = false } = propsState || {};
  const { size = 'm', ...rest } = configProps || {};
  const [state, updater, update] = useUpdate({
    visible: pVisible,
  });

  React.useEffect(() => {
    updater.visible(pVisible);
  }, [pVisible, updater]);

  const onClose = () => {
    updateState({ visible: false });
    operations?.close && execOperation(operations?.close);
  };

  const onSubmit = () => {
    onClose();
    operations?.submit && execOperation(operations?.submit);
  };

  return (
    <PureDrawer
      destroyOnClose
      {...rest}
      visible={state.visible}
      width={widthMap[size]}
      onClose={onClose}
      footer={footer}
    >
      <div className="dice-cp-drawer-content">{content}</div>
      {operations?.submit && (
        <div className="dice-cp-drawer-operation border-top">
          <NusiButton onClick={onSubmit} type="primary">
            {operations.submit?.text || i18n.t('ok')}
          </NusiButton>
        </div>
      )}
    </PureDrawer>
  );
};
