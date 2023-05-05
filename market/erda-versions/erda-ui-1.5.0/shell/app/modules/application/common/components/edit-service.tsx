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

import { FormComponentProps, FormInstance } from 'core/common/interface';
import React, { PureComponent } from 'react';
import { isEqual, map } from 'lodash';
import { Form, Input, Button } from 'antd';
import ObjectInput from './object-input-group';
import ListInput from './list-input-group';
import ResourceField from './resource-field';
import DeploymentsField from './deployments-field';
import HealthCheckField from './health-check-field';
import PortsField from './port-field';
import { resourcesValidator, portsValidator } from '../dice-yml-editor-validator';
import i18n from 'i18n';
import './edit-service.scss';

const { Item } = Form;
interface IEditServiceProps {
  service: any;
  editing: boolean;
  jsonContent: any;
  onSubmit: (options: any, json: any) => void;
}

interface IFormComponentState {
  service: {
    ports: string[];
    envs: object;
  };
}

class EditService extends PureComponent<IEditServiceProps & FormComponentProps, IFormComponentState> {
  formRef = React.createRef<FormInstance>();

  state = {
    service: {},
    originName: null,
  };

  static getDerivedStateFromProps(nextProps: any, prevState: any) {
    if (!isEqual(nextProps.service, prevState.service)) {
      return {
        service: nextProps.service,
        originName: nextProps.service.name,
      };
    }
    return null;
  }

  render() {
    const {
      ports = [],
      binds = [],
      // volumes = [],
      hosts = [],
      expose = [],
      envs = {},
      resources = {},
      health_check = {},
      deployments = {
        replicas: 1,
      },
      name,
      cmd,
      image,
    } = this.state.service;
    const { editing } = this.props;

    const nameField = (
      <Item
        label={i18n.t('service name')}
        name="name"
        initialValue={name}
        rules={[
          {
            required: true,
            message: i18n.t('dop:please enter the service name'),
          },
        ]}
      >
        <Input disabled={!editing} placeholder={i18n.t('dop:please enter the service name')} />
      </Item>
    );
    const _ports: any[] = [];
    map(ports, (p) => {
      if (typeof p !== 'object') {
        _ports.push({ port: p });
      } else {
        _ports.push(p);
      }
    });
    const portsField = (
      <Item
        name="ports"
        initialValue={_ports}
        rules={[
          {
            validator: portsValidator,
          },
        ]}
      >
        <PortsField disabled={!editing} />
      </Item>
    );

    let exposeField = (
      <Item
        name="expose"
        initialValue={expose}
        getValueFromEvent={(val: Array<{ value: string }>) => {
          return val?.length ? val.map((v) => v.value) : val;
        }}
      >
        <ListInput
          disabled={!editing}
          type="number"
          label={i18n.t('dop:please enter the exposed port')}
          placeholder={i18n.t('dop:exposed port')}
        />
      </Item>
    );
    if (!editing && (!expose || (expose && !expose.length))) {
      exposeField = null;
    }
    let hostsField = (
      <Item
        name="hosts"
        initialValue={hosts}
        getValueFromEvent={(val: Array<{ value: string }>) => {
          return val?.length ? val.map((v) => v.value) : val;
        }}
      >
        <ListInput disabled={!editing} label={i18n.t('dop:hosts mapping')} />
      </Item>
    );
    if (!editing && (!hosts || (hosts && !hosts.length))) {
      hostsField = null;
    }
    const resourceField = (
      <Item
        label={i18n.t('dop:resources')}
        name="resources"
        initialValue={resources}
        rules={[
          {
            required: true,
            message: i18n.t('dop:please select a resource'),
          },
          {
            validator: resourcesValidator,
          },
        ]}
      >
        <ResourceField disabled={!editing} placeholder={i18n.t('dop:please select a resource')} />
      </Item>
    );
    const deploymentsField = (
      <Item label={i18n.t('dop:deployment strategy')} name="deployments" initialValue={deployments}>
        <DeploymentsField disabled={!editing} placeholder={i18n.t('dop:please select a deployment strategy')} />
      </Item>
    );

    let envsField = (
      <Item
        name="envs"
        initialValue={envs}
        rules={[
          {
            required: true,
            message: i18n.t('dop:please enter an environment variable'),
          },
        ]}
      >
        <ObjectInput
          disabled={!editing}
          label={i18n.t('dop:environment variable')}
          errorMessage={i18n.t('dop:environment variables cannot be empty')}
        />
      </Item>
    );

    if (!editing && (!envs || (envs && !Object.keys(envs).length))) {
      envsField = null;
    }
    let cmdField = (
      <Item label={i18n.t('dop:start command')} name="cmd" initialValue={cmd}>
        <Input disabled={!editing} placeholder={i18n.t('dop:please enter the start command')} />
      </Item>
    );

    if (!editing && !cmd) {
      cmdField = null;
    }
    let bindsField = (
      <Item
        name="binds"
        initialValue={binds}
        getValueFromEvent={(val: Array<{ value: string }>) => {
          return val?.length ? val.map((v) => v.value) : val;
        }}
      >
        <ListInput
          disabled={!editing}
          required={false}
          label={i18n.t('dop:mounting')}
          placeholder={i18n.t('dop:please enter the mount directory')}
        />
      </Item>
    );
    if (!editing && (!binds || (binds && !binds.length))) {
      bindsField = null;
    }
    let healthCheckField = (
      <Item label={i18n.t('dop:health check')} name="health_check" initialValue={health_check}>
        <HealthCheckField disabled={!editing} />
      </Item>
    );

    if (!editing && (!health_check || (health_check && !Object.keys(health_check).length))) {
      healthCheckField = null;
    }
    // 未完成功能
    // const volumesField = getFieldDecorator('volumes', {
    //   initialValue: volumes,
    // })(<VolumesField required={false} label="持久化目录" placeholder="请输入文件目录" />);

    let imageField = (
      <Item label={i18n.t('dop:image name')} name="image" initialValue={image}>
        <Input disabled={!editing} placeholder={i18n.t('dop:please enter the image name')} />
      </Item>
    );
    if (!editing && !image) {
      imageField = null;
    }
    return (
      <Form ref={this.formRef} className="edit-service-container" layout="vertical">
        {nameField}
        {portsField}
        {resourceField}
        {deploymentsField}
        {healthCheckField || editing ? healthCheckField : null}
        {envsField || editing ? envsField : null}
        {exposeField || editing ? exposeField : null}
        {hostsField || editing ? hostsField : null}
        {bindsField || editing ? bindsField : null}
        {cmdField || editing ? cmdField : null}
        {imageField || editing ? imageField : null}
        {editing ? (
          <Button type="primary" ghost onClick={this.onSubmit}>
            {i18n.t('save')}
          </Button>
        ) : null}
      </Form>
    );
  }

  private onSubmit = () => {
    const { originName } = this.state;
    const { onSubmit, jsonContent } = this.props;
    const form = this.formRef.current;
    form
      ?.validateFields()
      .then((values: any) => {
        const ports: any[] = [];
        map(values.ports, (p: any) => {
          if (p.port !== undefined) {
            ports.push(p.protocol ? p : p.port); // 如果没有配协议，就只存端口号
          }
        });
        onSubmit(
          {
            ...values,
            ports,
            originName,
          },
          jsonContent,
        );
      })
      .catch((e: Obj) => {
        if (e.errorFields) {
          form?.scrollToField(e.errorFields?.[0]?.name);
        }
      });
  };
}

export default EditService;
