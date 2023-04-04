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

import {
  DeleteOutlined,
  EditOutlined,
  LoadingOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Button, List, Popconfirm } from 'antd';
import { ListItem } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { getRoles } from 'app/pages/MainPage/pages/MemberPage/slice/thunks';
import {
  VariableScopes,
  VariableTypes,
  VariableValueTypes,
} from 'app/pages/MainPage/pages/VariablePage/constants';
import {
  RowPermission,
  Variable,
} from 'app/pages/MainPage/pages/VariablePage/slice/types';
import { SubjectForm } from 'app/pages/MainPage/pages/VariablePage/SubjectForm';
import { VariableFormModel } from 'app/pages/MainPage/pages/VariablePage/types';
import { VariableForm } from 'app/pages/MainPage/pages/VariablePage/VariableForm';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import classnames from 'classnames';
import { CommonFormTypes, TIME_FORMATTER } from 'globalConstants';
import { Moment } from 'moment';
import {
  memo,
  ReactElement,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { monaco } from 'react-monaco-editor';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_MD, SPACE_XS } from 'styles/StyleConstants';
import { errorHandle, uuidv4 } from 'utils/utils';
import { selectVariables } from '../../../VariablePage/slice/selectors';
import { getVariables } from '../../../VariablePage/slice/thunks';
import { ViewViewModelStages } from '../../constants';
import { EditorContext } from '../../EditorContext';
import { useViewSlice } from '../../slice';
import { selectCurrentEditingViewAttr } from '../../slice/selectors';
import { getEditorProvideCompletionItems } from '../../slice/thunks';
import { VariableHierarchy } from '../../slice/types';
import { comparePermissionChange } from '../../utils';
import Container from './Container';

