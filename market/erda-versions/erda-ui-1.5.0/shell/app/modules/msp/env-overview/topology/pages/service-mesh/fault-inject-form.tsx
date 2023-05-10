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
import { Tabs, Button, Collapse, Pagination, Input, Popconfirm, Switch } from 'antd';
import { isEmpty, map, filter, find, compact, get } from 'lodash';
import { FormInstance } from 'core/common/interface';
import i18n from 'i18n';
import { RenderForm, EmptyHolder, Icon as CustomIcon, FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import serviceMeshStore from '../../stores/service-mesh';
import { validators } from 'common/utils';
import './service-mesh.scss';

const { TabPane } = Tabs;
const { Panel } = Collapse;

interface IProps {
  visible: boolean;
  type: string;
  node: any;
  onClose: () => void;
}

interface IHttpForm {
  data?: TOPOLOGY.IFaultInjectHttp[];
  submitForm: (
    arg: TOPOLOGY.IFaultInjectHttp,
    { isAdd, isHttp }?: { isAdd?: boolean; isHttp?: boolean },
  ) => Promise<string | void>;
  deleteHttp: (arg: string) => void;
}

const validateDelayEmpty = (formRef: any) => (rule: any, val: string, callback: Function) => {
  let curForm = null as any;
  if (formRef && formRef.current) {
    curForm = formRef.current;
  } else if (formRef && formRef.getFieldsValue) {
    curForm = formRef;
  }
  if (curForm) {
    const values = curForm.getFieldsValue() as any;
    const { fixedDelay, delayPercentage } = values;
    const pass = compact([fixedDelay, delayPercentage]).length !== 1;
    return callback(
      pass
        ? undefined
        : val
        ? undefined
        : i18n.t('msp:delay time and delay ratio should should be set at the same time'),
    );
  }
  return callback();
};

const validateErrorEmpty = (formRef: any) => (rule: any, val: string, callback: Function) => {
  let curForm = null as any;
  if (formRef && formRef.current) {
    curForm = formRef.current;
  } else if (formRef && formRef.getFieldsValue) {
    curForm = formRef;
  }
  if (curForm) {
    const values = curForm.getFieldsValue() as any;
    const { abortStatus, abortPercentage } = values;
    const pass = compact([abortStatus, abortPercentage]).length !== 1;
    return callback(
      pass ? undefined : val ? undefined : i18n.t('msp:error code and error ratio should be set at the same time'),
    );
  }
  return callback();
};

const HttpForm = ({ data = [], submitForm, deleteHttp }: IHttpForm) => {
  const [useData, setUseData] = React.useState([] as TOPOLOGY.IFaultInjectHttp[]);
  const formRef = React.useRef(null as any);
  const [{ searchKey, pageNo, showAdd }, updater] = useUpdate({
    searchKey: undefined,
    pageNo: 1,
    showAdd: false,
  });
  const pageSize = 10;
  React.useEffect(() => {
    updater.pageNo(1);
    if (!isEmpty(data)) {
      setUseData(
        searchKey
          ? filter(data, (item) => (item.path || '').toLowerCase().includes((searchKey || '').toLowerCase()))
          : data,
      );
    } else {
      setUseData([]);
    }
  }, [data, searchKey, updater]);

  const httpFieldsList = [
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
      label: i18n.t('path prefix'),
      name: 'path',
      initialValue: '/',
      rules: [
        { pattern: /^\//, message: i18n.t('path starts with /') },
        {
          validator: (rule: any, v: string, callback: Function) => {
            const curHttp = find(data, { path: v });
            if (curHttp) {
              callback(i18n.t('path already exists'));
            } else {
              callback();
            }
          },
        },
      ],
    },
    {
      label: i18n.t('msp:delay time'),
      name: 'fixedDelay',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 10 }) },
        { validator: validateDelayEmpty(get(formRef, 'current')) },
      ],
      itemProps: {
        suffix: i18n.t('common:second(s)'),
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 10 }),
      },
    },
    {
      label: i18n.t('msp:delay ratio'),
      name: 'delayPercentage',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateDelayEmpty(get(formRef, 'current')) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 100 }),
      },
    },
    {
      label: i18n.t('msp:error code'),
      name: 'abortStatus',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 100, max: 600 }) },
        { validator: validateErrorEmpty(get(formRef, 'current')) },
      ],
      itemProps: {
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 100, max: 600 }),
      },
    },
    {
      label: i18n.t('msp:error ratio'),
      name: 'abortPercentage',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateErrorEmpty(get(formRef, 'current')) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 100 }),
      },
    },
    {
      label: i18n.t('whether to enable'),
      name: 'enable',
      type: 'switch',
      initialValue: false,
      required: false,
    },
  ];

  const toggleShowAdd = (show?: boolean) => {
    updater.showAdd(show === undefined ? !showAdd : show);
  };

  const handleFormSubmit = (values: any) => {
    submitForm(values, { isAdd: true });
    toggleShowAdd(false);
  };

  const onDelete = (item: TOPOLOGY.IFaultInjectHttp) => {
    deleteHttp(item.id);
    updater.pageNo(1);
  };

  const len = useData.length;
  const currentData = useData.slice((pageNo - 1) * pageSize, pageNo * pageSize);
  return (
    <div className="fault-inject-dubbo h-full">
      <div className="service-mesh-forms-container h-full">
        <div className="service-mesh-search">
          <Input
            placeholder={i18n.t('msp:filter by path name')}
            onChange={(e: any) => updater.searchKey(e.target.value)}
          />
          <Button
            type="primary"
            onClick={() => {
              toggleShowAdd(true);
            }}
          >
            {i18n.t('dop:add')}
          </Button>
        </div>
        {isEmpty(useData) ? (
          <EmptyHolder relative />
        ) : (
          <div className="service-mesh-collapse-forms">
            <Collapse>
              {map(currentData, (item) => (
                <Panel
                  header={
                    <div className="collapse-form-header">
                      {item.path}
                      <Popconfirm
                        title={`${i18n.t('is it confirmed?')}`}
                        onConfirm={(e: any) => {
                          e.stopPropagation();
                          onDelete(item);
                        }}
                        onCancel={(e: any) => {
                          e.stopPropagation();
                        }}
                      >
                        <CustomIcon
                          className="cursor-pointer"
                          type="shanchu"
                          onClick={(e: any) => e.stopPropagation()}
                        />
                      </Popconfirm>
                    </div>
                  }
                  key={`${item.id}`}
                >
                  <div className="collapse-form-item">
                    <HttpFormItem data={item} submitForm={submitForm} allData={data} />
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
                updater.pageNo(pgNo);
              }}
            />
          </div>
        )}
      </div>
      <FormModal
        name="http"
        fieldsList={httpFieldsList}
        visible={showAdd}
        ref={formRef}
        onOk={handleFormSubmit}
        onCancel={() => toggleShowAdd(false)}
        modalProps={{
          destroyOnClose: true,
          maskClosable: false,
        }}
      />
    </div>
  );
};

