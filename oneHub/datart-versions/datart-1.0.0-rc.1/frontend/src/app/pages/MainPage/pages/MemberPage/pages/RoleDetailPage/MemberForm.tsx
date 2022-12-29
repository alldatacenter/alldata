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

import { Form, FormInstance, ModalProps, Transfer } from 'antd';
import { LoadingMask, ModalForm } from 'app/components';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { User } from 'app/slice/types';
import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';
import { selectMemberListLoading, selectMembers } from '../../slice/selectors';
import { getMembers } from '../../slice/thunks';

interface MemberFormProps extends ModalProps {
  initialValues: User[];
  onChange: (members: User[]) => void;
}

export const MemberForm = memo(
  ({ initialValues, onChange, onCancel, ...modalProps }: MemberFormProps) => {
    const [targetKeys, setTargetKeys] = useState<string[]>([]);
    const dispatch = useDispatch();
    const formRef = useRef<FormInstance>();
    const members = useSelector(selectMembers);
    const memberListLoading = useSelector(selectMemberListLoading);
    const orgId = useSelector(selectOrgId);
    const dataSource = useMemo(
      () => members.map(m => ({ ...m, key: m.id })),
      [members],
    );

    useEffect(() => {
      if (modalProps.visible) {
        dispatch(getMembers(orgId));
      }
    }, [dispatch, orgId, modalProps.visible]);

    useEffect(() => {
      setTargetKeys(initialValues.map(({ id }) => id));
    }, [initialValues]);

    const save = useCallback(() => {
      onChange(targetKeys.map(id => members.find(m => m.id === id)!));
      onCancel && onCancel(null as any);
    }, [targetKeys, members, onCancel, onChange]);

    const renderTitle = useCallback(
      ({ name, username, email }) => (
        <ItemTitle>
          {name && <span>{name}</span>}
          {username && <span>{` (${username})`}</span>}
          {email && <span className="email">{email}</span>}
        </ItemTitle>
      ),
      [],
    );

    const filterMemberListOptions = useCallback(
      (inputValue, option: User) =>
        [option.email, option.name, option.username].some(text =>
          text?.includes(inputValue.trim()),
        ),
      [],
    );

    return (
      <ModalForm
        {...modalProps}
        onSave={save}
        onCancel={onCancel}
        ref={formRef}
      >
        <TransferWrapper>
          <LoadingMask loading={memberListLoading}>
            <Transfer
              dataSource={dataSource}
              targetKeys={targetKeys}
              render={renderTitle}
              onChange={setTargetKeys}
              filterOption={filterMemberListOptions}
              showSearch
            />
          </LoadingMask>
        </TransferWrapper>
      </ModalForm>
    );
  },
);

const TransferWrapper = styled(Form.Item)`
  position: relative;

  .ant-transfer-list {
    flex: 1;
    height: ${SPACE_TIMES(100)};
  }
`;

const ItemTitle = styled.div`
  .email {
    margin-left: ${SPACE_XS};
    color: ${p => p.theme.textColorLight};
  }
`;
