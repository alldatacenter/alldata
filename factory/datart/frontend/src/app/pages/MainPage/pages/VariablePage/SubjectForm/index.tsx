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

import { Modal, ModalProps, Tabs } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import moment from 'moment';
import { Key, memo, useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_XS } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import {
  selectMemberListLoading,
  selectMembers,
  selectRoleListLoading,
  selectRoles,
} from '../../MemberPage/slice/selectors';
import { SubjectTypes } from '../../PermissionPage/constants';
import { VariableScopes, VariableValueTypes } from '../constants';
import { RowPermission, RowPermissionSubject, Variable } from '../slice/types';
import { RowPermissionTable } from './RowPermissionTable';

interface SubjectFormProps extends ModalProps {
  scope: VariableScopes;
  editingVariable: undefined | Variable;
  loading?: boolean;
  rowPermissions: undefined | RowPermission[];
  onSave: (rowPermissions: RowPermission[]) => void;
}

export const SubjectForm = memo(
  ({
    scope,
    editingVariable,
    loading,
    rowPermissions,
    afterClose,
    onSave,
    ...modalProps
  }: SubjectFormProps) => {
    const [tab, setTab] = useState('role');
    const [selectedRoleRowKeys, setSelectedRoleRowKeys] = useState<Key[]>([]);
    const [selectedMemberRowKeys, setSelectedMemberRowKeys] = useState<Key[]>(
      [],
    );
    const [roleRowPermissionSubjects, setRoleRowPermissionSubjects] = useState<
      undefined | RowPermissionSubject[]
    >(void 0);
    const [memberRowPermissionSubjects, setMemberRowPermissionSubjects] =
      useState<undefined | RowPermissionSubject[]>(void 0);
    const roles = useSelector(selectRoles);
    const members = useSelector(selectMembers);
    const roleListLoading = useSelector(selectRoleListLoading);
    const memberListLoading = useSelector(selectMemberListLoading);
    const t = useI18NPrefix('variable');

    useEffect(() => {
      if (editingVariable && rowPermissions && roles) {
        const roleRowPermissions = rowPermissions.filter(
          ({ subjectType }) => subjectType === SubjectTypes.Role,
        );
        const rowPermissionSubjects: RowPermissionSubject[] = [];
        const selectedRowKeys: string[] = [];
        roles.forEach(({ id, name }) => {
          const permission = roleRowPermissions.find(
            ({ subjectId }) => subjectId === id,
          );

          rowPermissionSubjects.push({
            id,
            name,
            type: SubjectTypes.Role,
            useDefaultValue: permission ? permission.useDefaultValue : true,
            value: permission?.value
              ? editingVariable.valueType === VariableValueTypes.Date
                ? permission.value.map(str => moment(str))
                : permission.value
              : void 0,
          });
          if (permission) {
            selectedRowKeys.push(id);
          }
        });
        setRoleRowPermissionSubjects(rowPermissionSubjects);
        setSelectedRoleRowKeys(selectedRowKeys);
      }
    }, [editingVariable, rowPermissions, roles]);

    useEffect(() => {
      if (editingVariable && rowPermissions && members) {
        const memberRowPermissions = rowPermissions.filter(
          ({ subjectType }) => subjectType === SubjectTypes.User,
        );
        const rowPermissionSubjects: RowPermissionSubject[] = [];
        const selectedRowKeys: string[] = [];
        members.forEach(({ id, name, username, email }) => {
          const permission = memberRowPermissions.find(
            ({ subjectId }) => subjectId === id,
          );
          rowPermissionSubjects.push({
            id,
            name: name ? `${name}(${username})` : username,
            email,
            type: SubjectTypes.User,
            useDefaultValue: permission ? permission.useDefaultValue : true,
            value: permission?.value
              ? editingVariable.valueType === VariableValueTypes.Date
                ? permission.value.map(str => moment(str))
                : permission.value
              : void 0,
          });
          if (permission) {
            selectedRowKeys.push(id);
          }
        });
        setMemberRowPermissionSubjects(rowPermissionSubjects);
        setSelectedMemberRowKeys(selectedRowKeys);
      }
    }, [editingVariable, rowPermissions, members]);

    const save = useCallback(() => {
      const roleRowPermissions: RowPermission[] = selectedRoleRowKeys.map(
        key => {
          const permission = roleRowPermissionSubjects?.find(
            rps => rps.id === key,
          )!;
          return {
            id: uuidv4(),
            variableId: editingVariable!.id,
            subjectId: key as string,
            subjectType: SubjectTypes.Role,
            useDefaultValue: permission.useDefaultValue,
            value: permission.value,
          };
        },
      );
      const roleMemberPermissions: RowPermission[] = selectedMemberRowKeys.map(
        key => {
          const permission = memberRowPermissionSubjects?.find(
            rps => rps.id === key,
          )!;
          return {
            id: uuidv4(),
            variableId: editingVariable!.id,
            subjectId: key as string,
            subjectType: SubjectTypes.User,
            useDefaultValue: permission.useDefaultValue,
            value: permission.value,
          };
        },
      );
      onSave(roleRowPermissions.concat(roleMemberPermissions));
    }, [
      editingVariable,
      roleRowPermissionSubjects,
      memberRowPermissionSubjects,
      selectedRoleRowKeys,
      selectedMemberRowKeys,
      onSave,
    ]);

    const onAfterClose = useCallback(() => {
      setSelectedRoleRowKeys([]);
      setSelectedMemberRowKeys([]);
      afterClose && afterClose();
    }, [afterClose]);

    return (
      <StyledModal
        {...modalProps}
        width={768}
        title={
          scope === VariableScopes.Public ? (
            <>
              <StyledTabs defaultActiveKey={tab} onChange={setTab}>
                <Tabs.TabPane key="role" tab={t('relatedRole')} />
                <Tabs.TabPane key="member" tab={t('relatedMember')} />
              </StyledTabs>
            </>
          ) : (
            t('relatedRole')
          )
        }
        onOk={save}
        afterClose={onAfterClose}
        destroyOnClose
      >
        <RowPermissionTable
          type="role"
          visible={tab === 'role'}
          editingVariable={editingVariable}
          loading={!!loading}
          listLoading={roleListLoading}
          selectedRowKeys={selectedRoleRowKeys}
          rowPermissionSubjects={roleRowPermissionSubjects}
          onSelectedRowKeyChange={setSelectedRoleRowKeys}
          onRowPermissionSubjectChange={setRoleRowPermissionSubjects}
        />
        {scope === VariableScopes.Public && (
          <RowPermissionTable
            type="member"
            visible={tab === 'member'}
            editingVariable={editingVariable}
            loading={!!loading}
            listLoading={memberListLoading}
            selectedRowKeys={selectedMemberRowKeys}
            rowPermissionSubjects={memberRowPermissionSubjects}
            onSelectedRowKeyChange={setSelectedMemberRowKeys}
            onRowPermissionSubjectChange={setMemberRowPermissionSubjects}
          />
        )}
      </StyledModal>
    );
  },
);

const StyledModal = styled(Modal)`
  .ant-modal-header {
    padding-top: ${SPACE_XS};
    padding-bottom: 0;
  }
  .ant-modal-body {
    padding-bottom: 0;
  }
`;

const StyledTabs = styled(Tabs)`
  .ant-tabs-nav {
    margin-bottom: 0 !important;

    &:before {
      border-bottom: 0 !important;
    }
  }
`;
