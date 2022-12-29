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

import { Form, FormItemProps } from 'antd';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';

const FormItemEx: FC<FormItemProps> = memo(({ children, ...rest }) => {
  return <StyledFromItemEx {...rest}>{children}</StyledFromItemEx>;
});

export default FormItemEx;

const StyledFromItemEx = styled(Form.Item)`
  margin: 0 0 0 0;

  .ant-form-item-control {
    display: flex;
    flex-flow: row;
    align-items: center;
  }

  .ant-form-item-explain {
    padding-left: 10px;
  }

  .ant-form-item-control-input {
    width: 100%;
  }
`;
