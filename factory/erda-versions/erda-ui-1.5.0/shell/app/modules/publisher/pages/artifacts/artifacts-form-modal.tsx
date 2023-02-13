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

/**
 * Created by 含光<jiankang.pjk@alibaba-inc.com> on 2020/4/21 14:50.
 */
import { ImageUpload, RenderPureForm } from 'common';
import i18n from 'i18n';
import React from 'react';
import { Form, Modal } from 'antd';
import { isEmpty, get, map, pick } from 'lodash';
import { ArtifactsTypeMap } from './config';
import { FormInstance } from 'core/common/interface';
import { insertWhen, regRules, isPromise } from 'common/utils';
import publisherStore from 'app/modules/publisher/stores/publisher';

const { addArtifacts, updateArtifacts } = publisherStore.effects;
const modalName = i18n.t('publisher:publisher content');

interface IProps {
  visible: boolean;
  formData?: PUBLISHER.IArtifacts;
  title?: string;
  name?: string;
  form: FormInstance;
  onCancel: () => void;
  beforeSubmit?: (valuse: any, form: FormInstance) => Promise<any>;
  afterSubmit?: (isUpdate?: boolean, data?: PUBLISHER.IArtifacts) => any;
}

interface IState {
  isAdd: boolean;
  isGeofence: boolean;
  type: string;
  confirmLoading: boolean;
  visible: boolean;
}

class ArtifactsFormModal extends React.PureComponent<IProps, IState> {
  static getDerivedStateFromProps(nextProps: IProps, preState: IState): Partial<IState> {
    const { formData = {} as PUBLISHER.IArtifacts, visible } = nextProps;
    const isAdd = isEmpty(formData);
    if (!preState.visible && visible) {
      return {
        isGeofence: formData.isGeofence || false,
        isAdd,
        type: formData.type || ArtifactsTypeMap.MOBILE.value,
        visible,
      };
    } else {
      return {
        visible,
      };
    }
  }

  formRef = React.createRef<FormInstance>();

  state = {
    isAdd: false,
    isGeofence: false,
    type: ArtifactsTypeMap.MOBILE.value,
    confirmLoading: false,
    visible: false,
  };

  getFieldsList = () => {
    const { state } = this;
    const { isAdd } = state;
    const isEdit = !isAdd;
    let fieldsList = [
      {
        label: i18n.t('publisher:publisher content name'),
        name: 'name',
        rules: [regRules.commonStr],
        itemProps: {
          disabled: isEdit,
          maxLength: 200,
        },
      },
      {
        label: i18n.t('publisher:publisher display name'),
        name: 'displayName',
        itemProps: {
          maxLength: 200,
        },
      },
      {
        label: i18n.t('type'),
        name: 'type',
        type: 'select',
        initialValue: state.type,
        itemProps: {
          disabled: isEdit,
          onChange: this.changeType,
        },
        options: map(ArtifactsTypeMap, ({ name, value }) => ({ name, value })),
      },
      {
        label: i18n.t('description'),
        name: 'desc',
        required: false,
        type: 'textArea',
        itemProps: {
          maxLength: 200,
        },
      },
      {
        label: i18n.t('publisher:whether to publish'),
        name: 'public',
        required: false,
        initialValue: false,
        type: 'switch',
        itemProps: { type: 'hidden' },
      },
      {
        label: i18n.t('icon'),
        name: 'logo',
        viewType: 'image',
        required: false,
        getComp: ({ form }: { form: FormInstance }) => (
          <ImageUpload id="logo" form={form} showHint queryData={{ public: true }} />
        ),
      },
      // {
      //   label: i18n.t('publisher:background image'),
      //   name: 'backgroundImage',
      //   viewType: 'image',
      //   required: false,
      //   getComp: ({ form }: { form: FormInstance }) => (
      //     <ImageUpload
      //       id="backgroundImage"
      //       form={form}
      //       showHint
      //       isSquare={false}
      //       queryData={{ public: true }}
      //     />
      //   ),
      // },
      // {
      //   label: i18n.t('publisher:preview image'),
      //   name: 'previewImages',
      //   viewType: 'images',
      //   required: false,
      //   getComp: ({ form }: { form: FormInstance }) => (
      //     <ImageUpload
      //       id="previewImages"
      //       form={form}
      //       showHint
      //       isMulti
      //       isSquare={false}
      //       queryData={{ public: true }}
      //     />),
      // },
      ...insertWhen(state.type === ArtifactsTypeMap.MOBILE.value, [
        {
          label: i18n.t('publisher:jail break control'),
          name: 'noJailbreak',
          type: 'switch',
          initialValue: false,
          required: false,
        },
        {
          label: i18n.t('publisher:geographical fence control'),
          name: 'isGeofence',
          type: 'switch',
          initialValue: false,
          required: false,
          itemProps: {
            onChange: this.changeGeofence,
          },
        },
      ]),
    ];
    if (state.type === ArtifactsTypeMap.MOBILE.value && state.isGeofence) {
      fieldsList = fieldsList.concat([
        {
          label: i18n.t('publisher:longitude of center coordinates'),
          name: 'geofenceLon',
          rules: [
            {
              pattern: /^[-+]?(0?\d{1,2}\.\d{1,6}|1[0-7]?\d{1}\.\d{1,6}|180\.0{1,6})$/,
              message: `-180.0～+180.0, ${i18n.t('publisher:1-6 decimal places')}`,
            },
          ],
        },
        {
          label: i18n.t('publisher:latitude of center coordinates'),
          name: 'geofenceLat',
          rules: [
            {
              pattern: /^[-+]?([0-8]?\d{1}\.\d{1,6}|90\.0{1,6})$/,
              message: `-90.0～+90.0, ${i18n.t('publisher:1-6 decimal places')}`,
            },
          ],
        },
        {
          label: i18n.t('publisher:radius from center'),
          name: 'geofenceRadius',
          rules: [{ pattern: /^[0-9]+$/, message: i18n.t('please fill in the correct value') }],
          itemProps: {
            suffix: i18n.t('meters'),
          },
        },
      ] as any);
    }
    return fieldsList;
  };

