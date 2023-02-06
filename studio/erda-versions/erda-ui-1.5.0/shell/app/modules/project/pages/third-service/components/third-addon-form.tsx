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

import { FileEditor, RenderPureForm } from 'common';
import { useUpdate } from 'common/use-hooks';
import { insertWhen, regRules } from 'common/utils';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import { get, isEmpty, map } from 'lodash';
import { Form, Select } from 'antd';
import React, { forwardRef, useImperativeHandle } from 'react';
import routeInfoStore from 'core/stores/route';
import { AddonType } from 'project/pages/third-service/components/config';

const { Option } = Select;

const CREATE_MAP = {
  CREATE: 'create',
  IMPORT: 'import',
};

interface IProps {
  addonInsList: ADDON.Instance[];
  addonSpecList: CUSTOM_ADDON.Item[];
  currentAddon: CUSTOM_ADDON.Item | Obj;
  editData: ADDON.Instance | null;
  form: FormInstance;
  configKV: React.RefObject<any>;
  category?: string;
  onFieldChange: (k: string, v: any) => void;
  setOneStep: (f: boolean) => void;
}
const ThirdAddonForm = (props: IProps) => {
  const { form, addonInsList, addonSpecList, editData, currentAddon, category, onFieldChange, setOneStep } = props;
  const curAddon = currentAddon || {};
  const query = routeInfoStore.useStore((s) => s.query);
  const [{ workspace, createType }, updater] = useUpdate({
    workspace: '',
    createType: CREATE_MAP.CREATE,
  });
  const _addonInsList = addonInsList || [];

  React.useEffect(() => {
    onFieldChange('addonName', query.addon);
  }, [onFieldChange, query.addon]);

  const getFields = () => {
    const typeField = {
      label: i18n.t('dop:third service'),
      name: 'addonName',
      type: 'select',
      initialValue: editData ? editData.addonName : query.addon || null,
      itemProps: {
        onChange(v: string) {
          onFieldChange('addonName', v);
          form.setFieldsValue({ plan: undefined });
        },
        disabled: editData !== null || query.addon === AddonType.APIGateway,
      },
      options: () =>
        map(addonSpecList, (v) => (
          <Option key={v.id} value={v.addonName}>
            {v.displayName}
          </Option>
        )),
    };
    const nameField = {
      label: i18n.t('name'),
      name: 'name',
      initialValue: editData ? editData.name || '' : null,
      itemProps: { disabled: editData !== null },
      rules: [
        { max: 30, message: i18n.t('dop:no more than 30 characters') },
        regRules.commonStr,
        {
          validator: (_rule: any, value: any, callback: any) => {
            if (!editData && value && value.length > 0) {
              const _workspace = workspace || form.getFieldValue('workspace');
              const match = _addonInsList.find((a) => a.name === value && a.workspace === _workspace);
              if (match) {
                callback(i18n.t('dop:service instance name is repeated'));
              } else {
                callback();
              }
            } else {
              callback();
            }
          },
        },
      ],
    };

    const createTypeField = {
      label: i18n.t('dop:creation method'),
      name: 'createType',
      type: 'radioGroup',
      itemProps: {
        buttonStyle: undefined,
        onChange: (e: any) => {
          updater.createType(e.target.value);
          setOneStep(e.target.value === CREATE_MAP.IMPORT);
        },
      },
      initialValue: createType,
      options: [
        { value: CREATE_MAP.CREATE, name: i18n.t('dop:Manual entry') },
        { value: CREATE_MAP.IMPORT, name: i18n.t('dop:Config import') },
      ],
    };
    const appendField = [
      {
        label: i18n.t('dop:environments'),
        name: 'workspace',
        itemProps: {
          disabled: editData !== null || query.addon === AddonType.APIGateway,
          onChange(v: string) {
            updater.workspace(v);
            form.validateFields(['name'], { force: true });
          },
        },
        type: 'select',
        // 数据源管理页面：新增数据源暂时只能为【测试环境】
        options: category
          ? [{ name: i18n.t('test'), value: 'TEST' }]
          : [
              { name: i18n.t('develop'), value: 'DEV' },
              { name: i18n.t('test'), value: 'TEST' },
              { name: i18n.t('staging'), value: 'STAGING' },
              { name: i18n.t('prod'), value: 'PROD' },
            ],
        initialValue: query.env || (category ? 'TEST' : 'DEV'),
      },
      ...insertWhen(curAddon.plan, [
        {
          label: i18n.t('dop:plan'),
          name: 'plan',
          itemProps: {
            disabled: editData !== null || query.addon === AddonType.APIGateway,
            onChange(v: string) {
              updater.workspace(v);
              form.validateFields(['name'], { force: true });
            },
          },
          type: 'select',
          options: map(curAddon.plan, (p) => ({ name: p.label, value: p.value })),
          initialValue: get(curAddon, 'plan[0].value'),
        },
      ]),
      {
        label: i18n.t('tag'),
        name: 'tag',
        initialValue: editData ? editData.tag || '' : null,
        itemProps: { disabled: editData !== null },
        required: false,
      },
    ];

    if (curAddon.addonName === 'custom') {
      // 导入模式
      if (createType === CREATE_MAP.IMPORT) {
        return [
          typeField,
          createTypeField,
          {
            label: i18n.t('dop:Config content'),
            name: 'importConfig',
            getComp: () => <FileEditor fileExtension="json" minLines={8} />,
          },
        ];
      }
      return [typeField, createTypeField, nameField, ...appendField];
    }
    return [typeField, nameField, ...appendField];
  };

  return (
    <div className="third-addon-form">
      <RenderPureForm className={'addon-ins-form'} layout="vertical" form={form} list={getFields()} />
    </div>
  );
};

const FCForm = forwardRef((props: IProps, ref: any) => {
  const [form] = Form.useForm();
  useImperativeHandle(ref, () => ({
    form,
  }));
  return <ThirdAddonForm {...props} form={form} />;
});

export default FCForm as any as (p: Merge<Omit<IProps, 'form'>, { ref: any }>) => JSX.Element;
