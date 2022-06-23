/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from "react";
import styled from "styled-components";

import { Sidebar, Main } from "../shared";
import { get } from "../../utils/theme";

const Wrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  color: ${get("colors.text")};
  background: ${get("colors.background")};
`;

const Title = styled.h1`
  margin: 0;
  font-size: 42px;
  font-weight: 400;
  color: ${get("colors.primary")};
`;

const Subtitle = styled.p`
  margin: 0;
  font-size: 18px;
`;

export const NotFound = () => (
  <Main>
    <Sidebar />
    <Wrapper>
      <Title>Page Not Found</Title>
      <Subtitle>
        Check if you changed the document route or deleted it!
      </Subtitle>
    </Wrapper>
  </Main>
);