  changeType = (type: string) => {
    this.setState(
      {
        type,
      },
      () => {
        if (type === ArtifactsTypeMap.MOBILE.value) {
          const { isGeofence } = this.state;
          const { formData } = this.props;
          const form = this.formRef.current;
          const keys = isGeofence ? ['isGeofence', 'geofenceLon', 'geofenceLat', 'geofenceRadius'] : ['isGeofence'];
          form?.setFieldsValue(pick(formData, keys));
        }
      },
    );
  };

  changeGeofence = (isGeofence: boolean) => {
    this.setState(
      {
        isGeofence,
      },
      () => {
        if (isGeofence) {
          const { formData } = this.props;
          const form = this.formRef.current;
          const keys = ['geofenceLon', 'geofenceLat', 'geofenceRadius'];
          form?.setFieldsValue(pick(formData, keys));
        }
      },
    );
  };

  submit = (checkedValues: PUBLISHER.IArtifacts) => {
    const { formData, onCancel, afterSubmit } = this.props;
    const { geofenceLon, geofenceLat, geofenceRadius } = checkedValues;
    let reData = { ...checkedValues };
    if (geofenceLon !== undefined) {
      reData = {
        ...checkedValues,
        geofenceLon: Number(geofenceLon),
        geofenceLat: Number(geofenceLat),
        geofenceRadius: Number(geofenceRadius),
      };
    }
    const data = this.state.isAdd ? reData : { artifactsId: get(formData, 'id'), ...reData };
    const submitResult = this.state.isAdd ? addArtifacts : updateArtifacts;

    this.setState({ confirmLoading: true });
    submitResult(data)
      .then((res) => {
        onCancel();
        afterSubmit && afterSubmit(!this.state.isAdd, this.state.isAdd ? res : data);
      })
      .finally(() => {
        this.setState({ confirmLoading: false });
      });
  };

  handleOk = () => {
    const { beforeSubmit } = this.props;
    const form = this.formRef.current;
    return new Promise((resolve, reject) => {
      form
        ?.validateFields()
        .then((values: any) => {
          let submitValue = values;
          if (beforeSubmit) {
            submitValue = beforeSubmit(values, form);
            if (isPromise(submitValue)) {
              // 当需要在提交前做后端检查且不能清除表单域的情况下，可以在beforeSubmit返回promise，通过then结果判定是否真实提交
              return submitValue.then((checkedValues: any) => {
                if (checkedValues === null) {
                  return resolve();
                }
                this.submit(checkedValues);
              });
            } else if (submitValue === null) {
              return resolve();
            }
          }
          this.submit(submitValue);
        })
        .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
          form?.scrollToField(errorFields[0].name);
          reject(errorFields);
        });
    }).then(() => form?.resetFields());
  };

  componentDidUpdate(prevProps: Readonly<IProps>) {
    const { isAdd } = this.state;
    const { formData } = this.props;
    const form = this.formRef.current;
    const fieldsList = this.getFieldsList();
    const keys = fieldsList.filter((f) => f.name !== undefined).map((f) => f.name) as string[];
    if (!prevProps.visible) {
      if (!isAdd) {
        setTimeout(() => {
          form?.setFieldsValue(pick(formData as object, keys));
        }, 0);
      } else {
        form?.resetFields();
      }
    }
  }

  render(): React.ReactNode {
    const { isAdd, confirmLoading } = this.state;
    const { visible, onCancel } = this.props;
    const form = this.formRef.current;
    const fieldsList = this.getFieldsList();
    const title = isAdd ? i18n.t('add {name}', { name: modalName }) : i18n.t('edit {name}', { name: modalName });
    let content = null;
    if (fieldsList) {
      content = (
        <div className="max-modal-height">
          <RenderPureForm layout="vertical" onlyItems list={fieldsList} form={form} />
        </div>
      );
    }
    return (
      <Modal title={title} visible={visible} onCancel={onCancel} confirmLoading={confirmLoading} onOk={this.handleOk}>
        <Form layout="vertical" ref={this.formRef}>
          {content}
        </Form>
      </Modal>
    );
  }
}

export default ArtifactsFormModal as any as (p: Omit<IProps, 'form'>) => JSX.Element;
