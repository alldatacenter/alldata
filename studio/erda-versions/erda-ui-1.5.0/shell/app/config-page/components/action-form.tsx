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
import { map, get, isEmpty, pick, find, isEqual, omit, has } from 'lodash';
import { Button } from 'antd';
import { useUpdate } from 'common/use-hooks';
import { useUpdateEffect } from 'react-use';
import { IProps as FormModalProps } from 'app/configForm/nusi-form/form-modal';
import { Form as PureForm } from 'dop/pages/form-editor/index';
import { produce } from 'immer';
import i18n from 'i18n';

export interface IProps extends FormModalProps, CONFIG_PAGE.ICommonProps {
  name: string;
  props: {
    fields: any[];
  };
}

// 目前使用组件化协议的action-form白名单，后端还未实现接口控制，
export const protocolActionForms = [
  'mysql-cli',
  'redis-cli',
  'api-test',
  'dice-deploy-release',
  'dice-deploy-rollback',
  'git-checkout',
  'git-push',
  'gitbook',
  'manual-review',
  'dice-version-archive',
];

const clearEmptyField = (ObjData: any) => {
  const filledFields: string[] = [];
  const findData = (obj: any, parentArray: string[]) => {
    Object.keys(obj).forEach((key) => {
      const currentParent = [...parentArray, key];
      const value = get(obj, key);
      if (typeof value === 'object' && value !== null) {
        findData(value, currentParent);
      } else if (value || value === 0) {
        filledFields.push(currentParent.join('.'));
      }
    });
  };
  findData(ObjData, []);
  return pick(ObjData, filledFields);
};
const noop = () => {};
export const ActionForm = (props: IProps) => {
  const { props: configProps, state, customOp, operations, execOperation = noop, updateState = noop } = props;
  const { fields } = configProps || {};
  const { formData: fD } = state || ({} as Obj);

  const { otherTaskAlias, onSubmit: propsSubmit, chosenAction, chosenActionName, nodeData, editing } = customOp || {};

  const [{ formData }, updater] = useUpdate({
    formData: isEmpty(fD) ? (!isEmpty(nodeData) ? { ...nodeData } : {}) : fD,
  });

  useUpdateEffect(() => {
    if (state && has(state, 'formData')) {
      updater.formData({ ...(state.formData || {}) });
    }
  }, [state, updater]);

  const formRef = React.useRef(null as any);

  const useableFields = React.useMemo(() => {
    const newFields = produce(fields, (draft) => {
      return map(draft, (item) => {
        const curItem = { ...item, disabled: editing ? item.disabled : true };
        if (item.key === 'alias') {
          curItem.rules = [
            {
              validator: (val = '') => {
                let tip = '';
                if (!val) {
                  tip = i18n.t('dop:please enter the task name');
                } else if (otherTaskAlias.includes(val)) {
                  tip = i18n.t('dop:An Action with the same name exists.');
                }
                return [!tip, tip];
              },
            },
          ];
        }
        const curKeyOperation = get(operations, ['change', item.key]);
        if (curKeyOperation) {
          curItem.componentProps = {
            ...curItem.componentProps,
            onChange: () => {
              const validFormData = formRef.current.getData();
              execOperation({ key: 'change', ...curKeyOperation }, { formData: validFormData });
            },
          };
        }
        return curItem;
      });
    });
    formRef.current?.setFields(newFields);
    return newFields;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fields]);

  const onSubmit = (val: Obj) => {
    let data: Obj = { resource: { ...val, type: chosenActionName } };

    const defaultResource = {
      // 默认resources
      cpu: `${get(find(fields, { key: 'resources.cpu' } as Obj), 'defaultValue')}`,
      mem: `${get(find(fields, { key: 'resources.mem' } as Obj), 'defaultValue')}`,
    };
    const editedResources = {
      // 用户修改的resources
      cpu: `${get(val, 'resources.cpu')}`,
      mem: `${get(val, 'resources.mem')}`,
    };
    // 若用户使用默认的resources，不体现在返回数据上（从原逻辑上摘下来）
    if (isEqual(editedResources, defaultResource)) {
      data = omit(data, ['resource.resources']);
    }

    const _type = get(data, 'resource.type');
    if (_type === 'custom-script') {
      // 自定义任务，如果镜像值跟默认的一直，不体现在返回数据上（从原逻辑上摘下来）
      const defaultImg = get(find(fields, { key: 'image' } as Obj), 'defaultValue');
      if (defaultImg === get(data, 'resource.image')) {
        data = omit(data, ['resource.image']);
      }
    }
    const filledFieldsData = clearEmptyField(data);
    const resData = { ...filledFieldsData, action: chosenAction } as any;
    propsSubmit(resData);
  };

  const onSave = () => {
    const curForm = formRef && (formRef.current as any);
    if (curForm) {
      curForm.onSubmit(onSubmit);
    }
  };

  return (
    <>
      <PureForm fields={useableFields || []} formRef={formRef} value={formData} />
      {editing ? (
        <Button type="primary" ghost onClick={onSave}>
          {i18n.t('save')}
        </Button>
      ) : null}
    </>
  );
};