interface IHttpFormItem {
  data?: TOPOLOGY.IFaultInjectHttp;
  allData: TOPOLOGY.IFaultInjectHttp[];
  submitForm: (
    arg: TOPOLOGY.IFaultInjectHttp,
    { isAdd, isHttp }?: { isAdd?: boolean; isHttp?: boolean },
  ) => Promise<string | void>;
}
const HttpFormItem = ({ data, submitForm, allData }: IHttpFormItem) => {
  const formRef = React.useRef(null as any);
  React.useEffect(() => {
    const curFormRef = formRef && formRef.current;
    if (curFormRef && !isEmpty(data)) {
      curFormRef.setFieldsValue({ ...data });
    }
  }, [data, formRef]);

  const handleSubmit = (form: FormInstance) => {
    form.validateFields().then((values: any) => {
      submitForm({ ...data, ...values }, { isHttp: true });
    });
  };
  const httpFieldsList = [
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
      label: i18n.t('path prefix'),
      name: 'path',
      initialValue: '/',
      rules: [
        { pattern: /^\//, message: i18n.t('path starts with /') },
        {
          validator: (rule: any, v: string, callback: Function) => {
            const curHttp = find(allData, { path: v });
            if (curHttp && data && v !== data.path) {
              callback(i18n.t('path already exists'));
            } else {
              callback();
            }
          },
        },
      ],
    },
    {
      label: i18n.t('msp:delay time'),
      name: 'fixedDelay',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 10 }) },
        { validator: validateDelayEmpty(formRef) },
      ],
      itemProps: {
        suffix: i18n.t('common:second(s)'),
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 10 }),
      },
    },
    {
      label: i18n.t('msp:delay ratio'),
      name: 'delayPercentage',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateDelayEmpty(formRef) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 100 }),
      },
    },
    {
      label: i18n.t('msp:error code'),
      name: 'abortStatus',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 100, max: 600 }) },
        { validator: validateErrorEmpty(formRef) },
      ],
      itemProps: {
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 100, max: 600 }),
      },
    },
    {
      label: i18n.t('msp:error ratio'),
      name: 'abortPercentage',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateErrorEmpty(formRef) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 100 }),
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
    <div className="fault-inject-http">
      <RenderForm layout="vertical" list={httpFieldsList} ref={formRef} />
    </div>
  );
};

