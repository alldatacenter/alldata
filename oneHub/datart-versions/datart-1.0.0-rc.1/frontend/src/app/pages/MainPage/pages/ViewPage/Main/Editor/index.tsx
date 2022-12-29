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

import React, { memo } from 'react';
import styled from 'styled-components/macro';
import { SQLEditor } from './SQLEditor';
import { Toolbar } from './Toolbar';

interface EditorProps {
  allowManage: boolean;
  allowEnableViz: boolean | undefined;
}

export const Editor = memo(({ allowManage, allowEnableViz }: EditorProps) => {
  return (
    <Wrapper>
      <Toolbar
        type="SQL"
        allowManage={allowManage}
        allowEnableViz={allowEnableViz}
      />
      <SQLEditor />
    </Wrapper>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
`;
