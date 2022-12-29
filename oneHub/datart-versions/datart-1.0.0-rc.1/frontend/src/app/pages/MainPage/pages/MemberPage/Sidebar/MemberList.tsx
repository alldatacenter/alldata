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

import { LoadingOutlined, UserAddOutlined } from '@ant-design/icons';
import { List, Modal } from 'antd';
import { Avatar, ListItem, ListTitle } from 'app/components';
import { TenantManagementMode } from 'app/constants';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectSystemInfo } from 'app/slice/selectors';
import {
  memo,
  ReactElement,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { SPACE_MD } from 'styles/StyleConstants';
import { selectOrgId } from '../../../slice/selectors';
import { InviteForm } from '../InviteForm';
import {
  selectInviteMemberLoading,
  selectMemberListLoading,
  selectMembers,
} from '../slice/selectors';
import { getMembers, inviteMember } from '../slice/thunks';

export const MemberList = memo(() => {
  const [inviteFormVisible, setInviteFormVisible] = useState(false);
  const dispatch = useDispatch();
  const history = useHistory();
  const systemInfo = useSelector(selectSystemInfo);
  const orgId = useSelector(selectOrgId);
  const list = useSelector(selectMembers);
  const listLoading = useSelector(selectMemberListLoading);
  const inviteLoading = useSelector(selectInviteMemberLoading);
  const matchRoleDetail = useRouteMatch<{ memberId: string }>(
    '/organizations/:orgId/members/:memberId',
  );
  const memberId = matchRoleDetail?.params.memberId;
  const t = useI18NPrefix('member.sidebar');

  const { filteredData, debouncedSearch } = useDebouncedSearch(
    list,
    (keywords, d) => {
      const name = d.name || '';
      return (
        name.toLowerCase().includes(keywords.toLowerCase()) ||
        d.username.toLowerCase().includes(keywords.toLowerCase()) ||
        d.email.toLowerCase().includes(keywords.toLowerCase())
      );
    },
  );

  useEffect(() => {
    dispatch(getMembers(orgId));
  }, [dispatch, orgId]);

  const showInviteForm = useCallback(() => {
    setInviteFormVisible(true);
  }, []);

  const hideInviteForm = useCallback(() => {
    setInviteFormVisible(false);
  }, []);

  const invite = useCallback(
    values => {
      dispatch(
        inviteMember({
          params: { ...values, orgId: orgId },
          resolve: ({ success, fail }) => {
            let title: string[] = [];
            let content: ReactElement[] = [];

            if (values.sendMail) {
              if (success.length > 0) {
                title.push(t('inviteSuccess'));
              }
            } else {
              if (success.length > 0) {
                dispatch(getMembers(orgId));
              }
            }

            if (fail.length > 0) {
              title.push(t('invalidEmail'));
              fail.forEach(e => {
                content.push(<p>{e}</p>);
              });
            }

            if (title.length > 0) {
              Modal.info({
                title: title.join('；'),
                content: <Emails>{content}</Emails>,
                maskClosable: false,
              });
            }
            setInviteFormVisible(false);
          },
        }),
      );
    },
    [dispatch, orgId, t],
  );

  const toDetail = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/members/${id}`);
    },
    [history, orgId],
  );

  const toAdd = useCallback(() => {
    toDetail('add')();
  }, [toDetail]);

  const titleProps = useMemo(
    () => ({
      key: 'list',
      subTitle: t('memberTitle'),
      search: true,
      add:
        systemInfo?.tenantManagementMode === TenantManagementMode.Platform
          ? {
              items: [{ key: 'invite', text: t('inviteMember') }],
              icon: <UserAddOutlined />,
              callback: showInviteForm,
            }
          : {
              items: [{ key: 'add', text: t('addMember') }],
              icon: <UserAddOutlined />,
              callback: toAdd,
            },
      onSearch: debouncedSearch,
    }),
    [systemInfo, showInviteForm, toAdd, debouncedSearch, t],
  );
  return (
    <Wrapper>
      <ListTitle {...titleProps} />
      <ListWrapper>
        <List
          dataSource={filteredData}
          loading={listLoading && { indicator: <LoadingOutlined /> }}
          renderItem={({ id, name, email, username, avatar }) => (
            <ListItem
              selected={memberId === id}
              withAvatar
              onClick={toDetail(id)}
            >
              <List.Item.Meta
                avatar={
                  <Avatar src={avatar} size="large">
                    {username.substr(0, 1).toUpperCase()}
                  </Avatar>
                }
                title={`${name || username}`}
                description={email}
              />
            </ListItem>
          )}
        />
      </ListWrapper>
      <InviteForm
        title={t('inviteMember')}
        visible={inviteFormVisible}
        confirmLoading={inviteLoading}
        onSave={invite}
        onCancel={hideInviteForm}
        maskClosable={false}
      />
    </Wrapper>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
`;

const ListWrapper = styled.div`
  flex: 1;
  overflow-y: auto;
`;

const Emails = styled.code`
  padding: ${SPACE_MD};
`;