interface IDubboForm {
  data?: TOPOLOGY.IFaultInjectDubbo[];
  hideNoRule: boolean;
  submitForm: (
    arg: TOPOLOGY.IFaultInjectDubbo,
    { isAdd, isHttp }?: { isAdd?: boolean; isHttp?: boolean },
  ) => Promise<string | void>;
  onSwitchChange: (value: boolean) => void;
}
const DubboForm = ({ data = [], submitForm, onSwitchChange, hideNoRule }: IDubboForm) => {
  const [useData, setUseData] = React.useState([] as TOPOLOGY.IFaultInjectDubbo[]);
  const [searchKey, setSearchKey] = React.useState();
  const [pageNo, setPageNo] = React.useState(1);
  const setHideNoRule = (value: boolean) => {
    onSwitchChange(value);
  };
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
    <div className="fault-inject-dubbo h-full">
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
                  <div className="collapse-form-item fault-inject-dubbo">
                    <DubboFormItem data={{ ...item }} submitForm={submitForm} />
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
  data?: TOPOLOGY.IFaultInjectDubbo;
  submitForm: (
    arg: TOPOLOGY.IFaultInjectDubbo,
    { isAdd, isHttp }?: { isAdd?: boolean; isHttp?: boolean },
  ) => Promise<string | void>;
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
      label: i18n.t('msp:delay time'),
      name: 'fixedDelay',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 10 }) },
        { validator: validateDelayEmpty(formRef) },
      ],
      itemProps: {
        suffix: i18n.t('common:second(s)'),
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 10 }),
      },
    },
    {
      label: i18n.t('msp:delay ratio'),
      name: 'delayPercentage',
      required: false,
      rules: [
        { validator: validators.validateNumberRange({ min: 1, max: 100 }) },
        { validator: validateDelayEmpty(formRef) },
      ],
      itemProps: {
        suffix: '%',
        placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 1, max: 100 }),
      },
    },
    {
      label: i18n.t('whether to enable'),
      name: 'enable',
      type: 'switch',
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

const FaultInjectForm = (props: IProps) => {
  const { visible, node } = props;
  const { getFaultInject, saveFaultInject, deleteFaultInject } = serviceMeshStore.effects;
  const { clearFaultInject } = serviceMeshStore.reducers;
  const faultInject = serviceMeshStore.useStore((s) => s.faultInject);
  const [hideNoRule, setHideNoRule] = React.useState(false);

  const query = React.useMemo(() => {
    const { runtimeId, runtimeName, applicationId, serviceName } = node;
    return { runtimeId, runtimeName, applicationId, serverName: serviceName, hideNoRule: hideNoRule ? 'on' : 'off' };
  }, [node, hideNoRule]);

  React.useEffect(() => {
    if (visible && node) {
      getFaultInject(query);
    } else {
      clearFaultInject();
    }
  }, [visible, node, query, getFaultInject, clearFaultInject]);

  const submitForm = (values: any, extra: { isAdd?: boolean; isHttp?: boolean } = {}) => {
    const { isAdd, isHttp } = extra || {};
    if (isAdd) {
      return saveFaultInject({ query, data: values }).then((res) => {
        if (res) getFaultInject(query);
      });
    }
    const save = saveFaultInject({ query, data: values });
    if (isHttp) {
      return save.then((res) => {
        if (res) getFaultInject(query);
      });
    } else {
      return save;
    }
  };

  const deleteHttp = (id: string) => {
    deleteFaultInject({ id, ...query }).then((res) => {
      if (res) getFaultInject(query);
    });
  };

  return (
    <div className="service-mesh-form">
      {!isEmpty(faultInject) ? (
        <Tabs className="service-mesh-tab" defaultActiveKey="http">
          <TabPane className="service-mesh-tab-panel" tab="http" key="http">
            <HttpForm data={faultInject.http} submitForm={submitForm} deleteHttp={deleteHttp} />
          </TabPane>
          <TabPane className="service-mesh-tab-panel" tab="dubbo" key="dubbo">
            <DubboForm
              data={faultInject.dubbo}
              submitForm={submitForm}
              hideNoRule={hideNoRule}
              onSwitchChange={setHideNoRule}
            />
          </TabPane>
        </Tabs>
      ) : null}
    </div>
  );
};

export default FaultInjectForm;
