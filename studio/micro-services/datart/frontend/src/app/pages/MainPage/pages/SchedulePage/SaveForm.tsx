/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Form, FormInstance, Input, TreeSelect } from 'antd';
import { ModalForm, ModalFormProps } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import { useCallback, useContext, useEffect, useMemo, useRef } from 'react';
import { useSelector } from 'react-redux';
import { getCascadeAccess } from '../../Access';
import {
  selectIsOrgOwner,
  selectOrgId,
  selectPermissionMap,
} from '../../slice/selectors';
import { PermissionLevels, ResourceTypes } from '../PermissionPage/constants';
import { SaveFormContext } from './SaveFormContext';
import { makeSelectScheduleFolderTree } from './slice/selectors';

type SaveFormProps = Omit<ModalFormProps, 'type' | 'visible' | 'onSave'>;

export function SaveForm({ formProps, ...modalProps }: SaveFormProps) {
  const selectScheduleFolderTree = useMemo(makeSelectScheduleFolderTree, []);
  const {
    scheduleType,
    type,
    visible,
    simple,
    parentIdLabel,
    initialValues,
    onSave,
    onCancel,
    onAfterClose,
  } = useContext(SaveFormContext);
  const isOwner = useSelector(selectIsOrgOwner);
  const permissionMap = useSelector(selectPermissionMap);
  const getDisabled = useCallback(
    (_, path: string[]) =>
      !getCascadeAccess(
        isOwner,
        permissionMap,
        ResourceTypes.Schedule,
        path,
        PermissionLevels.Create,
      ),
    [isOwner, permissionMap],
  );

  const folderTree = useSelector(state =>
    selectScheduleFolderTree(state, { id: initialValues?.id, getDisabled }),
  );
  const orgId = useSelector(selectOrgId);
  const formRef = useRef<FormInstance>();
  const t = useI18NPrefix('schedule.saveForm');
  const tg = useI18NPrefix('global');

  useEffect(() => {
    if (initialValues) {
      formRef.current?.setFieldsValue({
        ...initialValues,
        parentId: initialValues.parentId || void 0,
      });
    }
  }, [initialValues]);

  const save = useCallback(
    values => {
      onSave(
        {
          ...values,
        },
        onCancel,
      );
    },
    [onSave, onCancel],
  );

  const afterClose = useCallback(() => {
    formRef.current?.resetFields();
    onAfterClose && onAfterClose();
  }, [onAfterClose]);
  return (
    <ModalForm
      formProps={formProps}
      {...modalProps}
      title={t(scheduleType)}
      type={type}
      visible={visible}
      onSave={save}
      onCancel={onCancel}
      afterClose={afterClose}
      ref={formRef}
      maskClosable={false}
    >
      {!simple && (
        <Form.Item
          name="name"
          label={t('name')}
          getValueFromEvent={event => event.target.value?.trim()}
          rules={[
            {
              required: true,
              message: `${t('name')}${tg('validation.required')}`,
            },
            {
              validator: debounce((_, value) => {
                if (!value || initialValues?.name === value) {
                  return Promise.resolve();
                }
                if (!value.trim()) {
                  return Promise.reject(
                    `${t('name')}${tg('validation.required')}`,
                  );
                }
                const parentId = formRef.current?.getFieldValue('parentId');
                const data = {
                  name: value,
                  orgId,
                  parentId: parentId || null,
                };
                return fetchCheckName('schedules', data);
              }, DEFAULT_DEBOUNCE_WAIT),
            },
          ]}
        >
          <Input />
        </Form.Item>
      )}
      <Form.Item name="parentId" label={parentIdLabel}>
        <TreeSelect
          placeholder={t('root')}
          treeData={folderTree || []}
          allowClear
          onChange={() => {
            formRef.current?.validateFields();
          }}
        />
      </Form.Item>
    </ModalForm>
  );
}
