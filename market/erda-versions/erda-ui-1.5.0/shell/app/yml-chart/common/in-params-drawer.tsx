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
import { Drawer, Button } from 'antd';
import { map, get, find, isNaN, compact, isEmpty, isBoolean } from 'lodash';
import FormEditor from 'app/yml-chart/form-editor-for-pipeline/form-editor';
import { Form } from 'dop/pages/form-editor/index';
import i18n from 'i18n';
import './in-params-drawer.scss';

export interface IInPramasDrawerProps {
  visible: boolean;
  nodeData: { data: PIPELINE.IPipelineInParams[] };
  editing: boolean;
  closeDrawer: () => void;
  onSubmit?: (arg: PIPELINE.IPipelineInParams[]) => void;
}

const noop = () => {};

const InParamsDrawer = (props: IInPramasDrawerProps) => {
  const { visible, closeDrawer, onSubmit: submit = noop, nodeData, editing } = props;
  const formEditorRef = React.useRef(null as any);
  const [fields, setFields] = React.useState([] as any[]);
  const [formKey, setFormKey] = React.useState(1);

  const onSubmit = () => {
    if (formEditorRef && formEditorRef.current) {
      const formConfig = formEditorRef.current.getConfigInfo();
      submit(formDataToYmlData(formConfig));
      closeDrawer();
    }
  };
  React.useEffect(() => {
    setFields(
      map(ymlDataToFormData(get(nodeData, 'data') || []), (item) => {
        return editing ? { ...item } : { ...item, disabled: true };
      }),
    );
  }, [editing, nodeData, visible]);

  React.useEffect(() => {
    setFormKey((preFormKey) => preFormKey + 1);
  }, [fields]);

  const drawerProps = editing
    ? {
        title: i18n.t('dop:inputs configuration'),
        width: '80%',
        maskClosable: false,
      }
    : {
        title: i18n.t('dop:inputs form'),
        width: '40%',
        maskClosable: true,
      };

  return (
    <Drawer
      visible={visible}
      onClose={closeDrawer}
      {...drawerProps}
      destroyOnClose
      className="pipeline-in-params-drawer"
    >
      {editing ? (
        <>
          <FormEditor fields={fields} ref={formEditorRef} />
          <div className="pipeline-in-params-drawer-footer">
            <Button onClick={closeDrawer} className="mr-2">
              {i18n.t('cancel')}
            </Button>
            <Button onClick={onSubmit} type="primary">
              {i18n.t('ok')}
            </Button>
          </div>
        </>
      ) : isEmpty(fields) ? (
        <div>{i18n.t('dop:have no params')}</div>
      ) : (
        <Form fields={fields} key={formKey} />
      )}
    </Drawer>
  );
};

export default InParamsDrawer;

interface IFormData {
  key: string; // 对应name
  required: boolean; // 对应required
  defaultValue?: string; // 对应default
  component: string; // 对应type
  label: string; // 等于key，不可修改
  labelTip?: string; // 对应desc
}

const typeMapping = [
  { type: 'string', component: 'input' }, // string类型对应于input组件
  { type: 'int', component: 'inputNumber' }, // int类型对应inputNumber组件
  { type: 'boolean', component: 'switch' }, // boolean类型对应switch组件
];
const componentList = map(typeMapping, 'component');

// formData转为ymlData
export const formDataToYmlData = (data: IFormData[]): PIPELINE.IPipelineInParams[] => {
  return compact(
    map(data, (item) => {
      const { key, required, defaultValue, component, labelTip } = item;
      const type = get(find(typeMapping, { component }), 'type') || component;
      let _default = defaultValue as any;
      if (type === 'int') _default = isNaN(+_default) ? undefined : +_default;
      if (type === 'boolean') _default = isBoolean(_default) ? _default : _default === 'true';
      if (!key) return null;
      const dataItem = {
        name: key,
        required,
        default: _default || undefined,
        type,
        desc: labelTip,
      };
      const res = {};
      map(dataItem, (val, k) => {
        if (val !== undefined) {
          res[k] = val;
        }
      });
      return res as PIPELINE.IPipelineInParams;
    }),
  );
};

export const ymlDataToFormData = (data: PIPELINE.IPipelineInParams[], val: Obj = {}): IFormData[] => {
  return map(data, (item) => {
    const { name, required = false, default: _default, type, desc, inConfig, value } = item;
    let _defaultVal = val[item.name] !== null && val[item.name] !== undefined ? val[item.name] : _default || undefined;
    const component = get(find(typeMapping, { type }), 'component') || type;
    let labelTip = desc;
    if (_defaultVal !== null && _defaultVal !== undefined && value === _defaultVal) {
      labelTip = `${inConfig ? i18n.t('dop:default value comes from the environment configuration') : ''}; ${desc}`;
    }
    if (type === 'boolean' && _defaultVal !== true) _defaultVal = false;
    if (type === 'int') _defaultVal = _defaultVal === null ? undefined : isNaN(+_defaultVal) ? undefined : +_defaultVal;
    return {
      key: name,
      label: name,
      required,
      defaultValue: _defaultVal,
      component: componentList.includes(component) ? component : 'input',
      labelTip,
    };
  });
};
