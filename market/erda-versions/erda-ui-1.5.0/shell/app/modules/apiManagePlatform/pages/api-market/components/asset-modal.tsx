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

import React, { MutableRefObject } from 'react';
import { FormModal, ImageUpload, IFormItem, ErdaIcon } from 'common';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import { getUploadProps } from 'common/utils/upload-props';
import { Button, message, Upload } from 'antd';
import { insertWhen } from 'common/utils';
import { map } from 'lodash';
import apiMarketStore from 'apiManagePlatform/stores/api-market';
import { protocolMap } from './config';

export type IScope = 'asset' | 'version';
export type IMode = 'add' | 'edit';

interface IProps {
  scope: IScope;
  mode: IMode;
  formData?: API_MARKET.Asset;
  visible: boolean;
  onCancel: () => void;
  afterSubmit?: (data: any) => void;
}

const allSuffix = map(protocolMap, (t) => t.suffix).join(',');
const idReg = /^([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]$/;

const titleMap: { [key in IScope]: { [type in IMode]: string } } = {
  asset: {
    add: i18n.t('add {name}', { name: i18n.t('dop:resources') }),
    edit: i18n.t('edit {name}', { name: i18n.t('dop:resources') }),
  },
  version: {
    add: i18n.t('add {name}', { name: i18n.t('version') }),
    edit: i18n.t('edit {name}', { name: i18n.t('version') }),
  },
};

const formatPayload = (scope: IScope, mode: IMode, data: any, formData?: API_MARKET.Asset) => {
  const { version, specProtocol, specDiceFileUUID, ...rest } = data;
  let versions = {};
  if (version) {
    const [majorVersion, minorVersion, patchVersion] = (version || '').split('.') as [string, string, string];
    versions = {
      major: +majorVersion,
      minor: +minorVersion,
      patch: +patchVersion,
    };
  }
  let payload = {};
  if (scope === 'version') {
    payload = {
      assetID: formData?.assetID,
      specDiceFileUUID,
      specProtocol,
      ...versions,
    };
  }
  if (scope === 'asset' && mode === 'add') {
    payload = {
      ...rest,
      versions: [
        {
          specProtocol,
          specDiceFileUUID,
          ...versions,
        },
      ],
    };
  }
  if (scope === 'asset' && mode === 'edit') {
    payload = {
      ...rest,
    };
  }
  return payload;
};

const AssetModal = ({ scope, visible, onCancel, afterSubmit, mode, formData }: IProps) => {
  const { createAsset, addNewVersion, editAsset } = apiMarketStore.effects;
  const [suffix, setSuffix] = React.useState(allSuffix);
  const formRef = React.useRef({}) as MutableRefObject<FormInstance>;
  const [uploadFile, setUploadFile] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  React.useEffect(() => {
    if (!visible) {
      setUploadFile('');
    }
  }, [visible]);
  const handleSelectProtocol = (v: API_MARKET.SpecProtocol) => {
    setSuffix(protocolMap[v].suffix);
    // 文件后缀和资源协议不一致
    if (uploadFile && !protocolMap[v].pattern.test(uploadFile)) {
      setUploadFile('');
      formRef.current && formRef.current.setFieldsValue({ specDiceFileUUID: undefined });
    }
  };
  const nameToId = (e: React.FocusEvent<HTMLInputElement>) => {
    const name = e.target.value;
    const assetID = formRef.current.getFieldValue('assetID');
    if (!assetID && idReg.test(name) && name.length <= 20) {
      formRef.current.setFieldsValue({ assetID: name });
    }
  };
  const showVersionField = scope === 'version' || (scope === 'asset' && mode === 'add');
  const showAssetField = scope === 'asset';
  const fieldsList: IFormItem[] = [
    ...insertWhen(showAssetField, [
      {
        label: i18n.t('API name'),
        name: 'assetName',
        type: 'input',
        required: true,
        itemProps: {
          placeholder: i18n.t('default:please enter'),
          autoComplete: 'off',
          maxLength: 50,
          onBlur: nameToId,
        },
      },
      {
        label: 'API ID',
        type: 'input',
        name: 'assetID',
        required: true,
        itemProps: {
          placeholder: i18n.t('default:please enter'),
          autoComplete: 'off',
          maxLength: 20,
          disabled: mode === 'edit',
        },
        rules: [
          {
            pattern: idReg,
            message: i18n.t(
              'default:start with number or letter, can contain numbers, letters, dots, hyphens and underscores',
            ),
          },
        ],
      },
      {
        label: i18n.t('API description'),
        type: 'textArea',
        name: 'desc',
        required: false,
        itemProps: {
          placeholder: i18n.t('default:please enter'),
          autoComplete: 'off',
          maxLength: 1024,
        },
      },
    ]),
    ...insertWhen(showVersionField, [
      {
        label: i18n.t('default:resource version'),
        type: 'input',
        name: 'version',
        required: false,
        itemProps: {
          placeholder: i18n.t('Please enter version number, such as x.y.z.'),
          autoComplete: 'off',
        },
        rules: [
          {
            pattern: /^(?:[1-9]\d*|0)\.(?:[1-9]\d*|0)\.(?:[1-9]\d*|0)$/,
            message: i18n.t('Please enter a valid version number, such as x.y.z.'),
          },
        ],
      },
      {
        label: i18n.t('API description document protocol'),
        type: 'select',
        name: 'specProtocol',
        options: map(protocolMap, ({ fullName }, key) => ({ name: fullName, value: key })),
        itemProps: {
          placeholder: i18n.t('please select'),
          onSelect: handleSelectProtocol,
        },
      },
    ]),
    ...insertWhen(showVersionField, [
      {
        label: i18n.t('default:API description document'),
        name: 'specDiceFileUUID',
        required: true,
        getComp: ({ form }: { form: FormInstance }) => {
          const uploadProps = getUploadProps({
            onChange: ({ file }: any) => {
              setLoading(true);
              if (file.response) {
                setLoading(false);
                const { success, err, data } = file.response;
                if (!success) {
                  message.error(err.msg);
                } else {
                  form.setFieldsValue({
                    specDiceFileUUID: data.uuid,
                  });
                  setUploadFile(data.name);
                }
              }
              return file;
            },
          });
          return (
            <div className="upload-container">
              <Upload accept={suffix} {...uploadProps}>
                <Button className="flex items-center">
                  <ErdaIcon
                    type="upload"
                    className="mr-1"
                    size="14"
                  />{' '}
                  {i18n.t('upload')}
                </Button>
              </Upload>
              <span className="text-desc ml-2">{uploadFile ? i18n.t('selected {xx}', { xx: uploadFile }) : null}</span>
            </div>
          );
        },
      },
    ]),
    ...insertWhen(showAssetField, [
      {
        label: i18n.t('API logo'),
        name: 'logo',
        required: false,
        getComp: ({ form }: { form: FormInstance }) => <ImageUpload id="logo" form={form} showHint />,
      },
    ]),
  ];
  const handleOk = () => {
    formRef.current.validateFields().then(async (data: any) => {
      const payload = formatPayload(scope, mode, data, formData);
      let request: any = createAsset;
      if (scope === 'version') {
        request = addNewVersion;
      } else if (scope === 'asset' && mode === 'edit') {
        request = editAsset;
      } else if (scope === 'asset' && mode === 'add') {
        request = createAsset;
      }
      const res = await request(payload);
      onCancel();
      afterSubmit && afterSubmit(res);
    });
  };
  const footer = (
    <>
      <Button key="back" onClick={onCancel}>
        {i18n.t('cancel')}
      </Button>
      <Button key="submit" type="primary" disabled={loading} onClick={handleOk}>
        {i18n.t('ok')}
      </Button>
    </>
  );
  return (
    <FormModal
      title={titleMap[scope][mode]}
      visible={visible}
      fieldsList={fieldsList}
      ref={formRef}
      onCancel={onCancel}
      formData={formData || {}}
      loading={loading}
      modalProps={{
        destroyOnClose: true,
        footer,
      }}
    />
  );
};

export default AssetModal;
