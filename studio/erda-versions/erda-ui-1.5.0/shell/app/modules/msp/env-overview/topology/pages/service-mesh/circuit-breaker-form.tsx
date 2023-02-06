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
import { Tabs, Button, Collapse, Pagination, Input, Form, Switch } from 'antd';
import { isEmpty, map, filter, compact } from 'lodash';
import { FormInstance } from 'core/common/interface';
import i18n from 'i18n';
import { RenderForm, EmptyHolder } from 'common';
import serviceMeshStore from '../../stores/service-mesh';
import { validators } from 'common/utils';
import './service-mesh.scss';

const { TabPane } = Tabs;
const { Panel } = Collapse;
const FormItem = Form.Item;

interface IProps {
  visible: boolean;
  type: string;
  node: any;
  onClose: () => void;
}

// @ts-ignore
const validateEmpty = (formRef: any) => (rule: any, val: string, callback: Function) => {
  const curForm = formRef && formRef.current;
  if (curForm) {
    const values = curForm.getFieldsValue() as any;
    const { consecutiveErrors, interval, baseEjectionTime, maxEjectionPercent } = values;
    const valuesLen = compact([consecutiveErrors, interval, baseEjectionTime, maxEjectionPercent]).length;
    const pass = valuesLen === 0 || valuesLen === 4;
    return callback(
      pass
        ? undefined
        : val
        ? undefined
        : i18n.t('msp:failure detection rules and instance isolation should be set at the same time'),
    );
  }
  return callback();
};

interface IHttpForm {
  data?: TOPOLOGY.ICircuitBreakerHttp;
  submitForm: (arg: TOPOLOGY.ICircuitBreakerHttp) => Promise<string>;
}

const ErrorCheckForm = ({ formRef }: { formRef: any }) => {
  return (
    <div className="error-check-form">
      <FormItem
        name="interval"
        rules={[
          { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
          { validator: validateEmpty(formRef) },
        ]}
      >
        <Input
          className="error-check-item"
          suffix={i18n.t('common:second(s)')}
          placeholder={`${i18n.t('recommended value {value}', { value: 10 })}`} // 10
        />
      </FormItem>
      <span className="ml-2 mr-2">{i18n.t('msp:continuous failure')}</span>
      <FormItem
        name="consecutiveErrors"
        rules={[
          { validator: validators.validateNumberRange({ min: 1, max: 600 }) },
          { validator: validateEmpty(formRef) },
        ]}
      >
        <Input
          className="error-check-item"
          suffix={i18n.t('times')}
          placeholder={`${i18n.t('recommended value {value}', { value: 30 })}`} // 30
        />
      </FormItem>
    </div>
  );
};

const HttpForm = ({ data, submitForm }: IHttpForm) => {
  const formRef = React.useRef(null as any);
  React.useEffect(() => {
    const curFormRef = formRef && formRef.current;
    if (curFormRef && !isEmpty(data)) {
      curFormRef.setFieldsValue({ ...data });
    }
  }, [data, formRef]);
  const handleSubmit = (form: FormInstance) => {
    form.validateFields().then((values: any) => {
      submitForm({ ...data, ...values }).then((res) => {
        form.setFieldsValue({ id: res });
      });
    });
  };
  const fieldsList = [
    {
      label: 'id',
      name: 'id',
      required: false,
      itemProps: { type: 'hidden' },
    },
    {
      label: 'type',
      name: 'type',
      initialValue: 'http',
      itemProps: { type: 'hidden' },
    },
    {
      label: i18n.t('msp:maximum number of connections'),
      name: 'maxConnections',
      required: false,
      rules: [{ validator: validators.validateNumberRange({ min: 1, max: 1024 }) }],
      itemProps: {
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 1024 }),
      },
    },
    {
      label: i18n.t('msp:maximum number of waiting requests'),
      name: 'maxPendingRequests',
      required: false,
      rules: [{ validator: validators.validateNumberRange({ min: 1, max: 1024 }) }],
      itemProps: {
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 1024 }),
      },
    },
    {
      label: i18n.t('msp:maximum number of retries'),
      name: 'maxRetries',
      required: false,
      rules: [{ validator: validators.validateNumberRange({ min: 1, max: 3 }) }],
      itemProps: {
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 3 }),
      },
    },
    {
      label: i18n.t('msp:failure detection rules'),
      getComp: () => {
        return <ErrorCheckForm formRef={formRef} />;
      },
    },
    {
      label: i18n.t('msp:shortest isolation time'),
      name: 'baseEjectionTime',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 600 }) },
        { validator: validateEmpty(formRef) },
      ],
      itemProps: {
        suffix: i18n.t('common:second(s)'),
        placeholder: `${i18n.t('please enter a number between {min} ~ {max}', {
          min: 1,
          max: 600,
        })} ${i18n.t('recommended value {value}', { value: 30 })}`, // 30
      },
    },
    {
      label: i18n.t('msp:largest isolation ratio'),
      name: 'maxEjectionPercent',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateEmpty(formRef) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: `${i18n.t('please enter a number between {min} ~ {max}', {
          min: 1,
          max: 100,
        })} ${i18n.t('recommended value {value}', { value: 50 })}`, // 50
      },
    },
    {
      label: i18n.t('whether to enable'),
      name: 'enable',
      type: 'switch',
      initialValue: false,
      required: false,
    },
    {
      getComp: ({ form }: { form: FormInstance }) => (
        <div className="mt-5">
          <Button type="primary" onClick={() => handleSubmit(form)}>
            {i18n.t('save')}
          </Button>
        </div>
      ),
    },
  ];
  return (
    <div className="circuit-breaker-http">
      <RenderForm layout="vertical" list={fieldsList} ref={formRef} />
    </div>
  );
};

