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

import { LoadingOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, message, Popconfirm, Select } from 'antd';
import { Authorized, EmptyFiller } from 'app/components';
import { DetailPageHeader } from 'app/components/DetailPageHeader';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useAccess, useCascadeAccess } from 'app/pages/MainPage/Access';
import {
  PermissionLevels,
  ResourceTypes,
} from 'app/pages/MainPage/pages/PermissionPage/constants';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import {
  CommonFormTypes,
  DEFAULT_DEBOUNCE_WAIT,
  TIME_FORMATTER,
} from 'globalConstants';
import moment from 'moment';
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  SPACE_LG,
  SPACE_MD,
  SPACE_TIMES,
} from 'styles/StyleConstants';
import { request2 } from 'utils/request';
import {
  errorHandle,
  getInsertedNodeIndex,
  getPath,
  uuidv4,
} from 'utils/utils';
import {
  selectDataProviderConfigTemplateLoading,
  selectDataProviderListLoading,
  selectDataProviders,
  selectIsOrgOwner,
  selectOrgId,
} from '../../../slice/selectors';
import { getDataProviderConfigTemplate } from '../../../slice/thunks';
import { UNPERSISTED_ID_PREFIX } from '../../ViewPage/constants';
import { QueryResult } from '../../ViewPage/slice/types';
import { SaveFormContext } from '../SaveFormContext';
import { useSourceSlice } from '../slice';
import {
  selectDeleteSourceLoading,
  selectEditingSource,
  selectSaveSourceLoading,
  selectSources,
  selectSyncSourceSchemaLoading,
  selectUnarchiveSourceLoading,
} from '../slice/selectors';
import {
  addSource,
  deleteSource,
  editSource,
  getSource,
  syncSourceSchema,
  unarchiveSource,
} from '../slice/thunks';
import { SourceFormModel, SourceSimple } from '../slice/types';
import { allowManageSource } from '../utils';
import { ConfigComponent } from './ConfigComponent';

