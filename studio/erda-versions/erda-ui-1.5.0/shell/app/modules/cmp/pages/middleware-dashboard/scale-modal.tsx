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
import { RenderPureForm } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Alert, Modal, Form } from 'antd';
import i18n from 'i18n';
import { IFormItem } from 'common/components/render-formItem/render-formItem';
import middlewareDashboardStore from 'cmp/stores/middleware-dashboard';
import { isEqual, pick } from 'lodash';
import { FormInstance } from 'core/common/interface';
import './modal.scss';

interface IProps {
  visible: boolean;
  formData: Merge<MIDDLEWARE_DASHBOARD.IScaleData, { name: string; projectName: string }>;
  form: FormInstance;
  onCancel: () => void;
  afterSubmit?: () => void;
}

type cb = (str?: string) => void;

type IValidate = [any, string, cb];

const minCPU = 0.5;
const minMEM = 512;

const setDecimal = (num = 0) => {
  return Math.round(num * 100) / 100;
};

const validateCPU = (max: number, _rule: any, value: string, callback: cb) => {
  if (value && (isNaN(+value) || +value < minCPU)) {
    callback(i18n.t('cmp:please enter a number greater than or equal to {min}', { min: minCPU }));
  } else if (max < minCPU || +value > max) {
    // 可输入最大值小于设置的最小值或者输入的值大于可输入最大值均为资源不足
    callback(i18n.t('cmp:lack of resources'));
  } else {
    callback();
  }
};

const validateNodes = (_rule: any, value: string, callback: cb) => {
  if (value && (isNaN(+value) || +value % 2 === 0 || +value > 15 || +value <= 0)) {
    callback(i18n.t('cmp:please enter an odd number no greater than {max}', { max: 15 }));
  } else {
    callback();
  }
};

const validateMEM = (max: number, _rule: any, value: string, callback: cb) => {
  if (value && (isNaN(+value) || +value < minMEM)) {
    callback(i18n.t('cmp:please enter a number greater than or equal to {min}', { min: minMEM }));
  } else if (max < minMEM || +value > max) {
    // 可输入最大值小于设置的最小值或者输入的值大于可输入最大值均为资源不足
    callback(i18n.t('cmp:lack of resources'));
  } else {
    callback();
  }
};

