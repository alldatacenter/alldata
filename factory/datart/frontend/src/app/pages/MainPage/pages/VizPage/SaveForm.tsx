import { Form, FormInstance, Input, Radio, TreeSelect } from 'antd';
import { ModalForm, ModalFormProps } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BoardTypes } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import {
  CommonFormTypes,
  DatartFileSuffixes,
  DEFAULT_DEBOUNCE_WAIT,
} from 'globalConstants';
import { useCallback, useContext, useEffect, useMemo, useRef } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { getCascadeAccess } from '../../Access';
import {
  selectIsOrgOwner,
  selectOrgId,
  selectPermissionMap,
} from '../../slice/selectors';
import { PermissionLevels, ResourceTypes } from '../PermissionPage/constants';
import { FileUpload } from '../ResourceMigrationPage/FileUpload';
import { SaveFormContext } from './SaveFormContext';
import {
  makeSelectStoryboradFolderTree,
  makeSelectVizFolderTree,
  selectSaveFolderLoading,
  selectSaveStoryboardLoading,
} from './slice/selectors';

type SaveFormProps = Omit<ModalFormProps, 'type' | 'visible' | 'onSave'>;

export function SaveForm({ formProps, ...modalProps }: SaveFormProps) {
  const {
    vizType,
    type,
    visible,
    initialValues,
    onSave,
    onCancel,
    onAfterClose,
  } = useContext(SaveFormContext);
  const selectVizFolderTree = useMemo(makeSelectVizFolderTree, []);
  const selectStoryboradFolderTree = useMemo(
    makeSelectStoryboradFolderTree,
    [],
  );
  const saveFolderLoading = useSelector(selectSaveFolderLoading);
  const saveStoryboardLoading = useSelector(selectSaveStoryboardLoading);
  const orgId = useSelector(selectOrgId);
  const isOwner = useSelector(selectIsOrgOwner);
  const permissionMap = useSelector(selectPermissionMap);
  const formRef = useRef<FormInstance>();
  const t = useI18NPrefix('viz.saveForm');
  const tg = useI18NPrefix('global');

  const getDisabled = useCallback(
    (_, path: string[]) =>
      !getCascadeAccess(
        isOwner,
        permissionMap,
        ResourceTypes.Viz,
        path,
        PermissionLevels.Create,
      ),
    [isOwner, permissionMap],
  );

  const folderTreeData = useSelector(state =>
    selectVizFolderTree(state, { id: initialValues?.id, getDisabled }),
  );
  const storyboardTreeData = useSelector(state =>
    selectStoryboradFolderTree(state, { id: initialValues?.id, getDisabled }),
  );

  const save = useCallback(
    values => {
      onSave(values, onCancel);
    },
    [onSave, onCancel],
  );

  const afterClose = useCallback(() => {
    formRef.current?.resetFields();
    onAfterClose && onAfterClose();
  }, [onAfterClose]);

  useEffect(() => {
    if (initialValues) {
      formRef.current?.setFieldsValue(initialValues);
    }
  }, [initialValues]);

  const boardTips = () => {
    return (
      <>
        <span>{t('boardType.autoTips')}</span>
        <br />
        <span>{t('boardType.freeTips')}</span>
      </>
    );
  };

  return (
    <ModalForm
      formProps={formProps}
      {...modalProps}
      title={t(`vizType.${vizType.toLowerCase()}`)}
      type={type}
      visible={visible}
      confirmLoading={saveFolderLoading || saveStoryboardLoading}
      onSave={save}
      onCancel={onCancel}
      afterClose={afterClose}
      ref={formRef}
    >
      <IdField name="id" hidden={type === CommonFormTypes.Add}>
        <Input />
      </IdField>
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
                vizType,
                parentId: parentId || null,
              };
              return fetchCheckName('viz', data);
            }, DEFAULT_DEBOUNCE_WAIT),
          },
        ]}
      >
        <Input />
      </Form.Item>
      {vizType === 'DATACHART' && !(type === CommonFormTypes.SaveAs) && (
        <Form.Item name="description" label={t('description')}>
          <Input.TextArea />
        </Form.Item>
      )}
      {vizType === 'DASHBOARD' && type === CommonFormTypes.Add && (
        <Form.Item
          rules={[
            {
              required: true,
              message: t('boardType.requiredMessage'),
            },
          ]}
          name="boardType"
          label={t('boardType.label')}
          tooltip={boardTips()}
        >
          <Radio.Group>
            <Radio.Button value={BoardTypes[0]}>
              {t('boardType.auto')}
            </Radio.Button>
            <Radio.Button value={BoardTypes[1]}>
              {t('boardType.free')}
            </Radio.Button>
          </Radio.Group>
        </Form.Item>
      )}
      {vizType === 'TEMPLATE' && type === CommonFormTypes.Add && (
        <Form.Item
          rules={[
            {
              required: true,
              message: t('template.requiredMessage'),
            },
          ]}
          name="file"
          label={t('template.label')}
        >
          <FileUpload
            suffix={DatartFileSuffixes.Template}
            uploadText={t('template.upload')}
          />
        </Form.Item>
      )}

      {vizType !== 'STORYBOARD' && (
        <Form.Item name="parentId" label={t('parent')}>
          <TreeSelect
            placeholder={t('root')}
            treeData={folderTreeData}
            allowClear
            onChange={() => {
              formRef.current?.validateFields();
            }}
          />
        </Form.Item>
      )}
      {vizType === 'STORYBOARD' && (
        <Form.Item name="parentId" label={t('parent')}>
          <TreeSelect
            placeholder={t('root')}
            treeData={storyboardTreeData}
            allowClear
            onChange={() => {
              formRef.current?.validateFields();
            }}
          />
        </Form.Item>
      )}
    </ModalForm>
  );
}

const IdField = styled(Form.Item)`
  display: none;
`;
