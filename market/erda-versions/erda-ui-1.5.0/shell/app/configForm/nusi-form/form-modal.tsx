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
import { Modal, Button } from 'antd';
import { Form } from 'dop/pages/form-editor/index';
import i18n from 'i18n';
import './form-modal.scss';

export interface IProps {
  width?: number;
  name?: string;
  title?: string;
  fieldList: any[];
  visible: boolean;
  formData?: object;
  formRef?: any;
  modalProps?: object;
  marginStyle?: 'normal' | 'tense';
  onCancel?: () => void;
  onFailed?: (res?: object, isEdit?: boolean) => void;
  onFieldsChange?: (v: Obj) => void;
  onOk?: (result: object, isEdit: boolean) => Promise<any> | void;
}

export const isPromise = (obj: any) => {
  return !!obj && (typeof obj === 'object' || typeof obj === 'function') && typeof obj.then === 'function';
};

const noop = () => {};
export const FormModal = (props: IProps) => {
  const {
    width,
    formData,
    visible,
    onCancel,
    onOk,
    onFailed = noop,
    name,
    title,
    fieldList,
    onFieldsChange,
    formRef,
    modalProps = {},
    marginStyle = 'normal',
  } = props;
  const _title = title || (name ? i18n.t(!formData ? 'add {name}' : 'edit {name}', { name }) : '');

  const form = React.useRef();
  const [submitLoading, setSubmitLoading] = React.useState(false);

  const handleCancel = () => {
    onCancel && onCancel();
    setTimeout(() => {
      const curForm = form && (form.current as any);
      if (curForm) {
        curForm.reset();
      }
    });
  };

  React.useEffect(() => {
    if (formRef) {
      formRef.current = form.current;
    }
  }, [formRef]);

  const submit = (value: object, isEdit: boolean) => {
    setSubmitLoading(true);
    const res = onOk && onOk(value, isEdit);
    if (res && isPromise(res)) {
      res
        .then(() => {
          handleCancel();
        })
        .finally(() => {
          setSubmitLoading(false);
        });
    } else {
      setSubmitLoading(false);
      handleCancel();
    }
  };

  const handleOk = () => {
    const curForm = form && (form.current as any);
    if (curForm) {
      curForm.onSubmit(submit, onFailed);
    }
  };

  return (
    <Modal
      title={_title}
      width={width}
      destroyOnClose
      visible={visible}
      onCancel={handleCancel}
      onOk={handleOk}
      className={marginStyle === 'tense' ? 'dice-cp-form-modal-tense' : ''}
      footer={[
        onCancel ? (
          <Button key="back" onClick={handleCancel}>
            {i18n.t('cancel')}
          </Button>
        ) : null,
        onOk ? (
          <Button key="submit" type="primary" loading={submitLoading} onClick={handleOk}>
            {i18n.t('ok')}
          </Button>
        ) : null,
      ]}
      {...modalProps}
    >
      <Form formRef={form} fields={fieldList} value={formData} onChange={onFieldsChange} />
    </Modal>
  );
};
