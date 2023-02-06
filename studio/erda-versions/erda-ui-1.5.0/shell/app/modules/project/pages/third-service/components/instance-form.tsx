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

import { KeyValueEditor, RenderPureForm } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';
import { isEmpty } from 'lodash';
import { Form } from 'antd';
import customAddonStore from 'project/stores/custom-addon';
import React, { forwardRef, useImperativeHandle } from 'react';
import {
  useDBFields,
  useMysqlFields,
  useOnsFields,
  useRedisFields,
  useTopicFields,
  useBucketField,
  AddonType,
  useGatewayFields,
} from './config';
import { insertWhen } from 'common/utils';

export const CLOUD_TYPES = [
  AddonType.AliCloudRds,
  AddonType.AliCloudRedis,
  AddonType.AliCloudOns,
  AddonType.AliCloudOss,
];
const MODE_MAP = {
  NEW: 'new',
  EXIST: 'exist',
  CUSTOM: 'custom',
};

const defaultInstance: CUSTOM_ADDON.GatewayInstance = {
  name: i18n.t('default:add') + i18n.t('default:instance'),
  instanceID: '-1',
};

interface IProps {
  form: FormInstance;
  addonProto: CUSTOM_ADDON.Item;
  editData: ADDON.Instance | null;
  workspace: string;
  edit: React.RefObject<any>;
  category?: string;
}
const InstanceForm = ({ form, editData, addonProto, workspace, edit, category }: IProps) => {
  const [state, updater] = useUpdate({
    mode: MODE_MAP.EXIST,
    existInstances: [] as CUSTOM_ADDON.CloudInstance[],
    gatewaysInstances: [defaultInstance] as CUSTOM_ADDON.GatewayInstance[],
    slbsInstances: [defaultInstance] as CUSTOM_ADDON.GatewayInstance[],
  });
  const { addonName } = addonProto;
  const isEditMode = !isEmpty(editData);
  React.useEffect(() => {
    let _mode = MODE_MAP.EXIST;
    if (addonName && !CLOUD_TYPES.includes(addonName)) {
      // 类型不为云Addon时时，只能填自定义变量
      _mode = MODE_MAP.CUSTOM;
    } else if ([AddonType.AliCloudOss, AddonType.AliCloudRedis].includes(addonName)) {
      // oss和redis默认为新购买
      _mode = MODE_MAP.NEW;
    }
    updater.mode(_mode);
    form.setFieldsValue({ mode: _mode });
  }, [addonName, updater, form]);

  React.useEffect(() => {
    const curMode = editData && editData.customAddonType;
    if (curMode !== 'cloud') {
      updater.mode(MODE_MAP.CUSTOM);
      form.setFieldsValue({ mode: MODE_MAP.CUSTOM });
    }
  }, [editData, updater, form]);

  React.useEffect(() => {
    if (state.mode === MODE_MAP.EXIST) {
      const nameToType = {
        'alicloud-rds': 'mysql',
        // 'alicloud-redis': 'redis',
        'alicloud-ons': 'ons',
        // 'alicloud-oss': 'oss',
      };
      if (nameToType[addonName]) {
        workspace &&
          customAddonStore
            .getCloudInstances({ vendor: 'alicloud', type: nameToType[addonName], workspace })
            .then((res) => {
              updater.existInstances(res.list);
            });
      }
    }
  }, [addonName, state.mode, updater, workspace]);

  const fieldsMap = {
    [AddonType.AliCloudOns]: useOnsFields(),
    [AddonType.AliCloudOss]: [],
    [AddonType.AliCloudRds]: useMysqlFields(form),
    [AddonType.AliCloudRedis]: useRedisFields(),
  };

  const subFieldsMap = {
    [AddonType.AliCloudOns]: useTopicFields(),
    [AddonType.AliCloudOss]: useBucketField(),
    [AddonType.AliCloudRds]: useDBFields(),
    [AddonType.AliCloudRedis]: [],
  };

  const getKeyValueEditorValue = () => {
    const { vars } = addonProto;
    const defaultValue = editData && editData.config;
    const defaultKV = defaultValue || {};
    if (isEmpty(defaultValue)) {
      (vars || []).forEach((k: string) => {
        defaultKV[k] = '';
      });
    }
    return {
      ...defaultKV,
    };
  };

  const subFields = addonName && subFieldsMap[addonName] ? subFieldsMap[addonName] : [];
  const modeToField = {
    [MODE_MAP.NEW]: addonName && fieldsMap[addonName] ? fieldsMap[addonName].concat(subFields) : [],
    [MODE_MAP.EXIST]: [
      {
        label: i18n.t('resource:existing cloud instance'),
        name: 'instanceID',
        type: 'select',
        options: state.existInstances.map((a) => ({ name: `${a.name} (${a.id})`, value: a.id })),
        // itemProps: { disabled: editData !== null },
      },
      ...subFields,
    ],
    [MODE_MAP.CUSTOM]: [
      {
        getComp: () => (
          <>
            <KeyValueEditor form={form} dataSource={getKeyValueEditorValue()} ref={edit} />
            <div className="text-red">
              {i18n.t('dop:Modifying service parameters will restart all associated applications.')}
            </div>
            <div className="text-red">{i18n.t('dop:op-affect-related-app')}</div>
            <div className="text-red">{i18n.t('dop:key-secret-encrypt-tip')}</div>
          </>
        ),
      },
    ],
  };

  const appendField = modeToField[state.mode];
  const fields = [
    ...insertWhen(CLOUD_TYPES.includes(addonName), [
      {
        label: i18n.t('dop:mode'),
        name: 'mode',
        type: 'radioGroup',
        options: [
          {
            name: i18n.t('resource:select existing'),
            value: 'exist',
            disabled: [AddonType.AliCloudOss, AddonType.AliCloudRedis].includes(addonName) || isEditMode,
          }, // redis/oss不能通过已有实例创建
          {
            name: i18n.t('resource:new purchase'),
            value: 'new',
            // 在数据源页面禁用
            disabled: isEditMode || category === 'DATA_SOURCE',
          },
          { name: i18n.t('customize'), value: 'custom' },
        ],
        initialValue: state.mode,
        itemProps: {
          buttonStyle: undefined,
          onChange: (e: any) => {
            // 清空前一个mode的表单
            const names = appendField.map((f: any) => f.name);
            form.resetFields(names);
            updater.mode(e.target.value);
          },
        },
      },
    ]),
    ...appendField,
  ];

  return <RenderPureForm layout="vertical" form={form} list={fields} />;
};

const FCForm = forwardRef((props: IProps, ref: any) => {
  const [form] = Form.useForm();

  useImperativeHandle(ref, () => ({
    form,
  }));
  return <InstanceForm {...props} form={form} />;
});

export default FCForm as any as (p: Merge<Omit<IProps, 'form'>, { ref: any }>) => JSX.Element;