interface IDubboForm {
  data?: TOPOLOGY.ICircuitBreakerDubbo[];
  hideNoRule: boolean;
  submitForm: (arg: TOPOLOGY.ICircuitBreakerDubbo) => Promise<string>;
  onSwitchChange: (value: boolean) => void;
}
const DubboForm = ({ data = [], submitForm, hideNoRule, onSwitchChange }: IDubboForm) => {
  const [useData, setUseData] = React.useState([] as TOPOLOGY.ICircuitBreakerDubbo[]);
  const [searchKey, setSearchKey] = React.useState();
  const setHideNoRule = (value: boolean) => {
    onSwitchChange(value);
  };
  const [pageNo, setPageNo] = React.useState(1);
  const pageSize = 10;
  React.useEffect(() => {
    setPageNo(1);
    if (!isEmpty(data)) {
      setUseData(
        searchKey
          ? filter(data, (item) => (item.interfaceName || '').toLowerCase().includes((searchKey || '').toLowerCase()))
          : data,
      );
    } else {
      setUseData([]);
    }
  }, [data, searchKey]);

  const len = useData.length;
  const currentData = useData.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  return (
    <div className="circuit-breaker-dubbo h-full">
      <div className="service-mesh-forms-container h-full">
        <div className="service-mesh-search">
          <Input
            placeholder={i18n.t('msp:filter by interface name')}
            onChange={(e: any) => setSearchKey(e.target.value)}
          />
          <div className="hide-no-rule-interface h-full">
            <span className="hide-no-rule-interface-label h-full">{i18n.t('msp:hide no rule interface')}</span>
            <Switch
              checked={hideNoRule}
              checkedChildren="ON"
              unCheckedChildren="OFF"
              onChange={(checked: boolean) => {
                setHideNoRule(checked);
              }}
            />
          </div>
        </div>
        {isEmpty(useData) ? (
          <EmptyHolder relative />
        ) : (
          <div className="service-mesh-collapse-forms">
            <Collapse>
              {map(currentData, (item) => (
                <Panel header={`${item.interfaceName}`} key={`${item.interfaceName}`}>
                  <div className="collapse-form-item">
                    <DubboFormItem data={item} submitForm={submitForm} />
                  </div>
                </Panel>
              ))}
            </Collapse>
            <Pagination
              className="p-5"
              pageSize={pageSize}
              total={len}
              current={pageNo}
              onChange={(pgNo: number) => {
                setPageNo(pgNo);
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

interface IDubboFormItem {
  data?: TOPOLOGY.ICircuitBreakerDubbo;
  submitForm: (arg: TOPOLOGY.ICircuitBreakerDubbo) => Promise<string>;
}
const DubboFormItem = ({ data, submitForm }: IDubboFormItem) => {
  const formRef = React.useRef(null as any);

  React.useEffect(() => {
    const curFormRef = formRef && formRef.current;
    if (curFormRef && !isEmpty(data)) {
      curFormRef.setFieldsValue({ ...data });
    }
  }, [data, formRef]);

  const handleSubmit = (form: FormInstance) => {
    form.validateFields().then((values: any) => {
      submitForm({ ...data, ...values }).then((res) => {
        form.setFieldsValue({ id: res });
      });
    });
  };

  const fieldsList = [
    {
      label: 'id',
      name: 'id',
      required: false,
      itemProps: { type: 'hidden' },
    },
    {
      label: 'type',
      name: 'type',
      initialValue: 'dubbo',
      itemProps: { type: 'hidden' },
    },
    {
      label: i18n.t('msp:maximum number of connections'),
      name: 'maxConnections',
      type: 'inputNumber',
      required: false,
      itemProps: {
        min: 1,
        max: 1024,
        precision: 0,
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 1024 }),
      },
    },
    {
      label: i18n.t('msp:maximum number of retries'),
      name: 'maxRetries',
      type: 'inputNumber',
      required: false,
      itemProps: {
        min: 1,
        max: 3,
        precision: 0,
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 3 }),
      },
    },
    {
      label: i18n.t('msp:failure detection rules'),
      getComp: () => {
        return <ErrorCheckForm formRef={formRef} />;
      },
    },
    {
      label: i18n.t('msp:shortest isolation time'),
      name: 'baseEjectionTime',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 600 }) },
        { validator: validateEmpty(formRef) },
      ],
      itemProps: {
        suffix: i18n.t('common:second(s)'),
        placeholder: `${i18n.t('please enter a number between {min} ~ {max}', {
          min: 1,
          max: 600,
        })} ${i18n.t('recommended value {value}', { value: 30 })}`, // 30
      },
    },
    {
      label: i18n.t('msp:largest isolation ratio'),
      name: 'maxEjectionPercent',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateEmpty(formRef) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: `${i18n.t('please enter a number between {min} ~ {max}', {
          min: 1,
          max: 100,
        })} ${i18n.t('recommended value {value}', { value: 50 })}`, // 50
      },
    },
    {
      label: i18n.t('whether to enable'),
      name: 'enable',
      type: 'switch',
      initialValue: false,
      required: false,
    },
    {
      getComp: ({ form }: { form: FormInstance }) => (
        <div className="mt-5">
          <Button type="primary" onClick={() => handleSubmit(form)}>
            {i18n.t('save')}
          </Button>
        </div>
      ),
    },
  ];
  return <RenderForm layout="vertical" list={fieldsList} ref={formRef} />;
};

