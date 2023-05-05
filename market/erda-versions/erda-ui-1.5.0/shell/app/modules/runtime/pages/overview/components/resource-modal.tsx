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
import i18n from 'i18n';
import { FormModal } from 'common';
import { isEqual } from 'lodash';

import './resource-modal.scss';

interface IProps {
  visible: boolean;
  service: any;
  editDisabled?: boolean;
  onOk: (data: any) => void;
  onCancel: () => void;
}

const ResourceModal = ({ visible, service, editDisabled, onOk, onCancel }: IProps) => {
  const onSubmit = (data: any) => {
    const { replicas, cpu, mem } = data;
    const resources = { cpu: +cpu, mem: +mem };
    const deployments = { ...service.deployments, replicas: +replicas };
    if (!(isEqual(resources, service.resources) && isEqual(service.deployments, deployments))) {
      onOk({ resources, deployments });
    }
    onCancel();
  };

  const realPattern = /^(\+?[1-9][0-9]*|[0-9]+\.\d+)$/;

  const { resources = {} } = service;
  const resourceFields = [
    {
      label: 'CPU',
      name: 'cpu',
      type: 'inputNumber',
      pattern: realPattern,
      initialValue: resources.cpu,
      itemProps: { step: 0.01, min: 0.1 },
      formItemLayout: {
        labelCol: {
          sm: { span: 24 },
          md: { span: 10 },
          lg: { span: 12 },
        },
        wrapperCol: {
          sm: { span: 24 },
          md: { span: 14 }, // 12
          lg: { span: 12 },
        },
      },
    },
    {
      label: 'Memory(MB)',
      name: 'mem',
      type: 'inputNumber',
      pattern: realPattern,
      initialValue: resources.mem,
      itemProps: { min: 1 },
      formItemLayout: {
        labelCol: {
          sm: { span: 24 },
          md: { span: 14 },
          lg: { span: 14 },
        },
        wrapperCol: {
          sm: { span: 24 },
          md: { span: 10 },
          lg: { span: 10 },
        },
      },
    },
    {
      label: 'Scale',
      name: 'replicas',
      type: 'inputNumber',
      pattern: /^\d+$/,
      initialValue: service.deployments.replicas,
      itemProps: { min: 0, max: 50 },
      formItemLayout: {
        labelCol: {
          sm: { span: 24 },
          md: { span: 10 },
          lg: { span: 12 },
        },
        wrapperCol: {
          sm: { span: 24 },
          md: { span: 14 },
          lg: { span: 12 },
        },
      },
    },
  ];

  return (
    <FormModal
      width={800}
      title={i18n.t('runtime:resource adjust')}
      visible={visible}
      fieldsList={resourceFields}
      onOk={onSubmit}
      alertProps={{
        type: 'warning',
        showIcon: true,
        className: 'mb-3',
        message: i18n.t('dop:runtime-resource-config-form-tip'),
      }}
      onCancel={onCancel}
      modalProps={{
        className: 'adjust-resource',
        destroyOnClose: true,
      }}
      formProps={{
        layout: 'inline',
        className: `${editDisabled ? 'not-allowed' : ''}`,
      }}
    />
  );
};

export default ResourceModal;