const ScaleModal = ({ visible, formData, onCancel, afterSubmit }: IProps) => {
  const [form] = Form.useForm();
  const [{ leftCPU, leftMEM, leftResources }, updater, update] = useUpdate({
    leftResources: {} as MIDDLEWARE_DASHBOARD.LeftResources,
    leftCPU: 0,
    leftMEM: 0,
  });
  React.useEffect(() => {
    if (visible) {
      middlewareDashboardStore.effects.getLeftResources({ name: formData.projectName }).then((res) => {
        const node = formData.nodes || 1;
        const { availableCpu = 0, availableMem = 0 } = res;
        update({
          leftResources: res,
          leftCPU: setDecimal(availableCpu / node + (formData.cpu || 0)),
          leftMEM: setDecimal((availableMem * 1024) / node + (formData.mem || 0)),
        });
      });
    }
  }, [visible, formData.nodes, update, formData.projectName, formData.cpu, formData.mem]);
  const handleOk = () => {
    // 先判断数据是否发生改变，如果为改变，则无需执行后面的校验
    // 资源不足时，如果执行校验，会校验不通过， 因此需要将数据对比放在校验之前，数据未改变无需提交
    const current = form.getFieldsValue(['mem', 'cpu', 'nodes']);
    const currData = {
      cpu: +current.cpu,
      nodes: +current.nodes,
      mem: +current.mem,
    };
    const preData = pick(formData, ['mem', 'cpu', 'nodes']);
    if (isEqual(preData, currData)) {
      onCancel();
      return;
    }
    form.validateFields().then((data: any) => {
      const payload = {
        ...formData,
        cpu: +data.cpu,
        nodes: +data.nodes,
        mem: +data.mem,
      };
      middlewareDashboardStore.effects.scale(payload).then(() => {
        onCancel();
        afterSubmit && afterSubmit();
      });
    });
  };

  const handleChangeNode = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = +e.target.value || 1;
    if (value < 0) {
      return;
    }
    const { availableCpu = 0, availableMem = 0 } = leftResources;
    const { nodes = 0, cpu = 0, mem = 0 } = formData;
    update({
      leftCPU: setDecimal((availableCpu + cpu * nodes) / +value),
      leftMEM: setDecimal((availableMem * 1024 + mem * nodes) / +value),
    });
    setTimeout(() => {
      form.validateFields(['cpu', 'mem']);
    }, 100);
  };

  const fieldsList: IFormItem[] = [
    {
      label: i18n.t('default:name'),
      required: true,
      name: 'name',
      initialValue: formData.name,
      itemProps: {
        disabled: true,
      },
    },
    {
      label: i18n.t('cmp:number of nodes'),
      required: true,
      name: 'nodes',
      initialValue: formData.nodes,
      itemProps: {
        placeholder: i18n.t('cmp:please enter an odd number no greater than {max}', { max: 15 }),
        autoComplete: 'off',
        onChange: handleChangeNode,
      },
      rules: [
        {
          validator: validateNodes,
        },
      ],
    },
    {
      label: '',
      getComp() {
        return <Alert message={i18n.t('cmp:must be odd and cannot be greater than 15')} type="info" showIcon />;
      },
    },
    {
      label: `CPU(${i18n.t('cmp:at least {limit} or more', { limit: minCPU + i18n.t('default:core') })})`,
      required: true,
      name: 'cpu',
      initialValue: formData.cpu,
      itemProps: {
        placeholder: i18n.t('cmp:please enter a number greater than or equal to {min}', { min: minCPU }),
        addonAfter: i18n.t('default:core'),
        autoComplete: 'off',
      },
      rules: [
        {
          validator: (...arg: IValidate) => {
            validateCPU(leftCPU, ...arg);
          },
        },
      ],
    },
    {
      label: `MEM(${i18n.t('cmp:at least {limit} or more', { limit: `${minMEM}MiB` })})`,
      required: true,
      name: 'mem',
      initialValue: formData.mem,
      itemProps: {
        placeholder: i18n.t('cmp:please enter a number greater than or equal to {min}', { min: minMEM }),
        addonAfter: 'MiB',
        autoComplete: 'off',
      },
      rules: [
        {
          validator: (...arg: IValidate) => {
            validateMEM(leftMEM, ...arg);
          },
        },
      ],
    },
  ];

  const tips = React.useMemo(() => {
    const { totalCpu, totalMem, availableMem, availableCpu } = leftResources as MIDDLEWARE_DASHBOARD.LeftResources;
    const { cpu = 0, nodes = 0, mem = 0 } = formData;
    const leftCpu = availableCpu + cpu * nodes;
    const leftMem = availableMem * 1024 + mem * nodes;
    return (
      <>
        <div>
          <span className="mr-4">
            {i18n.t('cmp:total project resources')}：CPU：{totalCpu}
            {i18n.t('default:core')}
          </span>
          <span>MEM：{setDecimal(totalMem * 1024)}MiB</span>
        </div>
        <div>
          <span className="mr-4">
            {i18n.t('cmp:available resources')}：CPU：{setDecimal(leftCpu)}
            {i18n.t('default:core')}
          </span>
          <span>MEM：{setDecimal(leftMem)}MiB</span>
        </div>
      </>
    );
  }, [leftResources, formData]);

  return (
    <Modal
      title={i18n.t('cmp:scale')}
      className="middleware-op-modal scale"
      visible={visible}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
    >
      <RenderPureForm className="middleware-op-modal" list={fieldsList} form={form} layout="vertical" />
      <Alert message={tips} />
    </Modal>
  );
};

export default ScaleModal as any as (p: Omit<IProps, 'form'>) => JSX.Element;
