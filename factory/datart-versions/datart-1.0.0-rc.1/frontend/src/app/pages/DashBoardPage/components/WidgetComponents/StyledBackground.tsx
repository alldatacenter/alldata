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
  BackgroundConfig,
  BorderConfig,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { getBackgroundImage } from 'app/pages/DashBoardPage/utils';
import styled from 'styled-components/macro';
export interface StyledBackgroundProps {
  bg: BackgroundConfig;
  bd?: BorderConfig;
}

const StyledBackground = styled.div<StyledBackgroundProps>`
  background-color: ${p => p.bg?.color};
  background-image: ${p => getBackgroundImage(p.bg?.image)};
  background-repeat: ${p => p.bg?.repeat};
  background-size: ${p => p.bg?.size};
  border-color: ${p => p?.bd?.color};
  border-style: ${p => p?.bd?.style};
  border-width: ${p => (p?.bd?.width || 0) + 'px'};
  border-radius: ${p => (p?.bd?.radius || 0) + 'px'};
`;
export default StyledBackground;
