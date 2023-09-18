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
import { DetailPageHeader } from 'app/components/DetailPageHeader';
import { TenantManagementMode } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectSystemInfo } from 'app/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import React, { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_LG } from 'styles/StyleConstants';
import { getPasswordValidator } from 'utils/validators';
import { selectOrgId } from '../../../slice/selectors';
import { useMemberSlice } from '../slice';
import {
  selectEditingMember,
  selectGetMemberRolesLoading,
  selectGrantOwnerLoading,
  selectMembers,
  selectRemoveMemberLoading,
  selectRoleListLoading,
  selectRoles,
  selectSaveMemberLoading,
} from '../slice/selectors';
import {
  deleteMember,
  getMemberRoles,
  getRoles,
  grantOwner,
  removeMember,
  revokeOwner,
  saveMember,
} from '../slice/thunks';

export function MemberDetailPage() {
  const [formType, setFormType] = useState(CommonFormTypes.Add);
  const [passwordVisible, setPasswordVisible] = useState(true);
  const { actions } = useMemberSlice();
  const dispatch = useDispatch();
  const history = useHistory();
  const systemInfo = useSelector(selectSystemInfo);
  const orgId = useSelector(selectOrgId);
  const members = useSelector(selectMembers);
  const editingMember = useSelector(selectEditingMember);
  const getMemberRolesLoading = useSelector(selectGetMemberRolesLoading);
  const saveMemberLoading = useSelector(selectSaveMemberLoading);
  const removeMemberLoading = useSelector(selectRemoveMemberLoading);
  const roles = useSelector(selectRoles);
  const roleListLoading = useSelector(selectRoleListLoading);
  const grantLoading = useSelector(selectGrantOwnerLoading);
  const {
    params: { memberId },
  } = useRouteMatch<{ memberId: string }>();
  const [form] = Form.useForm();
  const isPlatformMode =
    systemInfo?.tenantManagementMode === TenantManagementMode.Platform;
  const t = useI18NPrefix('member.memberDetail');
  const tg = useI18NPrefix('global');

  const resetForm = useCallback(() => {
    form.resetFields();
    dispatch(actions.clearEditingMember());
  }, [dispatch, form, actions]);

  useEffect(() => {
    dispatch(getRoles(orgId));
  }, [dispatch, orgId]);

  useEffect(() => {
    resetForm();
    if (memberId === 'add') {
      setFormType(CommonFormTypes.Add);
      setPasswordVisible(true);
    } else {
      setFormType(CommonFormTypes.Edit);
      setPasswordVisible(false);
      if (members.length > 0) {
        dispatch(actions.initEditingMember(memberId));
        dispatch(getMemberRoles({ orgId, memberId }));
      }
    }
  }, [dispatch, resetForm, actions, orgId, members, memberId]);

  useEffect(() => {
    if (editingMember) {
      form.setFieldsValue({
        ...editingMember.info,
        roleIds: editingMember?.roles?.map(({ id }) => id),
      });
    }
  }, [form, editingMember]);

  useEffect(() => {
    return () => {
      resetForm();
    };
  }, [resetForm]);

  const roleListVisibleChange = useCallback(
    open => {
      if (open) {
        dispatch(getRoles(orgId));
      }
    },
    [dispatch, orgId],
  );

  const showPassword = useCallback(() => {
    setPasswordVisible(true);
  }, []);

  const save = useCallback(() => {
    form.submit();
  }, [form]);

  const formSubmit = useCallback(
    async values => {
      await dispatch(
        saveMember({
          type:
            formType === CommonFormTypes.Add
              ? 'add'
              : isPlatformMode
              ? 'update'
              : 'edit',
          orgId,
          ...values,
          resolve: () => {
            message.success(
              formType === CommonFormTypes.Add
                ? t('createSuccess')
                : tg('operation.updateSuccess'),
            );
          },
        }),
      );
    },
    [dispatch, orgId, formType, isPlatformMode, t, tg],
  );

  const remove = useCallback(() => {
    dispatch(
      removeMember({
        id: editingMember!.info.id,
        orgId,
        resolve: () => {
          message.success(t('removeSuccess'));
          history.replace(`/organizations/${orgId}/members`);
        },
      }),
    );
  }, [dispatch, history, orgId, editingMember, t]);

  const del = useCallback(() => {
    dispatch(
      deleteMember({
        id: editingMember!.info.id,
        orgId,
        resolve: () => {
          message.success(t('deleteSuccess'));
          history.replace(`/organizations/${orgId}/members`);
        },
      }),
    );
  }, [dispatch, history, orgId, editingMember, t]);

  const grantOrgOwner = useCallback(
    (grant: boolean) => () => {
      if (editingMember?.info) {
        const params = {
          userId: editingMember.info.id,
          orgId,
          resolve: () => {
            message.success(grant ? t('grantSuccess') : t('revokeSuccess'));
          },
        };
        dispatch(grant ? grantOwner(params) : revokeOwner(params));
      }
    },
    [dispatch, orgId, editingMember, t],
  );

  return (
    <Wrapper>
      <DetailPageHeader
        title={
          formType === CommonFormTypes.Add ? t('titleAdd') : t('titleDetail')
        }
        actions={
          <>
            <Button type="primary" loading={saveMemberLoading} onClick={save}>
              {tg('button.save')}
            </Button>
            {formType === CommonFormTypes.Edit && (
              <>
                {editingMember?.info.orgOwner ? (
                  <Button
                    loading={grantLoading}
                    onClick={grantOrgOwner(false)}
                    danger
                  >
                    {t('revokeOwner')}
                  </Button>
                ) : (
                  <Button loading={grantLoading} onClick={grantOrgOwner(true)}>
                    {t('grantOwner')}
                  </Button>
                )}
                {isPlatformMode ? (
                  <Popconfirm title={t('removeConfirm')} onConfirm={remove}>
                    <Button loading={removeMemberLoading} danger>
                      {t('remove')}
                    </Button>
                  </Popconfirm>
                ) : (
                  <Popconfirm title={t('deleteConfirm')} onConfirm={del}>
                    <Button loading={removeMemberLoading} danger>
                      {t('delete')}
                    </Button>
                  </Popconfirm>
                )}
              </>
            )}
          </>
        }
      />
      <Content>
        <Card bordered={false}>
          <Form
            name="member_form_"
            form={form}
            labelAlign="left"
            labelCol={{ span: 8 }}
            wrapperCol={{ span: 16 }}
            onFinish={formSubmit}
          >
            {isPlatformMode ? (
              <>
                <Form.Item label={t('username')}>
                  {editingMember?.info.username}
                </Form.Item>
                <Form.Item label={t('email')}>
                  {editingMember?.info.email}
                </Form.Item>
                <Form.Item label={t('name')}>
                  {editingMember?.info.name || '-'}
                </Form.Item>
              </>
            ) : (
              <>
                {formType === CommonFormTypes.Edit && (
                  <Form.Item name="id" hidden>
                    <Input />
                  </Form.Item>
                )}
                <Form.Item
                  name="username"
                  label={t('username')}
                  rules={[
                    {
                      required: true,
                      message: `${t('username')}${tg('validation.required')}`,
                    },
                    {
                      // validator: nameValidator,
                    },
                  ]}
                >
                  <Input
                    placeholder={t('username')}
                    disabled={formType === CommonFormTypes.Edit}
                  />
                </Form.Item>
                <Form.Item
                  name="email"
                  label={t('email')}
                  rules={[
                    {
                      required: true,
                      message: `${t('email')}${tg('validation.required')}`,
                    },
                    { type: 'email' },
                  ]}
                >
                  <Input type="email" placeholder={t('email')} />
                </Form.Item>

                <Form.Item
                  name="password"
                  label={t('password')}
                  rules={
                    passwordVisible
                      ? [
                          {
                            required: true,
                            message: `${t('password')}${tg(
                              'validation.required',
                            )}`,
                          },
                          {
                            validator: getPasswordValidator(
                              tg('validation.invalidPassword'),
                            ),
                          },
                        ]
                      : void 0
                  }
                >
                  {passwordVisible ? (
                    <Input.Password placeholder={t('password')} />
                  ) : (
                    <Button type="link" size="small" onClick={showPassword}>
                      {t('changePassword')}
                    </Button>
                  )}
                </Form.Item>
                <Form.Item name="name" label={t('name')}>
                  <Input placeholder={t('name')} />
                </Form.Item>
                <Form.Item name="description" label={t('description')}>
                  <Input.TextArea placeholder={t('description')} />
                </Form.Item>
              </>
            )}
            <Form.Item name="roleIds" label={t('roles')}>
              {getMemberRolesLoading ? (
                <LoadingOutlined />
              ) : (
                <Select
                  placeholder={t('assignRole')}
                  mode="multiple"
                  optionFilterProp="children"
                  loading={roleListLoading}
                  onDropdownVisibleChange={roleListVisibleChange}
                >
                  {roles?.map(({ id, name }) => (
                    <Select.Option key={id} value={id}>
                      {name}
                    </Select.Option>
                  ))}
                </Select>
              )}
            </Form.Item>
          </Form>
        </Card>
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
    max-width: 480px;
  }
`;
