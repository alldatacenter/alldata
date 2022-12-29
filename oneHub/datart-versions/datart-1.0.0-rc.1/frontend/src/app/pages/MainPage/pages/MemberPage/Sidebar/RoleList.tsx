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

import { LoadingOutlined, PlusOutlined } from '@ant-design/icons';
import { List } from 'antd';
import { ListItem, ListTitle } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo, useCallback, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { selectOrgId } from '../../../slice/selectors';
import { selectRoleListLoading, selectRoles } from '../slice/selectors';
import { getRoles } from '../slice/thunks';

export const RoleList = memo(() => {
  const dispatch = useDispatch();
  const history = useHistory();
  const list = useSelector(selectRoles);
  const listLoading = useSelector(selectRoleListLoading);
  const orgId = useSelector(selectOrgId);
  const matchRoleDetail = useRouteMatch<{ roleId: string }>(
    '/organizations/:orgId/roles/:roleId',
  );
  const roleId = matchRoleDetail?.params.roleId;
  const t = useI18NPrefix('member.sidebar');

  const { filteredData, debouncedSearch } = useDebouncedSearch(
    list,
    (keywords, d) => d.name.toLowerCase().includes(keywords.toLowerCase()),
  );

  useEffect(() => {
    dispatch(getRoles(orgId));
  }, [dispatch, orgId]);

  const toAdd = useCallback(() => {
    history.push(`/organizations/${orgId}/roles/add`);
  }, [history, orgId]);

  const toDetail = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/roles/${id}`);
    },
    [history, orgId],
  );

  const titleProps = useMemo(
    () => ({
      key: 'list',
      subTitle: t('roleTitle'),
      search: true,
      add: {
        items: [{ key: 'add', text: t('addRole') }],
        icon: <PlusOutlined />,
        callback: toAdd,
      },
      onSearch: debouncedSearch,
    }),
    [toAdd, debouncedSearch, t],
  );

  return (
    <Wrapper>
      <ListTitle {...titleProps} />
      <ListWrapper>
        <List
          dataSource={filteredData}
          loading={listLoading && { indicator: <LoadingOutlined /> }}
          renderItem={({ id, name, description }) => (
            <ListItem selected={roleId === id} onClick={toDetail(id)}>
              <List.Item.Meta title={name} description={description || '-'} />
            </ListItem>
          )}
        />
      </ListWrapper>
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