const CircuitBreakerForm = (props: IProps) => {
  const { visible, node } = props;
  const { getCircuitBreaker, saveCircuitBreaker } = serviceMeshStore.effects;
  const { clearCircuitBreaker } = serviceMeshStore.reducers;
  const circuitBreaker = serviceMeshStore.useStore((s) => s.circuitBreaker);
  const [hideNoRule, setHideNoRule] = React.useState(false);

  const query = React.useMemo(() => {
    const { runtimeId, runtimeName, applicationId, serviceName } = node;
    return { runtimeId, runtimeName, applicationId, serverName: serviceName, hideNoRule: hideNoRule ? 'on' : 'off' };
  }, [node, hideNoRule]);

  React.useEffect(() => {
    if (visible && node) {
      getCircuitBreaker(query);
    } else {
      clearCircuitBreaker();
    }
  }, [visible, node, query, getCircuitBreaker, clearCircuitBreaker]);

  const submitForm = (values: any) => {
    return saveCircuitBreaker({ query, data: values });
  };

  return (
    <div className="service-mesh-form">
      {!isEmpty(circuitBreaker) ? (
        <Tabs className="service-mesh-tab" defaultActiveKey="http">
          <TabPane className="service-mesh-tab-panel" tab="http" key="http">
            <HttpForm data={circuitBreaker.http} submitForm={submitForm} />
          </TabPane>
          <TabPane className="service-mesh-tab-panel" tab="dubbo" key="dubbo">
            <DubboForm
              hideNoRule={hideNoRule}
              data={circuitBreaker.dubbo}
              submitForm={submitForm}
              onSwitchChange={setHideNoRule}
            />
          </TabPane>
        </Tabs>
      ) : null}
    </div>
  );
};

export default CircuitBreakerForm;
