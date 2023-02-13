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

import { DatePicker, Form, Modal, Radio, Select, Space } from 'antd';
import { FormItemEx } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useMemberSlice } from 'app/pages/MainPage/pages/MemberPage/slice';
import {
  selectMembers,
  selectRoles as rdxSelectRoles,
} from 'app/pages/MainPage/pages/MemberPage/slice/selectors';
import {
  getMembers,
  getRoles,
} from 'app/pages/MainPage/pages/MemberPage/slice/thunks';
import { selectIsOrgOwner } from 'app/pages/MainPage/slice/selectors';
import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { FC, memo, useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE } from 'styles/StyleConstants';
import { AuthenticationModeType, RowPermissionByType } from './slice/constants';
import { ShareDetail } from './slice/type';

const ShareLinkModal: FC<{
  orgId: string;
  vizType: string;
  visibility: boolean;
  shareData?: ShareDetail | null;
  onOk?;
  onCancel?;
}> = memo(({ visibility, onOk, onCancel, shareData, orgId }) => {
  useMemberSlice();

  const t = useI18NPrefix(`viz.action`);
  const dispatch = useDispatch();
  const [expiryDate, setExpiryDate] = useState<string | Date>('');
  const [authenticationMode, setAuthenticationMode] = useState(
    AuthenticationModeType.none,
  );
  const [rowPermissionBy, setRowPermissionBy] = useState('');
  const [selectUsers, setSelectUsers] = useState<string[] | null>([]);
  const [selectRoles, setSelectRoles] = useState<string[] | null>([]);
  const [btnLoading, setBtnLoading] = useState<boolean>(false);
  const usersList = useSelector(selectMembers);
  const rolesList = useSelector(rdxSelectRoles);
  const isOwner = useSelector(selectIsOrgOwner);

  const handleOkFn = useCallback(
    async ({
      expiryDate,
      authenticationMode,
      roles,
      users,
      rowPermissionBy,
    }) => {
      setBtnLoading(true);

      try {
        let paramsData = {
          expiryDate,
          authenticationMode,
          roles,
          users,
          rowPermissionBy,
        };
        if (shareData) {
          paramsData = Object.assign({}, shareData, paramsData);
        }

        await onOk(paramsData);
        setBtnLoading(false);
      } catch (err) {
        setBtnLoading(false);
        throw err;
      }
    },
    [onOk, shareData],
  );

  const handleAuthenticationMode = useCallback(async e => {
    const value = e.target.value;

    setSelectRoles([]);
    setSelectUsers([]);
    setRowPermissionBy('');

    if (value === AuthenticationModeType.login) {
      setRowPermissionBy(RowPermissionByType.visitor);
    }

    setAuthenticationMode(e.target.value);
  }, []);

  const handleRowPermissionBy = useCallback(e => {
    setRowPermissionBy(e.target.value);
  }, []);

  const handleChangeMembers = useCallback(users => {
    setSelectUsers(users);
  }, []);

  const handleChangeRoles = useCallback(roles => {
    setSelectRoles(roles);
  }, []);

  const handleDefauleValue = useCallback((shareData: ShareDetail) => {
    setExpiryDate(shareData.expiryDate);
    setAuthenticationMode(shareData.authenticationMode);
    setRowPermissionBy(shareData.rowPermissionBy);
    setSelectUsers(shareData.users);
    setSelectRoles(shareData.roles);
  }, []);

  useEffect(() => {
    if (isOwner) {
      dispatch(getRoles(orgId));
      dispatch(getMembers(orgId));
    }
  }, [orgId, dispatch, isOwner]);

  useEffect(() => {
    shareData && handleDefauleValue(shareData);
  }, [handleDefauleValue, shareData]);

  return (
    <StyledShareLinkModal
      title={t('share.shareLink')}
      visible={visibility}
      okText={shareData ? t('share.save') : t('share.generateLink')}
      onOk={() =>
        handleOkFn?.({
          expiryDate,
          authenticationMode,
          roles: selectRoles,
          users: selectUsers,
          rowPermissionBy,
        })
      }
      okButtonProps={{ loading: btnLoading }}
      onCancel={onCancel}
      destroyOnClose
      forceRender
    >
      <Form
        preserve={false}
        labelCol={{ span: 8 }}
        wrapperCol={{ span: 16 }}
        autoComplete="off"
      >
        <FormItemEx label={t('share.expireDate')}>
          <DatePicker
            value={expiryDate ? moment(expiryDate, TIME_FORMATTER) : null}
            showTime
            disabledDate={current => {
              return current && current < moment().endOf('day');
            }}
            onChange={(_, dateString) => {
              setExpiryDate(dateString);
            }}
          />
        </FormItemEx>
        <FormItemEx label={t('share.verificationMethod')}>
          <Radio.Group
            onChange={handleAuthenticationMode}
            value={authenticationMode}
          >
            <Radio value={AuthenticationModeType.none}>{t('share.NONE')}</Radio>
            <Radio value={AuthenticationModeType.code}>{t('share.CODE')}</Radio>
            <Radio value={AuthenticationModeType.login}>
              {t('share.LOGIN')}
            </Radio>
          </Radio.Group>
        </FormItemEx>
        {authenticationMode === AuthenticationModeType.login && (
          <>
            <FormItemEx label={t('share.dataPermission')}>
              <Radio.Group
                onChange={handleRowPermissionBy}
                value={rowPermissionBy}
              >
                <Radio value={RowPermissionByType.visitor}>
                  {t('share.loginUser')}
                </Radio>
                <Radio value={RowPermissionByType.creator}>
                  {t('share.shareUser')}
                </Radio>
              </Radio.Group>
            </FormItemEx>
            {isOwner && (
              <FormItemEx label={t('share.userOrRole')}>
                <Space>
                  <StyledSelection
                    onChange={handleChangeRoles}
                    placeholder={t('share.selectRole')}
                    mode="multiple"
                    maxTagCount="responsive"
                    optionFilterProp="label"
                    value={selectRoles || []}
                  >
                    {rolesList?.map((v, i) => {
                      return (
                        <Select.Option key={i} value={v.id} label={v.name}>
                          {v.name}
                        </Select.Option>
                      );
                    })}
                  </StyledSelection>
                  <StyledSelection
                    onChange={handleChangeMembers}
                    placeholder={t('share.selectUser')}
                    mode="multiple"
                    maxTagCount="responsive"
                    optionFilterProp="label"
                    value={selectUsers || []}
                  >
                    {usersList?.map((v, i) => {
                      return (
                        <Select.Option key={i} value={v.id} label={v.username}>
                          {v.username}
                        </Select.Option>
                      );
                    })}
                  </StyledSelection>
                </Space>
              </FormItemEx>
            )}
          </>
        )}
      </Form>
    </StyledShareLinkModal>
  );
});

export default ShareLinkModal;

const StyledShareLinkModal = styled(Modal)`
  & .ant-modal-body .ant-row {
    margin-top: ${SPACE};
    margin-bottom: ${SPACE};
  }
`;

const StyledSelection = styled(Select)`
  min-width: 100px;
`;
