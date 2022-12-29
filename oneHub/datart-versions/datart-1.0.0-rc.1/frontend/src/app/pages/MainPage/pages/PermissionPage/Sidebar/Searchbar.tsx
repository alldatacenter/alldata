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

import { SearchOutlined } from '@ant-design/icons';
import { Col, Input, Row } from 'antd';
import styled from 'styled-components/macro';
import { SPACE, SPACE_MD, SPACE_UNIT } from 'styles/StyleConstants';

interface SearchbarProps {
  placeholder?: string;
  onSearch: (e) => void;
}

export function Searchbar({ placeholder, onSearch }: SearchbarProps) {
  return (
    <Wrapper className="search">
      <Col span={24}>
        <Input
          prefix={<SearchOutlined className="icon" />}
          placeholder={placeholder}
          className="search-input"
          bordered={false}
          onChange={onSearch}
        />
      </Col>
    </Wrapper>
  );
}

const Wrapper = styled(Row)`
  padding: ${SPACE} 0;

  .icon {
    color: ${p => p.theme.textColorLight};
  }

  .search-input {
    padding-left: ${SPACE_MD};
  }

  .ant-tree .ant-tree-treenode {
    padding-left: ${SPACE_UNIT * 2 + 2}px;
  }
`;
