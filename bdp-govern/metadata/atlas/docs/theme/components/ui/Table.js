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

import { get } from "../../utils/theme";
import { mq } from "../../styles/responsive";

const Wrapper = styled.div`
  overflow-x: auto;
  padding: 2px;
  margin-bottom: 30px;

  ${mq({
    marginBottom: [20, 40],
    maxWidth: ["calc(100vw - 40px)", "calc(100vw - 80px)", "100%"]
  })};
`;

const TableStyled = styled.table`
  padding: 0;
  table-layout: auto;
  box-shadow: 0 0 0 1px ${get("colors.border")};
  background-color: transparent;
  border-spacing: 0;
  border-collapse: collapse;
  border-style: hidden;
  border-radius: ${get("radii")};
  font-size: 14px;
  color: ${get("colors.tableColor")};

  ${mq({
    overflowX: ["initial", "initial", "initial", "hidden"],
    display: ["table", "table", "table", "table"]
  })}

  & thead {
    color: ${get("colors.theadColor")};
    background: ${get("colors.theadBg")};
  }

  & thead th {
    font-weight: 400;
    padding: 10px;
    text-align: left;

    &:nth-of-type(1) {
      ${mq({
        width: ["20%", "20%", "20%", "auto"]
      })};
    }

    &:nth-of-type(2) {
      ${mq({
        width: ["10%", "10%", "10%", "auto"]
      })};
    }

    &:nth-of-type(3) {
      ${mq({
        width: ["10%", "10%", "10%", "auto"]
      })};
    }

    &:nth-of-type(4) {
      ${mq({
        width: ["10%", "10%", "10%", "auto"]
      })};
    }

    &:nth-of-type(5) {
      ${mq({
        width: ["20%", "20%", "20%", "auto"]
      })};
    }
  }

  & tbody td {
    padding: 10px;
    line-height: 2;
    font-weight: 200;
    text-align: left;
  }

  & tbody > tr {
    display: table-row;
    border-top: 1px solid ${get("colors.border")};
  }

  ${get("styles.table")};
`;

export const Table = props => (
  <Wrapper>
    <TableStyled {...props} />
  </Wrapper>
);
