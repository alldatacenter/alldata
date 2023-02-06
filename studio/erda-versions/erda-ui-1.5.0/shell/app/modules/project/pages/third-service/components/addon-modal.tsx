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
import { find, isEmpty, cloneDeep, map } from 'lodash';
import { Button, message, Modal } from 'antd';
import React from 'react';
import InstanceForm from './instance-form';
import ThirdAddonForm from './third-addon-form';
import './addon-modal.scss';
import { AddonType } from 'project/pages/third-service/components/config';

const STEP = {
  FIRST: 0,
  SECOND: 1,
};

interface IProps {
  visible: boolean;
  category?: string;
  addonInsList: ADDON.Instance[];
  addonSpecList: CUSTOM_ADDON.Item[];
  editData: ADDON.Instance | null;
  onOk: (data: CUSTOM_ADDON.AddBody) => Promise<any>;
  onCancel: () => void;
}
interface IState {
  step: number;
  currentType: string;
  submitLoading: boolean;
  chosenWorkspace: string;
  onlyOneStep: boolean;
}
class AddonModal extends React.PureComponent<IProps, IState> {
  thirdFormRef = React.createRef<any>();

  instanceFormRef = React.createRef<any>();

  edit = React.createRef<any>();

  configKV = React.createRef<any>();

  constructor(props: IProps) {
    super(props);
    this.state = {
      step: STEP.FIRST,
      currentType: '',
      submitLoading: false,
      chosenWorkspace: '',
      onlyOneStep: false,
    };
  }

  checkKVConfig = (kvObj: Obj) => {
    const errorKeys = [];
    const regRule = /^[a-zA-Z0-9._-]*$/;
    map(kvObj, (_v, k) => {
      if (!regRule.test(k)) {
        errorKeys.push(k);
      }
    });
    return [
      !!errorKeys.length,
      i18n.t('dop:parameter key can only contain letters, numbers, dots, underscores and hyphens', {
        keySeparator: '>',
      }),
    ];
  };

  handleOk = () => {
    this.checkForms([this.thirdFormRef, this.instanceFormRef]).then(([form1, form2]: any[]) => {
      const { mode, ...form2Rest } = cloneDeep(form2);
      const isCustom = mode === 'custom' || !mode; // 第一步选Custom类型时，第二步没有mode字段
      const finData = {
        ...form1,
        customAddonType: isCustom ? 'custom' : 'cloud',
      };
      // mode为custom时，kv-table模式下没有其他key，或kv-text模式下为空，提示一下
      // const kvTableData = pickBy(form2Rest, (v, k) => k.startsWith('_tb_'));
      // const kvTextData = form2Rest['kv-text'];
      if (isCustom) {
        const configs = this.edit.current?.getEditData();
        if (isEmpty(configs)) {
          message.warn(i18n.t('parameter cannot be empty'));
          return;
        }
        const [hasErr, msg] = this.checkKVConfig(configs);
        if (hasErr) {
          message.warn(msg);
          return;
        }
        // const configs = isEmpty(kvTableData) ? convertTextToMapData(kvTextData) : KeyValueTable.dealTableData(kvTableData);
        finData.configs = configs;
        finData.extra = {};
      } else {
        finData.extra = form2Rest;
        if (form2Rest.storageSize) {
          finData.extra.storageSize = +form2Rest.storageSize;
          finData.extra.source = 'addon';
        }
        if (form2Rest.topics) {
          form2Rest.topics.forEach((t: any) => ({ ...t, messageType: Number(t.messageType) }));
        }
        finData.configs = {};
      }
      this.setState({ submitLoading: true });
      this.props
        .onOk(finData)
        .then(() => {
          this.handleCancel();
        })
        .finally(() => {
          this.setState({ submitLoading: false });
        });
    });
  };

  handleSaveDiceAddons = () => [
    this.checkForms([this.thirdFormRef]).then(([data]: any[]) => {
      const configs: Obj = this.configKV.current && this.configKV.current.getEditData();
      const [hasErr, msg] = this.checkKVConfig(configs);
      if (hasErr) {
        message.warn(msg);
        return;
      }
      const withoutTb = {
        configs,
      };
      // 移除 kvtable 的原始值，都已放在了 configs
      map(data, (v, k) => {
        if (!k.startsWith('_tb_')) {
          withoutTb[k] = v;
        }
      });
      this.setState({ submitLoading: true });
      this.props
        .onOk(withoutTb as any)
        .then(() => {
          this.handleCancel();
        })
        .finally(() => {
          this.setState({ submitLoading: false });
        });
    }),
  ];

