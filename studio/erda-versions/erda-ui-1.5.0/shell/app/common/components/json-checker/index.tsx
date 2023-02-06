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

/*
 * JSON 查看、复制的弹窗，暴露出一个按钮触发
 * @Author: licao
 * @Date: 2019-02-21 19:05:08
 * @Last Modified by: licao
 * @Last Modified time: 2019-02-21 19:34:32
 */
import React from 'react';
import { Button, Modal } from 'antd';
import { Copy, ErdaIcon } from 'common';
import i18n from 'i18n';

import './index.scss';

interface IProps {
  jsonString?: string;
  buttonText?: string;
  modalConfigs?: object;
  onToggle?: (visible: boolean) => void;
}

interface IState {
  visible: boolean;
}

class JsonChecker extends React.PureComponent<IProps, IState> {
  state = {
    visible: false,
  };

  toggleVisible = () => {
    this.setState(
      {
        visible: !this.state.visible,
      },
      () => {
        const { onToggle } = this.props;
        if (onToggle) {
          onToggle(this.state.visible);
        }
      },
    );
  };

  render() {
    const { modalConfigs, jsonString, buttonText } = this.props;
    const configs = {
      visible: this.state.visible,
      width: 800,
      title: `${i18n.t('common:view')} JSON`,
      footer: null,
      onCancel: this.toggleVisible,
      ...modalConfigs,
    };

    return (
      <React.Fragment>
        <Button type="primary" size="small" ghost onClick={this.toggleVisible}>
          {buttonText || `${i18n.t('common:view')} JSON`}
        </Button>
        <Modal className="json-checker-modal" {...configs}>
          <div className="json-detail-wrap">
            {jsonString ? (
              <>
                <Button
                  className="json-detail-btn cursor-copy json-checker-copy"
                  shape="circle"
                  icon={<ErdaIcon className="mt-1" type="copy" size="16" />}
                />
                <Copy selector=".json-checker-copy" opts={{ text: () => jsonString }} />
              </>
            ) : null}
            <pre>{jsonString}</pre>
          </div>
        </Modal>
      </React.Fragment>
    );
  }
}

export default JsonChecker;
