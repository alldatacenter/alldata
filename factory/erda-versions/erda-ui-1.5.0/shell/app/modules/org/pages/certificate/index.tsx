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
import { CRUDTable, DeleteConfirm, Icon as CustomIcon, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import certificateStore from '../../stores/certificate';
import { Upload, message, Button, Select, Input } from 'antd';
import { formatTime } from 'common/utils';
import { FormInstance } from 'core/common/interface';
import { get, map, set } from 'lodash';
import './index.scss';
import { getUploadProps } from 'common/utils/upload-props';
import DetailModal from './detail-modal';
import orgStore from 'app/org-home/stores/org';

const { Option } = Select;
export const typeMap = {
  IOS: { value: 'IOS', label: 'IOS' },
  Android: { value: 'Android', label: 'Android' },
  // Message: { value: 'Message', label: i18n.t('message') },
};

interface IFileChangeArg {
  uuid: string | undefined;
  fileName: string | undefined;
}
interface IUploadProps {
  form: FormInstance;
  onChangeFile: ({ uuid, fileName }: IFileChangeArg) => void;
  fileNameKey: string | string[];
  fileAccept: string;
}
const UploadComp = ({ form, onChangeFile, fileNameKey, fileAccept }: IUploadProps) => {
  const acceptProp = { accept: fileAccept };
  const uploadProps = getUploadProps({
    ...acceptProp,
    data: {
      fileFrom: 'certificate manager',
    },
    onChange: ({ file }: any) => {
      if (file.response) {
        const { success, err, data } = file.response;
        if (!success) {
          message.error(err.msg);
        } else {
          onChangeFile({ uuid: data.uuid, fileName: data.name });
        }
      }
      return file;
    },
  });
  const deleteFile = () => {
    onChangeFile({ uuid: undefined, fileName: undefined });
  };
  const fileName = form.getFieldValue(fileNameKey);
  return (
    <div className="upload-container">
      <Upload {...uploadProps}>
        <Button className="flex items-center">
          <ErdaIcon type="upload" size="16" className="mr-1" /> {i18n.t('upload')}
        </Button>
      </Upload>
      {fileName && (
        <div className="flex justify-between items-center upload-file-item nowrap">
          <span>{fileName}</span>
          <CustomIcon type="thin-del" className="hover-active" onClick={deleteFile} />
        </div>
      )}
    </div>
  );
};

export const keyPrefix = {
  iosDebug: ['iosInfo', 'debugProvision'],
  iosKeyChainP12: ['iosInfo', 'keyChainP12'],
  iosRelease: ['iosInfo', 'releaseProvision'],
  adrAuto: ['androidInfo', 'autoInfo'],
  adrManualDebug: ['androidInfo', 'manualInfo', 'debugKeyStore'],
  adrManualRelease: ['androidInfo', 'manualInfo', 'releaseKeyStore'],
};

const getUploadFieldProps = ({ form, onChangeFile, fileNameKey, fileAccept }: IUploadProps) => {
  return {
    rules: [{ required: true, message: i18n.t('common:Please select the file to be uploaded') }],
    config: {
      getValueFromEvent: (e: any) => {
        if (Array.isArray(e)) return e;
        return e && e.fileList;
      },
    },
    getComp: () => {
      return <UploadComp form={form} onChangeFile={onChangeFile} fileNameKey={fileNameKey} fileAccept={fileAccept} />;
    },
  };
};

const Certificate = () => {
  const [{ chosenType, manualCreate, chosenRowId }, updater, update] = useUpdate({
    chosenType: '',
    manualCreate: 'true',
    detailVis: false,
    detail: {} as Certificate.Detail,
    chosenRowId: undefined as undefined | number,
  });
  const orgId = orgStore.useStore((s) => s.currentOrg.id);
  const getColumns = (effects: any, { onEdit, reloadList }: any) => {
    const { deleteItem } = effects;
    const columns = [
      {
        title: i18n.t('name'),
        dataIndex: 'name',
        width: 200,
      },
      {
        title: i18n.t('description'),
        dataIndex: 'desc',
      },
      {
        title: i18n.t('type'),
        dataIndex: 'type',
        width: 140,
        render: (text: string) => typeMap[text] && typeMap[text].label,
      },
      {
        title: i18n.t('create time'),
        dataIndex: 'createdAt',
        width: 180,
        render: (text: string) => formatTime(text),
      },
      {
        title: i18n.t('operation'),
        dataIndex: 'op',
        width: 160,
        render: (_v: any, record: Certificate.Detail) => {
          return (
            <div className="table-operations">
              {record.type === 'Message' ? (
                <a
                  className="table-operations-btn"
                  download={get(record, 'messageInfo.uuid')}
                  href={`/api/files/${get(record, 'messageInfo.uuid')}`}
                >
                  {i18n.t('download')}
                </a>
              ) : (
                <span className="table-operations-btn" onClick={() => updater.chosenRowId(record.id)}>
                  {i18n.t('download')}
                </span>
              )}
              {/* <span className="table-operations-btn" onClick={() => onEdit(record)}>{i18n.t('edit')}</span> */}
              <DeleteConfirm
                title={i18n.t('delete certificate')}
                secondTitle={i18n.t('cmp:confirm-delete-certificate')}
                onConfirm={() => deleteItem(record).then(() => reloadList())}
              >
                <span className="table-operations-btn">{i18n.t('delete')}</span>
              </DeleteConfirm>
            </div>
          );
        },
      },
    ];
    return columns;
  };
  const getFieldsList = (form: FormInstance, isEdit: boolean) => {
    const basicFieldsList = [
      {
        name: 'id',
        itemProps: {
          type: 'hidden',
        },
      },
      {
        label: i18n.t('name'),
        name: 'name',
        itemProps: {
          maxLength: 100,
          disabled: isEdit,
        },
      },
      {
        label: i18n.t('description'),
        name: 'desc',
        required: false,
        itemProps: {
          maxLength: 1000,
        },
      },
      {
        label: i18n.t('type'),
        name: 'type',
        type: 'custom',
        itemProps: {
          disabled: isEdit,
          onChange: (val: string) => {
            updater.chosenType(val);
          },
        },
        getComp: () => {
          const curValue = form.getFieldValue('type');
          return (
            <Select
              placeholder={i18n.t('please select')}
              onSelect={(e) => {
                curValue !== e && form.resetFields(['filename', 'uuid']);
              }}
            >
              {map(typeMap, ({ value, label }) => (
                <Option key={value} value={value}>
                  {label}
                </Option>
              ))}
            </Select>
          );
        },
      },
    ];

    const typeFieldsMap = {
      IOS: [
        {
          label: `Keychain-p12 ${i18n.t('file')}`,
          name: keyPrefix.iosKeyChainP12.concat(['uuid']),
          type: 'custom',
          ...getUploadFieldProps({
            form,
            onChangeFile: ({ uuid, fileName }: IFileChangeArg) => {
              let value = {};
              set(value, keyPrefix.iosKeyChainP12, {
                fileName,
                uuid,
                password: undefined, // 每次修改uuid后，需要重置密码
              });
              form.setFieldsValue(value);
            },
            fileNameKey: [...keyPrefix.iosKeyChainP12, 'fileName'],
            fileAccept: '.p12',
          }),
        },
        {
          label: `Keychain-p12 ${i18n.t('password')}`,
          name: keyPrefix.iosKeyChainP12.concat(['password']),
          type: 'custom',
          required: false,
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          name: keyPrefix.iosKeyChainP12.concat(['fileName']),
          itemProps: {
            type: 'hidden',
          },
        },
        {
          label: `Debug-mobileprovision ${i18n.t('file')}`,
          name: keyPrefix.iosDebug.concat(['uuid']),
          type: 'custom',
          ...getUploadFieldProps({
            form,
            onChangeFile: ({ uuid, fileName }: IFileChangeArg) => {
              let value = {};
              set(value, keyPrefix.iosDebug, {
                fileName,
                uuid,
              });
              form.setFieldsValue(value);
            },
            fileNameKey: [...keyPrefix.iosDebug, 'fileName'],
            fileAccept: '.mobileprovision',
          }),
        },
        {
          name: keyPrefix.iosDebug.concat(['fileName']),
          itemProps: {
            type: 'hidden',
          },
        },
        {
          label: `Release-mobileprovision ${i18n.t('file')}`,
          name: keyPrefix.iosRelease.concat(['uuid']),
          type: 'custom',
          ...getUploadFieldProps({
            form,
            onChangeFile: ({ uuid, fileName }: IFileChangeArg) => {
              let value = {};
              set(value, keyPrefix.iosRelease, {
                fileName,
                uuid,
              });
              form.setFieldsValue(value);
            },
            fileNameKey: [...keyPrefix.iosRelease, 'fileName'],
            fileAccept: '.mobileprovision',
          }),
        },
        {
          name: keyPrefix.iosRelease.concat(['fileName']),
          itemProps: {
            type: 'hidden',
          },
        },
      ],
      Android: [
        {
          label: i18n.t('msp:create a way'),
          name: ['androidInfo', 'manualCreate'],
          type: 'radioGroup',
          initialValue: 'true',
          itemProps: {
            onChange: (e: any) => {
              updater.manualCreate(e.target.value);
            },
          },
          options: [
            { name: i18n.t('manually create'), value: 'true' },
            { name: i18n.t('auto create'), value: 'false' },
          ],
        },
      ],
      Message: [
        {
          name: ['messageInfo', 'fileName'],
          itemProps: {
            type: 'hidden',
          },
        },
        {
          label: i18n.t('file'),
          name: ['messageInfo', 'uuid'],
          type: 'custom',
          ...getUploadFieldProps({
            form,
            onChangeFile: ({ uuid, fileName }: IFileChangeArg) => {
              form.setFieldsValue({
                messageInfo: {
                  fileName,
                  uuid,
                },
              });
            },
            fileNameKey: ['messageInfo', 'fileName'],
            fileAccept: '',
          }),
        },
      ],
    };

    const createFieldsMap = {
      manual: [
        {
          label: `Debug-key/store ${i18n.t('file')}`,
          name: keyPrefix.adrManualDebug.concat(['uuid']),
          type: 'custom',
          ...getUploadFieldProps({
            form,
            onChangeFile: ({ uuid, fileName }: IFileChangeArg) => {
              let value = {};
              set(value, keyPrefix.adrManualDebug, {
                fileName,
                uuid,
                keyPassword: undefined,
                storePassword: undefined,
                alias: undefined,
              });
              form.setFieldsValue(value);
            },
            fileNameKey: [...keyPrefix.adrManualDebug, 'fileName'],
            fileAccept: '.keystore',
          }),
        },
        {
          label: `Debug-key ${i18n.t('cmp:alias')}`,
          name: keyPrefix.adrManualDebug.concat(['alias']),
        },
        {
          label: `Debug-key ${i18n.t('password')}`,
          name: keyPrefix.adrManualDebug.concat(['keyPassword']),
          type: 'custom',
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          label: `Debug-store ${i18n.t('password')}`,
          name: keyPrefix.adrManualDebug.concat(['storePassword']),
          type: 'custom',
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          name: keyPrefix.adrManualDebug.concat(['fileName']),
          itemProps: {
            type: 'hidden',
          },
        },
        {
          label: `Release-key/store ${i18n.t('file')}`,
          name: keyPrefix.adrManualRelease.concat(['uuid']),
          type: 'custom',
          ...getUploadFieldProps({
            form,
            onChangeFile: ({ uuid, fileName }: IFileChangeArg) => {
              let value = {};
              set(value, keyPrefix.adrManualRelease, {
                fileName,
                uuid,
                keyPassword: undefined,
                storePassword: undefined,
                alias: undefined,
              });
              form.setFieldsValue(value);
            },
            fileNameKey: [...keyPrefix.adrManualRelease, 'fileName'],
            fileAccept: '.keystore',
          }),
        },
        {
          label: `Release-key ${i18n.t('cmp:alias')}`,
          name: keyPrefix.adrManualRelease.concat(['alias']),
        },
        {
          label: `Release-key ${i18n.t('password')}`,
          name: keyPrefix.adrManualRelease.concat(['keyPassword']),
          type: 'custom',
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          label: `Release-store ${i18n.t('password')}`,
          name: keyPrefix.adrManualRelease.concat(['storePassword']),
          type: 'custom',
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          name: keyPrefix.adrManualRelease.concat(['fileName']),
          itemProps: {
            type: 'hidden',
          },
        },
      ],
      auto: [
        {
          label: `Debug-key ${i18n.t('cmp:alias')}`,
          name: keyPrefix.adrAuto.concat(['debugKeyStore', 'alias']),
        },
        {
          label: `Debug-key ${i18n.t('password')}`,
          name: keyPrefix.adrAuto.concat(['debugKeyStore', 'keyPassword']),
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          label: `Debug-store ${i18n.t('password')}`,
          name: keyPrefix.adrAuto.concat(['debugKeyStore', 'storePassword']),
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          label: `Release-key ${i18n.t('cmp:alias')}`,
          name: keyPrefix.adrAuto.concat(['releaseKeyStore', 'alias']),
        },
        {
          label: `Release-key ${i18n.t('password')}`,
          name: keyPrefix.adrAuto.concat(['releaseKeyStore', 'keyPassword']),
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          label: `Release-store ${i18n.t('password')}`,
          name: keyPrefix.adrAuto.concat(['releaseKeyStore', 'storePassword']),
          rules: [{ pattern: /^[\s\S]{6,30}$/, message: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}` }],
          itemProps: {
            placeholder: `${i18n.t('length is {min}~{max}', { min: 6, max: 30 })}`,
          },
          getComp: () => <Input.Password />,
        },
        {
          label: `${i18n.t('name or surname')}/CN`,
          name: keyPrefix.adrAuto.concat(['name']),
        },
        {
          label: `${i18n.t('organization unit')}/OU`,
          name: keyPrefix.adrAuto.concat(['ou']),
        },
        {
          label: `${i18n.t('common:organization')}/O`,
          name: keyPrefix.adrAuto.concat(['org']),
        },
        {
          label: `${i18n.t('city')}/L`,
          name: keyPrefix.adrAuto.concat(['city']),
        },
        {
          label: `${i18n.t('province')}/ST`,
          name: keyPrefix.adrAuto.concat(['province']),
        },
        {
          label: `${i18n.t('country')}/C`,
          name: keyPrefix.adrAuto.concat(['state']),
        },
      ],
    } as any;

    let fieldsList = basicFieldsList.concat(typeFieldsMap[chosenType] || []);
    if (chosenType === 'Android') {
      fieldsList = fieldsList.concat(createFieldsMap[manualCreate === 'true' ? 'manual' : 'auto']);
    }
    return fieldsList;
  };

  const filterConfig = React.useMemo(
    () => [
      {
        type: Input,
        name: 'q',
        customProps: {
          placeholder: i18n.t('search by name'),
        },
      },
    ],
    [],
  );

  const handleFormSubmit = (data: Certificate.Detail, { addItem }: { addItem: (arg: any) => Promise<any> }) => {
    const reData = { ...data, orgId };
    if (reData.androidInfo) {
      reData.androidInfo.manualCreate = `${reData.androidInfo.manualCreate}` === 'true';
    }
    return addItem(reData);
  };

  return (
    <>
      <CRUDTable.StoreTable<Certificate.Detail>
        name={i18n.t('certificate')}
        showTopAdd
        getColumns={getColumns}
        getFieldsList={getFieldsList}
        store={certificateStore}
        handleFormSubmit={handleFormSubmit}
        filterConfig={filterConfig}
        onModalClose={() => {
          update({
            chosenType: '',
            manualCreate: 'true',
          });
        }}
      />
      <DetailModal id={chosenRowId} onClose={() => updater.chosenRowId('')} />
    </>
  );
};

export default Certificate;
