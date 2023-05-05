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
import { KeyValueEditor, RenderForm } from 'common';
import { map } from 'lodash';
import i18n from 'i18n';

class ConfigFormModal extends React.PureComponent {
  form = React.createRef();

  state = {
    modalVisible: false,
    dataSource: {},
    existKeys: [],
    namespace: undefined,
    workspace: undefined,
  };

  showConfigFormModal = ({ namespace, workspace, existConfig, existKeys } = {}) => {
    const state = { modalVisible: true, namespace, workspace, existKeys };
    if (existConfig) {
      state.dataSource = existConfig;
    }
    this.setState(state);
  };

  hideAddModal = () => {
    this.setState({
      modalVisible: false,
      dataSource: {},
    });
  };

  getSpaceConfig = () => ({
    namespace: this.state.namespace,
    workspace: this.state.workspace,
  });

  onSubmit = () => {
    this.form.current.validateFields().then(() => {
      const data = this.editor.getEditData();
      const configs = map(data, (v, k) => ({
        key: k,
        value: v,
        comment: '',
      }));
      this.props.addConfigs(configs);
      this.hideAddModal();
    });
  };

  onCancel = () => {
    this.hideAddModal();
  };

  render() {
    const { modalVisible, dataSource, existKeys } = this.state;
    const { title, keyDisabled, isNeedTextArea, isValueTextArea, disableAdd, disableDelete } = this.props;

    const fieldsList = [
      {
        name: 'configs',
        getComp: ({ form }) => {
          return (
            <KeyValueEditor
              dataSource={dataSource}
              disableDelete={disableDelete}
              form={form}
              keyDisabled={keyDisabled}
              isNeedTextArea={isNeedTextArea}
              isValueTextArea={isValueTextArea}
              disableAdd={disableAdd}
              existKeys={existKeys}
              ref={(ref) => {
                this.editor = ref;
              }}
            />
          );
        },
      },
    ];
    return (
      <Modal
        title={title || i18n.t('common:new configuration')}
        width={800}
        visible={modalVisible}
        onOk={this.onSubmit}
        onCancel={this.onCancel}
        okText={i18n.t('save')}
        maskClosable={false}
        destroyOnClose
      >
        <RenderForm list={fieldsList} ref={this.form} />
      </Modal>
    );
  }
}

export default ConfigFormModal;