export function SourceDetailPage() {
  const [formType, setFormType] = useState(CommonFormTypes.Add);
  const [providerType, setProviderType] = useState('');
  const [testLoading, setTestLoading] = useState(false);
  const [lastUpdateTime, setLastUpdateTime] = useState<string | undefined>();
  const { actions } = useSourceSlice();
  const dispatch = useDispatch();
  const history = useHistory();
  const orgId = useSelector(selectOrgId);
  const isOwner = useSelector(selectIsOrgOwner);
  const editingSource = useSelector(selectEditingSource);
  const dataProviders = useSelector(selectDataProviders);
  const dataProviderListLoading = useSelector(selectDataProviderListLoading);
  const dataProviderConfigTemplateLoading = useSelector(
    selectDataProviderConfigTemplateLoading,
  );
  const saveSourceLoading = useSelector(selectSaveSourceLoading);
  const unarchiveSourceLoading = useSelector(selectUnarchiveSourceLoading);
  const deleteSourceLoading = useSelector(selectDeleteSourceLoading);
  const syncSourceSchemaLoading = useSelector(selectSyncSourceSchemaLoading);
  const sourceData = useSelector(selectSources);
  const { params } = useRouteMatch<{ sourceId: string }>();
  const { sourceId } = params;
  const [form] = Form.useForm<SourceFormModel>();
  const { showSaveForm } = useContext(SaveFormContext);
  const t = useI18NPrefix('source');
  const tg = useI18NPrefix('global');
  const isArchived = editingSource?.status === 0;
  const allowCreate = sourceId === 'add';
  const path = useMemo(
    () =>
      sourceData
        ? getPath(
            sourceData as Array<{ id: string; parentId: string }>,
            { id: sourceId, parentId: editingSource?.parentId || null },
            ResourceTypes.Source,
          )
        : [],
    [sourceData, sourceId, editingSource?.parentId],
  );
  const allowManage =
    useCascadeAccess(allowManageSource(path))(true) && sourceId !== 'add';
  const allowEnableView = useAccess({
    type: 'module',
    module: ResourceTypes.View,
    id: '',
    level: PermissionLevels.Enable,
  })(true);

  const config = useMemo(
    () => dataProviders[providerType]?.config,
    [providerType, dataProviders],
  );

  const resetForm = useCallback(() => {
    setProviderType('');
    form.resetFields();
    dispatch(actions.clearEditingSource());
  }, [dispatch, form, actions]);

  useEffect(() => {
    resetForm();
    if (sourceId === 'add') {
      setFormType(CommonFormTypes.Add);
    } else {
      setFormType(CommonFormTypes.Edit);
      dispatch(getSource(sourceId));
    }
  }, [dispatch, resetForm, sourceId]);

  useEffect(() => {
    if (editingSource) {
      const { name, type, config } = editingSource;
      try {
        setProviderType(type);
        setLastUpdateTime(editingSource?.schemaUpdateDate);
        form.setFieldsValue({ name, type, config: JSON.parse(config) });
      } catch (error) {
        message.error(tg('operation.parseError'));
        throw error;
      }
    }
  }, [form, editingSource, tg]);

  useEffect(() => {
    if (
      dataProviders[providerType]?.config === null &&
      !dataProviderConfigTemplateLoading
    ) {
      dispatch(getDataProviderConfigTemplate(providerType));
    }
  }, [
    dispatch,
    providerType,
    dataProviders,
    dataProviderConfigTemplateLoading,
  ]);

  useEffect(() => {
    return () => {
      resetForm();
    };
  }, [resetForm]);

  const dataProviderChange = useCallback(
    val => {
      setProviderType(val);
      if (dataProviders[val].config === null) {
        dispatch(getDataProviderConfigTemplate(val));
      }
    },
    [dispatch, dataProviders],
  );

  const dbTypeChange = useCallback(
    val => {
      const dbTypeConfig = config?.attributes.find(
        ({ name }) => name === 'dbType',
      );
      if (dbTypeConfig) {
        const selected = dbTypeConfig.options?.find(
          ({ dbType }) => dbType === val,
        );
        if (selected) {
          const { url, driverClass } = selected;
          form.setFieldsValue({ config: { url, driverClass } });
        }
      }
    },
    [config, form],
  );

  const test = useCallback(async () => {
    await form.validateFields();
    const { type, config } = form.getFieldsValue();
    const { name } = dataProviders[type];
    setTestLoading(true);
    try {
      await request2<QueryResult>({
        url: '/data-provider/test',
        method: 'POST',
        data: { name, type, properties: config },
      });
      message.success(t('testSuccess'));
    } catch (error) {
      errorHandle(error);
    }
    setTestLoading(false);
  }, [form, dataProviders, t]);

  const subFormTest = useCallback(
    async (config, callback) => {
      const { name } = dataProviders[providerType];
      setTestLoading(true);
      try {
        const { data } = await request2<QueryResult>({
          url: '/data-provider/test',
          method: 'POST',
          data: {
            name,
            type: providerType,
            properties:
              providerType === 'FILE'
                ? { path: config.path, format: config.format }
                : config,
            sourceId: editingSource?.id,
          },
        });
        callback(data);
      } catch (error) {
        errorHandle(error);
      }
      setTestLoading(false);
    },
    [dataProviders, providerType, editingSource],
  );

  const formSubmit = useCallback(
    (values: SourceFormModel) => {
      const { config, ...rest } = values;
      let configStr = '';

      try {
        configStr = JSON.stringify(config);
      } catch (error) {
        message.error((error as Error).message);
        throw error;
      }

      switch (formType) {
        case CommonFormTypes.Add:
          showSaveForm({
            sourceType: 'title',
            type: CommonFormTypes.Add,
            visible: true,
            simple: true,
            parentIdLabel: t('sidebar.parent'),
            onSave: (val, onClose) => {
              dispatch(
                addSource({
                  source: {
                    ...rest,
                    parentId: val.parentId,
                    orgId,
                    config: configStr,
                  },
                  resolve: (id: string) => {
                    message.success(t('createSuccess'));
                    history.push(`/organizations/${orgId}/sources/${id}`);
                    onClose();
                  },
                }),
              );
            },
          });
          break;
        case CommonFormTypes.Edit:
          dispatch(
            editSource({
              source: {
                ...(editingSource as SourceSimple),
                ...rest,
                orgId,
                config: configStr,
              },
              resolve: () => {
                message.success(tg('operation.updateSuccess'));
              },
            }),
          );
          break;
        default:
          break;
      }
    },
    [formType, showSaveForm, t, dispatch, editingSource, orgId, history, tg],
  );

  const del = useCallback(
    archive => () => {
      dispatch(
        deleteSource({
          id: editingSource!.id,
          archive,
          resolve: () => {
            message.success(
              archive
                ? tg('operation.archiveSuccess')
                : tg('operation.deleteSuccess'),
            );
            history.replace(`/organizations/${orgId}/sources`);
          },
        }),
      );
    },
    [dispatch, history, orgId, editingSource, tg],
  );

  const unarchive = useCallback(() => {
    const { id, name } = editingSource!;
    showSaveForm({
      sourceType: 'folder',
      type: CommonFormTypes.Edit,
      visible: true,
      simple: false,
      initialValues: { id, name, parentId: null },
      parentIdLabel: t('sidebar.parent'),
      onSave: (values, onClose) => {
        let index = getInsertedNodeIndex(values, sourceData);
        dispatch(
          unarchiveSource({
            source: {
              name: values.name,
              parentId: values.parentId || null,
              id,
              index,
            },
            resolve: () => {
              message.success(tg('operation.restoreSuccess'));
              history.replace(`/organizations/${orgId}/sources`);
              onClose();
            },
          }),
        );
      },
    });
  }, [
    editingSource,
    showSaveForm,
    t,
    sourceData,
    dispatch,
    tg,
    history,
    orgId,
  ]);

  const titleLabelPrefix = useMemo(
    () => (isArchived ? t('archived') : tg(`modal.title.${formType}`)),
    [isArchived, formType, t, tg],
  );

  const addNewView = useCallback(() => {
    history.push({
      pathname: `/organizations/${orgId}/views/${`${UNPERSISTED_ID_PREFIX}${uuidv4()}`}`,
      state: {
        sourcesId: editingSource?.id,
      },
    });
  }, [history, orgId, editingSource]);

  const handleSyncDatabase = async () => {
    if (!editingSource?.id) {
      return;
    }
    await dispatch(syncSourceSchema({ sourceId: editingSource.id }));
    setLastUpdateTime(moment().format(TIME_FORMATTER));
    message.success(t('syncDatabaseSchemaSuccess'));
  };

  return (
    <Authorized
      authority={allowCreate || allowManage}
      denied={<EmptyFiller title={t('noPermission')} />}
    >
      <Wrapper>
        <DetailPageHeader
          title={`${titleLabelPrefix}${t('source')}`}
          actions={
            !isArchived ? (
              <>
                {allowEnableView && (
                  <Button
                    disabled={!(formType === CommonFormTypes.Edit)}
                    type="primary"
                    onClick={addNewView}
                  >
                    {t('creatView')}
                  </Button>
                )}
                <Button
                  disabled={!Boolean(editingSource?.id)}
                  loading={syncSourceSchemaLoading}
                  onClick={handleSyncDatabase}
                >
                  {t('syncDatabase')}
                </Button>
                <Button
                  type="primary"
                  loading={saveSourceLoading}
                  onClick={form.submit}
                >
                  {tg('button.save')}
                </Button>
                {formType === CommonFormTypes.Edit && (
                  <Popconfirm
                    title={tg('operation.archiveConfirm')}
                    onConfirm={del(true)}
                  >
                    <Button loading={deleteSourceLoading} danger>
                      {tg('button.archive')}
                    </Button>
                  </Popconfirm>
                )}
              </>
            ) : isOwner ? (
              <>
                <Button loading={unarchiveSourceLoading} onClick={unarchive}>
                  {tg('button.restore')}
                </Button>
                <Popconfirm
                  title={tg('operation.deleteConfirm')}
                  onConfirm={del(false)}
                >
                  <Button loading={deleteSourceLoading} danger>
                    {tg('button.delete')}
                  </Button>
                </Popconfirm>
              </>
            ) : (
              <></>
            )
          }
        />
        <Content>
          <Card
            bordered={false}
            extra={
              <div>
                {lastUpdateTime && `${t('lastUpdateTime')}: ${lastUpdateTime}`}
              </div>
            }
          >
            <Form
              name="source_form_"
              className="detailForm"
              form={form}
              labelAlign="left"
              labelCol={{ offset: 1, span: 5 }}
              wrapperCol={{ span: 8 }}
              onFinish={formSubmit}
            >
              <Form.Item
                name="name"
                label={t('form.name')}
                validateFirst
                getValueFromEvent={event => event.target.value?.trim()}
                rules={[
                  {
                    required: true,
                    message: `${t('form.name')}${tg('validation.required')}`,
                  },
                  {
                    validator: debounce((_, value) => {
                      if (value === editingSource?.name) {
                        return Promise.resolve();
                      }
                      if (!value.trim()) {
                        return Promise.reject(
                          `${t('form.name')}${tg('validation.required')}`,
                        );
                      }
                      const data = { name: value, orgId };
                      return fetchCheckName('sources', data);
                    }, DEFAULT_DEBOUNCE_WAIT),
                  },
                ]}
              >
                <Input disabled={isArchived} />
              </Form.Item>
              <Form.Item
                name="type"
                label={t('form.type')}
                rules={[
                  {
                    required: true,
                    message: `${t('form.type')}${tg('validation.required')}`,
                  },
                ]}
              >
                <Select
                  loading={dataProviderListLoading}
                  disabled={isArchived}
                  onChange={dataProviderChange}
                >
                  {Object.keys(dataProviders).map(key => (
                    <Select.Option key={key} value={key}>
                      {key}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
              {dataProviderConfigTemplateLoading && <LoadingOutlined />}

              {config?.attributes.map(attr => (
                <ConfigComponent
                  key={`${providerType}_${attr.name}`}
                  attr={attr}
                  form={form}
                  sourceId={editingSource?.id}
                  testLoading={testLoading}
                  disabled={isArchived}
                  allowManage={allowManage}
                  onTest={test}
                  onSubFormTest={subFormTest}
                  onDbTypeChange={dbTypeChange}
                />
              ))}
            </Form>
          </Card>
        </Content>
      </Wrapper>
    </Authorized>
  );
}

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
`;

const Content = styled.div`
  flex: 1;
  padding: ${SPACE_LG};
  overflow-y: auto;

  .ant-card {
    margin-top: ${SPACE_LG};
    background-color: ${p => p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadowBlock};

    &:first-of-type {
      margin-top: 0;
    }
  }

  .detailForm {
    max-width: ${SPACE_TIMES(400)};
    padding-top: ${SPACE_MD};
  }
`;
