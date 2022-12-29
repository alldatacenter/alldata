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
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { Button, Col, Input, Row, Table } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { User } from 'app/slice/types';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import debounce from 'lodash/debounce';
import { Key, memo, useCallback, useMemo, useState } from 'react';
import styled from 'styled-components/macro';
import { SPACE } from 'styles/StyleConstants';

interface MemberTableProps {
  loading: boolean;
  dataSource: User[];
  onAdd: () => void;
  onChange: (members: User[]) => void;
}

export const MemberTable = memo(
  ({ loading, dataSource, onAdd, onChange }: MemberTableProps) => {
    const [keywords, setKeywords] = useState('');
    const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
    const t = useI18NPrefix('member.roleDetail');
    const tg = useI18NPrefix('global');

    const filteredSource = useMemo(
      () =>
        dataSource.filter(
          ({ username, email, name }) =>
            username.toLowerCase().includes(keywords) ||
            email.toLowerCase().includes(keywords) ||
            (name && name.toLowerCase().includes(keywords)),
        ),
      [dataSource, keywords],
    );

    const debouncedSearch = useMemo(() => {
      const search = e => {
        setKeywords(e.target.value);
      };
      return debounce(search, DEFAULT_DEBOUNCE_WAIT);
    }, []);

    const removeMember = useCallback(
      id => () => {
        onChange(dataSource.filter(d => d.id !== id));
        setSelectedRowKeys([]);
      },
      [dataSource, onChange],
    );

    const removeSelectedMember = useCallback(() => {
      onChange(dataSource.filter(d => !selectedRowKeys.includes(d.id)));
      setSelectedRowKeys([]);
    }, [dataSource, selectedRowKeys, onChange]);

    const columns = useMemo(
      () => [
        { dataIndex: 'username', title: t('username') },
        { dataIndex: 'email', title: t('email') },
        { dataIndex: 'name', title: t('name') },
        {
          title: tg('title.action'),
          width: 80,
          align: 'center' as const,
          render: (_, record) => (
            <Action onClick={removeMember(record.id)}>{t('remove')}</Action>
          ),
        },
      ],
      [removeMember, t, tg],
    );

    return (
      <>
        <Toolbar>
          <Col span={4}>
            <Button
              type="link"
              icon={<PlusOutlined />}
              className="btn"
              onClick={onAdd}
            >
              {t('addMember')}
            </Button>
          </Col>
          <Col span={14}>
            {selectedRowKeys.length > 0 && (
              <Button
                type="link"
                icon={<DeleteOutlined />}
                className="btn"
                onClick={removeSelectedMember}
              >
                {t('deleteAll')}
              </Button>
            )}
          </Col>
          <Col span={6}>
            <Input
              placeholder={t('searchMember')}
              prefix={<SearchOutlined className="icon" />}
              bordered={false}
              onChange={debouncedSearch}
            />
          </Col>
        </Toolbar>
        <Table
          rowKey="id"
          dataSource={filteredSource}
          columns={columns}
          loading={loading}
          size="small"
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          bordered
        />
      </>
    );
  },
);

const Toolbar = styled(Row)`
  .btn {
    padding: ${SPACE};
  }

  .icon {
    color: ${p => p.theme.textColorLight};
  }
`;

const Action = styled.a``;
