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
import { map, get } from 'lodash';
import { Modal, Row, Col } from 'antd';
import { clusterImgMap } from '../config';
import i18n from 'i18n';
import './cluster-type-modal.scss';

export const clusterTypeMap = [
  [
    {
      type: 'alicloud-cs-managed',
      name: i18n.t('cmp:alibaba Cloud Container Service Cluster (Hosted Version)'),
      icon: clusterImgMap['alicloud-cs-managed'],
      description: i18n.t('cmp:based on Alibaba Cloud Container Service (managed), create an Erda managed cluster'),
    },
    {
      type: 'alicloud-cs',
      name: i18n.t('cmp:alibaba Cloud Container Service Cluster (proprietary version)'),
      icon: clusterImgMap['alicloud-cs'],
      description: i18n.t('cmp:based on Alibaba Cloud Container Service (Dedicated), create an Erda managed cluster'),
    },
    {
      type: 'erdc', // existing-resource-deploy-cluster
      name: i18n.t('cmp:self-built cluster'),
      icon: clusterImgMap.erdc,
      description: i18n.t('cmp:based on existing resources, build an Erda managed cluster'),
    },
  ],
  [
    {
      type: 'k8s',
      name: 'Kubernetes',
      icon: clusterImgMap.k8s,
      description: i18n.t('cmp:import an existing Erda {type} cluster', { type: 'Kubernetes' }),
    },
    {
      type: 'edas',
      name: 'EDAS',
      icon: clusterImgMap.edas,
      description: i18n.t('cmp:import an existing Erda {type} cluster', { type: 'EDAS' }),
    },
  ],
];

const TypeCard = (props: any) => {
  const { onChosen, ...type } = props;
  const [isHover, setIsHover] = React.useState(false);
  const onHover = () => setIsHover(true);
  const outHover = () => setIsHover(false);
  return (
    <div
      className="cluster-type-card"
      onMouseEnter={onHover}
      onMouseLeave={outHover}
      onClick={() => {
        onChosen(type);
      }}
    >
      <div className="type-icon">
        <img src={isHover ? get(type, 'icon.active') : get(type, 'icon.default')} />
      </div>
      <div className="type-name">{type.name}</div>
      <div className="type-description">{type.description}</div>
    </div>
  );
};

export const ClusterTypeModal = (props: any) => {
  const { visible, toggleModal, onSubmit } = props;

  const handleSubmit = (data: any) => {
    onSubmit(data.type);
    toggleModal();
  };

  return (
    <Modal
      title={i18n.t('cmp:select the cluster type')}
      visible={visible}
      onOk={handleSubmit}
      onCancel={toggleModal}
      width={650}
      footer={null}
    >
      <div>
        {map(clusterTypeMap, (subItem, idx) => {
          return (
            <Row key={idx} gutter={16} className="cluster-type-row mb-4">
              {map(subItem, (item) => (
                <Col key={item.type} span={8}>
                  <TypeCard onChosen={handleSubmit} {...item} />
                </Col>
              ))}
            </Row>
          );
        })}
      </div>
    </Modal>
  );
};