  handleCancel = () => {
    this.setState({ step: STEP.FIRST, currentType: '' });
    this.thirdFormRef.current.form.resetFields();
    this.instanceFormRef.current.form.resetFields();
    this.props.onCancel();
  };

  getFooter = () => {
    const { editData } = this.props;
    const { step } = this.state;
    if (editData) {
      return [
        <Button key="cancel" onClick={() => this.handleCancel()}>
          {i18n.t('cancel')}
        </Button>,
        <Button key="confirm" type="primary" onClick={() => this.handleOk()}>
          {i18n.t('dop:confirm')}
        </Button>,
      ];
    }
    if (step === STEP.FIRST) {
      const currentAddon = this.getCurAddon();
      return [
        <Button key="cancel" onClick={() => this.handleCancel()}>
          {i18n.t('cancel')}
        </Button>,
        // API 网关只有基础信息，不需要下一步
        currentAddon.vars === null || this.state.onlyOneStep ? (
          <Button
            key="confirm"
            type="primary"
            loading={this.state.submitLoading}
            onClick={() => this.handleSaveDiceAddons()}
          >
            {i18n.t('dop:confirm')}
          </Button>
        ) : (
          <Button key="next" type="primary" onClick={() => this.toStep(STEP.SECOND)}>
            {i18n.t('dop:next')}
          </Button>
        ),
      ];
    }
    return [
      <Button key="cancel" onClick={() => this.handleCancel()}>
        {i18n.t('cancel')}
      </Button>,
      <Button key="prev" onClick={() => this.toStep(STEP.FIRST)}>
        {i18n.t('dop:previous')}
      </Button>,
      <Button key="confirm" type="primary" loading={this.state.submitLoading} onClick={() => this.handleOk()}>
        {i18n.t('dop:confirm')}
      </Button>,
    ];
  };

  checkForms = (formRefs: any[]) => {
    return Promise.all(
      formRefs.map(
        (formRef) =>
          new Promise((resolve: any, reject: any) => {
            if (formRef.current) {
              formRef.current.form
                .validateFields()
                .then((values: any) => {
                  resolve(values);
                })
                .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
                  reject(errorFields);
                });
            }
          }),
      ),
    );
  };

  toStep = (step: number) => {
    // 往前走不做校验
    if (step < this.state.step) {
      return this.setState({ step });
    }
    const checkMap = {
      [STEP.FIRST]: [this.instanceFormRef],
      [STEP.SECOND]: [this.thirdFormRef],
    };
    const workspace = this.thirdFormRef.current.form.getFieldValue('workspace');
    this.checkForms(checkMap[step]).then(() => {
      this.setState({ step, chosenWorkspace: workspace });
    });
  };

  onFieldChange = (key: string, value: any) => {
    if (key === 'addonName') {
      this.setState({ currentType: value, onlyOneStep: false }); // 类型变了就重置onlyOneStep
    }
  };

  getCurAddon = (): CUSTOM_ADDON.Item | Obj => {
    const { addonSpecList, editData } = this.props;
    const { currentType } = this.state;
    return find(addonSpecList, { addonName: currentType || ((editData && editData.addonName) as string) }) || {};
  };

  render() {
    const { visible, addonInsList, addonSpecList, editData } = this.props;
    const { step, chosenWorkspace } = this.state;
    const currentAddon = this.getCurAddon();
    return (
      <Modal
        title={
          editData
            ? this.props.category === 'DATA_SOURCE'
              ? i18n.t('dop:edit custom data source')
              : i18n.t('dop:edit service instance')
            : i18n.t('dop:add service instance')
        }
        width={800}
        visible={visible}
        destroyOnClose
        maskClosable={false}
        onCancel={this.handleCancel}
        footer={null}
        wrapClassName="third-addon-modal"
      >
        <div className={step === STEP.FIRST && !editData ? 'block' : 'hidden'}>
          <ThirdAddonForm
            ref={this.thirdFormRef}
            category={this.props.category}
            addonInsList={addonInsList}
            editData={editData}
            addonSpecList={addonSpecList}
            currentAddon={currentAddon}
            onFieldChange={this.onFieldChange}
            setOneStep={(flag: boolean) => this.setState({ onlyOneStep: flag })}
            configKV={this.configKV}
          />
        </div>
        <div className={step === STEP.SECOND || editData ? 'block' : 'hidden'}>
          <InstanceForm
            ref={this.instanceFormRef}
            category={this.props.category}
            addonProto={currentAddon as CUSTOM_ADDON.Item}
            editData={editData}
            workspace={chosenWorkspace}
            edit={this.edit}
          />
        </div>
        <div className="footer">{this.getFooter()}</div>
      </Modal>
    );
  }
}

export default AddonModal;
