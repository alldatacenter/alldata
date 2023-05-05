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

import React, { PureComponent } from 'react';
import PropertyView from './property-view';
import i18n from 'i18n';
import './dice-service-view.scss';

interface IDataSource {
  ports: Array<{ protocol: string; port: number }>;
  expose: string[];
  hosts: string[];
  resources: {
    cpu: number;
    mem: number;
    disk: number;
    network: {
      mode: 'overlay' | 'host';
    };
  };
  deployments: {
    mode: 'global' | 'replicated';
    replicas: number;
    labels?: string[];
  };
  envs: object;
}

interface IPropertyViewProps {
  dataSource: IDataSource;
}

export default class extends PureComponent<IPropertyViewProps, any> {
  render() {
    const { dataSource } = this.props;
    const { resources, deployments, ports, envs } = dataSource;
    const envKeys = Object.keys(envs || {});
    const envContent = envKeys.length ? (
      <span className="envs-column w-full">
        {i18n.t('dop:environment variable')}: <PropertyView dataSource={envs} />
      </span>
    ) : null;
    return (
      <div>
        <span className="dice-service-detail-column">
          <span>CPU：</span>
          <span>{(resources && resources.cpu) || '-'}</span>
        </span>
        <span className="dice-service-detail-column">
          <span>{i18n.t('memory')}：</span>
          <span>{(resources && resources.mem) || '-'}</span>
        </span>
        <span className="dice-service-detail-column">
          <span>{i18n.t('number of instance')}：</span>
          <span>{(deployments && deployments.replicas) || '-'}</span>
        </span>
        <span className="dice-service-detail-column">
          <span>{i18n.t('port')}：</span>
          <span>
            {Array.isArray(ports)
              ? ports.map((p) => (typeof p === 'object' ? `${p.protocol || ''}:${p.port}` : p)).join('/')
              : '-'}
          </span>
        </span>
        {envContent}
      </div>
    );
  }
}
