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
import { map, get, isPlainObject, isArray, isString, isEmpty, filter } from 'lodash';
import { Form as DefaultForm } from '../form';
import { Modal } from './common';
import { Button, Modal as NusiModal } from 'antd';
import { FORM_GROUP } from '../nusi-form/config';
import { Form as DiceForm } from 'app/configForm/nusi-form/form';
import i18n from 'i18n';
import './form-preview.scss';

interface IProps {
  formFields: IField[];
  currentEditField: any;
  Form?: any;
  renderField?: any;
  componentMap: any;
  showGetConfig?: boolean;
  copyField: (arg: IField, key: string) => void;
  addFormField: (arg: any) => void;
  deleteField: (arg: IField) => void;
  editField: (arg: IField) => void;
  changeFieldPos: (form: number, to: number) => void;
  updateField: (arg: any, field: IField) => void;
  footer?: React.ReactNode;
  nameField?: IField;
}

// 中间层，接入antd或其他form，把field上的配置映射到组件的字段上
export const defaultPreviewField =
  ({ changePos, onEdit, onDelete, currentEditField, onCopy }: any) =>
  (compMap: any) =>
  ({ fields, form }: any) => {
    return (
      <div>
        {fields.map((f: any, index: number) => {
          const Component = compMap[f.component];
          const isOnEdit = get(currentEditField, 'key') === f.key;
          return (
            <div key={`${f.key}`} className={`form-preview-item ${isOnEdit ? 'on-edit' : ''}`}>
              <div className="form-view">
                <Component key={f.key} fieldConfig={f} form={form} />
              </div>
              <div className="form-operation">
                <span onClick={() => changePos(index, -1)}>{i18n.t('move up')}</span>
                <span onClick={() => changePos(index, 1)}>{i18n.t('move down')}</span>
                <span onClick={() => onCopy(f)}>{i18n.t('copy')}</span>
                <span onClick={() => onEdit(f)}>{i18n.t('edit')}</span>
                <span onClick={() => onDelete(f)}>{i18n.t('delete')}</span>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

export const FormPreview = React.forwardRef((props: IProps, ref: any) => {
  const {
    addFormField,
    formFields,
    deleteField,
    editField,
    copyField,
    changeFieldPos,
    currentEditField,
    Form = DefaultForm,
    renderField = defaultPreviewField,
    componentMap,
    showGetConfig = true,
    footer = null,
    nameField,
  } = props;
  const form = React.useRef(null as any);
  const addForm = React.useRef(null as any);
  const addGroupForm = React.useRef(null as any);
  const configRef = React.useRef(null as any);

  const [fields, setFields] = React.useState([] as any[]);
  const [modalVis, setModalVis] = React.useState(false);
  const [configVis, setConfigVis] = React.useState(false);
  const [configInfo, setConfigInfo] = React.useState([] as any[]);
  const [copyData, setCopyData] = React.useState({} as any);
  const [curGroup, setCurGroup] = React.useState({} as any);
  const [groupModalVis, setGroupModalVis] = React.useState(false);
  const [previewVis, setPreviewVis] = React.useState(false);
  const [previewFields, setPreviewFields] = React.useState([] as any[]);

  const isEdit = !isEmpty(copyData);

  React.useEffect(() => {
    form.current.getConfigInfo = getConfigInfo;
    ref && (ref.current = form.current);
  }, [ref]);

  React.useEffect(() => {
    setFields(
      map(formFields, (item: any, index: number) => ({
        ...item,
        type: item.component,
        index,
      })),
    );
  }, [formFields]);

  const onFieldChange = (data: any) => {
    // console.log('data change-----', data);
  };

  const getConfigInfo = () => {
    const config = [] as any[];
    if (form && form.current) {
      const curConfig = form.current.getFields();
      map(curConfig, (item) => {
        const configItem = {};
        map(item, (objItem, key) => {
          if (typeof objItem !== 'function') {
            let useable = !(
              (key === 'validateTrigger' && objItem.toString() === 'onChange') ||
              ((isPlainObject(objItem) || isArray(objItem) || isString(objItem)) && isEmpty(objItem))
            );
            if (key === 'disabled' && !objItem) useable = false;
            if (key === 'required' && !objItem) useable = false;
            if (key === 'value') useable = false;
            if (key === 'rules') {
              useable = false;
              const useRules = [] as any[];
              map(objItem, (oItem) => {
                if (!oItem.validator) useRules.push(oItem);
              });
              if (!isEmpty(useRules)) {
                configItem[key] = useRules;
              }
            }
            if (useable) {
              configItem[key] = objItem;
            }
          }
        });
        const {
          valid,
          isTouched,
          converted,
          remove,
          visible,
          _disabledSubscribes,
          _disabledWatchers,
          _hideSubscribes,
          _hideWatchers,
          _removeSubscribes,
          _removeWatchers,
          ...rest
        } = configItem as any;

        config.push({ ...rest });
      });
    }
    return config;
  };

  const getConfig = () => {
    setConfigInfo(getConfigInfo());
    setConfigVis(true);
    if (configRef && configRef.current) {
      configRef.current.select();
    }
  };

  React.useEffect(() => {
    if (configVis && configRef && configRef.current) {
      configRef.current.select();
    }
  }, [configVis]);

  const closeConfig = () => {
    setConfigInfo([]);
    setConfigVis(false);
  };

  const onOk = () => {
    if (addForm && addForm.current) {
      addForm.current.validateFields().then((value: any) => {
        if (isEdit) {
          copyField(copyData, value.key);
          setCopyData({});
        } else {
          addFormField(value);
        }
        setModalVis(false);
      });
    }
  };

  const onAddGroupOk = () => {
    if (addGroupForm && addGroupForm.current) {
      addGroupForm.current.validateFields().then((value: any) => {
        const curGroupKey = get(curGroup, 'key');
        addFormField({ ...value, key: `${curGroupKey}.${value.key}`, group: curGroupKey });
        onCancelGroupAdd();
      });
    }
  };

  const onCopy = (data: any) => {
    setCopyData(data);
    setModalVis(true);
  };

  const onAddGroup = (gField: IField) => {
    setCurGroup(gField);
    setGroupModalVis(true);
  };

  const onCancelGroupAdd = () => {
    setGroupModalVis(false);
    setCurGroup({});
  };

  const renderF = React.useMemo(() => {
    return renderField({
      changePos: changeFieldPos,
      onEdit: editField,
      onDelete: deleteField,
      onCopy,
      onAddGroup,
      currentEditField,
    });
  }, [renderField, changeFieldPos, editField, deleteField, currentEditField]);

  const addFields = [
    nameField || {
      index: 0,
      label: i18n.t('field'),
      key: 'key',
      type: 'input',
      component: 'input',
      required: true,
      componentProps: {
        autoComplete: 'off',
        maxLength: 50,
      },
      rules: [
        {
          validator: (v: string) => {
            if (v === 'nodeName') {
              return [false, i18n.t('can not be nodeName')];
            }
            const curFields = (ref && ref.current && ref.current.getFields()) || [];
            const keyArr = map(curFields, 'key');
            return [!keyArr.includes(v), i18n.t('{name} already exists', { name: 'key' })];
          },
        },
      ],
    },
    {
      index: 1,
      label: i18n.t('component'),
      key: 'component',
      type: 'select',
      component: 'select',
      required: true,
      disabled: isEdit,
      dataSource: {
        static: map(componentMap, (item: any) => ({
          value: item.value,
          name: `${item.name}(${item.dataType || item.value})`,
        })),
      },
      componentProps: {
        showSearch: true,
      },
    },
  ];

  const addGroupItemFields = React.useMemo(
    () => [
      nameField || {
        index: 0,
        label: i18n.t('field'),
        key: 'key',
        type: 'input',
        component: 'input',
        required: true,
        componentProps: {
          autoComplete: 'off',
          maxLength: 50,
        },
        rules: [
          {
            validator: (v: string) => {
              if (v === 'nodeName') {
                return [false, i18n.t('can not be nodeName')];
              }
              const _g = get(curGroup, 'group');
              const curFields = (ref && ref.current && ref.current.getFields()) || [];
              const keyArr = map(curFields, 'key');
              return [!keyArr.includes(`${_g}.${v}`), i18n.t('{name} already exists', { name: 'key' })];
            },
          },
        ],
      },
      {
        index: 1,
        label: i18n.t('component'),
        key: 'component',
        type: 'select',
        component: 'select',
        required: true,
        disabled: isEdit,
        dataSource: {
          static: filter(
            map(componentMap, (item: any) => ({
              value: item.value,
              name: `${item.name}(${item.dataType || item.value})`,
            })),
            (cItem: any) => cItem.value !== FORM_GROUP,
          ),
        },
        componentProps: {
          showSearch: true,
        },
      },
      // eslint-disable-next-line react-hooks/exhaustive-deps
    ],
    [componentMap, curGroup],
  );

  const doPreview = () => {
    setPreviewFields(getConfigInfo());
    setPreviewVis(true);
  };

  const closePreview = () => {
    setPreviewFields([]);
    setPreviewVis(false);
  };

  return (
    <div className="dice-form-preview">
      <h4>{i18n.t('common:form preview')}</h4>
      <Form fields={fields} formRef={form} onChange={onFieldChange} renderField={renderF} />
      <div className="mt-4">
        <button className="dice-form-editor-button mr-4" onClick={() => setModalVis(true)}>
          +
        </button>
        {showGetConfig ? (
          <>
            <button className="dice-form-editor-button mr-4" onClick={getConfig}>
              {i18n.t('common:get configuration')}
            </button>

            <button className="dice-form-editor-button" onClick={doPreview}>
              {i18n.t('common:form preview')}
            </button>
          </>
        ) : null}
      </div>
      <Modal
        title={i18n.t('common:add form item')}
        visible={modalVis}
        onOk={onOk}
        onCancel={() => {
          setModalVis(false);
          setCopyData({});
        }}
      >
        <Form
          fields={addFields}
          formRef={addForm}
          value={copyData}
          // onChange={onFieldChange}
        />
      </Modal>
      <Modal
        // title={`添加表单组${get(curGroup, 'group')}子选项`}
        title={i18n.t('common:add form sub item {name}', { name: get(curGroup, 'group') })}
        visible={groupModalVis}
        onOk={onAddGroupOk}
        onCancel={onCancelGroupAdd}
      >
        {groupModalVis ? (
          <Form key={get(curGroup, 'group') || '_'} fields={addGroupItemFields} formRef={addGroupForm} />
        ) : null}
      </Modal>
      <Modal title={i18n.t('common:form configuration')} visible={configVis} onOk={closeConfig} onCancel={closeConfig}>
        <textarea ref={configRef} readOnly className="config-pre" value={JSON.stringify(configInfo, null, 2)} />
      </Modal>
      {footer ? <div className="dice-form-preview-footer">{footer}</div> : null}
      <PreviewForm visible={previewVis} fields={previewFields} onClose={closePreview} />
    </div>
  );
});

const PreviewForm = (props: any) => {
  const { fields = [], visible, onClose } = props;
  const formRef = React.useRef(null as any);

  const formConfig = React.useMemo(() => fields, [fields]);

  const onFinish = (data: any) => {
    // used log
    // eslint-disable-next-line no-console
    console.log('------', data);
    onClose();
  };

  return (
    <NusiModal visible={visible} title={i18n.t('common:form preview')} footer={null} onCancel={onClose}>
      {visible ? (
        <DiceForm fields={formConfig} formRef={formRef} onFinish={onFinish}>
          <DiceForm.Submit Button={Button} type="primary" />
        </DiceForm>
      ) : null}
    </NusiModal>
  );
};
