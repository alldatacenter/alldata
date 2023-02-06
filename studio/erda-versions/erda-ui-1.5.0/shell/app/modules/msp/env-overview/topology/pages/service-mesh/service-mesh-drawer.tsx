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
import { Drawer } from 'antd';
import { get } from 'lodash';
import i18n from 'i18n';
import CircuitBreakerForm from './circuit-breaker-form';
import faultInjectForm from './fault-inject-form';

export enum IMeshType {
  faultInject = 'faultInject',
  circuitBreaker = 'circuitBreaker',
}

interface IProps {
  visible: boolean;
  type: string;
  node: any;
  onClose: () => void;
}

const FormMap = {
  circuitBreaker: { Comp: CircuitBreakerForm, title: i18n.t('msp:circuit breaker and current limiting') },
  faultInject: { Comp: faultInjectForm, title: i18n.t('msp:fault inject') },
};
const emptyComp = () => <div />;
const ServiceMeshDrawer = (props: IProps) => {
  const { type, onClose, visible, node } = props;
  const FormComp = get(FormMap, `${type}.Comp`) || emptyComp;
  return (
    <Drawer
      visible={visible}
      title={`${get(FormMap, `${type}.title`)} (${get(node, 'serviceName')})`}
      width={800}
      onClose={onClose}
    >
      {visible ? <FormComp {...props} /> : null}
    </Drawer>
  );
};

export default ServiceMeshDrawer;
