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
import { memo } from 'react';
import styled from 'styled-components/macro';
import { LEVEL_50 } from 'styles/StyleConstants';
export const StyledWidgetToolBar: React.FC<{}> = memo(props => {
  return <StyleWrap>{props.children} </StyleWrap>;
});
const StyleWrap = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  z-index: ${LEVEL_50};
  overflow: hidden;
  text-align: right;
  .widget-tool-dropdown {
    visibility: hidden;
  }
`;
