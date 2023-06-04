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

import { DoubleRightOutlined } from '@ant-design/icons';
import {
  Button,
  Checkbox,
  Form,
  FormInstance,
  Input,
  InputNumber,
  Radio,
  Switch,
  TreeSelect,
} from 'antd';
import { ModalForm, ModalFormProps } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { APP_CURRENT_VERSION } from 'app/migration/constants';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_MD } from 'styles/StyleConstants';
import { getCascadeAccess } from '../../Access';
import {
  selectIsOrgOwner,
  selectOrgId,
  selectPermissionMap,
} from '../../slice/selectors';
import { PermissionLevels, ResourceTypes } from '../PermissionPage/constants';
import { ConcurrencyControlModes, ViewViewModelStages } from './constants';
import { SaveFormContext } from './SaveFormContext';
import {
  makeSelectViewFolderTree,
  selectCurrentEditingView,
} from './slice/selectors';

type SaveFormProps = Omit<ModalFormProps, 'type' | 'visible' | 'onSave'>;

export function SaveForm({ formProps, ...modalProps }: SaveFormProps) {
  const [advancedVisible, setAdvancedVisible] = useState(false);
  const [concurrencyControl, setConcurrencyControl] = useState(true);
  const [cache, setCache] = useState(false);
  const selectViewFolderTree = useMemo(makeSelectViewFolderTree, []);
  const [expensiveQuery, setExpensiveQuery] = useState(false); // beta.2 add expensiveQuery
  const {
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
        ResourceTypes.View,
        path,
        PermissionLevels.Create,
      ),
    [isOwner, permissionMap],
  );

  const folderTree = useSelector(state =>
    selectViewFolderTree(state, { id: initialValues?.id, getDisabled }),
  );
  const currentEditingView = useSelector(selectCurrentEditingView);
  const orgId = useSelector(selectOrgId);
  const formRef = useRef<FormInstance>();
  const t = useI18NPrefix('view.saveForm');
  const tg = useI18NPrefix('global');

  useEffect(() => {
    if (initialValues) {
      formRef.current?.setFieldsValue({
        ...initialValues,
        parentId: initialValues.parentId || void 0,
      });
    }
  }, [initialValues]);

  const toggleAdvanced = useCallback(() => {
    setAdvancedVisible(!advancedVisible);
  }, [advancedVisible]);

  const save = useCallback(
    values => {
      onSave(
        {
          ...values,
          config: { version: APP_CURRENT_VERSION, ...values.config },
        },
        onCancel,
      );
    },
    [onSave, onCancel],
  );

  const afterClose = useCallback(() => {
    formRef.current?.resetFields();
    setAdvancedVisible(false);
    setConcurrencyControl(true);
    setCache(false);
    onAfterClose && onAfterClose();
    setExpensiveQuery(false);
  }, [onAfterClose]);

  return (
    <ModalForm
      formProps={formProps}
      {...modalProps}
      title={t(simple ? 'folder' : 'title')}
      type={type}
      visible={visible}
      confirmLoading={currentEditingView?.stage === ViewViewModelStages.Saving}
      onSave={save}
      onCancel={onCancel}
      afterClose={afterClose}
      ref={formRef}
    >
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
              return fetchCheckName('views', data);
            }, DEFAULT_DEBOUNCE_WAIT),
          },
        ]}
      >
        <Input />
      </Form.Item>
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
      {!simple && initialValues?.config && (
        <>
          <AdvancedToggle
            type="link"
            icon={<DoubleRightOutlined rotate={advancedVisible ? -90 : 90} />}
            onClick={toggleAdvanced}
          >
            {t('advanced')}
          </AdvancedToggle>
          <AdvancedWrapper show={advancedVisible}>
            <Form.Item
              name={['config', 'concurrencyControl']}
              label={t('concurrencyControl')}
              valuePropName="checked"
              initialValue={concurrencyControl}
            >
              <Switch onChange={setConcurrencyControl} />
            </Form.Item>
            <Form.Item
              name={['config', 'concurrencyControlMode']}
              label={t('concurrencyControlMode')}
              initialValue={ConcurrencyControlModes.DirtyRead}
            >
              <Radio.Group disabled={!concurrencyControl}>
                {Object.values(ConcurrencyControlModes).map(value => (
                  <Radio key={value} value={value}>
                    {t(value.toLowerCase())}
                  </Radio>
                ))}
              </Radio.Group>
            </Form.Item>
            <Form.Item
              name={['config', 'cache']}
              label={t('cache')}
              valuePropName="checked"
              initialValue={cache}
            >
              <Switch onChange={setCache} />
            </Form.Item>
            <Form.Item
              name={['config', 'cacheExpires']}
              label={t('cacheExpires')}
              initialValue={0}
            >
              <InputNumber disabled={!cache} min={0} />
            </Form.Item>
            <Form.Item
              wrapperCol={{ span: 13, offset: 9 }}
              name={['config', 'expensiveQuery']}
              initialValue={expensiveQuery}
              valuePropName="checked"
            >
              <Checkbox>{t('expensiveQuery')}</Checkbox>
            </Form.Item>
          </AdvancedWrapper>
        </>
      )}
    </ModalForm>
  );
}

const AdvancedToggle = styled(Button)`
  margin-bottom: ${SPACE_MD};
`;

const AdvancedWrapper = styled.div<{ show: boolean }>`
  display: ${p => (p.show ? 'block' : 'none')};
`;
