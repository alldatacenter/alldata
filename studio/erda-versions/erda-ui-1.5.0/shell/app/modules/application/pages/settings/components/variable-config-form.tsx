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

import { FormModal, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { getUploadProps } from 'common/utils/upload-props';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import { Button, message, Spin, Upload } from 'antd';
import React from 'react';

export const ENV_I18N = {
  default: i18n.t('common:default config'),
  dev: i18n.t('dev environment'),
  test: i18n.t('test environment'),
  staging: i18n.t('staging environment'),
  prod: i18n.t('prod environment'),
};

const typeMap = {
  kv: 'kv',
  file: 'dice-file',
};
interface IProps {
  visible: boolean;
  formData: PIPELINE_CONFIG.ConfigItem | null;
  onOk: (data: any, isAdd: boolean) => any;
  onCancel: () => void;
}
export const VariableConfigForm = ({ formData, visible, onOk, onCancel }: IProps) => {
  const [{ type, uploadFile, uploading }, updater, _, reset] = useUpdate({
    type: typeMap.kv,
    uploadFile: '',
    uploading: false,
  });

  const fieldsList = (_formRef: FormInstance, isEdit: boolean) => [
    {
      label: 'Key',
      name: 'key',
      itemProps: {
        maxLength: 191,
        disabled: isEdit,
      },
      rules: [
        {
          pattern: /^[a-zA-Z_]+[.a-zA-Z0-9_-]*$/,
          message: i18n.t(
            'common:start with letters, which can contain letters, numbers, dots, underscores and hyphens.',
          ),
        },
      ],
    },
    {
      label: i18n.t('type'),
      name: 'type',
      type: 'select',
      options: [
        { name: i18n.t('value'), value: typeMap.kv },
        { name: i18n.t('file'), value: typeMap.file },
      ],
      initialValue: typeMap.kv,
      itemProps: {
        onChange(v: string) {
          updater.type(v);
          updater.uploadFile('');
          updater.uploading(false);
        },
        disabled: isEdit,
      },
    },
    type === typeMap.kv
      ? {
          label: 'Value',
          name: 'value',
          itemProps: {
            maxLength: 4096,
          },
          config: {
            getValueFromEvent: (e: any) => e.target.value,
          },
          required: formData ? formData.encrypt && isEdit : true, // 只有编辑加密的时，可为空，为空认为没有修改，不为空认为修改了
        }
      : {
          label: i18n.t('file'),
          name: 'value',
          type: 'custom',
          rules: [{ required: true, message: i18n.t('common:Please select the file to be uploaded') }],
          config: {
            getValueFromEvent: (e: any) => {
              if (Array.isArray(e)) {
                return e;
              }
              return e && e.fileList;
            },
          },
          getComp: ({ form }: { form: FormInstance }) => {
            const uploadProps = getUploadProps(
              {
                onChange: ({ file, event }: any) => {
                  if (event) {
                    updater.uploading(true); // 上传后后端还要处理一阵，不使用进度条
                  }
                  if (file.response) {
                    const { success, err, data } = file.response;
                    if (!success) {
                      message.error(err.msg);
                    } else {
                      form.setFieldsValue({
                        value: data.uuid,
                      });
                      updater.uploadFile(data.name);
                    }
                    updater.uploading(false);
                  }
                  return file;
                },
              },
              300,
            );
            return (
              <div className="upload-container">
                <Spin spinning={uploading} tip={i18n.t('uploading, please wait a moment')}>
                  <Upload {...uploadProps}>
                    <Button className="flex items-center">
                      <ErdaIcon className="mr-1 align-center" type="upload" /> {i18n.t('upload')}
                    </Button>
                  </Upload>
                  <span className="text-desc ml-2">
                    {uploadFile ? i18n.t('selected {xx}', { xx: uploadFile }) : null}
                  </span>
                  <div className="text-desc mt-2">{i18n.t('dop:upload-file-tip')}</div>
                </Spin>
              </div>
            );
          },
        },
    {
      label: i18n.t('dop:remark'),
      name: 'comment',
      required: false,
      itemProps: {
        maxLength: 200,
      },
    },
    {
      label: i18n.t('dop:encrypt'),
      name: 'encrypt',
      type: 'switch',
      initialValue: false,
      itemProps: {
        // disabled: isEdit,
      },
    },
  ];

  return (
    <FormModal
      name={i18n.t('config')}
      fieldsList={fieldsList}
      visible={visible}
      onOk={(data: any, isAdd: boolean) => {
        onOk(data, isAdd);
        reset();
      }}
      onCancel={() => {
        onCancel();
        reset();
      }}
      formData={formData}
      modalProps={{
        maskClosable: false,
        destroyOnClose: true,
      }}
    />
  );
};
