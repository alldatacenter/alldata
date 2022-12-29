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

import { Button, Card, Form, Input, message, Popconfirm } from 'antd';
import { DetailPageHeader } from 'app/components/DetailPageHeader';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { User } from 'app/slice/types';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import { CommonFormTypes, DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import React, { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_LG, SPACE_MD } from 'styles/StyleConstants';
import { selectOrgId } from '../../../../slice/selectors';
import { useMemberSlice } from '../../slice';
import {
  selectEditingRole,
  selectGetRoleMembersLoading,
  selectRoles,
  selectSaveRoleLoading,
} from '../../slice/selectors';
import {
  addRole,
  deleteRole,
  editRole,
  getRoleMembers,
} from '../../slice/thunks';
import { Role } from '../../slice/types';
import { MemberForm } from './MemberForm';
import { MemberTable } from './MemberTable';

export function RoleDetailPage() {
  const [formType, setFormType] = useState(CommonFormTypes.Add);
  const [memberFormVisible, setMemberFormVisible] = useState(false);
  const [memberTableDataSource, setMemberTableDataSource] = useState<User[]>(
    [],
  );
  const { actions } = useMemberSlice();
  const dispatch = useDispatch();
  const history = useHistory();
  const orgId = useSelector(selectOrgId);
  const roles = useSelector(selectRoles);
  const editingRole = useSelector(selectEditingRole);
  const getRoleMembersLoading = useSelector(selectGetRoleMembersLoading);
  const saveRoleLoading = useSelector(selectSaveRoleLoading);
  const t = useI18NPrefix('member.roleDetail');
  const tg = useI18NPrefix('global');
  const { params } = useRouteMatch<{ roleId: string }>();
  const { roleId } = params;
  const [form] = Form.useForm<Pick<Role, 'name' | 'description'>>();

  const resetForm = useCallback(() => {
    form.resetFields();
    dispatch(actions.clearEditingRole());
  }, [dispatch, form, actions]);

  useEffect(() => {
    resetForm();
    if (roleId === 'add') {
      setFormType(CommonFormTypes.Add);
    } else {
      if (roles.length > 0) {
        setFormType(CommonFormTypes.Edit);
        dispatch(actions.initEditingRole(roleId));
        dispatch(getRoleMembers(roleId));
      }
    }
  }, [dispatch, resetForm, actions, roles, roleId]);

  useEffect(() => {
    if (editingRole) {
      form.setFieldsValue(editingRole.info);
      setMemberTableDataSource(editingRole.members);
    }
  }, [dispatch, form, actions, editingRole]);

  useEffect(() => {
    return () => {
      resetForm();
    };
  }, [resetForm]);

  const showMemberForm = useCallback(() => {
    setMemberFormVisible(true);
  }, []);

  const hideMemberForm = useCallback(() => {
    setMemberFormVisible(false);
  }, []);

  const formSubmit = useCallback(
    (values: Pick<Role, 'name' | 'description'>) => {
      switch (formType) {
        case CommonFormTypes.Add:
          dispatch(
            addRole({
              role: { ...values, avatar: '', orgId },
              resolve: () => {
                message.success(t('createSuccess'));
                resetForm();
              },
            }),
          );
          break;
        case CommonFormTypes.Edit:
          dispatch(
            editRole({
              role: { ...values, orgId },
              members: memberTableDataSource,
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
    [dispatch, orgId, formType, memberTableDataSource, resetForm, t, tg],
  );

  const del = useCallback(() => {
    dispatch(
      deleteRole({
        id: editingRole!.info.id,
        resolve: () => {
          message.success(tg('operation.deleteSuccess'));
          history.replace(`/organizations/${orgId}/roles`);
        },
      }),
    );
  }, [dispatch, history, orgId, editingRole, tg]);

  return (
    <Wrapper>
      <DetailPageHeader
        title={`${tg(`modal.title.${formType}`)}${t('role')}`}
        actions={
          <>
            <Button
              type="primary"
              loading={saveRoleLoading}
              onClick={form.submit}
            >
              {tg('button.save')}
            </Button>
            {formType === CommonFormTypes.Edit && (
              <Popconfirm title={tg('operation.deleteConfirm')} onConfirm={del}>
                <Button danger>{`${tg('button.delete')}${t('role')}`}</Button>
              </Popconfirm>
            )}
          </>
        }
      />
      <Content>
        <Card bordered={false}>
          <Form
            name="role_form_"
            form={form}
            labelAlign="left"
            labelCol={{ span: 4 }}
            wrapperCol={{ span: 8 }}
            onFinish={formSubmit}
          >
            <Form.Item
              name="name"
              label={t('roleName')}
              validateFirst
              getValueFromEvent={event => event.target.value?.trim()}
              rules={[
                {
                  required: true,
                  message: `${t('roleName')}${tg('validation.required')}`,
                },
                {
                  validator: debounce((_, value) => {
                    if (value === editingRole?.info.name) {
                      return Promise.resolve();
                    }
                    const data = { name: value, orgId };
                    return fetchCheckName('roles', data);
                  }, DEFAULT_DEBOUNCE_WAIT),
                },
              ]}
            >
              <Input />
            </Form.Item>
            <Form.Item name="description" label={t('description')}>
              <Input.TextArea />
            </Form.Item>
            {formType === CommonFormTypes.Edit && (
              <Form.Item label={t('relatedMember')} wrapperCol={{ span: 17 }}>
                <MemberTable
                  loading={getRoleMembersLoading}
                  dataSource={memberTableDataSource}
                  onAdd={showMemberForm}
                  onChange={setMemberTableDataSource}
                />
              </Form.Item>
            )}
          </Form>
        </Card>
        <MemberForm
          title={t('addMember')}
          visible={memberFormVisible}
          width={992}
          onCancel={hideMemberForm}
          initialValues={memberTableDataSource}
          onChange={setMemberTableDataSource}
        />
      </Content>
    </Wrapper>
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
    background-color: ${p => p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadowBlock};
  }

  form {
    max-width: 960px;
    padding-top: ${SPACE_MD};
  }
`;