export const Variables = memo(() => {
  const { actions } = useViewSlice();
  const dispatch = useDispatch();
  const [formType, setFormType] = useState(CommonFormTypes.Add);
  const [formVisible, setFormVisible] = useState(false);
  const [editingVariable, setEditingVariable] = useState<
    undefined | VariableHierarchy
  >(void 0);
  const [subjectFormVisible, setSubjectFormVisible] = useState(false);
  const { editorCompletionItemProviderRef } = useContext(EditorContext);
  const variables = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'variables' }),
  ) as VariableHierarchy[];
  const stage = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'stage' }),
  ) as ViewViewModelStages;
  const sourceId = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'sourceId' }),
  ) as string;
  const orgId = useSelector(selectOrgId);
  const publicVariables = useSelector(selectVariables);
  const t = useI18NPrefix('view.variable');
  const tg = useI18NPrefix('global');

  useEffect(() => {
    if (editorCompletionItemProviderRef) {
      editorCompletionItemProviderRef.current?.dispose();
      dispatch(
        getEditorProvideCompletionItems({
          sourceId,
          resolve: getItem => {
            editorCompletionItemProviderRef.current =
              monaco.languages.registerCompletionItemProvider('sql', {
                provideCompletionItems: getItem,
              });
          },
        }),
      );
    }
  }, [
    dispatch,
    sourceId,
    variables,
    publicVariables,
    editorCompletionItemProviderRef,
  ]);

  const listSource = useMemo(
    () =>
      ([] as Array<VariableHierarchy | Variable>)
        .concat(variables)
        .concat(publicVariables),
    [variables, publicVariables],
  );

  const rowPermissions = useMemo(() => {
    try {
      return editingVariable?.relVariableSubjects.map(r => ({
        ...r,
        value: r.value && JSON.parse(r.value),
      }));
    } catch (error) {
      errorHandle(error);
      throw error;
    }
  }, [editingVariable]);

  useEffect(() => {
    dispatch(getRoles(orgId));
    dispatch(getVariables(orgId));
  }, [dispatch, orgId]);

  const showAddForm = useCallback(() => {
    setFormType(CommonFormTypes.Add);
    setFormVisible(true);
  }, []);

  const hideForm = useCallback(() => {
    setFormVisible(false);
  }, []);

  const showEditForm = useCallback(
    id => () => {
      setFormType(CommonFormTypes.Edit);
      setEditingVariable(variables.find(v => v.id === id));
      setFormVisible(true);
    },
    [variables],
  );

  const showSubjectForm = useCallback(
    id => () => {
      const variable = variables.find(v => v.id === id)!;
      setEditingVariable(variable);
      setSubjectFormVisible(true);
    },
    [variables],
  );

  const hideSubjectForm = useCallback(() => {
    setSubjectFormVisible(false);
  }, []);

  const afterFormClose = useCallback(() => {
    setEditingVariable(void 0);
  }, []);

  const save = useCallback(
    (values: VariableFormModel) => {
      let defaultValue: any = values.defaultValue;
      if (values.valueType === VariableValueTypes.Date && !values.expression) {
        defaultValue = values.defaultValue.map(d =>
          (d as Moment).format(values.dateFormat),
        );
      }

      try {
        if (defaultValue !== void 0 && defaultValue !== null) {
          defaultValue = JSON.stringify(defaultValue);
        }
      } catch (error) {
        errorHandle(error);
        throw error;
      }

      if (formType === CommonFormTypes.Add) {
        dispatch(
          actions.changeCurrentEditingView({
            variables: variables.concat({
              ...values,
              id: uuidv4(),
              defaultValue,
              relVariableSubjects: [],
            }),
          }),
        );
      } else {
        dispatch(
          actions.changeCurrentEditingView({
            variables: variables.map(v =>
              v.id === editingVariable!.id
                ? { ...editingVariable!, ...values, defaultValue }
                : v,
            ),
          }),
        );
      }
      setFormVisible(false);
    },
    [dispatch, actions, formType, editingVariable, variables],
  );

  const del = useCallback(
    id => () => {
      dispatch(
        actions.changeCurrentEditingView({
          variables: variables.filter(v => v.id !== id),
        }),
      );
    },
    [dispatch, actions, variables],
  );

  const saveRelations = useCallback(
    (changedRowPermissions: RowPermission[]) => {
      try {
        const changedRowPermissionsRaw = changedRowPermissions.map(cr => {
          const dateFormat =
            variables.find(v => v.id === cr.variableId)?.dateFormat ||
            TIME_FORMATTER;
          return {
            ...cr,
            value: JSON.stringify(
              cr.value &&
                (editingVariable?.valueType === VariableValueTypes.Date
                  ? cr.value.map(d => (d as Moment).format(dateFormat))
                  : cr.value),
            ),
          };
        });
        if (
          !comparePermissionChange(
            editingVariable?.relVariableSubjects || [],
            changedRowPermissionsRaw,
            (oe, ce) =>
              oe.useDefaultValue === ce.useDefaultValue &&
              oe.value === ce.value,
          )
        ) {
          dispatch(
            actions.changeCurrentEditingView({
              variables: variables.map(v =>
                v.id === editingVariable?.id
                  ? {
                      ...editingVariable,
                      relVariableSubjects: changedRowPermissionsRaw,
                    }
                  : v,
              ),
            }),
          );
        }
        setSubjectFormVisible(false);
      } catch (error) {
        errorHandle(error);
        throw error;
      }
    },
    [dispatch, actions, editingVariable, variables],
  );

  const renderTitleText = useCallback(
    item => {
      const isPrivate = !!item.relVariableSubjects;
      const isDuplicate = isPrivate
        ? publicVariables.some(v => v.name === item.name)
        : variables.some(v => v.name === item.name);
      return (
        <ListItemTitle className={classnames({ duplicate: isDuplicate })}>
          {!isPrivate && <span className="prefix">{t('prefix')}</span>}
          {item.name}
          {isDuplicate && <span className="suffix">{t('suffix')}</span>}
        </ListItemTitle>
      );
    },
    [variables, publicVariables, t],
  );

  const { filteredData, debouncedSearch } = useDebouncedSearch(
    listSource,
    (keywords, data) => {
      return data.name.includes(keywords);
    },
  );

  const titleProps = useMemo(
    () => ({
      title: 'variable',
      search: true,
      add: {
        items: [{ key: 'variable', text: t('add') }],
        callback: showAddForm,
      },
      onSearch: debouncedSearch,
    }),
    [showAddForm, t, debouncedSearch],
  );

  return (
    <Container {...titleProps}>
      <ListWrapper>
        <List
          dataSource={filteredData}
          loading={
            stage === ViewViewModelStages.Loading && {
              indicator: <LoadingOutlined />,
            }
          }
          renderItem={item => {
            let actions: ReactElement[] = [];
            if ((item as VariableHierarchy).relVariableSubjects) {
              actions = [
                <Button
                  key="edit"
                  type="link"
                  className="btn-hover"
                  icon={<EditOutlined />}
                  onClick={showEditForm(item.id)}
                />,
                <Popconfirm
                  key="del"
                  title={tg('operation.deleteConfirm')}
                  placement="bottom"
                  onConfirm={del(item.id)}
                >
                  <Button
                    type="link"
                    className="btn-hover"
                    icon={<DeleteOutlined />}
                  />
                </Popconfirm>,
              ];
              if (item.type === VariableTypes.Permission) {
                actions.unshift(
                  <Button
                    key="rel"
                    type="link"
                    icon={<TeamOutlined />}
                    className="btn-hover"
                    onClick={showSubjectForm(item.id)}
                  />,
                );
              }
            }
            return (
              <ListItem actions={actions}>
                <List.Item.Meta
                  avatar={
                    item.type === VariableTypes.Query ? (
                      <SearchOutlined className="query" />
                    ) : (
                      <SafetyCertificateOutlined className="permission" />
                    )
                  }
                  title={renderTitleText(item)}
                />
              </ListItem>
            );
          }}
        />
      </ListWrapper>
      <VariableForm
        scope={VariableScopes.Private}
        orgId={orgId}
        editingVariable={editingVariable}
        variables={variables}
        visible={formVisible}
        title={t('formTitle')}
        type={formType}
        onSave={save}
        onCancel={hideForm}
        afterClose={afterFormClose}
        keyboard={false}
        maskClosable={false}
      />
      <SubjectForm
        scope={VariableScopes.Private}
        editingVariable={editingVariable}
        visible={subjectFormVisible}
        rowPermissions={rowPermissions}
        onSave={saveRelations}
        onCancel={hideSubjectForm}
        afterClose={afterFormClose}
        keyboard={false}
        maskClosable={false}
      />
    </Container>
  );
});

const ListWrapper = styled.div`
  flex: 1;
  padding-bottom: ${SPACE_MD};
  overflow-y: auto;

  .query {
    color: ${p => p.theme.info};
  }

  .permission {
    color: ${p => p.theme.warning};
  }
`;

const ListItemTitle = styled.div`
  &.duplicate {
    color: ${p => p.theme.highlight};
  }

  .prefix {
    margin-right: ${SPACE_XS};
    color: ${p => p.theme.textColorDisabled};
  }

  .suffix {
    margin-left: ${SPACE_XS};
    color: ${p => p.theme.highlight};
  }
`;
